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

import static org.junit.Assert.assertTrue;
import junit.framework.Assert;


import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.aggregator.Aggregator;
import com.owlplatform.aggregator.AggregatorConfiguration;
import com.owlplatform.common.SampleMessage;
import com.owlplatform.sensor.SensorAggregatorInterface;
import com.owlplatform.sensor.listeners.ConnectionListener;
import com.owlplatform.solver.SolverAggregatorInterface;
import com.owlplatform.solver.listeners.SampleListener;
import com.owlplatform.solver.protocol.messages.SubscriptionMessage;
import com.owlplatform.solver.protocol.messages.Transmitter;
import com.owlplatform.solver.rules.SubscriptionRequestRule;

public class AggregatorTest implements SampleListener, ConnectionListener,
		com.owlplatform.solver.listeners.ConnectionListener {

	Logger log = LoggerFactory.getLogger(AggregatorTest.class);
	
	public static int SOLVER_PORT = 8008;
	public static int SENSOR_PORT = 8007;

	public static final int THROUGHPUT_SAMPLES = 1000;

	public static final int FILTER_SAMPLES = 100;

	private int receivedSampleIndex = 0;

	private boolean readySensor = false;

	private boolean readySolver = false;

	private long startReceive = 0l;
	private long endReceive = 0l;

	private static Aggregator aggregator;

	@BeforeClass
	public static void setupAggregator() {
		AggregatorConfiguration config = new AggregatorConfiguration();
		config.setSensorListenPort(SENSOR_PORT);
		config.setSolverListenPort(SOLVER_PORT);
		AggregatorTest.aggregator = new Aggregator();
		AggregatorTest.aggregator.setConfig(config);

		AggregatorTest.aggregator.init();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@AfterClass
	public static void destroyAggregator() {
		if (AggregatorTest.aggregator != null) {
			AggregatorTest.aggregator.shutdown();
		}
	}
	
	@Before
	public void prepVariables(){
		this.readySensor = false;
		this.readySolver = false;
		this.startReceive = 0l;
		this.endReceive = 0l;
		this.receivedSampleIndex = 0;
	}

	@Test
	public void throughputTest() {
		this.log.info("Testing Aggregator throughput.");
		

		SampleMessage[] samples = new SampleMessage[THROUGHPUT_SAMPLES];
		for (int i = 0; i < samples.length; ++i) {
			samples[i] = SampleMessage.getTestMessage();
			samples[i].setRssi(i * 0.01f);
		}

		SensorAggregatorInterface senseAgg = new SensorAggregatorInterface();
		senseAgg.setHost("localhost");
		senseAgg.setPort(SENSOR_PORT);
		senseAgg.addConnectionListener(this);

		SolverAggregatorInterface solverAgg = new SolverAggregatorInterface();
		solverAgg.setHost("localhost");
		solverAgg.setPort(SOLVER_PORT);

		solverAgg.addSampleListener(this);
		solverAgg.addConnectionListener(this);

		assertTrue("Unable to start Solver-Aggregator interface.",
				solverAgg.doConnectionSetup());

		assertTrue("Unable to start Sensor-Aggregator interface.",
				senseAgg.doConnectionSetup());

		while (!this.readySensor) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		while (!this.readySolver) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		this.log.info("Sending " + samples.length + " samples.");

		for (int i = 0; i < samples.length; ++i) {
			SampleMessage s = samples[i];
			assertTrue("Unable to send sample " + s, senseAgg.sendSample(s));
			if ((i & 0x0F) == 0) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		try {
			while(this.endReceive > (System.currentTimeMillis()-100)){
			this.log.info("Sleeping to allow samples to issue.");
			Thread.sleep(100);
			}
		} catch (InterruptedException ie) {

		}

		senseAgg.disconnect();
		solverAgg.disconnect();

		Assert.assertEquals("Didn't receive correct number of samples. Aggregator may be buffering too much.",samples.length, this.receivedSampleIndex);
		long totalTime = this.endReceive - this.startReceive;
		float rate = (samples.length * 1000f) / totalTime;
		this.log.info(String.format("Received %d samples in %d ms (%.2f S/s)\n",
				Integer.valueOf(samples.length), Long.valueOf(totalTime), Float.valueOf(rate)));
	}

	@Test
	public void filterTest() {
		this.log.info("Testing Aggregator filtering.");

		SampleMessage[] samples = new SampleMessage[FILTER_SAMPLES];
		for (int i = 0; i < samples.length; ++i) {
			samples[i] = SampleMessage.getTestMessage();
			samples[i].setRssi(i * 0.01f);
		}

		SensorAggregatorInterface senseAgg = new SensorAggregatorInterface();
		senseAgg.setHost("localhost");
		senseAgg.setPort(SENSOR_PORT);
		senseAgg.addConnectionListener(this);

		SolverAggregatorInterface solverAgg = new SolverAggregatorInterface();
		solverAgg.setHost("localhost");
		solverAgg.setPort(SOLVER_PORT);

		SubscriptionRequestRule rule = new SubscriptionRequestRule();
		rule.setPhysicalLayer((byte) 0);
		Transmitter txer = new Transmitter();
		txer.setBaseId(SampleMessage.getTestMessage().getDeviceId());
		txer.setMask(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF });
		rule.setTransmitters(new Transmitter[] { txer });
		rule.setUpdateInterval(5l);
		solverAgg.setRules(new SubscriptionRequestRule[] { rule });

		solverAgg.addSampleListener(this);
		solverAgg.addConnectionListener(this);

		assertTrue("Unable to start Solver-Aggregator interface.",
				solverAgg.doConnectionSetup());

		assertTrue("Unable to start Sensor-Aggregator interface.",
				senseAgg.doConnectionSetup());

		while (!this.readySensor) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		while (!this.readySolver) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		this.log.info("Sending " + samples.length + " samples.");

		for (int i = 0; i < samples.length; ++i) {
			SampleMessage s = samples[i];
			assertTrue("Unable to send sample " + s, senseAgg.sendSample(s));

			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		try {
			while(this.endReceive > (System.currentTimeMillis()-100)){
			this.log.info("Sleeping to allow samples to issue.");
			Thread.sleep(100);
			}
		} catch (InterruptedException ie) {

		}

		senseAgg.doConnectionTearDown();
		solverAgg.doConnectionTearDown();

		Assert.assertEquals("Didn't receive correct number of samples. Aggregator may be taking too long to process filters.",samples.length, this.receivedSampleIndex);
		long totalTime = this.endReceive - this.startReceive;
		float rate = (samples.length * 1000f) / totalTime;
		this.log.info(String.format("Received %d samples in %d ms (%.2f S/s)\n",
				samples.length, totalTime, rate));
	}

	@Override
	public void sampleReceived(SolverAggregatorInterface aggregator,
			SampleMessage sample) {
		// Assert.assertEquals(this.receivedSampleIndex++ *0.01f,
		// sample.getRssi(), 0.01f);
		// System.out.println("Received " + sample);
		if (this.receivedSampleIndex == 0) {
			this.startReceive = System.currentTimeMillis();
		}
		++this.receivedSampleIndex;
		this.endReceive = System.currentTimeMillis();
	}

	@Override
	public void connectionEnded(SensorAggregatorInterface aggregator) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectionEstablished(SensorAggregatorInterface aggregator) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectionInterrupted(SensorAggregatorInterface aggregator) {
		// TODO Auto-generated method stub

	}

	@Override
	public void readyForSamples(SensorAggregatorInterface aggregator) {
		this.log.info("Sensor connection ready.");
		this.readySensor = true;
	}

	@Override
	public void connectionEnded(SolverAggregatorInterface aggregator) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectionEstablished(SolverAggregatorInterface aggregator) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectionInterrupted(SolverAggregatorInterface aggregator) {
		// TODO Auto-generated method stub

	}

	@Override
	public void subscriptionReceived(SolverAggregatorInterface aggregator,
			SubscriptionMessage response) {
		this.log.info("Solver connection ready.");
		this.readySolver = true;
	}

}
