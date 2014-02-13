package fr.liglab.mining.internals;

import fr.liglab.mining.internals.Dataset.TransactionsIterable;
import fr.liglab.mining.internals.Selector.WrongFirstParentException;
import fr.liglab.mining.io.FileFilteredReader;
import fr.liglab.mining.io.FileReader;
import fr.liglab.mining.io.PerItemTopKCollector;
import fr.liglab.mining.io.PerItemTopKCollector.PatternWithFreq;
import fr.liglab.mining.util.ItemsetsFactory;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Represents an LCM recursion step. Its also acts as a Dataset factory.
 */
public final class ExplorationStep implements Cloneable {

	public static boolean verbose = false;
	public static boolean ultraVerbose = false;
	
	public final static String KEY_VIEW_SUPPORT_THRESHOLD = "toplcm.threshold.view";
	public final static String KEY_LONG_TRANSACTIONS_THRESHOLD = "toplcm.threshold.long";
	
	/**
	 * @see longTransactionsMode
	 */
	static int LONG_TRANSACTION_MODE_THRESHOLD = Integer.parseInt(
			System.getProperty(KEY_LONG_TRANSACTIONS_THRESHOLD, "2000"));

	/**
	 * When projecting on a item having a support count above
	 * VIEW_SUPPORT_THRESHOLD%, projection will be a DatasetView
	 */
	static double VIEW_SUPPORT_THRESHOLD = Double.parseDouble(
			System.getProperty(KEY_VIEW_SUPPORT_THRESHOLD, "0.15"));

	/**
	 * When set to true we stick to a complete LCMv2 implementation, with predictive 
	 * prefix-preservation tests and compressions at all steps.
	 * Setting this to false is better when mining top-k-per-item patterns.
	 */
	public static boolean LCM_STYLE = false;

	/**
	 * closure of parent's pattern UNION extension - using original item IDs
	 */
	public final int[] pattern;

	/**
	 * Extension item that led to this recursion step. Already included in
	 * "pattern".
	 */
	public final int core_item;

	public final Dataset dataset;

	public final Counters counters;

	/**
	 * Selectors chain - may be null when empty
	 */
	protected Selector selectChain;

	protected final FrequentsIterator candidates;

	/**
	 * When an extension fails first-parent test, it ends up in this map. Keys
	 * are non-first-parent items associated to their actual first parent.
	 */
	private final TIntIntHashMap failedFPTests;
	
	private final Queue<PatternWithFreq> upcomingExtensions = new ConcurrentLinkedQueue<PatternWithFreq>();
	private boolean preliminaryEnumerationTodo = true;

	/**
	 * Start exploration on a dataset contained in a file.
	 * 
	 * @param minimumSupport
	 * @param path
	 *            to an input file in ASCII format. Each line should be a
	 *            transaction containing space-separated item IDs.
	 */
	public ExplorationStep(int minimumSupport, String path) {
		this.core_item = Integer.MAX_VALUE;
		this.selectChain = null;

		FileReader reader = new FileReader(path);
		this.counters = new Counters(minimumSupport, reader);
		reader.close(this.counters.renaming);

		this.pattern = this.counters.closure;

		this.dataset = new Dataset(this.counters, reader);

		this.candidates = this.counters.getExtensionsIterator();

		this.failedFPTests = new TIntIntHashMap();
	}

	public ExplorationStep(int minimumSupport, FileFilteredReader reader, int maxItem, 
			int[] reverseGlobalRenaming) {
		this.core_item = Integer.MAX_VALUE;
		this.selectChain = null;

		this.counters = new Counters(minimumSupport, reader, maxItem+1, null, maxItem+1);
		
		int[] renaming = this.counters.compressRenaming(reverseGlobalRenaming);
		reader.close(renaming);
		
		this.dataset = new Dataset(this.counters, reader);
		
		this.pattern = this.counters.closure;
		if (this.pattern.length > 0) {
			for (int i = 0; i < this.pattern.length; i++) {
				this.pattern[i] = reverseGlobalRenaming[this.pattern[i]];
			}
		}
		
		this.candidates = this.counters.getExtensionsIterator();
		this.failedFPTests = new TIntIntHashMap();
	}
	
