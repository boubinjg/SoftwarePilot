package org.reroutlab.code.auav.routines;

import java.util.HashMap;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;

//sockets
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.Socket;
import java.net.ServerSocket;

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

/**
 * FaceDetect takes off, calibrates camera, and takes a picture
 * It dumps the image to a user provided directory, then determines
 * whether that image contains a human face
 * If it does, it writes the image to trace.data/selfies
 * Invoke this routine through external commands driver
 * <br><b>URL: coap:\\localhost:port\cr?</b>
 * <br>Example: coap:\\127.0.0.1:5117\cr?dn=rtn-dc=start-dp=FaceDetect-dp=../trace.data/PicTraceDriver/PicTrace
 * <br>
 * Note: all AUAV drivers have resource component "cr" for coap resource
 *
 * @author  Jayson Boubin, Yuping Liang
 * @version 1.0.3
 * @since   2017-11-09
 */
public class FaceDetect extends org.reroutlab.code.auav.routines.AuavRoutines{
		/**
		 *	 Check forceStop often and safely end routine if set
		 */
		public boolean forceStop = false;
		public long TIMEOUT = 10000;
		/**
		 *	 Routines are Java Threads.  The run() function is the 
		 *	 starting point for execution. 
		 * @version 1.0.1
		 * @since   2017-10-01			 
		 */
		public void run() {	
			//This routine currently assumes AUAVSim is active
			
			//this loads the JNI for tensorflow java on linux (assumes jar is in $AUAVHome/routines

			
			
			String succ = "";
			/*reads in a parameter: picDirectory
			 *picDirectory refers to the directory where the camera will dump the images when
			 *it captures them. Pictrace then reads the images from said directory.
			 */
			
			String args[] = params.split("-");
			String picDirectory = "";
			String writeDir = args[0].substring(3);
			setSimOff();
			String picDriver = "org.reroutlab.code.auav.drivers.CaptureImageDriver";
			if (getSim().equals("AUAVsim")) {
					picDriver="org.reroutlab.code.auav.drivers.PicTraceDriver";

					auavLock("PicTrace");
					picDirectory = args[1].substring(3);					
					succ = invokeDriver(picDriver,
						    "dc=dir-dp="+picDirectory, auavResp.ch );
					auavSpin();
					System.out.println("FaceDetect: (Simulation)" + auavResp.getResponse() );
			}

			//fly the drone up 
			// set location 0,0,4 -- snap pic                                                                                             
			auavLock("ConfigFlight");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver",
													"dc=cfg", auavResp.ch );
			auavSpin();

			auavLock("FlyDrone");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver",
					     "dc=lft", auavResp.ch );
			auavSpin();
			auavLock("Locate");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.LocationDriver",
													"dc=set-dp=0.00-dp=0.00-dp=4.00", auavResp.ch );
			auavSpin();

			System.out.println("FaceDetect: " + auavResp.getResponse() );
			
			byte[] pic;
			int picNum = 1;
			System.out.println("Querying PicTrace for image data for image: "+picNum);
			
			//read next picture from picTrace database
			pic = readNextPic(picNum);
				
			//read image into openCV and classify
			if((pic.length > 0)&& (classify(pic) == true) ){
					System.out.println("It's a selfie");
					writeImage(pic, Environment.getExternalStorageDirectory().getPath() + writeDir);
			}
			else {
					System.out.println("Not a selfie");
			}


			//call fly drone to land 
			auavLock("FlyDrone");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver",
													"dc=lnd", auavResp.ch );
			auavSpin();
			System.out.println("FaceDetect: Exiting"  );
			
		}

		byte[] readNextPic(int picNum){
				byte[] pic = new byte[0];		
				//byte buffer for reading images	
				byte[] buff = new byte[1024];	
			
				if (getSim().equals("AUAVsim") ) {
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
						auavLock("CaptureImage");
						System.out.println("######################### Locking on CaptureImage #########################");
						System.out.println("Envoking Capture Image Driver");
						String succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageDriver", "dc=get", auavResp.ch);
						auavSpin();
						System.out.println("######################### CaptureImage Unlocked ###########################");
				}
				try{			
						Socket client = new Socket("127.0.0.1", 44044); //connect to pictrace driver
						int k = -1;
						//read in byte array 1024 bytes at a time
						while((k = client.getInputStream().read(buff, 0, buff.length)) > -1){
								byte[] tbuff = new byte[pic.length + k];
								System.arraycopy(pic, 0, tbuff, 0, pic.length);
								System.arraycopy(buff, 0, tbuff, pic.length, k);
								pic = tbuff;
						}
						System.out.println(pic.length + "Bytes read from Camera Driver");
						client.close();
				} catch(Exception e){
						System.out.println("Problem reading from Camera Driver");
						e.printStackTrace();
				}
				
				return pic;
		}
		//write image stored in byte array pic in JPEG format to specified file location	
		void writeImage(byte[] pic, String fileLocation){
				try{
						OutputStream out = new FileOutputStream(fileLocation);
						out.write(pic);
						out.flush();
						out.close();
				} catch(Exception e){
						System.out.println("Problem writing image");
						e.printStackTrace();
				}
		}
		boolean classify(byte[] b){
				//create classifier by lodaing xml data
				CascadeClassifier faceDetector = null;
				if (getSim().equals("AUAVsim")) {
						String assetBase = System.getenv("AUAVHOME")+"/AUAVAndroid/app/src/main/assets/AUAVassets/"; 
						faceDetector = new CascadeClassifier(                        
													 assetBase + "facedetect.lbpcascade_frontalface.xml"); 
						if (faceDetector.empty()) {
								System.out.println( "FaceDetect: Failed to load cascade classifier");
								faceDetector = null;
						} 
				}
				else {
						//faceDetector = new CascadeClassifier(
						//							 "openCV/haarcascade_frontalface_default.xml"); 
						// Load native library after(!) OpenCV initialization
						//System.loadLibrary("detection_based_tracker");

						try {
								// load cascade file from application resources        
								File mCascadeFile = new File(Environment.getExternalStorageDirectory().getPath() + 
																						 "/AUAVassets/facedetect.lbpcascade_frontalface.xml");

								if (mCascadeFile.exists() == false) {
										System.out.println( "FaceDetect: Cascade classifier not a file");
								}

								faceDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
								faceDetector.load(mCascadeFile.getAbsolutePath());
								if (faceDetector.empty()) {
										System.out.println( "FaceDetect: Failed to load cascade classifier");
										System.out.println( "FaceDetect: " + mCascadeFile.getAbsolutePath());
										faceDetector = null;
								} 

						} catch (Exception e) {
								e.printStackTrace();
								System.out.println("FaceDetect: Failed to load cascade. Exception thrown: " + e);
						}

				}

				//load image
				Mat image = Highgui.imdecode(new MatOfByte(b), Highgui.CV_LOAD_IMAGE_UNCHANGED);
				
				//perform face detection (magic)
				if (faceDetector == null) {
					System.out.println("FaceDetect: faceDetector variable is null ");
					return false;
				}
				MatOfRect faceDetections = new MatOfRect();
				faceDetector.detectMultiScale(image, faceDetections);
    		
				return faceDetections.toArray().length > 0;
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


		public FaceDetect() {t = new Thread (this, "Main Thread");	}
		public String startRoutine() {
				if (t != null) {
						t.start(); return "FaceDetect: Started";
				}
				return "FaceDetect not Initialized";
		}
		public String stopRoutine() {
				forceStop = true;	return "FaceDetect: Force Stop set";
		}
		

		
}
