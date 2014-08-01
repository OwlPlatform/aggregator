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

public class AggregatorConfiguration {
	private int sensorListenPort = Aggregator.SENSOR_LISTEN_PORT;
	private int solverListenPort = Aggregator.SOLVER_LISTEN_PORT;
	private boolean useCache = true;
	
	public int getSolverListenPort() {
		return this.solverListenPort;
	}

	public void setSolverListenPort(int solverListenPort) {
		this.solverListenPort = solverListenPort;
	}

	public int getSensorListenPort() {
		return this.sensorListenPort;
	}

	public void setSensorListenPort(int listenPort) {
		this.sensorListenPort = listenPort;
	}

	public boolean isUseCache() {
		return useCache;
	}

	public void setUseCache(boolean useCache) {
		this.useCache = useCache;
	}
}
