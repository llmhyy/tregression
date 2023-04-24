package tregression.handler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import sav.common.core.Pair;

public class APIConfidenceDict implements Iterable<Map.Entry<String, Pair<Integer, Integer>>> {
	
	private final Map<String, Pair<Integer, Integer>> dictionary = new HashMap<>();
	
	public void addRecord(final String methodName, boolean correct) {
		Pair<Integer, Integer> newPair = null;
		if (this.dictionary.containsKey(methodName)) {
			Pair<Integer, Integer> pair = this.dictionary.get(methodName);
			newPair = Pair.of(correct ? pair.first()+1 : pair.first(), pair.second()+1);
		} else {
			newPair = Pair.of(correct ? 1 : 0, 1);
		}
		this.dictionary.put(methodName, newPair);
	}
	
	@Override
	public Iterator<Map.Entry<String, Pair<Integer, Integer>>> iterator() {
		return this.dictionary.entrySet().iterator();
	}
	
	public boolean contains(final String methodName) {
		return this.dictionary.containsKey(methodName);
	}
	
	public Pair<Integer, Integer> getValue(final String methodName) {
		return this.dictionary.get(methodName);
	}
	
	public void putValue(final String methodName, final int correctCount, final int totalCount) {
		this.putValue(methodName, Pair.of(correctCount, totalCount));
	}
	
	public void putValue(final String methodName, Pair<Integer, Integer> pair) {
		this.dictionary.put(methodName, pair);
	}
	
	public static APIConfidenceDict aggreateDict(Collection<APIConfidenceDict> dictionaries) {
		APIConfidenceDict dict = new APIConfidenceDict();
		for(APIConfidenceDict dictionary : dictionaries) {
			dict.addRecord(dictionary);
		}
		return dict;
	}
	
	public void addRecord(final APIConfidenceDict otherDict) {
		for(Map.Entry<String, Pair<Integer, Integer>> entry : otherDict) {
			final String invokingMethod = entry.getKey();
			final Pair<Integer, Integer> pair = entry.getValue();
			if (this.dictionary.containsKey(invokingMethod)) {
				final Pair<Integer, Integer> originPair = this.getValue(invokingMethod);
				this.putValue(invokingMethod, pair.first() + originPair.first(), pair.second() + originPair.second());
			} else {
				this.putValue(invokingMethod, pair);
			}
		}
	}
	
	public void printDict() {
		for (Map.Entry<String, Pair<Integer, Integer>> entry : this) {
			System.out.println(entry.getKey() + "," + entry.getValue().first() + "," + entry.getValue().second());
		}
	}
}
