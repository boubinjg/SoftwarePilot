// Action Items:
// 1. Avoid coming back to a visisted state.
// 2. Store the best captured image (with highest utility) and return that at the end.
// 3. Implement A* Search Algorithm.

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
 * @author Jayson Boubin
 * @version 1.0.0
 * @since   2018-9-17
 */
public class BasicRoutine extends org.reroutlab.code.auav.routines.AuavRoutines {
		/**
		 *	 Check forceStop often and safely end routine if set
		 */
		public boolean forceStop = false;
		public long TIMEOUT = 10000;
		public int MAX_TRIES = 10;
		private Properties configFile;
		//public final double CAMERA_FOV_HORIZ;
		//public final double CAMERA_FOV_VERT;
		//public final String [] MODELS;
		//public final String [] MODEL_NAMES;
        static byte[] pic;
        public String succ = "";
        public String IP = "";
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
            config();

            //takes off the UAV
            auavLock("Takeoff");
            succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=lft", auavResp.ch);
            auavSpin();

            auavLock("Left");
            succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=vec-dp=1-dp=p-dp=0-dp=p-dp=0-dp=p-dp=0-dp=p-dp=1000", auavResp.ch);
            auavSpin();

            auavLock("Left");
            succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver",  "dc=vec-dp=0-dp=p-dp=1-dp=p-dp=0-dp=p-dp=0-dp=p-dp=1000", auavResp.ch);
            auavSpin();

            auavLock("Left");
            succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver",  "dc=vec-dp=1-dp=m-dp=0-dp=p-dp=0-dp=p-dp=0-dp=p-dp=1000", auavResp.ch);
            auavSpin();

            auavLock("Left");
            succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver",  "dc=vec-dp=0-dp=p-dp=1-dp=m-dp=0-dp=p-dp=0-dp=p-dp=1000", auavResp.ch);
            auavSpin();

            //lands the UAV
            auavLock("land");
            succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=lnd", auavResp.ch);
            auavSpin();

        }
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
        public void sendToPort(byte[] b, String meta) throws IOException{
            try {
                Thread.sleep(3000);
            } catch(Exception e){

            }
            System.out.println("Client: Trying To Connect");
            Socket socket = new Socket(IP, 12013);
            System.out.println("Client: Connected");

            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            System.out.println("Writing "+b.length+" Bytes");
            dos.writeBytes(meta);
            dos.writeBytes("over\n");
            socket.close();

            socket = new Socket(IP, 12013);
            dos = new DataOutputStream(socket.getOutputStream());
            dos.writeInt(b.length);
            dos.write(b);

            socket.close();
        }
        //configures the camera and flight system
        void config(){
            setSimOff();

			auavLock("ssm");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver", "dc=ssm", auavResp.ch);
			auavSpin();

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


		public BasicRoutine() {
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
						t.start(); return "BasicRoutine: Started";
				}
				return "Detect not Initialized";
		}
		public String stopRoutine() {
				forceStop = true;	return "Detect: Force Stop set";
		}
}
