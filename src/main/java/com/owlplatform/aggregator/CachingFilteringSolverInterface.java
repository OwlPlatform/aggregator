/*
 * Aggregator for the Owl Platform
 * Copyright (C) 2012 Robert Moore
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.owlplatform.aggregator;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.common.SampleMessage;
import com.owlplatform.common.util.HashableByteArray;
import com.owlplatform.common.util.LRUCache;
import com.owlplatform.solver.rules.SubscriptionRequestRule;

public class CachingFilteringSolverInterface extends SolverInterface {

	/**
	 * Logger for this class.
	 */
	private static final Logger log = LoggerFactory
			.getLogger(CachingFilteringSolverInterface.class);

	private static final int MAX_DEVICES = 200;

	CopyOnWriteArrayList<SubscriptionRequestRule> effectiveRules = new CopyOnWriteArrayList<SubscriptionRequestRule>();

	Map<HashableByteArray, DeviceIdHashEntry> ruleCache = Collections
			.synchronizedMap(new LRUCache<HashableByteArray, DeviceIdHashEntry>(
					MAX_DEVICES));

	protected boolean hasEffectiveRules = false;

	protected volatile boolean reportedDrop = false;

	protected volatile AtomicInteger numDropped = new AtomicInteger(0);
	
	static final AtomicLongArray timing = new AtomicLongArray(8);
	static final AtomicIntegerArray counts = new AtomicIntegerArray(timing.length());
	public static final String[] TIMING_NAMES = {
		"HASH ID    ",
		"CACHE GET  ",
		"RULE CHECK ",
		"CACHE SUCC ",
		"CACHE FAIL ",
		"CACHE HIT  ",
		"SEND UPDATE",
		"DROP UPDATE"};

	@Override
	public synchronized boolean sendSample(SampleMessage sampleMessage) {

		if (this.session.getScheduledWriteMessages() > SolverInterface.MAX_OUTSTANDING_SAMPLES) {
			this.numDropped.incrementAndGet();
			return false;
		}

		if (!this.sentSubscriptionResponse) {
			return false;
		}

		if (!this.hasEffectiveRules) {
			return super.sendSample(sampleMessage);
		}
		final long[] times = new long[8];
		final long startTiming = System.nanoTime();
		try {
			

			HashableByteArray deviceHasher = new HashableByteArray(
					sampleMessage.getDeviceId());

			times[0] = System.nanoTime();

			DeviceIdHashEntry cacheResult = this.ruleCache.get(deviceHasher);

			times[1] = System.nanoTime();

			if (cacheResult == null) {
				SubscriptionRequestRule passedRule = null;
				for (SubscriptionRequestRule rule : this.effectiveRules) {
					log.debug("Checking rule {}.", rule);
					if (SubscriptionRuleFilter.applyRule(rule, sampleMessage)) {
						passedRule = rule;
						break;
					}
				}
				times[2] = System.nanoTime();

				if (passedRule != null) {
					log.debug("{} passed all rules.", sampleMessage);
					DeviceIdHashEntry hashEntry = new DeviceIdHashEntry();
					hashEntry.setPassedRules(true);
					hashEntry.setUpdateInterval(passedRule.getUpdateInterval());
					hashEntry.setNextPermittedTransmit(
							sampleMessage.getReceiverId(),
							System.currentTimeMillis()
									+ passedRule.getUpdateInterval());
					this.ruleCache.put(deviceHasher, hashEntry);
					times[3] = System.nanoTime();
					return super.sendSample(sampleMessage);
				}
				DeviceIdHashEntry hashEntry = new DeviceIdHashEntry();
				hashEntry.setPassedRules(false);
				this.ruleCache.put(deviceHasher, hashEntry);
				times[4] = System.nanoTime();
				return false;
			}
			times[5] = System.nanoTime();
			if (cacheResult.isPassedRules()) {
				long now = System.currentTimeMillis();
				long nextTransmit = cacheResult
						.getNextPermittedTransmit(sampleMessage.getReceiverId());
				if (nextTransmit <= now) {
					cacheResult.setNextPermittedTransmit(
							sampleMessage.getReceiverId(),
							now + cacheResult.getUpdateInterval());
					times[6] = System.nanoTime();
					return super.sendSample(sampleMessage);
				}
				times[7] = System.nanoTime();
				return false;
			}
		} finally {
			
			// Compute timing
			// These 2 execute every time
			timing.addAndGet(0, times[0]-startTiming);	// Time to hash ID
			counts.addAndGet(0, 1);
			timing.addAndGet(1,times[1]-times[0]);		// Time to retrieve cache
			counts.addAndGet(1,1);
			
			// Time to check all rules
			if(times[2] != 0){
				timing.addAndGet(2,times[2]-times[1]);
				counts.addAndGet(2,1);
			}
			// Time to create a cache entry (success) and update
			if(times[3] != 0){
				timing.addAndGet(3,times[3]-times[2]);
				counts.addAndGet(3, 1);
			}
			
			// Time to create a cache entry (failure) and update
			if(times[4] != 0){
				timing.addAndGet(4,times[4]-times[2]);
				counts.addAndGet(4, 1);
			}
			// Cache hit
			if(times[5] != 0){
				timing.addAndGet(5,times[5]-times[1]);
				counts.addAndGet(5, 1);
			}
			
			//  Can send sample based on update interval
			if(times[6] != 0){
				timing.addAndGet(6,times[6]-times[5]);
				counts.addAndGet(6,1);
			}
			// Cannot send sample based on update interval
			if(times[7] != 0){
				timing.addAndGet(7,times[7]-times[5]);
				counts.addAndGet(7,1);
			}
			
		}

		return false;
	}
	
	/**
	 * Returns an array of timing information of length N.  The first N/2
	 * elements are the average time it took to complete a section of the
	 * sendSample() method code (in nanoseconds).  The second N/2 elements
	 * are the number of times each section was executed.  N should always equal
	 * twice the length of {@link #TIMING_NAMES}.
	 * @return
	 */
	public static long[] getTiming(){
		final long[] count = new long[timing.length()];
		final long[] times = new long[timing.length()*2];
		for(int i = 0; i < count.length; ++i){
			count[i] = counts.getAndSet(i,0);
			final long time = timing.getAndSet(i,0);
			
			times[i] = count[i] == 0 ? 0 : time / count[i];
			times[i+count.length] = count[i];
		}
		return times;
	}

	public Collection<SubscriptionRequestRule> getEffectiveRules() {
		return this.effectiveRules;
	}

	/**
	 * A convenience method for adding a single rule to this Solver.
	 * 
	 * @param newRule
	 *            the rule to add to this Solver.
	 */
	public void addEffectiveRule(SubscriptionRequestRule newRule) {

		this.effectiveRules.add(newRule);

		for (SubscriptionRequestRule rule : this.effectiveRules) {
			if (rule.getNumTransmitters() > 0 || rule.getPhysicalLayer() != 0
					|| rule.getUpdateInterval() > 0) {
				this.hasEffectiveRules = true;
				break;
			}
		}
		log.info("Added {} to {}.", newRule, this);
	}

	public int getAndClearDroppedPackets() {

		return this.numDropped.getAndSet(0);
	}

	public void clearEffectiveRules() {
		this.effectiveRules.clear();
		this.hasEffectiveRules = false;
		log.debug("Cleared effective rules.");
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("CachingFilteringSolver @").append(
				this.session.getRemoteAddress());
		return sb.toString();
	}
}
