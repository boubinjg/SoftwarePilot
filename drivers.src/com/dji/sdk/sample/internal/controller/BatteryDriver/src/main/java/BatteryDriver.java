package com.dji.sdk.sample.internal.controller;

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
 * BatteryDriver connects to DJI <i>and</i> local compute.
 * It supports commands related to checking battery statuses.
 * Interact with this driver via CoAP (californium)
 * <br><b>URL: coap:\\localhost:port\cr?</b>
 * <br>Example: coap:\\127.0.0.1:10452\cr?dc=dji
 * <br>
 * Note: all AUAV drivers have resource component "cr" for coap resource
 *
 * @author  Christopher Charles Stewart
 * @version 1.0.2
 * @since   2017-05-01
 */
public class BatteryDriver extends com.dji.sdk.sample.internal.controller.AuavDrivers {

		private static int LISTEN_PORT = 0;
		private int driverPort = 0;
		private CoapServer cs;
		private int lclLastReading = 100;
		private long startTime = 0;
		private int djiLastReading = 0;
		private int djiLastMAH = 0;
		private int	djiCurrent = 0;
		private int djiVoltage = 0;
		private static BaseProduct mProduct;
		/**
		 *		usageInfo += "dc=[cmd]-dp=[option]<br>";
		 *		usageInfo += "cmd: <br>";
		 *		usageInfo += "dji -- Grab and store battery status of DJI<br>";
		 *		usageInfo += "lcl -- Grab and store battery status of compute<br>";
		 *		usageInfo += "qry -- Issue an SQL query against prior data<br>";
		 *		usageInfo += "help -- Return this usage information.<br>";
		 *		usageInfo += "option -- <br>";
		 *		usageInfo += "AUAVsim -- Do not access DJI or local compute.  Simulate.<br>";
		 *		usageInfo += "SQL-String -- Executed against SQLite<br>";
		 * @author  Christopher Charles Stewart
		 * @version 1.0.2
		 * @since   2017-05-01
		 */
		public String getUsageInfo() {
				String usageInfo = "";
				usageInfo += "dc=[cmd]-dp=[option]\n";
				usageInfo += "cmd: \n";
				usageInfo += "dji -- Grab and store battery status of DJI\n";
				usageInfo += "lcl -- Grab and store battery status of compute\n";
				usageInfo += "qry -- Issue an SQL query against prior data\n";
				usageInfo += "help -- Return this usage information.\n";
				usageInfo += "option --> \n";
				usageInfo += "AUAVsim -- Do not access DJI or local compute.  Simulate.\n";
				usageInfo += "SQL-String -- Executed against SQLite\n";
				return usageInfo;
		}

		//extends CoapResource class
		private class bdResource extends CoapResource {
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

						switch (args[0]) {
						case "dc=help":
								ce.respond(getUsageInfo());
								break;
						case "dc=qry":
								String qry = args[1].substring(3);
								ce.respond(queryH2(qry));
								break;
						case "dc=dji":
								System.out.println("Battery Value is: " + djiLastReading);
                                System.out.println("Battery MAH is: " + djiLastMAH);
                                System.out.println("Battery Current is: "+djiCurrent);
								System.out.println("Battery Voltage is: "+djiVoltage);
								ce.respond("Percent=" + djiLastReading+", MAH="+djiLastMAH);

						case "dc=lcl":
								if (AUAVsim == 1) {
										lclLastReading--;
										addReadingH2(lclLastReading, "lcl");
										ce.respond("Battery: " + Integer.toString(lclLastReading));
										break;
								}

								try {
										Class<?> c = Class.forName("android.app.ActivityThread");
										android.app.Application app =
												(android.app.Application) c.getDeclaredMethod("currentApplication").invoke(null);
										android.content.Context context = app.getApplicationContext();
										BatteryManager bm = (BatteryManager)context.getSystemService("batterymanager");
										int batLevel = bm.getIntProperty(4);
										lclLastReading = batLevel;
										addReadingH2(batLevel, "lcl");
										ce.respond("Battery: " + Integer.toString(batLevel));
								}
								catch (Exception e) {
										ce.respond("Battery: Error");
								}

								break;
						case "dc=cfg":
								if(AUAVsim != 1){
									initBatteryCallback();
								}
								ce.respond("Battery: Configured");
						default:
								ce.respond("Error: BatteryDriver unknown command\n");
						}
				}


