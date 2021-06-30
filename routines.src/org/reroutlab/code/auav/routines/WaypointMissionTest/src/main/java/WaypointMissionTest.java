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

/**
 * WaypointMissionTest is
 *
 * @author Bowen Li
 * @version 1.1.0
 * @since   2020-06-18
 */
public class WaypointMissionTest extends org.reroutlab.code.auav.routines.AuavRoutines {
		/**
		 *	 Check forceStop often and safely end routine if set
		 */
		public boolean forceStop = false;
/**
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
		private WaypointMissionOperatorListener listener;
		public long TIMEOUT = 10000;
**/
        static byte[] data;
		public String seperator = ",";
		static byte[] pic;
        public String succ = "";
        public String IP = "192.168.1.137";
		public String fname = "/AUAVtmp/waypoints.txt";
        /**
		 *	 Routines are Java Threads.  The run() function is the
		 *	 starting point for execution.
		 * @version 1.1.0
		 * @since   2020-06-18
		 */
		public void run() {
			/*reads in a parameter: picDirectory
			 *picDirectory refers to the directory where
			 *the camera will dump the images when
			 *it captures them. Pictrace then reads the
			 *images from said directory.
			 */
            String args[] = params.split("-"); //Arguments from the coap input string
			//IP = args[0];
			System.out.println("IP address is:"+IP);
			int PORT = 12013;

			config();
            		//initialize starting waypoint

			System.out.println("Reading Waypoint");
	  		//String fname= "/AUAVtmp/waypoints.txt";
			byte[] b = readWaypoint(fname);
			//String meta = "";

            auavLock("Initialize");

            succ = invokeDriver("org.reroutlab.code.auav.drivers.MissionDriver","dc=initWaypoint", auavResp.ch);
			//succ = invokeHostDriver("org.reroutlab.code.auav.drivers.MissionDriver-dc=initWaypoint",IP, auavResp.ch,true);
			auavSpin();

            //upload the mission
            auavLock("Upload");
//          succ = invokeDriver("org.reroutlab.code.auav.drivers.MissionDriver","dc=uploadMission", auavResp.ch);

            succ = invokeHostDriver("org.reroutlab.code.auav.drivers.MissionDriver-dc=uploadMission",IP, auavResp.ch,true);
			auavSpin();
            System.out.println("#####################Uploaded a mission#####################################");

            //start the mission
            auavLock("Start");
            succ = invokeHostDriver("org.reroutlab.code.auav.drivers.MissionDriver-dc=startMission",IP, auavResp.ch,true);
			auavSpin();
            System.out.println("#####################Started a mission#####################################");
            //take picture and pause mission
            //takeImg();
            auavLock("Pause");
            succ = invokeHostDriver("org.reroutlab.code.auav.drivers.MissionDriver-dc=pauseMission",IP, auavResp.ch,true);
			auavSpin();
            System.out.println("#####################Pasued a mission#####################################");

			//stop the mission
            auavLock("stop");
            succ = invokeHostDriver("org.reroutlab.code.auav.drivers.MissionDriver-dc=stopMission",IP, auavResp.ch,true);
			auavSpin();
           	System.out.println("#####################Stoped a mission#####################################");
			String receive = auavResp.getResponse();


		}
        byte[] readWaypoint(String fname){

       	    	//read in the file
	    	try{
	   		File f = new File(Environment.getExternalStorageDirectory().getPath()+fname);
			FileChannel fileChannel = new RandomAccessFile(f,"r").getChannel();
			MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY,0,fileChannel.size());
			data = new byte[buffer.capacity()];
			while(buffer.hasRemaining()){
				int remaining = data.length;
				if (buffer.remaining()<remaining){
					remaining = buffer.remaining();
				}
				buffer.get(data,0,remaining);
			}
			return data;
		   }catch (IOException e) {
	    	  	e.printStackTrace();
		   }
		return new byte[0];
	}
	//public String getData(String fname){
    		//read in the file
   		//try{
	   	//	File f = new File(Environment.getExternalStorageDirectory().getPath()+"/AUAVtmp/waypoints.txt");
	   		//System.out.print(f);
	   		//Scanner s = new Scanner(f);
	   		//while (s.hasNextLine()) {
           	   	//initialize waypoint
		   	//	String data = s.nextLine();
		   	//	String[] waypointObject = data.split(seperator);
		   	//	double lat = Double.parseDouble(waypointObject[0]); // TODO: Changeback after first round test
		   	//	double lon = Double.parseDouble(waypointObject[1]);
		   		//Waypoint w = new Waypoint(lat, lon, altitude);
		   		//Assume one action for each waypoint
		   		//if (w.addAction(new WaypointAction(WaypointActionType.STAY,1)) == false) { // TODO: add more actions after first round test
               				//System.out.println("Failed to add action to waypoint! Check the action count or setup");
        			//}
		 		//wList.add(w);
	   	//	}
    		//} catch (IOException e) {
    	   	//	e.printStackTrace();
    		//}
	//}

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
        //void config(){

            //initialize starting waypoint
        //    auavLock("Initialize");
        //    succ = invokeHostDriver("org.reroutlab.code.auav.drivers.MissionDriver-dc=initWaypoint", IP, auavResp.ch,true);
        //    auavSpin();



       	//}
	//captures image using the UAVs camera, downloads the image to the VM
	void takeImg(){
		System.out.println("taking the image entry to function..");
		System.out.println("SSM");
		auavLock("ssm");
		succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver","dc=ssm", auavResp.ch);
		auavSpin();

		System.out.println("Get");
		auavLock("Get Image");
		succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver","dc=get", auavResp.ch);
		auavSpin();

		System.out.println("DLD Full");
		auavLock("dld");
		succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver","dc=dldFull", auavResp.ch);
		auavSpin();
		System.out.println("Taking the image exit of the function..");
	}
	//Capable of sending image data to other functions

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
			String succ = invokeDriver("org.reroutlab.code.auav.drivers.PicTraceDriver",							       "dc=qrb-dp="+query+"", auavResp.ch);
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
		t = new Thread (this, "Main Thread");
	}
	public String startRoutine() {
			if (t != null) {
					t.start(); return "WaypointMissionTest: Started";
			}
			return "Detect not Initialized";
	}
	public String stopRoutine() {
			forceStop = true;	return "Detect: Force Stop set";
	}
}
