package org.panhandlers.sentimentalizer;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class DictionaryBuilder {
	private static int DEFAULT_MAX = 2000;
	private Set<String> dictionary;
	private HashMap<String, Integer> itemsWithToken;
	private int max;
	public DictionaryBuilder() {
		max = DEFAULT_MAX;
	}
	
	public DictionaryBuilder(int max) {
		this.max = max;
	}
	public Set<String> buildDictionary(HashMap<String, List<List<String>>> data) {
		dictionary = new HashSet<String>();
		HashMap<String, Integer> itemsWithToken = new HashMap<String, Integer>();
		HashMap<String, Integer> rawFrequency = new HashMap<String, Integer>();
		List<HashMap<String, Integer>> rawFrequenciesForItem = new LinkedList<HashMap<String, Integer>>();
		int totalItems = 0;
		Set<String> tokensInItem;
		for (List<List<String>> categoryItems : data.values()) {
			for(List<String> itemTokens : categoryItems) {
				totalItems++;
				tokensInItem = new HashSet<String>();
				rawFrequency = new HashMap<String, Integer>();
				for(String token : itemTokens) {
					if (sane(token)) {
						tokensInItem.add(token);
						updateFrequency(token, rawFrequency);
						dictionary.add(token);
					}
				}
				for(String token : tokensInItem) {
					updateExistenceSet(token, itemsWithToken);
				}
				rawFrequenciesForItem.add(rawFrequency);
			}
		}
		return tdifFilter(rawFrequenciesForItem, idf(dictionary, totalItems, itemsWithToken));
	}
	
	private boolean sane(String token) {
		if (token.length() < 3) return false;
		if (token.matches("\\d+.*")) return false;
		return true;
	}

	private HashMap<String, Double> idf(Set<String> tokens, int totalItems, HashMap<String, Integer> itemsWithToken) {
		double quota;
		double idf;
		HashMap<String, Double> output = new HashMap<String, Double>(totalItems);
		for (String token : tokens) {
			quota = (double) totalItems / (double) itemsWithToken.get(token);
			idf = Math.log(quota);
			output.put(token, idf);
		}
		return output;
	}
	private Set<String> tdifFilter(
			List<HashMap<String, Integer>> rawFrequenciesForItem, 
			HashMap<String, Double> idfValues) {
		HashMap<String, Double> tfdifValueMap = new HashMap<String, Double>();
		double tfidfValue;
		for(HashMap<String, Integer> frequencies : rawFrequenciesForItem) {
			for(Entry<String, Integer> pair : frequencies.entrySet()) {
				tfidfValue = ((double) pair.getValue()) * idfValues.get(pair.getKey());
				updateTfidf(pair.getKey(), tfidfValue, tfdifValueMap);
			}
		}
		Set<String> dictionary = new HashSet<String>(idfValues.size());
		TreeMap<Double, String> sortedMap = new TreeMap<Double, String>();
		for (Entry<String, Double> entry : tfdifValueMap.entrySet()) {
			sortedMap.put(entry.getValue(), entry.getKey());
		}
		Iterator<String> it = sortedMap.descendingMap().values().iterator();
		String token;
		for (int count = 0; count < max && count < sortedMap.size() ; count++) {
			token = it.next();
			dictionary.add(token);			
		}
		for (Entry<Double, String> entry : sortedMap.descendingMap().entrySet()) {
			System.out.println(entry.getKey() + " => " + entry.getValue());
		}
		return dictionary;
	}
	private void updateTfidf(String token, double tfidfValue,
			HashMap<String, Double> sortedTokens) {
		Double value = sortedTokens.get(token);
		if (value == null) {
			sortedTokens.put(token, tfidfValue);
		} else {
			sortedTokens.put(token, value + tfidfValue);
		}
	}
	private static void updateExistenceSet(String token, HashMap<String, Integer> itemsWithToken) {
		Integer count = itemsWithToken.get(token);
		if (count == null) {
			itemsWithToken.put(token, 1);
		} else {
			itemsWithToken.put(token, count + 1);
		}
	}
	private static void updateFrequency(String token, HashMap<String, Integer> rawFrequency) {
		Integer count = rawFrequency.get(token);
		if(count == null) {
			rawFrequency.put(token, 1);
		} else {
			rawFrequency.put(token, count + 1);
		}
	}
	
}
