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

import java.util.Properties;

/**
 * Selfie takes off, calibrates camera, and takes a picture
 * It dumps the image to a user provided directory, then determines
 * whether that image contains a human face
 * If it does, it writes the image to trace.data/selfies
 * Invoke this routine through external commands driver
 * <br><b>URL: coap:\\localhost:port\cr?</b>
 * <br>Example: coap:\\127.0.0.1:5117\cr?dn=rtn-dc=start-dp=Selfie-dp=../trace.data/PicTraceDriver/PicTrace
 * <br>
 * Note: all AUAV drivers have resource component "cr" for coap resource
 *
 * @author  Jayson Boubin, Yuping Liang
 * @version 1.0.3
 * @since   2017-11-09
 */
public class Selfie extends org.reroutlab.code.auav.routines.AuavRoutines {
		/**
		 *	 Check forceStop often and safely end routine if set
		 */
		public boolean forceStop = false;
		public long TIMEOUT = 10000;
		public int MAX_TRIES = 10;
		private Properties configFile;
		public final double CAMERA_FOV_HORIZ;
		public final double CAMERA_FOV_VERT;
		public final String [] MODELS;
		/**
		 *	 Routines are Java Threads.  The run() function is the
		 *	 starting point for execution.
		 * @version 1.0.1
		 * @since   2017-10-01
		 */
		public void run() {
			//This routine currently assumes AUAVSim is active

			String succ = "";

			/*reads in a parameter: picDirectory
			 *picDirectory refers to the directory where the camera will dump the images when
			 *it captures them. Pictrace then reads the images from said directory.
			 */


			String args[] = params.split("-");
			String picDirectory = "";
			String writeFile = args[0].substring(3);
			setSimOff();
			String picDriver = "org.reroutlab.code.auav.drivers.CaptureImageDriver";
			if (getSim().equals("AUAVsim")) {
					picDriver="org.reroutlab.code.auav.drivers.PicTraceDriver";

					auavLock("PicTrace");
					picDirectory = args[1].substring(3);
					succ = invokeDriver(picDriver,
						    "dc=dir-dp="+picDirectory, auavResp.ch );
					auavSpin();
					System.out.println("Selfie: (Simulation)" + auavResp.getResponse() );
			}

			auavLock("ssm");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver", "dc=ssm", auavResp.ch);
			auavSpin();

			//fly the drone up
			//set location 0,0,4 -- snap pic
			//configure fly drone driver

			auavLock("ConfigFlight");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver",
													"dc=cfg", auavResp.ch );
			auavSpin();

			//calibrate gimbal
			//auavLock("CalibrateGimbal");
			//succ = invokeDriver("org.reroutlab.code.auav.DroneGimbalDriver", "dc=cal", auavResp.ch);
			//auavSpin();

