package org.panhandlers.sentimentalizer.redis;

import java.util.Set;

import org.panhandlers.sentimentalizer.ClassifierStorage;
import org.panhandlers.sentimentalizer.Feature;

import redis.clients.jedis.Jedis;

public class RedisStorage implements ClassifierStorage {
	private static final String FEATURE = "bayes:feature:";
	private static final String CATEGORY_COUNTS = "bayes:category_counts";
	private static final String FEATURE_COUNTS = "bayes:feature_counts:";
	private static final String TOTAL_COUNT = "bayes:total_count";
	private Jedis jedis;
	
	public RedisStorage() {
		jedis = RedisConfig.getJedisPool().getResource();
	}
	@Override
	public void addFeature(String category, Feature feature) {
		jedis.hincrBy(CATEGORY_COUNTS, category, 1);
		jedis.hincrBy(getFeatureCountKey(category), feature.toString(), 1);
		jedis.incr(TOTAL_COUNT);
	}
	
	private String getFeatureCountKey(String category) {
		return FEATURE_COUNTS + category;
	}

	@Override
	public int getCategoryCount(String category) {
		return Integer.parseInt(jedis.hget(CATEGORY_COUNTS, category));
	}

	@Override
	public int getFeatureCount(String category, Feature feature) {
		return Integer.parseInt(jedis.hget(getFeatureCountKey(category), feature.toString()));
	}

	@Override
	public int getTotalCount() {
		return Integer.parseInt(jedis.get(TOTAL_COUNT));
	}

	@Override
	public Set<String> getCategories() {
		return jedis.hkeys(CATEGORY_COUNTS);
	}

}