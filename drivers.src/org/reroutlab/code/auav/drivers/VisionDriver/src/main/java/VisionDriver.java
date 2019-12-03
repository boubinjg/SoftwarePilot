package org.reroutlab.code.auav.drivers;

import java.util.*;
import java.lang.System.*;
//import org.reroutlab.code.auav.interfaces.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.InetSocketAddress;
import java.net.SocketException;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;



import java.util.Timer;
import java.util.TimerTask;
import java.lang.ProcessBuilder;
import java.lang.Runtime;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.concurrent.Semaphore;
import dji.common.error.DJIError;
import dji.common.flightcontroller.ConnectionFailSafeBehavior;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.ControlMode;
import dji.common.flightcontroller.virtualstick.*;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.flightcontroller.FlightAssistant;
import dji.sdk.products.Aircraft;
import dji.sdk.remotecontroller.RemoteController;
import dji.sdk.sdkmanager.DJISDKManager;
//import dji.thirdparty.eventbus.EventBus;
import dji.common.util.CommonCallbacks;

import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.io.File;
import java.nio.channels.FileChannel;
import java.io.PrintWriter;

import java.util.List;

/**
 * FlyDroneDriver connects to the DJI Aircraft instance.
 * It supports commands related to flying the aircraft.
 * Interact with this driver via CoAP (californium)
 * <br><b>URL: coap:\\localhost:port\cr?</b>
 * <br>coap:\\127.0.0.1:10241\cr?dc=lft
 * <br>
 * Note: all AUAV drivers have resource component "cr" for coap resource
 *
 * @author  Christopher Charles Stewart
 * @version 1.0.2
 * @since   2017-05-01
 */
public class VisionDriver extends org.reroutlab.code.auav.drivers.AuavDrivers { //implements???

    private float FmPitch;
		private float FmRoll;
		private float FmYaw;
		private float FmThrottle;
		private float RmPitch;
		private float RmRoll;
		private float RmYaw;
		private float RmThrottle;
		private Timer mTimer;
		private String LOG_TAG="FlyDroneDriver ";
		private CoapServer cs;
		private static int LISTEN_PORT = 0;
		private int driverPort = 0;
		private static boolean fin;
		private static Logger fddLogger =
				Logger.getLogger(VisionDriver.class.getName());

		private FlightController fc;
		/**
		 * Get a reference to the CoAP server for the
		 * driver.  Used to bootstrap our AUAV systems.
		 *
		 * @author  Christopher Charles Stewart
		 * @version 1.0.2
		 * @since   2017-09-01
		 */
		public CoapServer getCoapServer() {
				return (cs);
		}

		/**
		 * Set the log level for internal driver
		 * operations.
		 *
		 * @author  Christopher Charles Stewart
		 * @version 1.0.2
		 * @since   2017-09-01
		 */
		public void setLogLevel(Level l) {
				fddLogger.setLevel(l);
		}

		/**
		 * After the CoAP server is started,
		 * this method returns to UDP port
		 * that the driver listens on.
		 *
		 * @return  Intended UDP CoAP port.  0
		 * means use OS-chosen port.
		 * @author  Christopher Charles Stewart
		 * @version 1.0.2
		 * @since   2017-09-01
		 */
		public int getLocalPort() {
				return driverPort; //???
		}

		private HashMap driver2port;  // key=drivername value={port,usageInfo}
		/**
		 * Let a driver know about other available drivers.
		 *
		 * @author  Christopher Charles Stewart
		 * @version 1.0.2
		 * @since   2017-09-01
		 */
		public void setDriverMap(HashMap<String, String> m) {
				if (m != null) {
						driver2port = new HashMap<String, String>(m);
				}
		}

		/**
		 * Supported commands:
		 *   <br>usageInfo += "lft -- Lift off and hover";
		 *   <br>usageInfo += "ups -- Go up roughly 0.2 meters";
		 *   <br>usageInfo += "dwn -- Go down roughly 0.2 meters";
		 *   <br>usageInfo += "cfg -- Config intelligent flight ass";
		 *   <br>usageInfo += "lnd -- Land as soon as possible";
		 *   <br>usageInfo += "help - Return this info";
     *    <br>usageInfo += "AUAVsim -- Global flag. Do not access DJI.  Just simulate.\n";
		 *   <br>
		 *
		 *
		 * @return  String describing supported driver
		 * commands.
		 * @author  Jayson Boubin
		 * @version 1.0.2
		 * @since   2018-09-22
		 */
		public String getUsageInfo() {
				String usageInfo="";
				usageInfo += "AUAVsim -- Global flag. Do not access DJI.  Just simulate.\n";
				return usageInfo;
		}



		public VisionDriver() throws Exception {
				fddLogger.log(Level.FINEST, "In Constructor");
				cs = new CoapServer(); //initilize the server
				InetSocketAddress bindToAddress = new InetSocketAddress( LISTEN_PORT);//get the address
				CoapEndpoint tmp = new CoapEndpoint(bindToAddress); //create endpoint
				cs.addEndpoint(tmp);//add endpoint to server
				tmp.start();//Start this endpoint and all its components.
				driverPort = tmp.getAddress().getPort();

				cs.add(new fddResource());
		}


