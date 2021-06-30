package com.dji.sdk.sample.internal.controller.kernels;

import com.dji.sdk.sample.internal.controller.AuavDrivers;
import java.io.IOException;
import java.io.File;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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


import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;


/**
 *
 *This class works like handler, connects all drivers
 *
 *
 */
public class LinuxKernel {
		private Level AUAVLEVEL = Level.FINE; // set AuavLEVEL
		private static Logger theLogger =
				Logger.getLogger(LinuxKernel.class.getName());//get Logger object by calling getLogger receive the name of the LinuxKernal.class'name



		HashMap n2p = new HashMap<String, String>();
		AuavDrivers[] ad = new AuavDrivers[128];
		public LinuxKernel ()  {//setup constructor
				//				:/home/cstewart/reroutlab.code/reroutlab.cstewart.code.auav/libs/CaptureImageDriver.jar:/home/cstewart/reroutlab.code/reroutlab.cstewart.code.auav/libs/ChargingBatteryDriver.jar:
				String auavHome = System.getenv("AUAVHOME");
				File opencv2411 = new File(auavHome + "/externalDLLs/libopencv_java2411.so");
				/*if (opencv2411.exists()) {
						//System.load(auavHome + "/externalDLLs/libopencv_java2411.so");
						System.load("/usr/local/share/OpenCV/java/libopencv_java2411.so");
				}
				else {
						System.out.println("Unable to load " + auavHome + "/externalDLLs/libopencv_java2411.so");
						System.exit(0);
				}*/



				String jarList = System.getProperty("java.class.path");
				long st = System.currentTimeMillis();
				String[] fullPath = jarList.split(".jar:");
				String[] jarNames = new String[fullPath.length];
				int countDrivers = 0;
				for (int x =0; x < fullPath.length;x++){
						String[] seps = fullPath[x].split("/");
						if (seps[seps.length - 1].endsWith("Driver") == true) {
								jarNames[countDrivers] = seps[seps.length - 1];
								countDrivers++;
						}
				}
				theLogger.setLevel(AUAVLEVEL); //set logger's level

				for (int x = 0; x < countDrivers; x++) {
						System.out.println("Jar: "+jarNames[x]);
						ad[x] = instantiate(jarNames[x],com.dji.sdk.sample.internal.controller.AuavDrivers.class);
						String canon = ad[x].getClass().getCanonicalName();
						n2p.put(canon,
										new String(""+ad[x].getLocalPort()+"\n" ) );
						String nick = canon.substring(canon.lastIndexOf(".")+1);
						if (n2p.containsKey(nick) == false) {
								n2p.put(nick,
												new String(""+ad[x].getLocalPort()+"\n" ) );
						}
						else {
								n2p.remove(nick);
						}
				}

				// Printing the map object locally for logging
				String mapAsString = "Active Drivers\n";

				Set keys = n2p.keySet();
				for (Iterator i = keys.iterator(); i.hasNext(); ) {
						String name = (String) i.next();
						String value = (String) n2p.get(name);
						mapAsString = mapAsString + name + " --> " + value + "\n";
				}
				theLogger.log(Level.INFO,mapAsString);

				for (int x = 0; x < countDrivers; x++) {
						// Send the map back to each object
						ad[x].setDriverMap(n2p);
						ad[x].setLogLevel(AUAVLEVEL);
						ad[x].setStartTime(st);
						try {
								ad[x].getCoapServer().start();
						}
						catch (Exception e) {
								e.printStackTrace();
								System.out.println("Unable to start: "+jarNames[x]);
						}
				}




		}

		public static void main(String args[]) throws Exception {
				String base = "";
				String myIP = "127.0.0.1";
				for (String arg : args) {
						String[] prm = arg.split("=");
						if (prm[0].equals("base") ){
								base = prm[1];
						}
						else if (prm[0].equals("myip") ){
								myIP = prm[1];
						}
				}
				LinuxKernel k = new LinuxKernel();

				if (base.equals("") == false) {
						String mapAsString = "Active Drivers\n";
						Set keys = k.n2p.keySet();
						for (Iterator i = keys.iterator(); i.hasNext(); ) {
								String name = (String) i.next();
								String value = (String) k.n2p.get(name);
								name = myIP + ":" + name;
								value = myIP + ":" + value;
								mapAsString = mapAsString + name + " --> " + value + "\n";
						}

						CoapClient client = new CoapClient("coap://"+base+":5117/cr");
						CoapResponse response = client.put("dn=add-"+mapAsString,0);//create response
						System.out.println(response);
				}

		}

		// Code taken from stackoverflow in May 2017
		// Thanks Sean Patrick Floyd
		// Documentation by Christopher Stewart
		public <T> T instantiate(final String className, final Class<T> type){
				try{
						return type.cast(Class.forName(className).newInstance());
				} catch(InstantiationException
								          | IllegalAccessException
								| ClassNotFoundException e){
						throw new IllegalStateException(e);
				}
		}
}

