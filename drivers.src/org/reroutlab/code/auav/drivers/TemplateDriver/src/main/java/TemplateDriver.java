package org.reroutlab.code.auav.drivers;

import java.util.*;
import java.lang.System.*;
//import org.reroutlab.code.auav.interfaces.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.InetSocketAddress;
import java.net.SocketException;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.BatteryManager;
import android.content.IntentFilter;
import android.content.Intent;
import android.os.Bundle;
import android.app.Application;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.h2.*;
import org.h2.jdbcx.JdbcConnectionPool;

import dji.sdk.flightcontroller.FlightController;
import dji.sdk.flightcontroller.FlightAssistant;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.base.BaseProduct;
import dji.sdk.battery.Battery;
import dji.sdk.remotecontroller.RemoteController;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.common.battery.*;
//import dji.thirdparty.eventbus.EventBus;
import dji.common.util.CommonCallbacks;

/**
 * This is a Driver Template
 * It exists to facilitate the construction of new Drivers
 * Check the SoftwarePilot Developer Guide for more details
 * at Reroutlab.org/softwarepilot 
 * @author  Jayson Boubin
 * @version 1.0.9
 * @since   2019-12-26
 */
public class TemplateDriver extends org.reroutlab.code.auav.drivers.AuavDrivers {

		private static int LISTEN_PORT = 0;
		private int driverPort = 0;
		private CoapServer cs;
		private static Logger logger = Logger.getLogger(TemplateDriver.class.getName());
		/**
		 *		usageInfo += "help -- Add Usage Strings.<br>";
		 *		usageInfo += "AUAVsim -- Simulate.<br>";
		 * @author  Jayson Boubin
		 * @version 1.0.0
		 * @since   2019-12-26
		 */
		public String getUsageInfo() {
				String usageInfo = "";
				usageInfo += "AUAVsim -- Simulate.\n";
				return usageInfo;
		}

		//extends CoapResource class
		private class Resource extends CoapResource {
				public Resource(){
					super("cr");
					getAttributes().setTitle("cr");
				}
				@Override
				public void handlePUT(CoapExchange ce) {
						// Split on & and = then on ' '
						String outLine = "";
						byte[] payload = ce.getRequestPayload();
						String inputLine = new String(payload);
						int AUAVsim = 0;
						if (inputLine.contains("dp=AUAVsim")) {
							AUAVsim = 1;
						}
						String[] args = inputLine.split("-");//???

						if(args[0].equals("dc=help")) {
							ce.respond(getUsageInfo());
						} else {
							ce.respond("Error: unknown command\n");
						}

				}
		}

		public void setLogLevel(Level l){
			logger.setLevel(l);
		}

		//-----------------------------------------------------------
		// The code below is largely templated material that won't
		// change much between drivers.  However, I have not added
		// to the interface class in case there is a need for
		// customization as the projects advance.
		//
		// Obviously, this makes updating all drivers challenging,
		// but c'est la vie
		//
		// - Christopher Stewart
		// September 18
		//-----------------------------------------------------------
		
	
		public CoapServer getCoapServer() {
				return (cs);
		}

		public TemplateDriver() throws Exception {
				logger.log(Level.FINEST, "In Constructor");
				cs = new CoapServer(); //initilize the server
				InetSocketAddress bindToAddress =
						new InetSocketAddress("localhost", LISTEN_PORT);//get the address
				CoapEndpoint tmp = new CoapEndpoint(bindToAddress); //create endpoint
				cs.addEndpoint(tmp);//add endpoint to server
				tmp.start();//Start this endpoint and all its components.
				driverPort = tmp.getAddress().getPort();
				cs.add(new Resource());
		}

		public int getLocalPort() {
				return driverPort;
		}
		private HashMap driver2port;  // key=drivername value={port,usageInfo}
		public void setDriverMap(HashMap<String, String> m) {
				if (m != null) {
						driver2port = new HashMap<String, String>(m);
				}
		}
}

