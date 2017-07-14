/*
   Copyright 2009 IBM Corp

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package cue.lang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 
 * @author Jonathan Feinberg <jdf@us.ibm.com>
 * 
 */
public class Counter<T> {
	// delegate, don't extend, to prevent unauthorized monkeying with internals
	private final Map<T, Integer> items = new HashMap<T, Integer>();
	private int totalItemCount = 0;

	public Counter() {
	}

	public Counter(final Iterable<T> items) {
		noteAll(items);
	}

	public void noteAll(final Iterable<T> items) {
		for (final T t : items) {
			note(t, 1);
		}
	}

	public void note(final T item) {
		note(item, 1);
	}

	public void note(final T item, final int count) {
		final Integer existingCount = items.get(item);
		if (existingCount != null) {
			items.put(item, existingCount + count);
		} else {
			items.put(item, count);
		}
		totalItemCount += count;
	}

	public void merge(final Counter<T> c) {
		for (final Entry<T, Integer> e : c.items.entrySet()) {
			note(e.getKey(), e.getValue());
		}
	}

	public int getTotalItemCount() {
		return totalItemCount;
	}

	private final Comparator<Entry<T, Integer>> BY_FREQ_DESC = new Comparator<Entry<T, Integer>>() {
		public int compare(final Entry<T, Integer> o1,
				final Entry<T, Integer> o2) {
			return o2.getValue() - o1.getValue();
		}
	};

	/**
	 * @param n
	 * @return A list of the min(n, size()) most frequent items
	 */
	public List<T> getMostFrequent(final int n) {
		final List<Entry<T, Integer>> all = getAllByFrequency();
		final int resultSize = Math.min(n, items.size());
		final List<T> result = new ArrayList<T>(resultSize);
		for (final Entry<T, Integer> e : all.subList(0, resultSize)) {
			result.add(e.getKey());
		}
		return Collections.unmodifiableList(result);
	}

	public List<Entry<T, Integer>> getAllByFrequency() {
		final List<Entry<T, Integer>> all = new ArrayList<Entry<T, Integer>>(
				items.entrySet());
		Collections.sort(all, BY_FREQ_DESC);
		return Collections.unmodifiableList(all);
	}

	public Integer getCount(final T item) {
		final Integer freq = items.get(item);
		if (freq == null) {
			return 0;
		}
		return freq;
	}

	public void clear() {
		items.clear();
	}

	public Set<Entry<T, Integer>> entrySet() {
		return Collections.unmodifiableSet(items.entrySet());
	}

	public Set<T> keySet() {
		return Collections.unmodifiableSet(items.keySet());
	}

	public List<T> keyList() {
		return getMostFrequent(items.size());
	}

	@Override
	public String toString() {
		return items.toString();
	}
}
