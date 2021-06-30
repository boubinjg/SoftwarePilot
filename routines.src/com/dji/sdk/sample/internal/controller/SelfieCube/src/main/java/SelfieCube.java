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

//Import printWriter and JSON library.
import java.io.PrintWriter;
import org.json.simple.JSONObject;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.Date;
import java.sql.Timestamp;

import java.util.Properties;

/**
 * SelfieCube takes off, gimbal position changes four times and takes three picture and then gimbal position is reset back to original.
 * This procedure is repeated for every edge in the 3X3 cube.
 * It dumps the image to a user provided directory
 * Invoke this routine through external commands driver
 * <br><b>URL: coap:\\localhost:port\cr?</b>
 * <br>Example: coap:\\127.0.0.1:5117\cr?dn=rtn-dc=start-dp=SelfieCube-dp=/DroneImages/-dp=Drone is in front of person at center. It is indoor pic capture.
 * <br>
 * Note: all AUAV drivers have resource component "cr" for coap resource
 *
 * @author Naveen TR
 * @version 1.0.2
 * @since   2018-05-10
 */
public class SelfieCube extends org.reroutlab.code.auav.routines.AuavRoutines {
		/**
		 *	 Check forceStop often and safely end routine if set
		 */
		public boolean forceStop = false;
		String succ = ""; //string to store the success status.
		String writeFile="";
		int picNum = 0, selfieNum = 1; //Initialize the picNum and SelfieNum which will be used in run function.
		StringBuffer sb = new StringBuffer(); //StringBuffer to capture location.
		String rel = ""; //To capture the relative position of drone
		int picCount = 0; //Local PicCount maintained
		int angle = 0; //Variable to record the angle.
		Timestamp time = new Timestamp(System.currentTimeMillis());
		StringBuffer drone = new StringBuffer();
		/**
		 *	 Routine is a Java Thread.  The run() function is the
		 *	 starting point for execution.
		 */
		public void run() {
			//This routine currently assumes AUAVSim is active


			/*reads in a parameter: picDirectory
			 *picDirectory refers to the directory where the camera will dump the images when
			 *it captures them. Pictrace then reads the images from said directory.
			 */

			System.out.println("Fetching the arguments passed by user");
			String args[] = params.split("-");
			String picDirectory = "";
			writeFile = args[0].substring(3); //The folder where image needs to be written to.

			//Copy the testing conditions to StringBuffer named "drone"
			drone = new StringBuffer(args[1].substring(3));
			System.out.println("SelfieCubeRoutine: Input information = " +drone.toString());

			setSimOff();
			String picDriver = "org.reroutlab.code.auav.drivers.CaptureImageDriver";
			if (getSim().equals("AUAVsim"))
			{
			    picDriver="org.reroutlab.code.auav.drivers.PicTraceDriver";

			    auavLock("PicTrace");
			    picDirectory = args[1].substring(3);
			    succ = invokeDriver(picDriver,
				                "dc=dir-dp="+picDirectory, auavResp.ch );
			    auavSpin();
			    System.out.println("SelfieCube: (Simulation)" + auavResp.getResponse() );
			}

			//Capture image in shootPhoto mode.
			System.out.println("Capturing image using driver V2.0");
			auavLock("ssm");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver", "dc=ssm", auavResp.ch);
			auavSpin();

			System.out.println("Configuring the driver andf lifting off drone");

			// Call function to config and lift the drone.
			config_lift();

			sb.append('C');
			rel = "1_1_1"; //relative position in X-Y-Z axis.
			// Call the function to move the gimbal up/down and capture the image.
			gimbal_updwn_capture();


			//Call the horizontal slice function.
			move_lft_bck_right_fwd(); //This is like a horizontal slice covering all the state spaces.

			//Move the drone Downwards using "dwn" dc from FlydroneDriver.
                        auavLock("FlyDrone");
                        succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=dwn", auavResp.ch );
                        auavSpin();

			sb = new StringBuffer();
			sb.append('D');//append 'U' to current location.
			sb.append('W');
			sb.append('_');

			sb.append('C');
			rel = "1_1_1"; //relative position in X-Y-Z axis.
			// Call the function to move the gimbal up/down and capture the image.
			gimbal_updwn_capture();

			//Call the horizontal slice function.
			move_lft_bck_right_fwd();

			//Move the drone upwards using "ups" dc twice from FlydroneDriver.
                        auavLock("FlyDrone");
                        succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=ups", auavResp.ch );
                        auavSpin();

                        auavLock("FlyDrone");
                        succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=ups", auavResp.ch );
                        auavSpin();

			sb = new StringBuffer();
			sb.append('U'); //append 'U' to current location.
			sb.append('P');
			sb.append('_');
			sb.append('U');
			sb.append('P');
			sb.append('_');

			sb.append('C');
			rel = "1_1_1"; //relative position in X-Y-Z axis.
			// Call the function to move the gimbal up/down and capture the image.
			gimbal_updwn_capture();

			//Call the horizontal Slice function
			move_lft_bck_right_fwd(); //This is a horizontal slice covering 2D horizontal state spaces.

			/*
			// Call the function to move the drone back -> upward -> forward -> down (Something like one complete rotation)
			//move_bck_up_fwd_dwn(); //This is like a vertical slice covering all the state spaces.

			//Call the function to move the drone left -> backward -> right -> right -> forward -> left
			//move_lft_bck_right_right_fwd_left(); //This is like a horizontal slice covering all the state spaces.
			*/

			//call fly drone to land. Done with all the state computation.
			auavLock("FlyDrone");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=lnd", auavResp.ch );
			auavSpin();

			System.out.println("SelfieCube: Exiting land status="+succ  );

		}


