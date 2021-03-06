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
package fr.liglab.mining.internals.tidlist;

import fr.liglab.mining.internals.Counters;
import gnu.trove.impl.Constants;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public abstract class RandomItemsConcatenatedTidList extends TidList {

	private TIntIntMap startPositions;

	private TIntIntMap indexes;

	public RandomItemsConcatenatedTidList(Counters c) {
		int startPos = 0;
		this.startPositions = new TIntIntHashMap(c.getMaxFrequent() + 1, Constants.DEFAULT_LOAD_FACTOR, -1, -1);
		this.indexes = new TIntIntHashMap(c.getMaxFrequent() + 1);
		for (int i = 0; i < c.getMaxFrequent() + 1; i++) {
			this.startPositions.put(i, startPos);
			startPos += (1 + c.getDistinctTransactionsCount(i));
		}
		this.allocateArray(startPos);
	}

	abstract void allocateArray(int size);

	@Override
	public TidList clone() {
		RandomItemsConcatenatedTidList o = (RandomItemsConcatenatedTidList) super.clone();
		o.startPositions = new TIntIntHashMap(this.startPositions);
		o.indexes = new TIntIntHashMap(o.indexes);
		return o;
	}

	@Override
	public TIntIterator get(final int item) {
		final int startPos = this.startPositions.get(item);
		if (startPos == -1) {
			throw new IllegalArgumentException("item " + item + " has no tidlist");
		}
		final int length = this.indexes.get(item);
		return new TIntIterator() {
			int index = 0;

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean hasNext() {
				return this.index < length;
			}

			@Override
			public int next() {
				int res = read(startPos + index);
				this.index++;
				return res;
			}
		};
	}

	@Override
	public TIntIterable getIterable(final int item) {
		return new TIntIterable() {

			@Override
			public TIntIterator iterator() {
				return get(item);
			}
		};
	}

	@Override
	public void addTransaction(int item, int transaction) {
		final int startPos = this.startPositions.get(item);
		if (startPos == -1) {
			throw new IllegalArgumentException("item " + item + " has no tidlist");
		}
		int index = this.indexes.get(item);
		this.write(startPos + index, transaction);
		this.indexes.adjustOrPutValue(item, 1, 1);
	}

	abstract void write(int position, int transaction);

	abstract int read(int position);
}
