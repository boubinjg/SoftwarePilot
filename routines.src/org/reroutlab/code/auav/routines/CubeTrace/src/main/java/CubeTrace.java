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
 * @author  Jayson Boubin
 * @version 1.0.0
 * @since   2018-5-13
 */
public class CubeTrace extends org.reroutlab.code.auav.routines.AuavRoutines {
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
        byte[] pic;
		public int picNum = 0, selfieNum = 1;
        public String succ = "";
        public float heading = 0;
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

			String args[] = params.split("-");
			String picDirectory = "";
			String writeFile = args[0].substring(3);
			Loc pos = new Loc(0,0,0);
			//Configure the shoot mode, flight, and sim
			config();

            //take off and move to initial location:
            liftOff();

	        //fixes any rotation from the original heading due to enironmental factors
	        fixOffset();


		    for(int i = 0; i<4; ++i) {
		        boolean up = (i%2 == 0);
                lrFive("rgh", pos, up, writeFile); //left
                upd(up, pos, writeFile); //up if true, else down
                lrFive("lef", pos, up, writeFile); //right
                upd(up, pos, writeFile);
                lrFive("rgh", pos, up, writeFile);
                upd(up, pos, writeFile);
                lrFive("lef", pos, up, writeFile);
                upd(up, pos, writeFile);
                lrFive("rgh", pos, up, writeFile);

                byte[] pic = readNextPic(picNum);
                writeImage(pic, Environment.getExternalStorageDirectory().getPath() + writeFile+"TracePic"+picNum+".jpeg");
                picNum++;

                rcc(90);
                heading += 90;
                if(heading > 180){
                    heading -= 360;
                }
            }
			auavLock("FlyDrone");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=lnd", auavResp.ch );
			auavSpin();

			System.out.println("CubeTrace: Exiting");
		}
		void setLoc(Loc pos){
            auavLock("Locate");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.LocationDriver", "dc=set-dp="+pos.x+".00-dp="+pos.y+".00-dp="+pos.z+".00", auavResp.ch );
			auavSpin();
        }
		void lrFive(String cmd, Loc pos, Boolean up,String writeFile){
            for(int i = 0; i<4; i++){
                byte[] pic = readNextPic(picNum);
                writeImage(pic, Environment.getExternalStorageDirectory().getPath() + writeFile+"TracePic"+picNum+".jpeg");
                picNum++;

                auavLock("move");
                succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc="+cmd, auavResp.ch);
                auavSpin();

                if(cmd.equals("rgh"))
                    if (up == true)
                        ++pos.x;
                    else
                        ++pos.y;
                else
                    if(up == true)
                        --pos.x;
                    else
                        --pos.y;
                setLoc(pos);
                fixOffset();
            }
        }
		void rcc(int deg){
            auavLock("rcc");
            succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=rcc-dp="+deg, auavResp.ch);
            auavSpin();
        }
        void rcw(int deg){
            auavLock("rcw");
            succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver","dc=rcw-dp"+deg,auavResp.ch);
            auavSpin();
        }
		void upd(boolean ud, Loc pos, String writeFile){

            byte[] pic = readNextPic(picNum);
            writeImage(pic, Environment.getExternalStorageDirectory().getPath() + writeFile+"TracePic"+picNum+".jpeg");
            picNum++;

            String cmd = "";
            cmd = (ud) ? "dc=ups" : "dc=dwn";

            auavLock("FlyDroneDriver");
            succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", cmd, auavResp.ch );
			auavSpin();
			if(ud)
			    ++pos.z;
			else
			    --pos.z;
            setLoc(pos);
        }
        void liftOff(){
            //liftoff
			auavLock("FlyDrone");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver",
					     "dc=lft", auavResp.ch );
			auavSpin();

			for(int i = 0; i<3; i++){
                auavLock("FlyDrone");
			    succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=dwn", auavResp.ch);
			    auavSpin();
            }

			//set initial location
			auavLock("Locate");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.LocationDriver", "dc=set-dp=0.00-dp=0.00-dp=0.00", auavResp.ch );
			auavSpin();
        }
        void config(){
            setSimOff();

			auavLock("ssm");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver", "dc=ssm", auavResp.ch);
			auavSpin();

			auavLock("ConfigFlight");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=cfg", auavResp.ch);
			auavSpin();

		    heading = getHeading();
        }
        float getHeading() {
            auavLock("SetHeading");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=hdg", auavResp.ch);
			auavSpin();
		    return Float.valueOf(auavResp.getResponse());
        }
        void fixOffset(){
            /*auavLock("SetHeading");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=rca-dp="+heading, auavResp.ch);
			auavSpin();*/
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


		public CubeTrace() {
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