		// This method will configure the driver, lift the driver and set location to (0,0,4).
		public void config_lift()
		{
		        //This method configures the drivers and lifts up the drone
		        auavLock("ConfigFlight");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=cfg", auavResp.ch );
			auavSpin();

			//calibrate gimbal
			//auavLock("CalibrateGimbal");
			//succ = invokeDriver("org.reroutlab.code.auav.DroneGimbalDriver", "dc=cal", auavResp.ch);
			//auavSpin();

			System.out.println("Lifting off the Drone using Flydrone");
			//liftoff
			auavLock("FlyDrone");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=lft", auavResp.ch );
			auavSpin();

			//Check the lift-off status.
			System.out.println("Lift off complete with status="+succ);

			//set initial location
			auavLock("Locate");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.LocationDriver", "dc=set-dp=0.00-dp=0.00-dp=4.00", auavResp.ch );
			auavSpin();
		}



		//Function to capture and write image to Edge device three times.
		public void capture_thrice()
		{
		    byte[] pic;
		    // Capture three images. So iterate till 3.
		    for(int i = 0 ; i < 2 ; i++)
		    {

                        System.out.println("SelfieCubeRoutine: In Gimbal Loop");
                        //read next picture from picTrace database
                        pic = readNextPic(picNum);
                        picNum++;

                        if (getSim().equals("AUAVsim"))
			{
                            writeImage(pic, writeFile);
                        }
			else
			{

                            System.out.println("Writing Selfie After: "+picNum);
                            //Environment.getExternalStorageDirectory() is Android command to fetch the path to store image.

			    int j = i+1; //Variable to store the picture number.
			    picCount = j;
                            writeImage(pic, Environment.getExternalStorageDirectory().getPath() + writeFile +"SelfieCube"+ selfieNum +"_"+ sb.toString()+ j + ".jpeg");
                            writeToJSONFile();
			    ++selfieNum; //increment the selfieNum
                        }
		    }

                    try
		    {
			Thread.sleep(100);
                        System.out.println("SelfieCubeRoutine: Slept");
                    }
		    catch(Exception e){}
		}

		//Write a function to write the details to a JSON File.
		public void writeToJSONFile()
		{
		    JSONObject jo = new JSONObject();
		    jo.put("Image_Description", drone.toString());
		    jo.put("Relative_Position", rel);
		    jo.put("Path_taken", sb.toString());
		    jo.put("angle", angle);
		    jo.put("LocalPicCount", picCount);
		    jo.put("GlobalPicCount", selfieNum);
		    time = new Timestamp(System.currentTimeMillis()); //get the current time the image being taken.
		    jo.put("curTime", time);

		    //Write to output JSON_file using the current android working directory.
		    //File file;
		    try(FileWriter file = new FileWriter(Environment.getExternalStorageDirectory().getPath()+writeFile+"JSONfile"+selfieNum+".json"))
		    {
			file.write(jo.toString());
			System.out.println("SelfieCubeRoutine: Write success");
			System.out.println("SelfieCubeRoutine: Written contents="+jo);
		    }
		    catch(Exception e)
		    {
			System.out.println("SelfieCubeRoutine: FileNot Found");
			e.printStackTrace();
		    }
		    //PrintWriter pw = new PrintWriter(file);
		    //pw.write(jo.toJSONString());
		    //pw.flush();
		    //pw.close();
		}





