package fr.liglab.lcm.internals.nomaps;

import java.util.Iterator;

import fr.liglab.lcm.internals.TransactionReader;

/**
 * A dataset is a simple transaction database store, which *may* perform some
 * indexing for occurence delivery.
 * 
 * All actual implementations are package-visible - use DatasetFactory
 */
public abstract class Dataset {
	public final DatasetCountersRenamer counters;

	/**
	 * This constructor is only here to please the compiler
	 */
	protected Dataset(DatasetCountersRenamer counted) {
		this.counters = counted;
	}

	public abstract Iterator<TransactionReader> getSupport(int item);

	abstract Dataset project(int extension, DatasetCountersRenamer extensionCounters);

	/**
	 * Some lazy implementations may keep useless items in their transactions
	 * These will override this method so that such items will be ignored by
	 * DatasetCounters too
	 */
	int[] getItemsIgnoredForCounting() {
		return null;
	}

	public int ppTest(int extension) {
		return -1;
	}
}