			//liftoff
			auavLock("FlyDrone");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver",
					     "dc=lft", auavResp.ch );
			auavSpin();

			//set initial location
			auavLock("Locate");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.LocationDriver",
													"dc=set-dp=0.00-dp=0.00-dp=4.00", auavResp.ch );
			auavSpin();

			byte[] pic;
			int picNum = 0, selfieNum = 1;

			//8 rotations of 45 degrees for 360 degree coverage
			for(int rotations = 0; rotations < 8; rotations++) {
				//2 gimbal positions per rotation from the horizontal (0 degrees, -45 degrees)
				int angleX = 0, angleY = 0;
				for(int gimbalPositions = 0; gimbalPositions < 2; gimbalPositions++){

					System.out.println("SelfieRoutine: In Gimbal Loop");
					//read next picture from picTrace database
					pic = readNextPic(picNum);
					picNum++;

					//read image into openCV and classify
					AnglePair classif = classify(pic);

					if(classif.isFace) { //if pic is a selfie
						System.out.println("Selfie: Found one after " + picNum);
						if (getSim().equals("AUAVsim")) {
							writeImage(pic, writeFile);
						} else {
							angleX = (int)classif.angleX;
							angleY = (int)classif.angleY;
							System.out.println("========= angleX "+angleX);
							System.out.println("========= angleY "+angleY);
							if(classif.directionX){
								auavLock("rotate");
								succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver","dc=rcw-dc="+angleX,auavResp.ch);
								auavSpin();
								angleX = -angleX;
							} else {
								auavLock("rotate");
								succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver","dc=rcc-dc="+angleX,auavResp.ch);
								auavSpin();
							}

							if(classif.directionY){
								auavLock("gimbal");
								succ = invokeDriver("org.reroutlab.code.auav.drivers.DroneGimbalDriver","dc=upa-dp="+angleY,auavResp.ch);
								auavSpin();
							} else {
								auavLock("gimbal");
								succ = invokeDriver("org.reroutlab.code.auav.drivers.DroneGimbalDriver","dc=dna-dp="+angleY,auavResp.ch);
								auavSpin();
							}

							pic = readNextPic(++picNum);
							AnglePair isSelfie = classify(pic);
							if(isSelfie.isFace){
								System.out.println("Writing Selfie After: "+picNum);
								writeImage(pic, Environment.getExternalStorageDirectory().getPath() + writeFile +"Selfie"+ selfieNum + ".jpeg");
								++selfieNum;
							} else {
								System.out.println("SelfieRoutine: False Positive");
							}
						}
						//exit loop
						break;
					} else {//if it's not a selfie*//*
						System.out.println("No Selfie After: "+picNum);
						//lower the gimbal

						if(gimbalPositions < 1) {//only move down the first time
							System.out.println("SelfieRoutine: Gimbal Pos 1");

							auavLock("moveGimbalDown");
							succ = invokeDriver("org.reroutlab.code.auav.drivers.DroneGimbalDriver", "dc=dna-dp=45", auavResp.ch);
							auavSpin();
						}
						try{Thread.sleep(100);
						    System.out.println("SelfieRoutine: Slept");
						}catch(Exception e){}
					}
				}

				//rotate
				System.out.println("SelfieRoutine: Out of gimbal loop, rotating");

				auavLock("rotate");
				succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver","dc=rcw-dc="+(45+angleX),auavResp.ch);
				System.out.println("SelfieRoutine: rotated, resetting gimbal");

				auavSpin();

				auavLock("resetGimbal");
				succ = invokeDriver("org.reroutlab.code.auav.drivers.DroneGimbalDriver","dc=res", auavResp.ch);
				auavSpin();
				System.out.println("SelfieRoutine: Gimbal Reset");
			}
			//call fly drone to land

			auavLock("FlyDrone");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver",
													"dc=lnd", auavResp.ch );
			auavSpin();

			System.out.println("Selfie: Exiting"  );

		}

		byte[] readNextPic(int picNum) {
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
						String succ = invokeDriver("org.reroutlab.code.auav.drivers.PicTraceDriver",
																			 "dc=qrb-dp="+query+"", auavResp.ch);
						auavSpin();
				} else {
						System.out.println("Selfie: GET");

						auavLock("get");
						String succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver", "dc=get", auavResp.ch);
						auavSpin();

						//There's some sort of synchrhonization error between "get" and "dld"
						//in CaptureImageV2. This sleep eliminates that issue.
						try{Thread.sleep(1000);}catch(Exception e){}

						System.out.println("Selfie: DLD");
						auavLock("dld");
						succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver", "dc=dld", auavResp.ch);
						auavSpin();

						/*auavLock("qry");
						succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver", "dc=qry-dp=select * from data where rownum() = "+picNum, auavResp.ch);
						auavSpin();*/
				}
				String imageEnc = "";

				try{
					File file = new File(Environment.getExternalStorageDirectory().getPath()+"/AUAVtmp/pictmp.dat");
					FileChannel fileChannel = new RandomAccessFile(file, "r").getChannel();
					MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
					System.out.println(buffer.isLoaded());
					System.out.println(buffer.capacity());

					pic = new byte[buffer.capacity()];
					while(buffer.hasRemaining()) {
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
		AnglePair classify(byte[] b){
				if (b.length == 0) {
						return new AnglePair(0.0, false, 0.0, false, false);
				}
				//create classifier by lodaing xml data
				CascadeClassifier faceDetector = null;
				if (getSim().equals("AUAVsim")) {
						String assetBase = System.getenv("AUAVHOME")+"/AUAVAndroid/app/src/main/assets/AUAVassets/";
						faceDetector = new CascadeClassifier(assetBase + "facedetect.lbpcascade_frontalface.xml");
						if (faceDetector.empty()) {
								System.out.println( "Selfie: Failed to load cascade classifier");
								faceDetector = null;
						}
				} else {
						try {
								// load cascade file from application resources
								File mCascadeFile = new File(Environment.getExternalStorageDirectory().getPath() +
																						 "/AUAVassets/facedetect.lbpcascade_frontalface.xml");

								if (mCascadeFile.exists() == false) {
										System.out.println( "Selfie: Cascade classifier not a file");
								}

								faceDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
								faceDetector.load(mCascadeFile.getAbsolutePath());
								if (faceDetector.empty()) {
										System.out.println( "Selfie: Failed to load cascade classifier");
										System.out.println( "Selfie: " + mCascadeFile.getAbsolutePath());
										faceDetector = null;
								}

						} catch (Exception e) {
								e.printStackTrace();
								System.out.println("Selfie: Failed to load cascade. Exception thrown: " + e);
						}

				}

				//load image
				Mat image = Highgui.imdecode(new MatOfByte(b), Highgui.CV_LOAD_IMAGE_UNCHANGED);

				//perform face detection (magic)
				if (faceDetector == null) {
					System.out.println("Selfie: faceDetector variable is null ");
					return new AnglePair(0.0,false, 0.0, false, false);
				}
				MatOfRect faceDetections = new MatOfRect();
				faceDetector.detectMultiScale(image, faceDetections);

				if(faceDetections.toArray().length > 0){
					Rect rect = (faceDetections.toArray())[0];
					//find the largest face by width to center on
					for(Rect r : faceDetections.toArray()){
						if(r.width > rect.width)
							rect = r;
					}
					System.out.println("Detected " + faceDetections.toArray().length + " Faces");

					if(rect.width < image.width()/10){ //helps get rid of false positives
						System.out.println("background noise: not selfie");
						return new AnglePair(0.0, false, 0.0, false, false);//people in the background
					}				   //and photos in the background

					double imageCenterX = (image.width()/2);
					double faceCenterX = rect.x + (rect.width/2);
					double distanceFromCenterX = Math.abs(imageCenterX - faceCenterX);
					double angleX = (distanceFromCenterX/imageCenterX) * CAMERA_FOV_VERT/2;

					System.out.println("Image CenterX: "+imageCenterX +
							   "\nRect X: "+rect.x +
							   "\nRect Width: "+rect.width +
							   "\nFace Center: "+faceCenterX+
							   "\nDistance From Center: "+distanceFromCenterX+
							   "\nAngle: "+angleX);

					double imageCenterY = (image.height()/2);
					double faceCenterY = rect.y + (rect.height/2);
					double distanceFromCenterY = Math.abs(imageCenterY - faceCenterY);
					double angleY = (distanceFromCenterY/imageCenterY) * CAMERA_FOV_HORIZ/2;

					System.out.println("Image CenterY: "+imageCenterY +
							   "\nRect Y: "+rect.y +
							   "\nRect Height: "+rect.height +
							   "\nFace Center: "+faceCenterY+
							   "\nDistance From Center: "+distanceFromCenterY+
							   "\nAngle: "+angleY);

					AnglePair ret = new AnglePair();
					ret.angleX = angleX;
					ret.angleY = angleY;
					ret.isFace = true;

					if(imageCenterY < rect.x)
						ret.directionX = true;
					else
						ret.directionY = false;
					if(imageCenterY > rect.y)
						ret.directionY = true;
					else
						ret.directionY = false;
					return ret;

				}
				return new AnglePair(0.0, false, 0.0, false, false);
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


		public Selfie() {
			configFile = new java.util.Properties();
			try{
				File cfg = new File(Environment.getExternalStorageDirectory().getPath() + "/AUAVAssets/routineConfig.cfg");
				InputStream is = new FileInputStream(cfg);
				configFile.load(is);
			} catch(Exception e){
				e.printStackTrace();
			}
			CAMERA_FOV_HORIZ = Double.parseDouble(configFile.getProperty("CAMERA_FOV_HORIZ"));
			CAMERA_FOV_VERT = Double.parseDouble(configFile.getProperty("CAMERA_FOV_VERT"));
			MODELS = configFile.getProperty("MODELS").split(",");

			t = new Thread (this, "Main Thread");
		}
		public String startRoutine() {
				if (t != null) {
						t.start(); return "Selfie: Started";
				}
				return "Selfie not Initialized";
		}
		public String stopRoutine() {
				forceStop = true;	return "Selfie: Force Stop set";
		}
		public class AnglePair{
			boolean isFace;
			boolean directionX; //true if Clockwise, false if CCW
			boolean directionY; //true if up, false if down
			double angleX; //rotation angle
			double angleY; //gimbal pitch angle
			AnglePair(){
				angleX = 0.0;
				angleY = 0.0;
				directionX = false;
				directionY = false;
			}
			AnglePair(double ax, boolean dx, double ay, boolean dy, boolean isf){
				angleX = ax;
				directionX = dx;
				angleY = ay;
				directionY = dy;
				isFace = isf;
			}
		}
}
