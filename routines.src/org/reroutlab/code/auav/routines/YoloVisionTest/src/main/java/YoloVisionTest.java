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
 * @author  Jayson Boubin
 * @version 1.0.0
 * @since   2018-5-13
 */
public class YoloVisionTest extends org.reroutlab.code.auav.routines.AuavRoutines {
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
        byte[] pic;
        static int[] Lmoves = {1,1,1}; //Set of three moves i.e Left, Up and Forward
        static int[] Rmoves = {1,1,1}; //Complementary set of three moves i.e Right, Down and Backward
        public static int x=0,y=0,z=0,gimbal=0;
        public static double utility = 0;


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

            config();

            getDirs(1,1,1,0);
            //auavLock("Takeoff");
            //succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=lft", auavResp.ch);
            //auavSpin();

            //Termination conditions:
                //5 images
                //No direction increases gain
            //take off
            /*
            for(int i = 0; i<5; i++){
                String gains = getDirs(x,y,z,gimbal);
                HashMap<String, Double> gainMap = new HashMap<String, Double>();
                for(String s : gains.split(" ")) {
                    if(s.split("=")[0].equals("Utility"))
                        utility = Double.parseDouble(s.split("=")[1]);
                    else
                        gainMap.put(s.split("=")[0], Double.parseDouble(s.split("=")[1]));
                }
                System.out.println("Features = "+gains);
                System.out.println("Utility = "+utility);

                //sort those directions by greatest to least
                //choose the best one within our current boundary
                String dir = choose_best_action(gainMap); // Call this function to calculate the best action to take.
                System.out.println("Direction: "+dir);

                // Call the method to perform Gimbal movement according to the direction specified.
                driver_movt(dir);
            }
            */
            /*auavLock("land");
            succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=lnd", auavResp.ch);
            auavSpin();
            */
            System.out.println("CubeTrace: Exiting");
		}
        int printBat(){
            auavLock("bat");
            succ = invokeDriver("org.reroutlab.code.auav.drivers.BatteryDriver", "dc=dji", auavResp.ch);
            auavSpin();
            return Integer.parseInt(auavResp.getResponse().split(",")[1].split("=")[1].trim());
        }
        String getDirs(int x, int y, int z, int g){
            //takeImg();
            long start = System.currentTimeMillis();

            String meta = getMetadata(Environment.getExternalStorageDirectory().getPath() + "/AUAVtmp/fullPic.JPG");
            //String meta = getMetadata("../tmp/pictmp.jpg");
            meta += "X="+x+"\n";
            meta += "Y="+y+"\n";
            meta += "Z="+z+"\n";
            meta += "GimbalPosition="+g+"\n";
            System.out.println(meta);

            String features;
            System.out.println("Read Image");
            byte[] b = readImg();

            System.out.println("Call Driver");
            auavLock("vision");
            //succ = invokeDriver("org.reroutlab.code.auav.drivers.VisionDriver", "dc=knn", auavResp.ch);
            succ = invokeHostDriver("org.reroutlab.code.auav.drivers.VisionDriver-dc=yolo", "192.168.56.1", auavResp.ch, true);
            System.out.println("Sending");
            try {
                sendToPort(b, meta);
            } catch(Exception e){
                e.printStackTrace();
            }

            auavSpin();
            features = auavResp.getResponse();

            System.out.println("----------------------------------------------");
            System.out.println(System.currentTimeMillis() - start);

            return features;
        }
        void takeImg(){
                System.out.println("Get");
                auavLock("Get Image");
                succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver","dc=get", auavResp.ch);
                auavSpin();

                System.out.println("DLD Full");
                auavLock("dld");
                succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver","dc=dldFull", auavResp.ch);
                auavSpin();
        }
        byte[] readImg(){
            try {
                //BufferedImage origImage = ImageIO.read(imgPath);
                //ByteArrayOutputStream baos = new ByteArrayOutputStream();
                //ImageIO.write(origImage, "jpg", baos);
                //return baos.toByteArray();

                File file = new File(Environment.getExternalStorageDirectory().getPath()+"/AUAVtmp/fullPic.JPG");
                //File file = new File("../tmp/pictmp.jpg");
                FileChannel fileChannel = new RandomAccessFile(file, "r").getChannel();
                MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
                byte[] pic = new byte[buffer.capacity()];
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
        public void sendToPort(byte[] b, String meta) throws IOException{
            try {
                Thread.sleep(3000);
            } catch(Exception e){

            }
            System.out.println("Client: Trying To Connect");
            Socket socket = new Socket("192.168.56.1", 12013);
            System.out.println("Client: Connected");

            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            System.out.println("Writing "+b.length+" Bytes");
            dos.writeBytes(meta);
            dos.writeBytes("over\n");
            socket.close();

            socket = new Socket("192.168.56.1", 12013);
            dos = new DataOutputStream(socket.getOutputStream());
            dos.writeInt(b.length);
            dos.write(b);

            socket.close();
        }

        public String getMetadata(String fname){
            Metadata metadata;
            System.out.println("Meta");
            try{
                File f = new File(fname);
                metadata = ImageMetadataReader.readMetadata(f);
                String ret = "";

                for(Directory directory : metadata.getDirectories()){
                    for(Tag tag : directory.getTags()){
                        //System.out.println(tag.toString().split("-")[0].trim());
                        switch(tag.toString().split("-")[0].trim()){
                            case "[Exif SubIFD] Exposure Time":
                                String tagVal = tag.toString().split("-")[1].trim().split(" ")[0].trim();
                                double val = Double.parseDouble(tagVal.split("/")[0]);
                                try{
                                    val = Double.parseDouble(tagVal.split("/")[0])/
                                    Double.parseDouble(tagVal.split("/")[1]);
                                } catch(Exception e){}
                                ret += "ExposureTime="+val+"\n";
                                break;
                            case "[Exif SubIFD] Date/Time Original":
                                String time = tag.toString().split("-")[1].trim().split(" ")[1].trim();
                                String[] timeVals = time.split(":");
                                double timed = Double.parseDouble(timeVals[0]) * 3600 +
                                              Double.parseDouble(timeVals[1]) * 60 +
                                              Double.parseDouble(timeVals[2]);
                                ret+="time="+timed/(24*3600.0)+"\n";
                                break;
                            case "[GPS] GPS Latitude":
                                String[] coords = tag.toString().split(" ");
                                double sign = 1;
                                if(coords[4].charAt(0) == '-')
                                    sign = -1;
                                double lat = Double.parseDouble(coords[4].substring(0,coords[4].length()-1)) +
                                             sign * Double.parseDouble(coords[5].substring(0,coords[5].length()-1))/60.0 +
                                             sign * Double.parseDouble(coords[6].substring(0,coords[6].length()-1))/3600.0;
                                ret += "Latitude="+lat + "\n";
                                break;
                            case "[GPS] GPS Longitude":
                                coords = tag.toString().split(" ");
                                sign = 1;
                                if(coords[4].charAt(0) == '-')
                                    sign = -1;
                                double lon = Double.parseDouble(coords[4].substring(0,coords[4].length()-1)) +
                                             sign * Double.parseDouble(coords[5].substring(0,coords[5].length()-1))/60.0 +
                                             sign * Double.parseDouble(coords[6].substring(0,coords[6].length()-1))/3600.0;
                                ret += "Longitude="+lon+"\n";
                                break;
                        }
                    }
                }
                return ret;
            } catch(Exception e){
                e.printStackTrace();
            }
            return "";
        }

        void config(){
            setSimOff();

			/*auavLock("ssm");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageV2Driver", "dc=ssm", auavResp.ch);
			auavSpin();
            */
			auavLock("ConfigFlight");
			succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc=cfg", auavResp.ch);
			auavSpin();

		    /*heading = getHeading();*/
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

    /* Call the driver to move the drone to required position based on the direction returned.
            Use FlyDrone Driver and GimbbalDriver to accomplish this.
    */
    public void driver_movt(String dir)
    {
        if(dir.equals("g00"))
        {
            //reset the gimbal
            gimbal = 0;
            auavLock("resetGimbal");
            succ = invokeDriver("org.reroutlab.code.auav.drivers.DroneGimbalDriver","dc=res", auavResp.ch);
            auavSpin();
            auavLock("moveGimbalDown");
            succ = invokeDriver("org.reroutlab.code.auav.drivers.DroneGimbalDriver", "dc=dna-dp=00", auavResp.ch);
            auavSpin();
        }
        else if(dir.equals("g15"))
        {
            //reset the gimbal
            gimbal = 15;
            auavLock("resetGimbal");
            succ = invokeDriver("org.reroutlab.code.auav.drivers.DroneGimbalDriver","dc=res", auavResp.ch);
            auavSpin();
            auavLock("moveGimbalDown");
            succ = invokeDriver("org.reroutlab.code.auav.drivers.DroneGimbalDriver", "dc=dna-dp=15", auavResp.ch);
            auavSpin();
        }
        else if(dir.equals("g30"))
        {
            //reset the gimbal
            gimbal = 30;
            auavLock("resetGimbal");
            succ = invokeDriver("org.reroutlab.code.auav.drivers.DroneGimbalDriver","dc=res", auavResp.ch);
            auavSpin();
            auavLock("moveGimbalDown");
            succ = invokeDriver("org.reroutlab.code.auav.drivers.DroneGimbalDriver", "dc=dna-dp=30", auavResp.ch);
            auavSpin();
        }
        else
        {
            System.out.println("It's a flydrone driver, command="+dir);
            auavLock("FlyDrone");
            succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver", "dc="+dir, auavResp.ch);
            auavSpin();
        }


    }


    /* From all the possible best actions to take and based on fixed boundary, an appropriate best action is returned.
        Example: If move_right = 0.8666 and move_down = 0.7667:-
        - If the drone is already in rightmost position, then move_down action would be taken.
        - If drone is not in leftmost position, move right is taken.
     */

    public static String choose_best_action(Map<String, Double> actions)
    {

        //Use another map to store gain in utility value and next sort them in descending order.
        //Map<String, Double> gain =  new HashMap<>();

        // This Map is used for the best action to be taken.
        Map<String, Double> sorted_actions = new LinkedHashMap<>();
        /*
        for(Map.Entry<String, Double> entry : actions.entrySet())
        {

            String key = entry.getKey();
            //System.out.println("Key of map="+key);
            Double val = entry.getValue();
            //System.out.println("Val of map="+val);

            gain.put(key, val - current_feature.get(key));
        } */

        // Now pass the gain to be sorted in descending order.
        //Sort the Map of actions in descending order. (Using Java8 Lambda expression)
        sorted_actions = sortByComparator(actions, false);

        System.out.println("Sorted Hashmap:");
        for(HashMap.Entry<String, Double> e : sorted_actions.entrySet()){
                    System.out.println(e.getKey() + " "+ e.getValue());
        }
        System.out.println("End Sorted Hashmap");


        //System.out.println("Map After sorting in descending order:"+sorted_actions);

        //Create an iteartor and check if the enrty is permitted action to take.
        for(Map.Entry<String, Double> entry: sorted_actions.entrySet())
        {
            //Get the key from the "sorted_actions" Map.
            String key = entry.getKey();

            //Get the value from the "sorted_actions" Map.
            Double val = entry.getValue();

            //Get the current feature value from actions
            //Double cuVal = current.get(key);

            if(key.equals("lef"))
            {
            if ((Lmoves[0]-1>=0) && (Rmoves[0]+1<=2) && val>1)
            {
                //Decrement the remaining movelef value.
                Lmoves[0] -= 1;

                //Increment the remaining moveRight Value.
                Rmoves[0] += 1;

                return "lef";
            }

            //If that action is not permitted then just remove that from the map.
            else
            {
                sorted_actions.remove(key);
            }
            }

            else if(key.equals("rgh"))
            {
            if ((Rmoves[0]-1>=0) && (Lmoves[0]+1<=2) && val>1)
            {
                //Decrement the remaining moveLeft value.
                Rmoves[0] -= 1;

                //Increment the remaining moveRight Value.
                Lmoves[0] += 1;

                return "rgh";
            }

            //If that action is not permitted then just remove that from the map.
            else
            {
                sorted_actions.remove(key);
            }
            }

            else if(key.equals("ups"))
            {
            if ((Lmoves[1]-1>=0) && (Rmoves[1]+1<=2) && val>1)
            {
                //Decrement the remaining moveLeft value.
                Lmoves[1] -= 1;

                //Increment the remaining moveRight Value.
                Rmoves[1] += 1;

                return "ups";
            }

            //If that action is not permitted then just remove that from the map.
            else
            {
                sorted_actions.remove(key);
            }
            }

            else if(key.equals("dwn"))
            {
            if ((Rmoves[1]-1>=0) && (Lmoves[1]+1<=2) && val>1)
            {
                //Decrement the remaining moveLeft value.
                Rmoves[1] -= 1;

                //Increment the remaining moveRight Value.
                Lmoves[1] += 1;

                return "dwn";
            }

            //If that action is not permitted then just remove that from the map.
            else
            {
                sorted_actions.remove(key);
            }
            }

            else if(key.equals("fwd"))
            {
            if ((Lmoves[2]-1>=0) && (Rmoves[2]+1<=2) && val>1)
            {
                //Decrement the remaining moveLeft value.
                Lmoves[2] -= 1;

                //Increment the remaining moveRight Value.
                Rmoves[2] += 1;

                return "fwd";
            }

            //If that action is not permitted then just remove that from the map.
            else
            {
                sorted_actions.remove(key);
            }
            }

            else if(key.equals("bck"))
            {
            if ((Rmoves[2]-1>=0) && (Lmoves[2]+1<=2) && val>1)
            {
                //Decrement the remaining moveLeft value.
                Rmoves[2] -= 1;

                //Increment the remaining moveRight Value.
                Lmoves[2] += 1;

                return "bck";
            }
            //If that action is not permitted then just remove that from the map.
            else
            {
                sorted_actions.remove(key);
            }
            }

            else if(key.equals("g00") )
            {
            if (val>1 && gimbal != 0)
            {

                return "g00";
            }

            //If that action is not permitted then just remove that from the map.
            else
            {
                sorted_actions.remove(key);
            }
            }


            else if(key.equals("g15"))
            {
            if (val>1 && gimbal != 15)
            {

                return "g15";
            }

            //If that action is not permitted then just remove that from the map.
            else
            {
                sorted_actions.remove(key);
            }
            }

            else if(key.equals("g30") & gimbal != 30)
            {
            if (val>1)
            {

                return "g30";
            }

            //If that action is not permitted then just remove that from the map.
            else
            {
                sorted_actions.remove(key);
            }
            }

        }


        // If all the actions aren't possible, then just land the drone.
        if(sorted_actions.isEmpty())
        {
            System.out.println("No possible further move. Drone landing down!");
            return "lnd";
        }

        return "UnknownError";

     }
    private static Map<String, Double> sortByComparator(Map<String, Double> unsortMap, final boolean order)
    {

        List<Entry<String, Double>> list = new LinkedList<Entry<String, Double>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Entry<String, Double>>()
        {
            public int compare(Entry<String, Double> o1,
                    Entry<String, Double> o2)
            {
                if (order)
                {
                    return o1.getValue().compareTo(o2.getValue());
                }
                else
                {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
        for (Entry<String, Double> entry : list)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
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


		public YoloVisionTest() {
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
						t.start(); return "Detect: Started";
				}
				return "Detect not Initialized";
		}
		public String stopRoutine() {
				forceStop = true;	return "Detect: Force Stop set";
		}
}