		//Function to move gimbal up/Down and capture picture.
		// After this is done, gimbal would be reset back.
		public void gimbal_updwn_capture()
		{
		    //DJI Spark's Gimbal only move downwards (no upward movement)
		    //3 gimbal positions per rotation from the horizontal (0 degrees, -15 degrees, and -30 degrees)
		    for(int gimbalPositions = 0; gimbalPositions < 3; gimbalPositions++)
		    {

		    System.out.println("SelfieCubeRoutine: In Gimbal Loop");
		    //Call the function to capture the image thrice.
		    capture_thrice();


		    //Moving the gimbal down by 15 degrees.
		    auavLock("moveGimbalDown");
		    succ = invokeDriver("org.reroutlab.code.auav.drivers.DroneGimbalDriver", "dc=dna-dp=15", auavResp.ch);
		    auavSpin();
		    System.out.println("Gimbal moved down by 15 degrees, res="+succ);
		    angle -= 15;

		    }
		    //Let the thread go to sleep.
		    try
		    {
		        Thread.sleep(100);
		        System.out.println("SelfieCubeRoutine: Slept");
		    }
		    catch(Exception e){}

		    //Resetting the gimbal
	            System.out.println("SelfieCubeRoutine: Driver to reset the gimbal");

		    angle =0; //reset the global angle parameter.
		    auavLock("resetGimbal");
		    succ = invokeDriver("org.reroutlab.code.auav.drivers.DroneGimbalDriver","dc=res", auavResp.ch);
		    auavSpin();
		    System.out.println("SelfieCubeRoutine: Gimbal Reset status="+succ);
		}

		//Function to move the drone back, upwards, forward and down. (Vertical Slice and visiting all the state spaces)
		public void move_bck_up_fwd_dwn()
		{
		   	//Move the drone backwards using "bck" dc from FlydroneDriver.
                        auavLock("FlyDrone");
                        succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=bck", auavResp.ch );
                        auavSpin();

			sb.append('B'); //append 'B' to current location.
			//Capture the images at this position
			gimbal_updwn_capture();

		   	//Move the drone upwards using "ups" dc from FlydroneDriver.
                        auavLock("FlyDrone");
                        succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=ups", auavResp.ch );
                        auavSpin();

			sb.append('U'); //append 'U' to current location.
			//Capture the images at this position
			gimbal_updwn_capture();

		   	//Move the drone forwards using "fwd" dc from FlydroneDriver.
                        auavLock("FlyDrone");
                        succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=fwd", auavResp.ch );
                        auavSpin();

			sb.append('F'); //append 'F' to current location.
			//Capture the images at this position.
			gimbal_updwn_capture();


		   	//Move the drone downwards using "dwn" dc from FlydroneDriver.
                        auavLock("FlyDrone");
                        succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=dwn", auavResp.ch );
                        auavSpin();

			//At this position the drone is brought back to it's original position.
		}


		//Function to move the drone left -> back -> right -> forward. (Horizontal 2D slice covering 4 corners).
		public void move_lft_bck_right_fwd()
                {

                        //Move the drone left using "lef" dc from FlydroneDriver.
                        auavLock("FlyDrone");
                        succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=lef", auavResp.ch );
                        auavSpin();

			sb.append('L'); //append 'L' to current location string.
			rel = "0_1_1";
                        //Capture the images at this position
                        gimbal_updwn_capture();

                        //Move the drone upwards using "bck" dc from FlydroneDriver.
                        auavLock("FlyDrone");
                        succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=bck", auavResp.ch );
                        auavSpin();

			sb.append('B'); //append 'B' to current location string.
			rel = "0_1_0";
                        //Capture the images at this position
                        gimbal_updwn_capture();

                        //Move the drone rightwards using "rgh" dc from FlydroneDriver.
                        auavLock("FlyDrone");
                        succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=rgh", auavResp.ch );
                        auavSpin();

			sb.append('R'); //append 'R' to current location string.
			rel = "1_1_0";
                        //Capture the images at this position.
                        gimbal_updwn_capture();

                        //Move the drone forwards using "fwd" dc from FlydroneDriver.
                        auavLock("FlyDrone");
                        succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=fwd", auavResp.ch );
                        auavSpin();

                        //At this position the drone is brought back to it's original position.
                }