		private class fddResource extends CoapResource {
				public fddResource() {
						super("cr");//???
						getAttributes().setTitle("cr");//???
				}
				/**
				 *
				 * The CoAP driver support "PUT" requests.  See
				 * usage information for more details.
				 * @author  Christopher Charles Stewart
				 * @version 1.0.2
				 * @since   2017-09-01
				 */


				@Override
				public void handlePUT(CoapExchange ce) {
						// Split on & and = then on ' '
						byte[] payload = ce.getRequestPayload();
						String inputLine = "";
						try {
								inputLine  = new String(payload, "UTF-8");
						}
						catch ( Exception uee) {
								System.out.println(uee.getMessage());
						}
						System.out.println("\n InputLine: "+inputLine);

						String outLine = "";
						String[] args = inputLine.split("-");//???

						// Format: dc=driver_cmd [driver_prm=driver_arg]*
						boolean AUAVsim = false;
						for (String arg: args) {
								if (arg.equals("dp=AUAVsim")) {
										AUAVsim = true;
								}
						}
						if (args[0].equals("dc=help")) {
                            ce.respond(getUsageInfo());
                        } else if(args[0].equals("dc=fce")){
                            System.out.println("in fce");
                            String fName ="";
                            /*try{
                                fName = args[1].substring(3);
                            } catch(Exception E){
                                ce.respond("Please Specify File");
                            }*/
                            try{
                                System.out.println("Creating CMD");
                                // String cmd = "python3 ../Models/YoloRecognition.py"
				String cmd = "python3 ../Models/FaceRecognition.py";
                                System.out.println("Reading Pic");
				if(!AUAVsim){ 
					byte[] pic = readPic();
					writeByte(pic);
				}
                                Process p = Runtime.getRuntime().exec(cmd);
                                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                                String ret = in.readLine();
                                System.out.println("Face Box: "+ret);
                                ce.respond(ret);
                            } catch(Exception e) {
                                System.out.println("VisionError");
                                e.printStackTrace();
                                ce.respond("Vision Error");
                            }
                            ce.respond("Done");
			// not sure what this arg should actually equal
			} else if (args[0].equals("dc=yolo")) {
				System.out.println("in yolo");
				String fName = "";
				try {
					System.out.println("Creating CMD");
					String cmd = "python3 ../Models/DarknetRecognition.py";
					if(AUAVsim) {	
						cmd = "python3 ../../Models/DarknetRecognition.py";
					}
					System.out.println("Reading Pic");
					if(!AUAVsim){ 
						byte[] pic = readPic();
						writeByte(pic);
					}
					System.out.println(cmd);
					Process p = Runtime.getRuntime().exec(cmd);
					BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
					String ret = in.readLine();
					System.out.println("Face Box: " + ret);
					ce.respond(ret);
				} catch(Exception e) {
					System.out.println("VisionError");
					e.printStackTrace();
					ce.respond("Vision Error");
				}
				ce.respond("Done");
                        } else if (args[0].equals("dc=knn")) {
                            String featFile = args[1].substring(3);
                            String features = "";
                            long start = System.currentTimeMillis();
                            try{
                                System.out.println("Creating CMD");
                                String cmd = "bash ../externalModels/runParser.sh "+featFile;
                                readFullPic();
                                //writeByte(pic);
                                Process p = Runtime.getRuntime().exec(cmd);
                                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                                features = in.readLine();
                                System.out.println(features);
                                //ce.respond(ret);
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                            try {
                                PrintWriter writer = new PrintWriter("../externalModels/KNN_code/driver_code/input");
                                writer.println(features);
                                writer.close();

                                System.out.println("Creating CMD");
                                String cmd = "bash ../externalModels/KNN_code/driver_code/run.sh";
                                Process p = Runtime.getRuntime().exec(cmd);
                                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                                String ret = in.readLine();
                                System.out.println("Direction: "+ret);
                                ce.respond(ret);
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                        }
                        else if (args[0].equals("dc=ast")) {
                            String featFile = args[1].substring(3);
                            String features = "";
                            try{
                                System.out.println("Creating CMD");
                                String cmd = "bash ../externalModels/runParser.sh "+featFile;
                                readFullPic();
                                //writeByte(pic);
                                Process p = Runtime.getRuntime().exec(cmd);
                                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                                features = in.readLine();
                                System.out.println(features);
                                //ce.respond(ret);
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                            try {
                                PrintWriter writer = new PrintWriter("../externalModels/A_Star_code/driver_code/input");
                                writer.println(features);
                                writer.close();

                                System.out.println("Creating CMD");
                                String cmd = "bash ../externalModels/A_Star_code/driver_code/run.sh";
                                Process p = Runtime.getRuntime().exec(cmd);
                                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                                String ret = in.readLine();
				//String[] result = ret.split(" ")[1];
                                System.out.println(ret); //Printing out the direction....
                                ce.respond(ret);
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                        }
                        // else {
                        //    System.out.println(System.currentTimeMillis() - start);
                       // }
			 else {
				ce.respond("Error: VisionDriver unknown command\n");
				}
			}
		}
        void writeByte(byte[] b) {
            try{
                File f = new File("../tmp/pictmp.jpg");
                f.delete();
                MappedByteBuffer out = new RandomAccessFile("../tmp/pictmp.jpg","rw").getChannel().map(FileChannel.MapMode.READ_WRITE, 0, b.length);

                for(int j = 0; j<b.length; j++){
                    out.put(b[j]);
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }
        void writeYaml(String s){
            System.out.println("Write YAML:");
            try{
                PrintWriter p = new PrintWriter("../tmp/pictmp.yaml","UTF-8");
                p.print(s);
                p.close();
            } catch(Exception e){
                e.printStackTrace();
            }
            //System.out.println(s);
            System.out.println("End YAML");
        }
        byte[] readPic() throws IOException {
            ServerSocket ss = new ServerSocket(12013);
            System.out.println("Server: Waiting for Connection");
            Socket s = ss.accept();
            System.out.println("Server: Connection Reached");

            s = ss.accept();
            DataInputStream dIn = new DataInputStream(s.getInputStream());
            byte[] ret = new byte[0];
            //dIn.readInt();
            int length = dIn.readInt();
            System.out.println("Receiving "+length+" Bytes");
            if(length > 0) {
                ret = new byte[length];
                dIn.readFully(ret, 0, ret.length);
            }

            s.close();
            ss.close();
            return ret;
        }
        void readFullPic() throws IOException {
            ServerSocket ss = new ServerSocket(12013);
            System.out.println("Server: Waiting for Connection");
            Socket s = ss.accept();
            System.out.println("Server: Connection Reached");

            String line, message = "";
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));

            System.out.println("Reading Metadata");
            while((line = br.readLine())!= null) {
                if(!line.equals("over")) {
                    System.out.println(line);
                    message += line+"\n";
                } else {
                    System.out.println("OVER");
                    break;
                }
            }
            System.out.println("Metadata Done:");
            System.out.println(message);
            s.close();

            s = ss.accept();
            DataInputStream dIn = new DataInputStream(s.getInputStream());
            byte[] ret = new byte[0];

            int length = dIn.readInt();
            System.out.println("Receiving "+length+" Bytes");
            if(length > 0) {
                ret = new byte[length];
                dIn.readFully(ret, 0, ret.length);
            }

            System.out.println(message);
            s.close();
            ss.close();
            writeByte(ret);
            writeYaml(message);
        }
		CommonCallbacks.CompletionCallback fddHandler = new CommonCallbacks.CompletionCallback() {
						@Override
						public void onResult(DJIError djiError) {
								if (djiError == null) {
										System.out.println(LOG_TAG+"-"+lockStr+"-success");
										drvUnsetLock();
								}
								else {System.out.println(LOG_TAG+"-"+lockStr+"-fail");
										drvUnsetLock();}
						}
				};

		private static class FlightTimerTask extends TimerTask {
				String pos;
				FlightControlData fcd;
                Timer ctrl;
                int frequency = 3;
                int count = 0;
                CoapExchange ce;
                boolean send;
				private String LOG_TAG="FlightTimeTask.FlyDroneDriver ";
				FlightTimerTask(FlightControlData input) {
						super();
						fcd = input;
				}
		        FlightTimerTask(FlightControlData input, Timer t, int freq, CoapExchange ex, boolean sendMessage) {
						super();
						fcd = input;
						ctrl=t;
						frequency = freq;
						ce = ex;
						send = sendMessage;
				}

				@Override
				public void run() {
						Aircraft aircraft = (Aircraft)DJISDKManager.getInstance().getProduct();
						FlightController fc = aircraft.getFlightController();
						fc.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
						  @Override
						  public void onResult(DJIError djiError) {
								    if (djiError == null) {
										      System.out.println("Stick Enabled");
								    }
								    else {System.out.println("Stick Not Enabled");}
				          }
                        });
						fc.sendVirtualStickFlightControlData(fcd,
								new CommonCallbacks.CompletionCallback() {
										@Override
										public void onResult(DJIError djiError) {
												if (djiError == null) {
														System.out.println(LOG_TAG+"Flight Control Success");
												}
												else {
														System.out.println(LOG_TAG+djiError.getDescription());
												}
										}
								} );

        //System.out.println("Virtual Stick Mode: "+fc.isVirtualStickControlModeAvailable());
                        if (count < frequency) {
                            count++;
                            System.out.println("Inc Count to: "+count);
                        } else {
                            System.out.println("Cancel Task, count = "+count);
                            if(send)
                                ce.respond ("FlyDroneDriver: Timertask Complete");
                            ctrl.cancel();
                        }
				}
		}
		public Semaphore lockSema = new Semaphore(1);
    		        	public String lockStr = "continue";
               			public void drvSetLock(String v) {
                                	lockSema.acquireUninterruptibly();
                                	lockStr = v;
                		}
                		public void drvUnsetLock() {
                                	lockSema.release();
                		}
                		public void drvSpin() {
                        	        lockSema.acquireUninterruptibly();
                                	lockSema.release();
                		}
}

