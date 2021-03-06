/*
	This file is part of TopPI - see https://github.com/slide-lig/TopPI/
	
	Copyright 2016 Martin Kirchgessner, Vincent Leroy, Alexandre Termier, Sihem Amer-Yahia, Marie-Christine Rousset, Université Grenoble Alpes, LIG, CNRS
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	 http://www.apache.org/licenses/LICENSE-2.0
	 
	or see the LICENSE.txt file joined with this program.
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
package fr.liglab.mining.internals;

import fr.liglab.mining.CountersHandler;
import fr.liglab.mining.CountersHandler.TopPICounters;
import fr.liglab.mining.internals.Dataset.TransactionsIterable;
import fr.liglab.mining.internals.Selector.WrongFirstParentException;
import fr.liglab.mining.io.FileFilteredReader;
import fr.liglab.mining.io.FileReader;
import fr.liglab.mining.io.FileWithStringIDsReader;
import fr.liglab.mining.io.PerItemTopKCollector;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;

import javax.xml.ws.Holder;

import org.omg.CORBA.IntHolder;

/**
 * Represents a recursion step. Its also acts as a Dataset factory.
 */
public final class ExplorationStep implements Cloneable {

	public static boolean verbose = false;
	public static boolean ultraVerbose = false;
	public static boolean LOG_EPSILONS = false;

	// expressed in starter items base
	public static int INSERT_UNCLOSED_UP_TO_ITEM = Integer.MAX_VALUE;
	@SuppressWarnings("boxing")
	public static int USE_SPARSE_COUNTERS_FROM_ITEM = Integer.valueOf(System.getProperty("toppi.sparse.from",
			Integer.toString(Integer.MAX_VALUE)));
	public static boolean INSERT_UNCLOSED_FOR_FUTURE_EXTENSIONS = false;
	public static boolean EARLYCOLLECTION = true;

	public final static String KEY_VIEW_SUPPORT_THRESHOLD = "toppi.threshold.view";
	public final static String KEY_LONG_TRANSACTIONS_THRESHOLD = "toppi.threshold.long";

	/**
	 * @see longTransactionsMode
	 */
	static int LONG_TRANSACTION_MODE_THRESHOLD = Integer.parseInt(System.getProperty(KEY_LONG_TRANSACTIONS_THRESHOLD,
			"2000"));

	/**
	 * When projecting on a item having a support count above
	 * VIEW_SUPPORT_THRESHOLD%, projection will be a DatasetView
	 */
	static double VIEW_SUPPORT_THRESHOLD = Double.parseDouble(System.getProperty(KEY_VIEW_SUPPORT_THRESHOLD, "0.15"));

	public static boolean COMPRESS_LVL1 = false;

	/**
	 * Extension item that led to this recursion step. Already included in
	 * "pattern".
	 */
	public final int core_item;

	public Dataset dataset;

	public DatasetProvider datasetProvider;

	public final Counters counters;

	/**
	 * Selectors chain - may be null when empty
	 */
	protected Selector selectChain;

	public final FrequentsIterator candidates;

	/**
	 * When an extension fails first-parent test, it ends up in this map. Keys
	 * are non-first-parent items associated to their actual first parent.
	 */
	private final TIntIntHashMap failedFPTests;

	public ExplorationStep(int minimumSupport, String path, int k) {
		this(minimumSupport, path, k, null);
	}
	
	/**
	 * Start exploration on a dataset contained in a file.
	 * 
	 * @param minimumSupport
	 * @param path
	 *            to an input file in ASCII format. Each line should be a
	 *            transaction containing space-separated item IDs.
	 * @param k
	 * @param itemIDmap set to null if your file already uses integer item IDs 
	 */
	public ExplorationStep(int minimumSupport, String path, int k, Holder<Map<String,Integer>> itemIDmap) {
		this.core_item = Integer.MAX_VALUE;
		this.selectChain = null;
		
		Iterator<TransactionReader> reader;
		
		if (itemIDmap == null) {
			reader = new FileReader(path);
		} else {
			reader = new FileWithStringIDsReader(path);
		}
		
		Holder<int[]> renamingHolder = new Holder<int[]>();
		
		DenseCounters firstCounters = new DenseCounters(minimumSupport, reader, renamingHolder);
		this.counters = firstCounters;
		
		if (itemIDmap == null) {
			((FileReader) reader).close(renamingHolder.value);
		} else {
			itemIDmap.value = ((FileWithStringIDsReader) reader).close();
			reader = new FileWithStringIDsReader(path, itemIDmap.value);
			reader = new TransactionsRenamingDecorator(reader, renamingHolder.value);
		}
		
		Dataset dataset = new Dataset(this.counters, reader, this.counters.getMinSupport(),
				this.counters.getMaxFrequent());
		this.dataset = dataset;
		this.candidates = this.counters.getExtensionsIterator();
		this.failedFPTests = new TIntIntHashMap();
		this.datasetProvider = new DatasetProvider(this);
		
		ExplorationStep.findUnclosedInsertionBound(firstCounters.getSupportCounts(), minimumSupport + k);
	}
	
