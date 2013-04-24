package org.panhandlers.sentimentalizer.redis;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.panhandlers.sentimentalizer.DataDivider;
import org.panhandlers.sentimentalizer.classifiers.ClassificationResult;
import org.panhandlers.sentimentalizer.classifiers.NaiveBayes;
import org.panhandlers.sentimentalizer.features.ExistenceFeatureExtractor;
import org.panhandlers.sentimentalizer.features.Feature;
import org.panhandlers.sentimentalizer.features.OccurrenceFeatureExtractor;
import org.panhandlers.sentimentalizer.storage.DictionaryBuilder;

public class RedisBayesTester {
	private RedisDataReader reader;
	private DataDivider divider;
	private OccurrenceFeatureExtractor extractor;
	private NaiveBayes classifier;
	private RedisStorage storage;
	private DictionaryBuilder dictionaryBuilder;
	RedisBayesTester() {
		storage = new RedisStorage();
		classifier = new NaiveBayes(storage);
		reader = new RedisDataReader("amazon-balanced-6cats");
		divider = new DataDivider(9);
		extractor = new OccurrenceFeatureExtractor();
		dictionaryBuilder = new DictionaryBuilder(500);
	}
	
	private void run() {
		train();
		test();
	}
	
	private void test() {
		ClassificationResult result;
		List<Feature> features;
		int successes = 0;
		int failures = 0;
		for (Entry<String, List<List <String>>> cat : divider.getTestData().entrySet()) {
			for (List<String> item : cat.getValue()) {
				features = extractor.extractFeatures(item);
				result = classifier.classify(features);
				if (result.getCategory().equals(cat.getKey())) {
					System.out.println("Succeeded for: " + detokenize(item) +"\n which was supposed to be " + cat.getKey());
					successes++;
				} else {
					System.out.println("Failed for: " + detokenize(item) +"\n which was supposed to be " + cat.getKey());
					failures++;
				}
			}
		}
		System.out.println("Successes: " + successes + " Failures: " + failures);
		double percentage = (double) successes / ((double) successes + failures);
		System.out.println("Percentage: " + percentage * 100);	
	}
	
	private String detokenize(List<String> item) {
		StringBuilder b = new StringBuilder();
		for (String s : item) {
			b.append(s);
		}
		return b.toString();
	}

	private void train() {
		storage.reset();
		List<List<String>> positive = reader.getItemsByCategoryAndSentiment("dvd", "pos");
		List<List<String>> negative = reader.getItemsByCategoryAndSentiment("dvd", "neg");
		HashMap<String, List<List<String>>> data = new HashMap<String, List<List<String>>>();
		data.put("pos", positive);
		data.put("neg", negative);
		divider.divide(data);
		Set<String> dictionary = dictionaryBuilder.buildDictionary(divider.getTrainingData());
		System.out.println("Dictionary built with length: " + dictionary.size());
		extractor.setDictionary(dictionary);
		List<Feature> features;
		for (Entry<String, List<List<String>>> cat: divider.getTrainingData().entrySet()) {
			System.out.println("Training for: " + cat.getKey() + " with " + cat.getValue().size() + " items");
			for(List<String> item : cat.getValue()) {	    
//				System.out.println("Training item with length: " + item.size());
				features = extractor.extractFeatures(item);
			    long startTime = System.currentTimeMillis();
//				System.out.println("Extraction complete with length: " + features.size());
				classifier.train(cat.getKey(), features);
				long stopTime = System.currentTimeMillis();
			    long elapsedTime = stopTime - startTime;
//			    System.out.println("Training  completed in " + elapsedTime);
			}
		}
		System.out.println("Training complete");
	}

	public static void main(String[] args) {
		RedisBayesTester t = new RedisBayesTester();
		t.run();
	}
}
