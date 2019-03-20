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
 * Detect takes off, calibrates camera, and takes a picture
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
public class Detect extends org.reroutlab.code.auav.routines.AuavRoutines {
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
		public final String [] MODEL_NAMES;

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
					System.out.println("Detect: (Simulation)" + auavResp.getResponse() );
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
				for(int gimbalPositions = 0; gimbalPositions < 2; gimbalPositions++){

					System.out.println("Detect: In Gimbal Loop");
					//read next picture from picTrace database
					pic = readNextPic(picNum);
					picNum++;

					//read image into openCV and classify
					boolean[] classif = classify(pic);
					for(int i = 0; i<MODELS.length; i++){
							if(classif[i]) { //if pic is a selfie
								System.out.println("Detect: Found one after " + picNum);
								if (getSim().equals("AUAVsim")) {
									writeImage(pic, writeFile);
								} else {
									System.out.println("Writing Selfie After: "+picNum);
									writeImage(pic, Environment.getExternalStorageDirectory().getPath() + writeFile +MODEL_NAMES[i] + selfieNum + ".jpeg");
									selfieNum++;
								}
							} else {//if it's not a selfie*//*
								System.out.println("No Selfie After: "+picNum);
								//lower the gimbal

								if(gimbalPositions < 1) {//only move down the first time
									System.out.println("DetectRoutine: Gimbal Pos 1");

									auavLock("moveGimbalDown");
									succ = invokeDriver("org.reroutlab.code.auav.drivers.DroneGimbalDriver", "dc=dna-dp=45", auavResp.ch);
									auavSpin();
								}
								try{Thread.sleep(100);
									System.out.println("DetectRoutine: Slept");
								}catch(Exception e){}
							}
					}
				}

				//rotate
				System.out.println("DetectRoutine: Out of gimbal loop, rotating");

				auavLock("rotate");
				succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver","dc=rcw-dc="+(45),auavResp.ch);
				System.out.println("DetectRoutine: rotated, resetting gimbal");

				auavSpin();

				auavLock("resetGimbal");
				succ = invokeDriver("org.reroutlab.code.auav.drivers.DroneGimbalDriver","dc=res", auavResp.ch);
				auavSpin();
				System.out.println("DetectRoutine: Gimbal Reset");
			}
			//call fly drone to land

			auavLock("FlyDrone");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver",
													"dc=lnd", auavResp.ch );
			auavSpin();

			System.out.println("Detect: Exiting"  );

		}

		void centerDrone(double angleX, double angleY){
				String succ = "";
				if(angleX > 0){
					auavLock("rotate");
					succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver","dc=rcw-dc="+angleX,auavResp.ch);
					auavSpin();
				} else {
					auavLock("rotate");
					succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver","dc=rcc-dc="+(-1*angleX),auavResp.ch);
					auavSpin();
				}

				if(angleY > 0){
					auavLock("gimbal");
					succ = invokeDriver("org.reroutlab.code.auav.drivers.DroneGimbalDriver","dc=upa-dp="+angleY,auavResp.ch);
					auavSpin();
				} else {
					auavLock("gimbal");
					succ = invokeDriver("org.reroutlab.code.auav.drivers.DroneGimbalDriver","dc=dna-dp="+(-1*angleY),auavResp.ch);
					auavSpin();
				}
		}

		byte[] readNextPic(int picNum) {
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
                System.out.println("ReadNextpic: Returning array of size: "+pic.length);
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
		boolean[] classify(byte[] b) {
				boolean ret[] = new boolean[MODELS.length];
				if (b.length == 0) {
						return ret;
				}
				for(int i = 0; i<MODELS.length; i++){
						//create classifier by lodaing xml data
						CascadeClassifier faceDetector = null;

						try {
							// load cascade file from application resources
							File mCascadeFile = new File(Environment.getExternalStorageDirectory()
										.getPath() + "/AUAVassets/"+MODELS[i]);

							System.out.println("Detect: "+mCascadeFile.getAbsolutePath());
							if (mCascadeFile.exists() == false) {
								System.out.println( "Detect: Cascade classifier not a file");
							}

							faceDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
							faceDetector.load(mCascadeFile.getAbsolutePath());
							if (faceDetector.empty()) {
										System.out.println( "Detect: Failed to load cascade classifier");
										System.out.println( "Detect: " + mCascadeFile.getAbsolutePath());
										faceDetector = null;
							}
						} catch (Exception e) {
								e.printStackTrace();
								System.out.println("Detect: Failed to load cascade. Exception thrown: " + e);
								continue;
						}

						//load image
						Mat image = Highgui.imdecode(new MatOfByte(b), Highgui.CV_LOAD_IMAGE_UNCHANGED);

						//perform face detection (magic)
						if (faceDetector == null) {
							System.out.println("Detect: faceDetector variable is null ");
							continue; //leave classification as false
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
								System.out.println("background noise: not a detection");
								continue;
							}
							ret[i] = true;
						}

				}
				return ret;
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


		public Detect() {
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
			MODEL_NAMES = configFile.getProperty("MODEL_NAMES").split(",");

			t = new Thread (this, "Main Thread");
		}
		public String startRoutine() {
				if (t != null) {
						t.start(); return "Detect: Started";
				}
				return "Detect not Initialized";
		}
		public String stopRoutine() {
				forceStop = true;	return "Detect: Force Stop set";
		}
}