	protected static void findUnclosedInsertionBound(int[] supportCounts, int supportBound) {
		int i = 0;
		
		while (i < supportCounts.length && supportCounts[i] >= supportBound) {
			i++;
		}
		
		INSERT_UNCLOSED_UP_TO_ITEM = i;
		//System.err.println("INSERT_UNCLOSED_UP_TO_ITEM = "+INSERT_UNCLOSED_UP_TO_ITEM);
	}
	
	/**
	 * Start exploration on an abstract dataset
	 */
	public ExplorationStep(int minimumSupport, int k, Iterable<TransactionReader> source) {
		this.core_item = Integer.MAX_VALUE;
		this.selectChain = null;
		Holder<int[]> renamingHolder = new Holder<int[]>();
		DenseCounters firstCounter = new DenseCounters(minimumSupport, source.iterator(), renamingHolder);
		this.counters = firstCounter;
		TransactionsRenamingDecorator filtered = new TransactionsRenamingDecorator(source.iterator(), renamingHolder.value);
		this.dataset = new Dataset(this.counters, filtered, this.counters.getMinSupport(),
				this.counters.getMaxFrequent());
		this.candidates = this.counters.getExtensionsIterator();
		this.failedFPTests = new TIntIntHashMap();
		
		this.datasetProvider = new DatasetProvider(this);
		ExplorationStep.findUnclosedInsertionBound(firstCounter.getSupportCounts(), minimumSupport + k);
	}
	
	/**
	 * Only for the Hadoop variant
	 */
	public ExplorationStep(int minimumSupport, FileFilteredReader reader, int maxItem, int[] reverseGlobalRenaming,
			Holder<int[]> renaming, int k) {
		this.core_item = Integer.MAX_VALUE;
		this.selectChain = null;

		DenseCounters firstCounters = new DenseCounters(minimumSupport, reader, maxItem + 1, null, maxItem + 1,
				reverseGlobalRenaming, new int[] {});
		this.counters = firstCounters;
		renaming.value = this.counters.compressRenaming(reverseGlobalRenaming);
		reader.close(renaming.value);

		this.dataset = new Dataset(this.counters, reader, this.counters.getMinSupport(), this.counters.getMaxFrequent());

		if (this.counters.getPattern().length > 0) {
			for (int i = 0; i < this.counters.getPattern().length; i++) {
				this.counters.getPattern()[i] = reverseGlobalRenaming[this.counters.getPattern()[i]];
			}
		}

		this.candidates = this.counters.getExtensionsIterator();
		this.failedFPTests = new TIntIntHashMap();
		
		this.datasetProvider = new DatasetProvider(this);
		
		ExplorationStep.findUnclosedInsertionBound(firstCounters.getSupportCounts(), minimumSupport + k);
	}
	
	/**
	 * Only for the Hadoop variant
	 */
	public ExplorationStep(int minimumSupport, Iterable<TransactionReader> reader, int maxItem, int[] reverseGlobalRenaming,
			Holder<int[]> renaming, int k) {
		this.core_item = Integer.MAX_VALUE;
		this.selectChain = null;

		DenseCounters firstCounters = new DenseCounters(minimumSupport, reader.iterator(), maxItem + 1, null, maxItem + 1,
				reverseGlobalRenaming, new int[] {});
		this.counters = firstCounters;
		renaming.value = this.counters.compressRenaming(reverseGlobalRenaming);
		TransactionsRenamingDecorator filtered = new TransactionsRenamingDecorator(reader.iterator(), renaming.value);

		this.dataset = new Dataset(this.counters, filtered, this.counters.getMinSupport(), this.counters.getMaxFrequent());

		if (this.counters.getPattern().length > 0) {
			for (int i = 0; i < this.counters.getPattern().length; i++) {
				this.counters.getPattern()[i] = reverseGlobalRenaming[this.counters.getPattern()[i]];
			}
		}

		this.candidates = this.counters.getExtensionsIterator();
		this.failedFPTests = new TIntIntHashMap();
		
		this.datasetProvider = new DatasetProvider(this);
		
		ExplorationStep.findUnclosedInsertionBound(firstCounters.getSupportCounts(), minimumSupport + k);
	}
	
