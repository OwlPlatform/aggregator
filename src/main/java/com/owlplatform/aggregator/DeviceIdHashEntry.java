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

import java.util.concurrent.ConcurrentHashMap;

import com.owlplatform.common.util.HashableByteArray;


public class DeviceIdHashEntry {
	private boolean passedRules = false;

	private long updateInterval = 0l;
	
	private int numAccess = 0;
	
	private ConcurrentHashMap<HashableByteArray, Long> nextPermittedTransmit = new ConcurrentHashMap<HashableByteArray, Long>();

	public boolean isPassedRules() {
		++this.numAccess;
		return passedRules;
	}

	public void setPassedRules(boolean passedRules) {
		this.passedRules = passedRules;
	}

	public long getUpdateInterval() {
		++this.numAccess;
		return updateInterval;
	}
	
	public int getNumAccess(){
		return this.numAccess;
	}

	public void setUpdateInterval(long updateInterval) {
		this.updateInterval = updateInterval;
	}

	public long getNextPermittedTransmit(byte[] receiverId) {
		HashableByteArray hash = new HashableByteArray(receiverId);
		++this.numAccess;
		Long nextTransmit = this.nextPermittedTransmit.get(hash);
		if(nextTransmit == null)
		{
			this.nextPermittedTransmit.put(hash,Long.valueOf(Long.MIN_VALUE));
			return Long.MIN_VALUE;
		}
		
		return nextTransmit.longValue();
	}

	public void setNextPermittedTransmit(byte[] receiverId, long nextPermittedTransmit) {
		HashableByteArray hash = new HashableByteArray(receiverId);
		this.nextPermittedTransmit.put(hash,Long.valueOf(nextPermittedTransmit));
	}

}
