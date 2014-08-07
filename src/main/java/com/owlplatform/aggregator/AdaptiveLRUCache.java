package com.owlplatform.aggregator;

import java.util.LinkedHashMap;
import java.util.Map;

import com.owlplatform.common.util.LRUCache;

/**
 * Simple implementation of an LRU cache based on LinkedHashMap.  Idea provided by Hank Gay on StackOverflow.com
 * 
 * Sourced from http://stackoverflow.com/questions/221525/how-would-you-implement-an-lru-cache-in-java-6
 * 
 * @author <a href="http://stackoverflow.com/users/4203/hank-gay">Hank Gay</a>
 * @author Robert Moore II
 *
 * @param <K> 
 * @param <V>
 */
public class AdaptiveLRUCache<K, V> extends LRUCache<K, V> {
	
	/**
	 * To be updated when the class members change.
	 */
  private static final long serialVersionUID = 5148706907508646895L;
 
  /**
	 * Maximum capacity of the cache.
	 */
	private int capacity;
	
	private final int maxCapacity;
	
	/**
	 * Creates a new LRU cache with the specified capacity.
	 * @param capacity the maximum capacity for this cache.
	 */
	public AdaptiveLRUCache(int initCapacity, int maxCapacity)
	{
		super(maxCapacity);
		this.capacity = initCapacity;
		this.maxCapacity = maxCapacity;
	}

	@Override
	protected boolean removeEldestEntry(final Map.Entry<K, V> entry)
	{
		final int size = super.size();
		if(size >= this.capacity && this.capacity < this.maxCapacity){
			this.capacity = Math.min(this.capacity+20, this.maxCapacity);
		}
		return size > this.capacity;
	}
	
	/**
	 * Sets the new working capacity of this LRU Cache.
	 * @param newCapacity the new value for the capacity, which cannot be less than 0 or greater than 
	 * maxCapacity.
	 * @return the new capacity
	 */
	public int setCapacity(int newCapacity){
		if(newCapacity < 0 ){
			return this.capacity;
		}
		if(newCapacity > this.maxCapacity){
			this.capacity = this.maxCapacity;
			return this.maxCapacity;
		}
		
		this.capacity = newCapacity;
		return this.capacity;
	}
}