		//Function to Capture the picture Three times.
		public void Capture_thrice_gimbal()
		{
		     gimbal_updwn_capture();
		     gimbal_updwn_capture();
		     gimbal_updwn_capture();
		}

		//Function to move the drone left, back, right, right, forward and left. (Horizontal Slice & visiting all state spaces)
                public void move_lft_bck_right_right_fwd_left()
                {
                        //Move the drone left using "lef" dc from FlydroneDriver.
                        auavLock("FlyDrone");
                        succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=lef", auavResp.ch );
                        auavSpin();

                        //Capture the images at this position
                        gimbal_updwn_capture();

                        //Move the drone upwards using "bck" dc from FlydroneDriver.
                        auavLock("FlyDrone");
                        succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=bck", auavResp.ch );
                        auavSpin();

                        //Capture the images at this position
                        gimbal_updwn_capture();

                        //Move the drone rightwards using "rgh" dc from FlydroneDriver.
                        auavLock("FlyDrone");
                        succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=rgh", auavResp.ch );
                        auavSpin();

                        //Capture the images at this position.
                        gimbal_updwn_capture();

			//Move the drone rightwards using "rgh" dc from FlydroneDriver.
                        auavLock("FlyDrone");
                        succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=rgh", auavResp.ch );
                        auavSpin();

                        //Capture the images at this position.
                        gimbal_updwn_capture();


                        //Move the drone forwards using "fwd" dc from FlydroneDriver.
                        auavLock("FlyDrone");
                        succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=fwd", auavResp.ch );
                        auavSpin();

			//Capture the images at this position.
                        gimbal_updwn_capture();


			//Move the drone leftwards using "lef" dc from FlydroneDriver.
                        auavLock("FlyDrone");
                        succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=lef", auavResp.ch );
                        auavSpin();


                        //At this position the drone is brought back to it's original position.
                }


		//Function to readpic by the drone
		byte[] readNextPic(int picNum)
		{
		    byte[] pic = new byte[0];
		    //byte buffer for reading images
		    byte[] buff = new byte[1024];

		    if (getSim().equals("AUAVsim")) {
		    //Select images from picTrace database
		    String query = "SELECT * FROM data WHERE rownum() = "+ picNum;
		    //socket for reading image
		    Socket client = null;

		    //call picTrace with query string to get next image
		    auavLock("PicTrace");
		    System.out.println("Envoking Pictrace driver in sim");
		    String succ = invokeDriver("org.reroutlab.code.auav.drivers.PicTraceDriver", "dc=qrb-dp="+query+"", auavResp.ch);
						auavSpin();
		    }
		    else
		    {
			System.out.println("Selfie: GET");

			auavLock("get");
			System.out.println("ssm: calling get from CaptureImageDriver");
			String succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver", "dc=get", auavResp.ch);
			auavSpin();

			//There's some sort of synchrhonization error between "get" and "dld"
			//in CaptureImageV2. This sleep eliminates that issue.
			try{Thread.sleep(1000);}catch(Exception e){}

			System.out.println("Selfie: DLD");
			auavLock("dld");
			System.out.println("Calling download..");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver", "dc=dld", auavResp.ch);
					    auavSpin();

			/*auavLock("qry");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver", "dc=qry-dp=select * from data where rownum() = "+picNum, auavResp.ch);
					     auavSpin();*/
		    }
		    String imageEnc = "";

		    try
		    {
			File file = new File(Environment.getExternalStorageDirectory().getPath()+"/AUAVtmp/pictmp.dat");
			FileChannel fileChannel = new RandomAccessFile(file, "r").getChannel();
			MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
			System.out.println(buffer.isLoaded());
			System.out.println(buffer.capacity());

			pic = new byte[buffer.capacity()];
			while(buffer.hasRemaining())
			{
			    int remaining = pic.length;
			    if(buffer.remaining() < remaining)
				remaining = buffer.remaining();
			    buffer.get(pic, 0, remaining);
			    System.out.println("Buffer Remaining: " + remaining);
			}

			file.delete();
			System.out.println("Selfie: done reading from file");
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


		public SelfieCube() {

			t = new Thread (this, "Main Thread");
		}
		public String startRoutine() {
				if (t != null) {
						t.start(); return "SelfieCube: Started";
				}
				return "SelfieCube not Initialized";
		}
		public String stopRoutine() {
				forceStop = true;	return "SelfieCube: Force Stop set";
		}
}