	/**
	 * Start exploration on one of Hadoop's sub-dataset
	 * @param minimumSupport
	 * @param transactions
	 * @param maxItem
	 * @param reverseRenaming
	 */
	public ExplorationStep(int minimumSupport, Iterable<TransactionReader> transactions,
			int maxItem, int[] reverseRenaming) {
		this.core_item = Integer.MAX_VALUE;
		this.selectChain = null;
		
		this.counters = new Counters(minimumSupport, transactions.iterator(), 
				maxItem+1, null, maxItem+1);
		
		Iterator<TransactionReader> trans = transactions.iterator();
		
		int[] renaming = this.counters.compressRenaming(reverseRenaming);
		trans = new TransactionsRenamingDecorator(trans, renaming);

		this.dataset = new Dataset(this.counters, trans);
		// FIXME
		// from here we actually instantiated 3 times the dataset's size
		// once in dataset.transactions, one in dataset.tidLists (both are OK) and 
		// worse, once again in transactions.cached
		
		this.pattern = this.counters.closure;
		if (this.pattern.length > 0) {
			for (int i = 0; i < this.pattern.length; i++) {
				this.pattern[i] = reverseRenaming[this.pattern[i]];
			}
		}
		
		this.candidates = this.counters.getExtensionsIterator();
		this.failedFPTests = new TIntIntHashMap();
	}

	private ExplorationStep(int[] pattern, int core_item, Dataset dataset, Counters counters, Selector selectChain,
			FrequentsIterator candidates, TIntIntHashMap failedFPTests) {
		super();
		this.pattern = pattern;
		this.core_item = core_item;
		this.dataset = dataset;
		this.counters = counters;
		this.selectChain = selectChain;
		this.candidates = candidates;
		this.failedFPTests = failedFPTests;
	}

	/**
	 * Finds an extension for current pattern in current dataset and returns the
	 * corresponding ExplorationStep (extensions are enumerated by ascending
	 * item IDs - in internal rebasing) Returns null when all valid extensions
	 * have been generated
	 * If it has not been done before, this method will perform the preliminary 
	 * breadth-first exploration
	 */
	public ExplorationStep next(PerItemTopKCollector collector) {
		if (preliminaryEnumerationTodo) {
			// fill this.upcomingExtensions !
			this.preliminaryExploration(collector);
			preliminaryEnumerationTodo = false;
		}
		
		return this.instantiateNextStep(collector);
	}
	
	private void preliminaryExploration(PerItemTopKCollector collector) {
		while (true) {
			int candidate = this.candidates.next();

			if (candidate < 0) {
				return;
			}

			try {
				if (this.selectChain == null || this.selectChain.select(candidate, this)) {
					int support = this.counters.supportCounts[candidate];
					PatternWithFreq tmp = collector.preCollect(support, this.pattern, 
							this.counters.reverseRenaming[candidate]);
					this.upcomingExtensions.add(tmp);
				}
			} catch (WrongFirstParentException e) {
				addFailedFPTest(e.extension, e.firstParent);
			}
		}
	}
	
	private ExplorationStep instantiateNextStep(PerItemTopKCollector collector) {
		while (true) {
			PatternWithFreq holder = this.upcomingExtensions.poll();
			
			if (holder == null) {
				return null;
			}
			
			if (holder.isStillInTopKs()) {
				final int candidate = holder.extension;
				TransactionsIterable support = this.dataset.getSupport(candidate);

				Counters candidateCounts = new Counters(this.counters.minSupport, support.iterator(), 
						candidate, this.dataset.getIgnoredItems(), this.counters.maxFrequent);

				// all candidates have been FP-tested by the preliminary pass
				
				// the upcoming instanciateDataset may choose to compress renaming - if
				// not, at least it's set for now.
				candidateCounts.reuseRenaming(this.counters.reverseRenaming);

				ExplorationStep next = new ExplorationStep(this, candidate, candidateCounts, support);
				
				// now let's collect the right pattern
				holder.setPattern(next.pattern);
				int insertionsCounter = 0;
				for (int item: candidateCounts.closure) {
					if (collector.insertPatternInTop(holder, this.counters.reverseRenaming[item])) {
						insertionsCounter++;
					}
				}
				if (insertionsCounter > 0) {
					holder.incrementRefCount(insertionsCounter);
				}
				
				return next;
			}
		}
	}

