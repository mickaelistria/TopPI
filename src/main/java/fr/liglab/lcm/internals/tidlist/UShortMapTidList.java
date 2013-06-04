package fr.liglab.lcm.internals.tidlist;

import fr.liglab.lcm.internals.Counters;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.iterator.TShortIterator;
import gnu.trove.list.TShortList;
import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class UShortMapTidList extends TidList {

	@SuppressWarnings("cast")
	public static boolean compatible(int maxTid) {
		return maxTid <= ((int) Short.MAX_VALUE) - ((int) Short.MIN_VALUE);
	}

	private TIntObjectMap<TShortList> occurrences = new TIntObjectHashMap<TShortList>();

	public UShortMapTidList(Counters c) {
		this(c.distinctTransactionsCounts);
	}

	public UShortMapTidList(final int[] lengths) {
		for (int i = 0; i < lengths.length; i++) {
			if (lengths[i] > 0) {
				this.occurrences.put(i, new TShortArrayList(lengths[i]));
			}
		}
	}

	@Override
	public TidList clone() {
		UShortMapTidList o = (UShortMapTidList) super.clone();
		o.occurrences = new TIntObjectHashMap<TShortList>(this.occurrences.size());
		TIntObjectIterator<TShortList> iter = this.occurrences.iterator();
		while (iter.hasNext()) {
			iter.advance();
			o.occurrences.put(iter.key(), new TShortArrayList(iter.value()));
		}
		return o;
	}

	@Override
	public TIntIterator get(final int item) {
		final TShortList l = this.occurrences.get(item);
		if (l == null) {
			throw new IllegalArgumentException("item " + item + " has no tidlist");
		} else {
			final TShortIterator iter = l.iterator();
			return new TIntIterator() {

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				@Override
				public boolean hasNext() {
					return iter.hasNext();
				}

				@Override
				public int next() {
					int v = iter.next();
					if (v >= 0) {
						return v;
					} else {
						return -v + Short.MAX_VALUE;
					}
				}
			};
		}
	}

	@Override
	public TIntIterable getIterable(int item) {
		final TShortList l = this.occurrences.get(item);
		if (l == null) {
			throw new IllegalArgumentException("item " + item + " has no tidlist");
		} else {
			return new TIntIterable() {

				@Override
				public TIntIterator iterator() {
					final TShortIterator iter = l.iterator();
					return new TIntIterator() {

						@Override
						public void remove() {
							throw new UnsupportedOperationException();
						}

						@Override
						public boolean hasNext() {
							return iter.hasNext();
						}

						@Override
						public int next() {
							int v = iter.next();
							if (v >= 0) {
								return v;
							} else {
								return -v + Short.MAX_VALUE;
							}
						}
					};
				}
			};
		}
	}

	@Override
	public void addTransaction(final int item, int transaction) {
		if (transaction > Short.MAX_VALUE) {
			transaction = -transaction + Short.MAX_VALUE;
			if (transaction < Short.MIN_VALUE) {
				throw new IllegalArgumentException(transaction + " too big for a short");
			}
		}
		TShortList l = this.occurrences.get(item);
		if (l == null) {
			throw new IllegalArgumentException("item " + item + " has no tidlist");
		}
		l.add((short) transaction);
	}

}