	private ExplorationStep(int core_item, Dataset dataset, Counters counters, Selector selectChain,
			FrequentsIterator candidates, TIntIntHashMap failedFPTests) {
		super();
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
	 * have been generated If it has not been done before, this method will
	 * perform the preliminary breadth-first exploration
	 */
	public ExplorationStep next(PerItemTopKCollector collector) {
		if (this.candidates == null) {
			return null;
		}

		while (true) {
			ExplorationStep res;

			int candidate = this.candidates.next();

			if (candidate < 0) {
				return null;
			} else {
				res = this.doDepthExplorationFromScratch(candidate, collector);
				if (LOG_EPSILONS) {
					synchronized (System.out) {
						if (res != null && res.counters != null && this.counters != null
								&& this.counters.getPattern() != null && this.counters.getPattern().length == 0) {
							System.out.println(candidate + " " + res.counters.getMinSupport());
						}
					}
				}
			}

			if (res != null) {
				return res;
			}
		}
	}

	/*
	 * No loop here, we need to see that we did the counters for all items, even
	 * if they fail with fptest for instance
	 */
	public Counters nextPreprocessed(PerItemTopKCollector collector, IntHolder candidateHolder, IntHolder boundHolder) {
		if (this.candidates == null) {
			candidateHolder.value = -1;
			return null;
		}
		int candidate = this.candidates.next();

		if (candidate < 0) {
			candidateHolder.value = -1;
			return null;
		} else {
			candidateHolder.value = candidate;
			return this.prepareExploration(candidate, collector, boundHolder);
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
	@SuppressWarnings("boxing")
	protected ExplorationStep(ExplorationStep parentEs, Dataset parentDataset, int extension, Counters candidateCounts,
			TransactionsIterable support) {

		this.core_item = extension;
		this.counters = candidateCounts;
		int[] reverseRenaming = parentEs.counters.getReverseRenaming();

		if (verbose) {
			if (parentEs.counters.getPattern().length == 0 || ultraVerbose) {
				System.err
						.format("{\"time\":\"%1$tY/%1$tm/%1$td %1$tk:%1$tM:%1$tS\",\"thread\":%2$d,\"pattern\":%3$s,\"extension_internal\":%4$d,\"extension\":%5$d}\n",
								Calendar.getInstance(), Thread.currentThread().getId(),
								Arrays.toString(parentEs.counters.getPattern()), extension, reverseRenaming[extension]);
			}
		}

		if (this.counters.getNbFrequents() == 0 || this.counters.getDistinctTransactionsCount() == 0) {
			this.candidates = null;
			this.failedFPTests = null;
			this.selectChain = null;
			this.dataset = null;
		} else {
			this.failedFPTests = new TIntIntHashMap();
			this.dataset = instanciateDatasetAndPickSelectors(parentEs, parentDataset, support);
			this.candidates = this.counters.getExtensionsIterator();
		}
	}

	private Dataset instanciateDatasetAndPickSelectors(ExplorationStep parentExplorationStep, Dataset parentDataset,
			TransactionsIterable support) {
		final double supportRate = this.counters.getDistinctTransactionsCount()
				/ (double) parentDataset.getStoredTransactionsCount();

		final int averageLen = this.counters.getDistinctTransactionLengthSum()
				/ this.counters.getDistinctTransactionsCount();

		if (averageLen < LONG_TRANSACTION_MODE_THRESHOLD && supportRate > VIEW_SUPPORT_THRESHOLD) {
			copySelectChainWithoutFPT(parentExplorationStep.selectChain);
			return new DatasetView(parentDataset, this.counters, support, this.core_item,
					this.counters.getMinSupport(), this.counters.getMaxFrequent());
		} else {
			if (averageLen > LONG_TRANSACTION_MODE_THRESHOLD) {
				copySelectChainWithFPT(parentExplorationStep.selectChain);
			} else {
				copySelectChainWithoutFPT(parentExplorationStep.selectChain);
			}

			final int[] renaming;
			renaming = this.counters.compressSortRenaming(null);
			TransactionsRenamingDecorator filtered = new TransactionsRenamingDecorator(support.iterator(), renaming);

			Dataset dataset = new Dataset(this.counters, filtered, Integer.MAX_VALUE, this.counters.getMinSupport(),
					this.counters.getMaxFrequent());

			return dataset;
		}
	}

	private void copySelectChainWithFPT(Selector chain) {
		if (chain == null) {
			this.selectChain = FirstParentTest.getTailInstance();
		} else if (chain.contains(FirstParentTest.class)) {
			this.selectChain = chain.copy();
		} else {
			this.selectChain = chain.append(FirstParentTest.getTailInstance());
		}
	}

	private void copySelectChainWithoutFPT(Selector chain) {
		if (chain == null) {
			this.selectChain = null;
		} else if (chain.contains(FirstParentTest.class)) {
			this.selectChain = chain.copy(null);
		} else {
			this.selectChain = chain.copy();
		}
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

		CountersHandler.increment(TopPICounters.FailedFPTests);
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
		return new ExplorationStep(core_item, dataset.clone(), counters.clone(), selectChain, candidates, failedFPTests);
	}

	protected Counters prepareExploration(int candidate, PerItemTopKCollector collector, IntHolder boundHolder) {
		return prepareExploration(candidate, collector, boundHolder, false);
	}

	protected Counters prepareExploration(int candidate, PerItemTopKCollector collector, IntHolder boundHolder,
			boolean regeneratedInResume) {
		try {
			if (ultraVerbose) {
				System.err.format("{\"time\":\"%1$tY/%1$tm/%1$td %1$tk:%1$tM:%1$tS\",\"thread\":%2$d,\"prepare_candidate\":%3$d}\n",
								Calendar.getInstance(), Thread.currentThread().getId(), candidate);
			}
			if (selectChain.select(candidate, ExplorationStep.this)) {
				Counters candidateCounts;
				boolean restart;
				boundHolder.value = Math.min(this.counters.getSupportCount(candidate) - collector.getK(),
						boundHolder.value);
				do {
					restart = false;
					Dataset suggestedDataset = this.datasetProvider.getDatasetForItem(candidate, boundHolder.value);
					TransactionsIterable support = suggestedDataset.getSupport(candidate);
					if ((this.counters.pattern == null || this.counters.pattern.length == 0)
							&& candidate >= USE_SPARSE_COUNTERS_FROM_ITEM) {
						candidateCounts = new SparseCounters(suggestedDataset.getMinSup(), support.iterator(),
								candidate, suggestedDataset.getIgnoredItems(), suggestedDataset.getMaxItem(),
								counters.getReverseRenaming(), counters.getPattern());
					} else {
						candidateCounts = new DenseCounters(suggestedDataset.getMinSup(), support.iterator(),
								candidate, suggestedDataset.getIgnoredItems(), suggestedDataset.getMaxItem(),
								counters.getReverseRenaming(), counters.getPattern());
					}

					int greatest = Integer.MIN_VALUE;
					for (int i = 0; i < candidateCounts.getClosure().length; i++) {
						if (candidateCounts.getClosure()[i] > greatest) {
							greatest = candidateCounts.getClosure()[i];
						}
					}

					if (greatest > candidate) {
						if (EARLYCOLLECTION && collector.isCollected(candidateCounts.getReverseRenaming()[greatest])) {
							collector.collect(candidateCounts.getTransactionsCount(), candidateCounts.getPattern());
						}
						throw new WrongFirstParentException(candidate, greatest);
					}
					if (!regeneratedInResume) {
						collector.collect(candidateCounts.getTransactionsCount(), candidateCounts.getPattern());
					}
					// this meanse that for candidate <
					// INSERT_UNCLOSED_UP_TO_ITEM we always use the dataset of
					// minimum support
					if (candidate < INSERT_UNCLOSED_UP_TO_ITEM) {
						boundHolder.value = candidateCounts.insertUnclosedPatterns(collector,
								INSERT_UNCLOSED_FOR_FUTURE_EXTENSIONS);
						if (boundHolder.value < suggestedDataset.getMinSup()
								&& suggestedDataset.getMinSup() > this.counters.getMinSupport()) {
							restart = true;
							// says let's switch to the next dataset
							CountersHandler.increment(TopPICounters.RedoCounters);
							boundHolder.value = suggestedDataset.getMinSup() - 1;
						}
					}
				} while (restart);
				// here we know that counters are ok for candidate, but not
				// necessarily for all items < candidate
				return candidateCounts;
			}
		} catch (WrongFirstParentException e) {
			addFailedFPTest(e.extension, e.firstParent);
		}
		return null;
	}

	public ExplorationStep resumeExploration(Counters candidateCounts, int candidate, PerItemTopKCollector collector,
			int countersMinSupportVerification) {
		if (ultraVerbose) {
			System.err.format("{\"time\":\"%1$tY/%1$tm/%1$td %1$tk:%1$tM:%1$tS\",\"thread\":%2$d,\"resume_candidate\":%3$d}\n",
							Calendar.getInstance(), Thread.currentThread().getId(), candidate);
		}
		
		// check that the counters we made are also ok for all items < candidate
		if (candidateCounts.getMinSupport() > countersMinSupportVerification &&
				candidateCounts.getMinSupport() > this.counters.getMinSupport()) {
			CountersHandler.increment(TopPICounters.RedoCounters);
			candidateCounts = prepareExploration(candidate, collector, new IntHolder(countersMinSupportVerification),
					true);
		}
		if (candidate < INSERT_UNCLOSED_UP_TO_ITEM) {
			candidateCounts.raiseMinimumSupport(collector);
			if (LOG_EPSILONS) {
				synchronized (System.out) {
					if (this.counters != null && this.counters.getPattern() != null
							&& this.counters.getPattern().length == 0) {
						System.out.println(candidate + " " + candidateCounts.getMinSupport());
					}
				}
			}
		}
		Dataset dataset = this.datasetProvider.getDatasetForSupportThreshold(candidateCounts.getMinSupport());
		ExplorationStep next = new ExplorationStep(this, dataset, candidate, candidateCounts,
				dataset.getSupport(candidate));
		return next;
	}

	protected ExplorationStep doDepthExplorationFromScratch(int candidate, PerItemTopKCollector collector) {
		try {
			if (selectChain.select(candidate, ExplorationStep.this)) {
				TransactionsIterable support = dataset.getSupport(candidate);
				Counters candidateCounts;
				if ((this.counters.pattern == null || this.counters.pattern.length == 0)
						&& candidate >= USE_SPARSE_COUNTERS_FROM_ITEM) {
					candidateCounts = new SparseCounters(counters.getMinSupport(), support.iterator(), candidate,
							dataset.getIgnoredItems(), counters.getMaxFrequent(), counters.getReverseRenaming(),
							counters.getPattern());
				} else {
					candidateCounts = new DenseCounters(counters.getMinSupport(), support.iterator(), candidate,
							dataset.getIgnoredItems(), counters.getMaxFrequent(), counters.getReverseRenaming(),
							counters.getPattern());
				}
				int greatest = Integer.MIN_VALUE;
				for (int i = 0; i < candidateCounts.getClosure().length; i++) {
					if (candidateCounts.getClosure()[i] > greatest) {
						greatest = candidateCounts.getClosure()[i];
					}
				}

				if (greatest > candidate) {
					if (EARLYCOLLECTION && collector.isCollected(candidateCounts.getReverseRenaming()[greatest])) {
						collector.collect(candidateCounts.getTransactionsCount(), candidateCounts.getPattern());
					}
					throw new WrongFirstParentException(candidate, greatest);
				}
				collector.collect(candidateCounts.getTransactionsCount(), candidateCounts.getPattern());
				// if we're here we're either not a starter or a starter that's
				// not likely to fill its topk
				// => no unclosed insertion, nor minsup raise
				ExplorationStep next = new ExplorationStep(this, this.dataset, candidate, candidateCounts, support);

				return next;
			}
		} catch (WrongFirstParentException e) {
			addFailedFPTest(e.extension, e.firstParent);
		}
		return null;
	}
}
