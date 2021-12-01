package com.dji.sdk.sample.internal.controller;

import java.util.HashMap;
import org.eclipse.californium.core.CoapHandler;
//sockets
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.InputStream;
import java.io.ObjectInputStream;
//openCV
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
//import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.highgui.Highgui;
import org.opencv.core.MatOfByte;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import java.io.*;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Base64;
import java.io.File;
import java.nio.file.*;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.io.IOException;
import java.net.Socket;
import java.io.DataInputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors.*;
//Metadata Extraction
import com.drew.metadata.*;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
//import com.drew.imaging.jpeg.JpegSegmentMetadataReader;
import com.drew.metadata.exif.ExifReader;
import com.drew.metadata.iptc.IptcReader;
import java.util.concurrent.Semaphore;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.error.DJIError;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import java.lang.*;
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
import dji.common.model.LocationCoordinate2D;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.ConnectionFailSafeBehavior;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.ControlMode;
import dji.common.flightcontroller.virtualstick.*;
import dji.common.util.CommonCallbacks;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.io.File;
import java.nio.channels.FileChannel;
import java.io.PrintWriter;
import java.util.List;
import dji.sdk.products.Aircraft;
/**
 * WaypointMission Driver.
 * It supports commands related to Waypoint Mission.
 * Interact with this driver via CoAP (californium).
 * @author  Jayson Boubin
 * @version 1.0.9
 * @since   2019-12-26
 */
public class MissionDriver extends com.dji.sdk.sample.internal.controller.AuavDrivers {

	private String LOG_TAG = "MissionDriver";
	public boolean forceStop = false;
	public long TIMEOUT = 10000;
	public int MAX_TRIES = 10;
	private Properties configFile;
	private float altitude = 100.0f; //default altitude of waypoint mission
	static byte[] pic;
        public String succ = "";
        public String IP = "";
	public String fname = "../AUAVtmp/waypoints.txt";
	public String seperator = ",";
	public WaypointMission.Builder builder;
	private FlightController fc;
	private WaypointMission mission;
	private WaypointMissionFinishedAction tFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
	private WaypointMissionHeadingMode tHeadingMode=WaypointMissionHeadingMode.AUTO;
	private float tSpeed = 10.0f;
	private float tMaxSpeed = 10.0f;
	private double homeLatitude = 27.2038;
	private double homeLongitude = 77.5011;

	private List<Waypoint> waypointList = new ArrayList<>();
	private WaypointMissionOperator instance;
	private WaypointMissionOperatorListener listener;
	private static int LISTEN_PORT = 0;
	private int driverPort = 0;
	private CoapServer cs;
    private Waypoint homeWP;
	private static Logger mdLogger = Logger.getLogger(MissionDriver.class.getName());
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