				public bdResource() {
						super("cr");	getAttributes().setTitle("cr");
				}

				public void addReadingH2(int reading, String type) {
						try {

								Connection conn = DriverManager.getConnection("jdbc:h2:mem:BatteryDriver;DB_CLOSE_DELAY=-1",
																											 "user", "password");
								Statement stmt = conn.createStatement();

								String sql = "INSERT INTO data (time, type, value) VALUES ("
										+ (System.currentTimeMillis()- startTime) + "," + type + "," + reading + ")";
						stmt.executeUpdate(sql);
						conn.close();
						}
						catch (Exception e) {
														e.printStackTrace();
						}



				}

				public String queryH2(String query) {
						String outLine = "";
						try {
								Connection conn = DriverManager.getConnection("jdbc:h2:mem:BatteryDriver;DB_CLOSE_DELAY=-1",
																											 "user", "password");
								Statement stmt = conn.createStatement();

								ResultSet rs = stmt.executeQuery(query);

								while(rs.next()) {
										outLine += "key="+rs.getInt("key")+
												"-time="+rs.getLong("time")+"-type="+rs.getString("type")+
												"-data="+rs.getString("value")+"---";
								}

								conn.close();
						}
						catch (Exception e) {
								e.printStackTrace();
						}
						return outLine;
				}

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
		private void initBatteryCallback(){
                        System.out.println("Reading Battery Info");
                        Battery batt = getBatteryInstance();
                        System.out.println("Obtaind Battery Info");

                        try{
                                batt.setStateCallback(new BatteryState.Callback() {
                                        @Override
                                        public void onUpdate(BatteryState djiBatteryState){
                                                djiLastReading = djiBatteryState.getChargeRemainingInPercent();
                                                djiLastMAH = djiBatteryState.getChargeRemaining();
												djiCurrent = djiBatteryState.getCurrent();
												djiVoltage = djiBatteryState.getVoltage();
                                        }
                                });
                        } catch(Exception e){
                                e.printStackTrace();
                        }

                }
		public static synchronized BaseProduct getProductInstance() {
                        if (null == mProduct) {
                                mProduct = DJISDKManager.getInstance().getProduct();
                        }
                        return mProduct;
                }

                public static synchronized Battery getBatteryInstance() {

                        if (getProductInstance() == null) return null;

                        Battery batt = null;

                        if (getProductInstance() instanceof Aircraft){
                                batt = ((Aircraft) getProductInstance()).getBattery();

                        } else if (getProductInstance() instanceof HandHeld) {
                                batt = ((HandHeld) getProductInstance()).getBattery();
                        }

                        return batt;
                }


		public CoapServer getCoapServer() {
				return (cs);
		}
		public BatteryDriver() throws Exception {
				bdLogger.log(Level.FINEST, "In Constructor");
				cs = new CoapServer(); //initilize the server
				InetSocketAddress bindToAddress =
						new InetSocketAddress("localhost", LISTEN_PORT);//get the address
				CoapEndpoint tmp = new CoapEndpoint(bindToAddress); //create endpoint
				cs.addEndpoint(tmp);//add endpoint to server
				startTime = System.currentTimeMillis();
				tmp.start();//Start this endpoint and all its components.
				driverPort = tmp.getAddress().getPort();
				cs.add(new bdResource());

				try {
						Class.forName("org.h2.Driver");
						Connection conn = DriverManager.getConnection("jdbc:h2:mem:BatteryDriver;DB_CLOSE_DELAY=-1",
																									 "user", "password");
						conn.createStatement().executeUpdate("CREATE TABLE data ("
																								 +" key  INTEGER AUTO_INCREMENT,"
																								 +" time BIGINT, "
																								 +" type VARCHAR(16), "
																								 +" value VARCHAR(1023) )");
						conn.close();
				}
				catch(Exception e) {
												e.printStackTrace();
				}

			}
		private static Logger bdLogger =
				Logger.getLogger(BatteryDriver.class.getName());
		public void setLogLevel(Level l) {
				bdLogger.setLevel(l);
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

