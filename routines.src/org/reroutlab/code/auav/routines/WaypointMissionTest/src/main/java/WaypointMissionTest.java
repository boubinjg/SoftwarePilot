package org.reroutlab.code.auav.routines;

import java.util.HashMap;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;

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
import java.util.*;

import dji.common.util.CommonCallbacks;
import dji.common.error.DJIError;

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
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.sdkmanager.DJISDKManager;
import java.lang.*;
/**
 * WaypointMission is a routine that
 * 1) Reads in a waypoint file
 * 2) constructs waypoint objects for each item
 * 3) creates a waypoint mission with 1 waypoint and no finish action
 * 4) executes that mission
 * 5) create a mission based on the neighbors of the current waypoint
 *
 * @author Bowen Li
 * @version 1.0.0
 * @since   2020-1-27
 */
public class WaypointMissionTest extends org.reroutlab.code.auav.routines.AuavRoutines {
		/**
		 *	 Check forceStop often and safely end routine if set
		 */
		public boolean forceStop = false;
		public long TIMEOUT = 10000;
		public int MAX_TRIES = 10;
		private Properties configFile;
		private float altitude = 100.0f; //default altitude of waypoint mission
		private float mSpeed = 10.0f;	 //default speed

        static byte[] pic;
        public String succ = "";
        public String IP = "";

