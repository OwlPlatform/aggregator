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
import com.owlplatform.sensor.protocol.messages.HandshakeMessage;

public class SensorInterface {

	protected HandshakeMessage sentHandshake = null;
	protected HandshakeMessage receivedHandshake = null;
	
	protected IoSession session;

	public IoSession getSession() {
		return session;
	}

	public void setSession(IoSession session) {
		this.session = session;
	}

	public HandshakeMessage getSentHandshake() {
		return sentHandshake;
	}

	public void setSentHandshake(HandshakeMessage sentHandshake) {
		this.sentHandshake = sentHandshake;
	}

	public HandshakeMessage getReceivedHandshake() {
		return receivedHandshake;
	}

	public void setReceivedHandshake(HandshakeMessage receivedHandshake) {
		this.receivedHandshake = receivedHandshake;
	}

	@Override
	public String toString()
	{
	    StringBuffer sb = new StringBuffer();
	    
	    sb.append("Sensor Interface @ ").append(this.session.getRemoteAddress());
	    
	    return sb.toString();
	}
}