	/**
	 * Instantiate state for a valid extension.
	 * 
	 * @param parent
	 * @param extension
	 *            a first-parent extension from parent step
	 * @param candidateCounts
	 *            extension's counters from parent step
	 * @param support
	 *            previously-computed extension's support
	 */
	protected ExplorationStep(ExplorationStep parent, int extension, Counters candidateCounts,
			TransactionsIterable support) {

		this.core_item = extension;
		this.counters = candidateCounts;
		int[] reverseRenaming = parent.counters.reverseRenaming;

		if (verbose) {
			if (parent.pattern.length == 0 || ultraVerbose) {
				System.err.format("{\"time\":\"%1$tY/%1$tm/%1$td %1$tk:%1$tM:%1$tS\",\"thread\":%2$d,\"pattern\":%3$s,\"extension_internal\":%4$d,\"extension\":%5$d}\n",
						Calendar.getInstance(), Thread.currentThread().getId(), Arrays.toString(parent.pattern),
						extension, reverseRenaming[extension]);
			}
		}

		this.pattern = ItemsetsFactory
				.extendRename(candidateCounts.closure, extension, parent.pattern, reverseRenaming);

		if (this.counters.nbFrequents == 0 || this.counters.distinctTransactionsCount == 0) {
			this.candidates = null;
			this.failedFPTests = null;
			this.selectChain = null;
			this.dataset = null;
		} else {
			this.failedFPTests = new TIntIntHashMap();

			if (parent.selectChain == null) {
				this.selectChain = null;
			} else {
				this.selectChain = parent.selectChain.copy();
			}
			
			this.dataset = instanciateDataset(parent, support);
			this.candidates = this.counters.getExtensionsIterator();

		}
	}

	private Dataset instanciateDataset(ExplorationStep parent, TransactionsIterable support) {
		final int[] renaming = this.counters.compressRenaming(parent.counters.getReverseRenaming());

		TransactionsRenamingDecorator filtered = new TransactionsRenamingDecorator(support.iterator(), renaming);

		try {
			Dataset dataset = new Dataset(this.counters, filtered, Integer.MAX_VALUE); // TODO the last argument is now obsolete
			if (parent.pattern.length == 0) {
				dataset.compress(this.core_item); // FIXME FIXME core_item refers an UNCOMPRESSED id
			}

			return dataset;
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("WAT core_item = "+this.core_item);
			e.printStackTrace();
			System.exit(1);
		}
		
		return null;
	}

	public int getFailedFPTest(final int item) {
		synchronized (this.failedFPTests) {
			return this.failedFPTests.get(item);
		}
	}

	private void addFailedFPTest(final int item, final int firstParent) {
		synchronized (this.failedFPTests) {
			this.failedFPTests.put(item, firstParent);
		}
	}

	public void appendSelector(Selector s) {
		if (this.selectChain == null) {
			this.selectChain = s;
		} else {
			this.selectChain = this.selectChain.append(s);
		}
	}

	public int getCatchedWrongFirstParentCount() {
		if (this.failedFPTests == null) {
			return 0;
		} else {
			return this.failedFPTests.size();
		}
	}

	public ExplorationStep copy() {
		return new ExplorationStep(pattern, core_item, dataset.clone(), counters.clone(), selectChain, candidates,
				failedFPTests);
	}
	
	public Progress getProgression() {
		return new Progress();
	}
	
	public final class Progress {
		public final int current;
		public final int last;
		
		protected Progress() {
			this.current = candidates.peek();
			this.last = candidates.last();
		}
	}
}