	public void setLogLevel(Level l){
		mdLogger.setLevel(l);
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
	public MissionDriver() {
		try{
			mdLogger.log(Level.FINEST, "In Constructor");
			cs = new CoapServer(); //initilize the server
			InetSocketAddress bindToAddress =
				new InetSocketAddress(LISTEN_PORT);//get the address
			CoapEndpoint tmp = new CoapEndpoint(bindToAddress); //create endpoint
			cs.addEndpoint(tmp);//add endpoint to server
			tmp.start();//Start this endpoint and all its components.
			driverPort = tmp.getAddress().getPort();
			cs.add(new mdResource());
		}catch(Exception e){
		}
	}
	//extends CoapResource class
	private class mdResource extends CoapResource {
		public mdResource(){
			super("cr");
			getAttributes().setTitle("cr");
		}
		@Override
		public void handlePUT(CoapExchange ce) {
			String outLine = "";
			byte[] payload = ce.getRequestPayload();
			String inputLine = "";
			try{

				inputLine = new String(payload,"UTF-8");
			} catch(Exception uee){
				System.out.println(uee.getMessage());
			}
			String[] args = inputLine.split("-");
			boolean AUAVsim = false;
			for (String arg : args){
				if (arg.equals("dp=AUAVsim")) {
					AUAVsim = true;
				}
			}
			if(args[0].equals("dc=help")) {
				ce.respond(getUsageInfo());
			}
            else if(args[0].equals("dc=init")){
                float alt = 0;
                float lat = 0;
                float lon = 0;
                try {
					System.out.println("Receiving Home Waypoint");
				    String lats = args[1].split("=")[1];
                    String lons = args[2].split("=")[1];
                    if(lats.charAt(0) != 'n') {
                        lat = Float.parseFloat(lats);
                        System.out.println("Latitude Positive: "+lat);
                    } else {
                        lat = -1.0f * Float.parseFloat(lats.substring(1));
                        System.out.println("Latitude Negative: "+lat);
                    }
                    if(lons.charAt(0) != 'n') {
                        lon = Float.parseFloat(lons);
                        System.out.println("Longitude Positive: "+lons);

                    } else {
                        lon = -1.0f * Float.parseFloat(lons.substring(1));
                        System.out.println("Longitude Negative: "+lons);
                    }
                    alt = Float.parseFloat(args[3].split("=")[1]);
                    System.out.println("Home Lat: "+lat);
                    System.out.println("Home Lon: "+lon);
                    System.out.println("Home Alt: "+alt);
                    homeWP = new Waypoint(lat, lon, alt);
                    WaypointAction WPA = new WaypointAction(WaypointActionType.STAY, 32000);
                    homeWP.addAction(WPA);
				} catch(Exception e) {
					System.out.println(e.getMessage());
                    ce.respond("invalid waypoint format");
			    }

                Aircraft mAircraft = (Aircraft)DJISDKManager.getInstance().getProduct();
				FlightController fc = mAircraft.getFlightController();

                fc.setHomeLocationUsingAircraftCurrentLocation(new CommonCallbacks.CompletionCallback() {
            				@Override
            				public void onResult(DJIError mError) {
              					if (mError != null){
							        System.out.println("setHomeLocation Failed:"+ mError.getDescription());

              					}
            				}
    			});
                ce.respond("RTH Location Set");
            } else if(args[0].equals("dc=initWaypoint")){
				float alt = 0;
                float lat = 0;
                float lon = 0;
                try {
					System.out.println("Receiving waypoints");
				    String lats = args[1].split("=")[1];
                    String lons = args[2].split("=")[1];
                    if(lats.charAt(0) != 'n') {
                        lat = Float.parseFloat(lats);
                        System.out.println("Latitude Positive: "+lat);
                    } else {
                        lat = -1.0f * Float.parseFloat(lats.substring(1));
                        System.out.println("Latitude Negative: "+lat);
                    }
                    if(lons.charAt(0) != 'n') {
                        lon = Float.parseFloat(lons);
                        System.out.println("Longitude Positive: "+lons);

                    } else {
                        lon = -1.0f * Float.parseFloat(lons.substring(1));
                        System.out.println("Longitude Negative: "+lons);
                    }
                    alt = Float.parseFloat(args[3].split("=")[1]);
				} catch(Exception e) {
					System.out.println(e.getMessage());
                    ce.respond("invalid waypoint format");
			    }

                Waypoint w = new Waypoint(lat, lon, alt);
                WaypointAction WPA = new WaypointAction(WaypointActionType.STAY, 32000);
                w.addAction(WPA);
                w.addAction(WPA);
                w.addAction(WPA);
                w.addAction(WPA);
                //Waypoint w = new Waypoint(40.154466f, -83.193903f, 5.0f);
                //Waypoint WaypointP2 = new Waypoint(40.154443f, -83.193988f, 5.0f);

                //Waypoint WaypointP1 = new Waypoint(39.347010, -84.423896, 5.0f);
                //Waypoint WaypointP2 = new Waypoint(39.346971, -84.423899, 5.0f);

                //if (builder == null){
				builder = new WaypointMission.Builder().finishedAction(tFinishedAction)
									       .headingMode(tHeadingMode)
									       .autoFlightSpeed(tSpeed)
									       .maxFlightSpeed(tMaxSpeed)
									       .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
				/*}else{
					builder.finishedAction(tFinishedAction)
						.headingMode(tHeadingMode)
						.autoFlightSpeed(tSpeed)
						.maxFlightSpeed(tMaxSpeed)
						.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
				}*/
				//waypointList.add(WaypointP1);
                //waypointList.add(WaypointP2)

                //builder.addWaypoint(WaypointP1);
                //builder.addWaypoint(WaypointP2);

                //builder.waypointList(waypointList).waypointCount(waypointList.size());
                builder.addWaypoint(w);
				builder.addWaypoint(homeWP);
                System.out.println("InitWaypoint: The number of waypoints in builder:"+builder.getWaypointList().size());
				if (builder.getWaypointList().size() > 0){
					for(int i=0; i < builder.getWaypointList().size();i++){
                        double curlat = builder.getWaypointList().get(i).coordinate.getLatitude();
                        double curlon = builder.getWaypointList().get(i).coordinate.getLongitude();
						System.out.println(curlat + " " + curlon);
					}
				}

				ce.respond("Done");
			}else if(args[0].equals("dc=uploadMission")){
				if (builder == null){
					builder = new WaypointMission.Builder().finishedAction(tFinishedAction)
									       .headingMode(tHeadingMode)
									       .autoFlightSpeed(tSpeed)
									       .maxFlightSpeed(tMaxSpeed)
									       .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
				}else{
					builder.finishedAction(tFinishedAction)
						.headingMode(tHeadingMode)
						.autoFlightSpeed(tSpeed)
						.maxFlightSpeed(tMaxSpeed)
						.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
				}
				System.out.println("UploadMission: The number of waypoints in builder:"+builder.getWaypointList().size());

				System.out.println("UploadedMission: set up flight controller");
				if (instance == null){
		    		instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
				}

				DJIError error = instance.loadMission(builder.build());
				if (error == null){
					System.out.println("uploaded Mission Successfully");
				}else{
					System.out.println("load Mission failed:"+ error.getDescription());
				}
		    		if((error == null)&&(instance.getCurrentState() == WaypointMissionState.READY_TO_UPLOAD)){
					instance.uploadMission(new CommonCallbacks.CompletionCallback() {
                    				@Override
                    				public void onResult(DJIError error){
                        				if (error == null) {
                            					System.out.println("Mission upload successfully!");
                        				} else {
                            					System.out.println("Mission upload failed, error: " + error.getDescription() );
                        				}
                    				}
               	 			});
		 		}
				ce.respond("Done");
			}else if(args[0].equals("dc=startMission")){
	    			if (instance == null){
					instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
	    			}
		    		if(instance.getCurrentState() == WaypointMissionState.READY_TO_EXECUTE) {
					instance.startMission(new CommonCallbacks.CompletionCallback() {
                    				@Override
                    				public void onResult(DJIError error) {

							if (error != null){
								System.out.println("Mission stopped:"+error.getDescription());
							}else{
								System.out.println("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
							}
						}
                			});
		    		}

				ce.respond("Done");
			}else if(args[0].equals("dc=stopMission")){
	    			//if (instance == null){
				//	instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
	    			//}
		    		if((instance.getCurrentState() == WaypointMissionState.EXECUTING)||(instance.getCurrentState() == WaypointMissionState.EXECUTION_PAUSED)){
					instance.stopMission(new CommonCallbacks.CompletionCallback() {
                    				@Override
						public void onResult(DJIError error) {
							if (error != null){
							System.out.println("Mission stopped:"+error.getDescription());
							}
						}
					});
				}
				ce.respond("Done");
			}else if (args[0].equals("dc=pauseMission")){
	    			if (instance == null){
					instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
	    			    }
		    		if(instance.getCurrentState() == WaypointMissionState.EXECUTING) {
					instance.pauseMission(new CommonCallbacks.CompletionCallback() {
                    				@Override
                    				public void onResult(DJIError error) {
							if (error != null){
								System.out.println("Mission cannot be pasused:"+error.getDescription());
							}else{
								System.out.println("Mission Paused: " + (error == null ? "Successfully" : error.getDescription()));
							}
						}
                			});
		    		}
				ce.respond("Done");
			}
			else {
				ce.respond("Error: unknown command\n");
			}
		}
	}
        void writePic(byte[] b) {
            try{
                File f = new File("../tmp/pictmp.jpg");
                f.delete();
                MappedByteBuffer out = new RandomAccessFile("../tmp/pictmp.jpg","rw").getChannel().map(FileChannel.MapMode.READ_WRITE, 0, b.length);
                for(int j = 0; j<b.length; j++){
                    out.put(b[j]);
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }
	void writeWaypoint(byte[] b) {
	    try{
		File f = new File(fname);
		f.delete();
		MappedByteBuffer out = new RandomAccessFile(fname, "rw").getChannel().map(FileChannel.MapMode.READ_WRITE, 0, b.length);
		for(int j = 0; j<b.length; j++){
			out.put(b[j]);
		}
	    } catch(Exception e){
		e.printStackTrace();
	    }
    	}
        void writeYaml(String s){
            System.out.println("Write YAML:");
            try{
                PrintWriter p = new PrintWriter("../tmp/pictmp.yaml","UTF-8");
                p.print(s);
                p.close();
            } catch(Exception e){
                e.printStackTrace();
            }
            System.out.println("End YAML");
        }
        byte[] readByte() throws IOException {
            ServerSocket ss = new ServerSocket(12013);
            System.out.println("Server: Waiting for Connection");
            Socket s = ss.accept();
            System.out.println("Server: Connection Reached");

            //s = ss.accept();
            DataInputStream dIn = new DataInputStream(s.getInputStream());
            byte[] ret = new byte[0];
            //dIn.readInt();
            int length = dIn.readInt();
            System.out.println("Receiving "+length+" Bytes");
            if(length > 0) {
                ret = new byte[length];
                dIn.readFully(ret, 0, ret.length);
            }

            s.close();
            ss.close();
            return ret;
        }

	CommonCallbacks.CompletionCallback fddHandler = new CommonCallbacks.CompletionCallback() {
		@Override
		public void onResult(DJIError djiError) {
			if (djiError == null) {
				System.out.println(LOG_TAG+"-"+lockStr+"-success");
				drvUnsetLock();
			}
			else {System.out.println(LOG_TAG+"-"+lockStr+"-fail");
				drvUnsetLock();}
			}
		};

	private static class FlightTimerTask extends TimerTask {
		String pos;
		FlightControlData fcd;
                Timer ctrl;
                int frequency = 3;
                int count = 0;
                CoapExchange ce;
                boolean send;
		private String LOG_TAG="FlightTimeTask.FlyDroneDriver ";
		FlightTimerTask(FlightControlData input) {
			super();
			fcd = input;
		}
	        FlightTimerTask(FlightControlData input, Timer t, int freq, CoapExchange ex, boolean sendMessage) {
			super();
			fcd = input;
			ctrl=t;
			frequency = freq;
			ce = ex;
			send = sendMessage;
		}
		@Override
		public void run() {
			Aircraft aircraft = (Aircraft)DJISDKManager.getInstance().getProduct();
			FlightController fc = aircraft.getFlightController();
			fc.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
				@Override
				public void onResult(DJIError djiError) {
					if (djiError == null) {
						System.out.println("Stick Enabled");
					}
					else {
						System.out.println("Stick Not Enabled");}
				        }
                        });
			fc.sendVirtualStickFlightControlData(fcd,new CommonCallbacks.CompletionCallback() {
				@Override
				public void onResult(DJIError djiError) {
					if (djiError == null) {
						System.out.println(LOG_TAG+"Flight Control Success");
					}
					else {
						System.out.println(LOG_TAG+djiError.getDescription());
					}
				}
			});
                     	//System.out.println("Virtual Stick Mode: "+fc.isVirtualStickControlModeAvailable());
                        if (count < frequency) {
                        	count++;
                        	System.out.println("Inc Count to: "+count);
                        } else {
                            	System.out.println("Cancel Task, count = "+count);
                            	if(send)
                                	ce.respond ("FlyDroneDriver: Timertask Complete");
				ctrl.cancel();
                        }
		}
	}
	public Semaphore lockSema = new Semaphore(1);
        public String lockStr = "continue";
	public void drvSetLock(String v){
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
}
