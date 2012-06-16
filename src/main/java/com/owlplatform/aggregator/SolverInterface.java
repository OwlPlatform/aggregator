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

import org.apache.mina.core.session.IoSession;
import com.owlplatform.common.SampleMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolverInterface {
	private static final Logger log = LoggerFactory.getLogger(SolverInterface.class);

	protected IoSession session;
	protected boolean sentSubscriptionResponse = false;
	
	static final int MAX_OUTSTANDING_SAMPLES = 200;

	/**
	 * @return the sentSubscriptionResponse
	 */
	public boolean isSentSubscriptionResponse() {
		return sentSubscriptionResponse;
	}

	/**
	 * @param sentSubscriptionResponse
	 *            the sentSubscriptionResponse to set
	 */
	public void setSentSubscriptionResponse(boolean sentSubscriptionResponse) {
		this.sentSubscriptionResponse = sentSubscriptionResponse;
	}

	/**
	 * @return the session
	 */
	public IoSession getSession() {
		return session;
	}

	/**
	 * @param session
	 *            the session to set
	 */
	public void setSession(IoSession session) {
		this.session = session;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof SolverInterface) {
			return this.equals((SolverInterface) o);
		}
		return super.equals(o);
	}

	public boolean equals(SolverInterface solver) {
		return this.session.equals(solver.session);
	}

	public boolean sendSample(SampleMessage sampleMessage) {
		if (this.session == null) {
			log.error("Solver IoSession is null, cannot send sample.");
			return false;
		}

		if (this.session.isClosing()) {
			log.error("Solver IoSession is closing, cannot send sample.");
			return false;
		}
		if (this.session.isConnected()) {
			this.session.write(sampleMessage);
			log.debug("Sent {}.", sampleMessage);
			return true;
		}
		log.warn("Solver IoSession is not connected, cannot send sample.");
		return false;
	}
	
	@Override
	public String toString()
	{
	    StringBuffer sb = new StringBuffer();
	    
	    sb.append("Solver Interface @ ").append(this.session.getRemoteAddress());
	    
	    return sb.toString();
	}

}