	//public String csvFile = "/home/SoftwarePilot/huh/test.txt";
	public String line = "";
	public String seperator = ",";
	public static WaypointMission.Builder builder;
	private WaypointMission mission;
	//public static WaypointMission.Builder builder;
	private WaypointMissionOperator instance;
	private WaypointMissionOperatorListener listener;// = new WaypointMissionOperatorListener();
	/* {
		@Override
		public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) {
			System.out.println("Upload finished: " + (uploadEvent == null ? "Success!" : uploadEvent.getError().getDescription()));
		}
		@Override
		public void onExecutionStart() {
			System.out.println("Execution started");
		}

		@Override
		public void onExecutionFinish(DJIError error) {
			System.out.println("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
		}
	};*/
        /**
		 *	 Routines are Java Threads.  The run() function is the
		 *	 starting point for execution.
		 * @version 1.0.1
		 * @since   2018-5-13
		 */
		public void run() {
			/*reads in a parameter: picDirectory
			 *picDirectory refers to the directory where
			 *the camera will dump the images when
			 *it captures them. Pictrace then reads the
			 *images from said directory.
			 */

            String args[] = params.split("-"); //Arguments from the coap input string
            //config();

            //takes off the UAV
            //auavLock("Takeoff");
            //succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=lft", auavResp.ch);
            //auavSpin();

            //lands the UAV
            //auavLock("land");
            //succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=lnd", auavResp.ch);
            //auavSpin()

	    //[0] Waypoint# (int)
	    //[1] GPS Lat (double)
	    //[2] GPS Lon (double)
	    //[3] action (String)
	    //[4]-[7] NSWE (int)
	    //ArrayList<String[]> wObjectList = new ArrayList<String[]>();
	    String currentW;	// index indicating current waypoint #
	    List<Waypoint> wList = new ArrayList<>();

        // read in the file
	    try{
		   File f = new File(Environment.getExternalStorageDirectory().getPath()+"/AUAVtmp/waypoints.txt");

		   System.out.print(f);
		   Scanner s = new Scanner(f);
		   while (s.hasNextLine()) {
               // initialize waypoint
			   String data = s.nextLine();
			   String[] waypointObject = data.split(seperator);
			   double lat = Double.parseDouble(waypointObject[0]); // TODO: Changeback after first round test
			   double lon = Double.parseDouble(waypointObject[1]);
			   //String act = waypointObject[3];
			   //wObjectList.add(waypointObject);
			   Waypoint w = new Waypoint(lat, lon, altitude);
			   //Assume one action for each waypoint
			   if (w.addAction(new WaypointAction(WaypointActionType.STAY,1)) == false) { // TODO: add more actions after first round test
                   System.out.println("Failed to add action to waypoint! Check the action count or setup");
               }
			   wList.add(w);

		   }

	    } catch (IOException e) {
	    	   e.printStackTrace();
	    }
	    //System.out.println("Finish add waypointList");
	    //Create a waypoint mission with the first waypoint
	    //currentW = "0";
        WaypointMission wMission = null;

	    builder = new WaypointMission.Builder();
	    builder.autoFlightSpeed(1f);
	    builder.maxFlightSpeed(15f);
	    builder.setExitMissionOnRCSignalLostEnabled(false);
	    builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
	    builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
	    builder.headingMode(WaypointMissionHeadingMode.AUTO);
	    //get the first waypoint
	    //builder.addWaypoint(wList.get(0));
	    
	    builder.finishedAction(WaypointMissionFinishedAction.NO_ACTION);
	    builder.repeatTimes(0);

            //System.out.println("finish before mission built");


	    builder.waypointList(wList).waypointCount(wList.size());

        // checkParameters() is compatible with waypoint mission object
        /*
	    if(builder.checkParameters() == null){
	    	wMission = builder.build();
            //System.out.println("Finish build mission");
	    } else{
	    	    System.out.print("check here: "+builder.checkParameters());
	    }
        */

        wMission = builder.build();
        DJIError checkError = wMission.checkParameters();
        if (checkError != null) {
            System.out.println("here:"+checkError.getDescription());
	}
	    //Instantiate a mission operator
	    //WaypointMissionOperator wMissionOperator = new WaypointMissionOperator();
	    if (instance == null){
		    instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
	    }
	    int count =1;
	    while(count==1) {	//TODO: add conditions to loop argument
		    count=count-1;
		    DJIError errorss = instance.loadMission(wMission);
		    //Upload the mission if check conditions are satisfied
		    try{
		    		Thread.sleep(3000);
			}catch(Exception e){
				System.out.println(e);
			}
		    if(errorss == null)
		    {
			    instance.uploadMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error)
		    {
                        if (error == null) {
                            System.out.println("Mission upload successfully!");
                        } else {
                            System.out.println("Mission upload failed, error: " + error.getDescription() );
                        }
                    }
               	 });

		 }/*else{
			 System.out.println("check here:"+errorss.getDescription()+" Cannot upload a mission to Operator"+builder.checkParameters()+"checkStatus:"+instance.getCurrentState());
		 }*/
		// }//else{
			 //System.out.println("check here:"+errorss.getDescription()+" Cannot upload a mission to Operator");
		 //}

		 try{
			 Thread.sleep(3000);
		    }catch(Exception e){
			    System.out.print(e);
		    }
		// System.out.println("check errorss:"+errorss+" check currentState:"+instance.getCurrentState());
		  // System.out.println("Mission uploaded");	  
	       	    //Execute the mission
		    if(instance.getCurrentState() == WaypointMissionState.READY_TO_EXECUTE) {
			    instance.startMission(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        System.out.println("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
                    }
                });
		    }
		    if(instance!=null && listener!=null){
		    	    instance.addListener(listener);
		    }
	    }


        }
	//TODO:Missing stop mission section
	//
	//
        //captures image using the UAVs camera, downloads the image to the VM
         void takeImg(boolean full){
                System.out.println("taking the image entry to function..");
                System.out.println("SSM");
                auavLock("ssm");
                succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver","dc=ssm", auavResp.ch);
                auavSpin();

                System.out.println("Get");
                auavLock("Get Image");
                succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver","dc=get", auavResp.ch);
                auavSpin();

                String dld;
                if(full){
                    System.out.println("DLD Full");
                    dld = "dc=dldFull";
                } else {
                    System.out.println("DLD");
                    dld = "dc=dld";
                }
                auavLock("dld");
                succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver",dld, auavResp.ch);
                auavSpin();
        }
        //reads a full 4k image from the VM to local memory
        byte[] read4k(){
            try {
                File file = new File(Environment.getExternalStorageDirectory().getPath()+"/AUAVtmp/fullPic.JPG");
                //File file = new File("../tmp/pictmp.jpg");
                FileChannel fileChannel = new RandomAccessFile(file, "r").getChannel();
                MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
                pic = new byte[buffer.capacity()];
                while(buffer.hasRemaining()){
                    int remaining = pic.length;
                    if(buffer.remaining() < remaining){
                        remaining = buffer.remaining();
                    }
                    buffer.get(pic, 0, remaining);
                }
                return pic;
            } catch(Exception e){
                e.printStackTrace();
            }
            return new byte[0];
        }
        //Capable of sending image data to other functions
         public void sendToPort(byte[] b, String IP, int port) throws IOException{
            try {
                Thread.sleep(3000);
            } catch(Exception e){

            }
            System.out.println("Client: Trying To Connect");
            Socket socket = new Socket(IP, port);
            System.out.println("Client: Connected");
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeInt(b.length);
            dos.write(b);

            socket.close();
        }
        //configures the camera and flight system
        void config(){
            setSimOff();

			//auavLock("ssm");
			//succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver", "dc=ssm", auavResp.ch);
			//auavSpin();

			auavLock("ConfigFlight");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=cfg", auavResp.ch);
			auavSpin();
        }
        //reads the last captured preview image from the UAV
       	byte[] readPreview(int picNum) {
				byte[] pic = new byte[0];
				//byte buffer for reading images
				//byte[] buff = new byte[1024];

				if (getSim().equals("AUAVsim")) {
						//Select images from picTrace database
						String query = "SELECT * FROM data WHERE rownum() = "+ picNum;
						//socket for reading image
						Socket client = null;

						//call picTrace with query string to get next image
						auavLock("PicTrace");
						System.out.println("Envoking Pictrace driver in sim");
						String succ = invokeDriver("org.reroutlab.code.auav.drivers.PicTraceDriver",
																			 "dc=qrb-dp="+query+"", auavResp.ch);
						auavSpin();
				} else {
						System.out.println("Detect: GET");

						auavLock("get");
						String succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver", "dc=get", auavResp.ch);
						auavSpin();

						//There's some sort of synchrhonization error between "get" and "dld"
						//in CaptureImageV2. This sleep eliminates that issue.
						try{Thread.sleep(1000);}catch(Exception e){}

						System.out.println("Detect: DLD");
						auavLock("dld");
						succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver", "dc=dld", auavResp.ch);
						auavSpin();

				}
				String imageEnc = "";

				try{
					File file = new File(Environment.getExternalStorageDirectory().getPath()+"/AUAVtmp/pictmp.dat");
					FileChannel fileChannel = new RandomAccessFile(file, "r").getChannel();
					MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
					System.out.println(buffer.isLoaded());
					System.out.println(buffer.capacity());

					pic = new byte[buffer.capacity()];
					while(buffer.hasRemaining()){
						int remaining = pic.length;
						if(buffer.remaining() < remaining)
							remaining = buffer.remaining();
						buffer.get(pic, 0, remaining);
						System.out.println("Buffer Remaining: " + remaining);
					}
					file.delete();
					System.out.println("Detect: done reading from file");
				} catch(Exception e){
					e.printStackTrace();
				}
				//convert file to string
				imageEnc = new String(pic);
				//convert string to byte array (allows for delimited files)
				pic = base64ToByte(imageEnc);

				return pic;
		}
		public byte[] base64ToByte(String str){
			byte[] ret = new byte[0];
			try{
				ret = Base64.decode(str, Base64.DEFAULT);
			} catch(Exception e){
				e.printStackTrace();
			}
			return ret;
		}
		//write image stored in byte array pic in JPEG format to specified file location
		void writeImage(byte[] pic, String fileLocation){
				try {
						OutputStream out = new FileOutputStream(fileLocation);
						out.write(pic);
						out.flush();
						out.close();
				} catch(Exception e) {
						System.out.println("Problem writing image");
						e.printStackTrace();
				}
		}

		//  The code below is mostly template material
		//  Most routines will not change the code below
		//
		//
		//
		//
		//
		//  Christopher Stewart
		//  2017-10-1
		//

		private Thread t = null;


		public WaypointMissionTest() {
			//configFile = new java.util.Properties();
			/*try{
				File cfg = new File(Environment.getExternalStorageDirectory().getPath() + "/AUAVAssets/routineConfig.cfg");
				InputStream is = new FileInputStream(cfg);
				configFile.load(is);
			} catch(Exception e){
				e.printStackTrace();
			}

			CAMERA_FOV_HORIZ = Double.parseDouble(configFile.getProperty("CAMERA_FOV_HORIZ"));
			CAMERA_FOV_VERT = Double.parseDouble(configFile.getProperty("CAMERA_FOV_VERT"));
			MODELS = configFile.getProperty("MODELS").split(",");
			MODEL_NAMES = configFile.getProperty("MODEL_NAMES").split(",");
            */
			t = new Thread (this, "Main Thread");
		}
		public String startRoutine() {
				if (t != null) {
						t.start(); return "WaypointMission: Started";
				}
				return "Detect not Initialized";
		}
		public String stopRoutine() {
				forceStop = true;	return "WaypointMission: Force Stop set";
		}
}
