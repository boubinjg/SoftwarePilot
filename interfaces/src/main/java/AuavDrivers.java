package org.reroutlab.code.auav.drivers;
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

import java.util.concurrent.Semaphore;
import java.util.HashMap;

abstract public class AuavDrivers {
		private long startTime=0;
		public long getStartTime() {
				return startTime;
		}		
		public void setStartTime(long millis) {
				startTime = millis;
		}

		public Semaphore lockSema = new Semaphore(1);
		public String lockStr = "continue";
		public void drvSetLock(String v) {
				lockSema.acquireUninterruptibly();
				lockStr = v;
		}
		public void drvUnsetLock() {
				lockSema.release();
		}
		public void drvSpin() {
				lockSema.acquireUninterruptibly();
				lockSema.release();
		}				
		
		abstract public int getLocalPort();
		abstract public String getUsageInfo();
		abstract public void setDriverMap(HashMap<String,String> m);
		abstract public void setLogLevel(Level l);
		abstract public CoapServer getCoapServer();
}
