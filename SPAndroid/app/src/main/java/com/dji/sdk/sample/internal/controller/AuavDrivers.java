package com.dji.sdk.sample.internal.controller;

import java.util.logging.Logger;
import java.util.logging.Level;
import org.eclipse.californium.core.CoapServer;
import java.util.HashMap;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.CoapHandler;


import java.util.HashMap;

abstract public class AuavDrivers {
    private long startTime=0;
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long millis) {
		startTime = millis;
	}

	abstract public int getLocalPort();
	abstract public String getUsageInfo();
	abstract public void setDriverMap(HashMap<String,String> m);
	abstract public void setLogLevel(Level l);
	abstract public CoapServer getCoapServer();
}
