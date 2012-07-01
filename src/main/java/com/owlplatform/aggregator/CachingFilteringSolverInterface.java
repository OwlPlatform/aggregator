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

  ConcurrentLinkedQueue<SubscriptionRequestRule> effectiveRules = new ConcurrentLinkedQueue<SubscriptionRequestRule>();

  Map<HashableByteArray, DeviceIdHashEntry> ruleCache = Collections
      .synchronizedMap(new LRUCache<HashableByteArray, DeviceIdHashEntry>(
          MAX_DEVICES));

  protected boolean hasEffectiveRules = false;

  protected volatile boolean reportedDrop = false;

  protected volatile int numDropped = 0;

  @Override
  public synchronized boolean sendSample(SampleMessage sampleMessage) {

    if (this.session.getScheduledWriteMessages() > SolverInterface.MAX_OUTSTANDING_SAMPLES) {
      if (!this.reportedDrop) {
        this.reportedDrop = true;
        log.warn("Dropping a sample for {}", this);
      }
      ++this.numDropped;
      return false;
    }
    if (this.reportedDrop) {
      log.warn("{} lost {} samples.", this, this.numDropped);
      this.numDropped = 0;
      this.reportedDrop = false;
    }

    if (!this.sentSubscriptionResponse) {
      return false;
    }

    if (!this.hasEffectiveRules) {
      return super.sendSample(sampleMessage);
    }

    HashableByteArray deviceHasher = new HashableByteArray(
        sampleMessage.getDeviceId());

    DeviceIdHashEntry cacheResult = this.ruleCache.get(deviceHasher);

    if (cacheResult == null) {
      SubscriptionRequestRule passedRule = null;
      for (SubscriptionRequestRule rule : this.effectiveRules) {
        log.debug("Checking rule {}.", rule);
        if (SubscriptionRuleFilter.applyRule(rule, sampleMessage)) {
          passedRule = rule;
          break;
        }
      }

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
        return super.sendSample(sampleMessage);
      }
      DeviceIdHashEntry hashEntry = new DeviceIdHashEntry();
      hashEntry.setPassedRules(false);
      this.ruleCache.put(deviceHasher, hashEntry);
      return false;
    }
    if (cacheResult.isPassedRules()) {
      long now = System.currentTimeMillis();
      long nextTransmit = cacheResult
          .getNextPermittedTransmit(sampleMessage.getReceiverId());
      if (nextTransmit <= now) {
        cacheResult.setNextPermittedTransmit(
            sampleMessage.getReceiverId(),
            now + cacheResult.getUpdateInterval());
        return super.sendSample(sampleMessage);
      }
      return false;
    }

    return false;
  }

  public Collection<SubscriptionRequestRule> getEffectiveRules() {
    return this.effectiveRules;
  }

  /**
   * A convenience method for adding a single rule to this Solver.
   * 
   * @param newRule the rule to add to this Solver.
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
