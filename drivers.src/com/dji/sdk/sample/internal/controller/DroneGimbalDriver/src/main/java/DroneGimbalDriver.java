package com.dji.sdk.sample.internal.controller;

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

import dji.common.error.DJIError;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.gimbal.GimbalMode;
import dji.common.gimbal.GimbalState;

import dji.common.util.CommonCallbacks;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;


import java.util.concurrent.Semaphore;

/**
 * DroneGimbalDriver connects to the DJI Gimbal.
 * It supports commands related to moving the camera position.
 * Interact with this driver via CoAP (californium)
 * <br><b>URL: coap:\\localhost:port\cr?</b>
 * <br>Example: coap:\\127.0.0.1:10452\cr?dc=pos
 * <br>
 * Note: all AUAV drivers have resource component "cr" for coap resource
 *
 * @author  Christopher Charles Stewart
 * @version 1.0.2
 * @since   2017-05-01
 */
public class DroneGimbalDriver extends com.dji.sdk.sample.internal.controller.AuavDrivers { //implements???

		private String LOG_TAG="DroneGimbalDriver ";

		private Timer mTimer;
		private GimbalRotateTimerTask mGimbalRotationTimerTask;
		private GimbalGetPositionTimerTask mGimbalGetPositionTimerTask;
		private GimbalGetPitchTimerTask mGimbalGetPitchTimerTask;
		private GimbalYawTimerTask mGimbalYawTimerTask;
		private AbsoluteGimbalRotationTimerTask aGimbalRotationTimerTask;
		private String retval = "";
		private float pitch = 0;
		private static int LISTEN_PORT = 0;
		private int driverPort = 0;
		private static Logger dgdLogger =
				Logger.getLogger(DroneGimbalDriver.class.getName());


		private CoapServer cs;


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
				dgdLogger.setLevel(l);
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
		 *		<br>usageInfo += "pos -- Get the position (pitch, roll, yaw)\n";
		 *		<br>usageInfo += "cal -- Calibrate the Gimbal\n";
		 *		<br>usageInfo += "ups -- Move up (pitch) \n";
		 *		<br>usageInfo += "dwn -- Move down (pitch) \n";
		 *		<br>usageInfo += "lft -- Move left (roll) \n";
		 *		<br>usageInfo += "rgt -- Move right (roll) \n";
		 *    <br>usageInfo += "AUAVsim -- Global flag. Do not access DJI.  Just simulate.\n";
		 *    <br>
		 * @return  String describing supported driver
		 * commands.
		 * @author  Christopher Charles Stewart
		 * @version 1.0.2
		 * @since   2017-09-01
		 */
		public String getUsageInfo() {
				String usageInfo = "";
				usageInfo += "dc=[cmd]-dp=[option]\n";
				usageInfo += "cmd options \n";
				usageInfo += "pos -- Get the position (pitch, roll, yaw)\n";
				usageInfo += "cal -- Calibrate the Gimbal\n";
				usageInfo += "upa -- Move up to a provided absolute heading\n";
				usageInfo += "dna -- Move down to a provided absolute position\n";
				usageInfo += "res -- Reset to an absolute heading of 0\n";
				usageInfo += "ups -- Move up (pitch) \n";
				usageInfo += "dwn -- Move down (pitch) \n";
				usageInfo += "lft -- Move left (roll) \n";
				usageInfo += "rgt -- Move right (roll) \n";
				usageInfo += "AUAVsim -- Global flag. Do not access DJI.  Just simulate.\n";
				return usageInfo;
		}





		//constructor???
		public DroneGimbalDriver() throws Exception {
				dgdLogger.log(Level.FINEST, "In Constructor");
				cs = new CoapServer(); //initilize the server
				InetSocketAddress bindToAddress = new InetSocketAddress( LISTEN_PORT);//get the address
				CoapEndpoint tmp = new CoapEndpoint(bindToAddress); //create endpoint
				cs.addEndpoint(tmp);//add endpoint to server
				tmp.start();//Start this endpoint and all its components.
				driverPort = tmp.getAddress().getPort();

				cs.add(new dgdResource());

		}




		//extends CoapResource class
		private class dgdResource extends CoapResource {
				public dgdResource() {
						super("cr");//???
						getAttributes().setTitle("cr");//???
				}
				/**
				 *
				 * This function process the input commands, if the there is list, append to
				 * the string, if not call sendTo methond find out the matching information
				 *
				 * @return information in the list
				 *
				 *
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

            boolean AUAVsim = false;
						for (String arg: args) {
								if (arg.contains("AUAVsim")) {
										AUAVsim = true;
								}
						}

						Aircraft aircraft = null;
						if (AUAVsim == false) {
								aircraft = (Aircraft)DJISDKManager.getInstance().getProduct();
								if(aircraft.getGimbal()==null){
										ce.respond("No Gimbal");
										return;
								}
						}

						// Format: dc=driver_cmd [driver_prm=driver_arg]*
						if (args[0].equals("dc=help")) {
								ce.respond(getUsageInfo());
						}
						else if (args[0].equals("dc=pos")) {
								System.out.println(LOG_TAG+ " Gimbal Pos");
								if (mTimer == null) {
										mTimer = new Timer();
										mGimbalGetPositionTimerTask = new GimbalGetPositionTimerTask();
										mTimer.schedule(mGimbalGetPositionTimerTask, 0, 100);
								}
								try {	Thread.sleep(2000);}
								catch (Exception e) {}
								ce.respond (mGimbalGetPositionTimerTask.pos);

								if (mTimer != null) {
										mGimbalGetPositionTimerTask.cancel();
										mTimer.cancel();
										mTimer.purge();
										mGimbalGetPositionTimerTask = null;
										mTimer = null;
								}
						}
						else if (args[0].equals("dc=cal")) {
								System.out.println(LOG_TAG+ " Calibrating Gimbal");
								if (AUAVsim == false) {
										aircraft.getGimbal().startCalibration(new CommonCallbacks.CompletionCallback() {
														@Override
														public void onResult(DJIError djiError) {
																if (djiError == null) {
																		System.out.println(LOG_TAG+"Move success");
																}
																else {
																		System.out.println(LOG_TAG+djiError.getDescription());

																}
														}
												}
												);
									try{Thread.sleep(2000);}
									catch(Exception e){}
								}
								ce.respond ("Gimbal: cal");
						}
						else if (args[0].equals("dc=res")) {
							if (mTimer == null) {
								pitch = 0;
								mTimer = new Timer();
								aGimbalRotationTimerTask = new AbsoluteGimbalRotationTimerTask((float)0.0);
								mTimer.schedule(aGimbalRotationTimerTask, 0, 100);
							}
							try {	Thread.sleep(100);}
							catch (Exception e) {}
							if (mTimer != null) {
								aGimbalRotationTimerTask.cancel();
								mTimer.cancel();
								mTimer.purge();
								aGimbalRotationTimerTask = null;
								mTimer = null;
							}

							ce.respond ("Gimbal: res");
						}
						else if (args[0].equals("dc=dna")){
							int curPitch = 0;
							try{
                                                                curPitch = Integer.parseInt(args[1].substring(3));
                                                        } catch(Exception e){
                                                                e.printStackTrace();
                                                                ce.respond("invalid parameter");
                                                                return;
                                                        }

							pitch -= curPitch;
							if(pitch < -90)
								pitch = -90;

							if (mTimer == null) {
								mTimer = new Timer();
								aGimbalRotationTimerTask = new AbsoluteGimbalRotationTimerTask((float)(pitch));
								mTimer.schedule(aGimbalRotationTimerTask, 0, 100);
							}
							try {	Thread.sleep(100);}
							catch (Exception e) {}
							if (mTimer != null) {
								aGimbalRotationTimerTask.cancel();
								mTimer.cancel();
								mTimer.purge();
								aGimbalRotationTimerTask = null;
								mTimer = null;
							}

							ce.respond ("Gimbal: dna");
						}
						else if (args[0].equals("dc=upa")) {
							int curPitch = 0;
							try{
                                                                curPitch = Integer.parseInt(args[1].substring(3));
                                                        } catch(Exception e){
                                                                e.printStackTrace();
                                                                ce.respond("invalid parameter");
                                                                return;
                                                        }

							pitch += curPitch;
							if(pitch > 0)
								pitch = 0;

							if (mTimer == null) {
								mTimer = new Timer();
								aGimbalRotationTimerTask = new AbsoluteGimbalRotationTimerTask((float)(pitch));
								mTimer.schedule(aGimbalRotationTimerTask, 0, 100);
							}
							try {	Thread.sleep(100);}
							catch (Exception e) {}
							if (mTimer != null) {
								aGimbalRotationTimerTask.cancel();
								mTimer.cancel();
								mTimer.purge();
								aGimbalRotationTimerTask = null;
								mTimer = null;
							}

							ce.respond ("Gimbal: upa");
						}
						else if (args[0].equals("dc=shk")){
							float origPitch = pitch;
							for(int i = 0; i<2; i++){
									if(pitch < -45){
										if (mTimer == null) {
											mTimer = new Timer();
											aGimbalRotationTimerTask = new AbsoluteGimbalRotationTimerTask((float)(0));
											mTimer.schedule(aGimbalRotationTimerTask, 0, 100);
										}
										try {	Thread.sleep(100);}
										catch (Exception e) {}
										if (mTimer != null) {
											aGimbalRotationTimerTask.cancel();
											mTimer.cancel();
											mTimer.purge();
											aGimbalRotationTimerTask = null;
											mTimer = null;
										}
									} else {
										if (mTimer == null) {
											mTimer = new Timer();
											aGimbalRotationTimerTask = new AbsoluteGimbalRotationTimerTask((float)(-80));
											mTimer.schedule(aGimbalRotationTimerTask, 0, 100);
										}
										try {	Thread.sleep(100);}
										catch (Exception e) {}
										if (mTimer != null) {
											aGimbalRotationTimerTask.cancel();
											mTimer.cancel();
											mTimer.purge();
											aGimbalRotationTimerTask = null;
											mTimer = null;
										}
									}
									try {	Thread.sleep(100);}
									catch (Exception e) {}
									//reset to original heading
									if (mTimer == null) {
										mTimer = new Timer();
										aGimbalRotationTimerTask = new AbsoluteGimbalRotationTimerTask((float)(origPitch));
										mTimer.schedule(aGimbalRotationTimerTask, 0, 100);
									}
									try {	Thread.sleep(100);}
									catch (Exception e) {}
									if (mTimer != null) {
										aGimbalRotationTimerTask.cancel();
										mTimer.cancel();
										mTimer.purge();
										aGimbalRotationTimerTask = null;
										mTimer = null;
									}
									ce.respond("Gimbal: Shake");
							}
						}
						else if (args[0].equals("dc=dwn")) {
								System.out.println(LOG_TAG+ " Moving Gimbal");
								if (mTimer == null) {
										mTimer = new Timer();
										mGimbalRotationTimerTask = new GimbalRotateTimerTask((float)-10);
										mTimer.schedule(mGimbalRotationTimerTask, 0, 100);
								}
								try {	Thread.sleep(2000);}
								catch (Exception e) {}
								if (mTimer != null) {
										mGimbalRotationTimerTask.cancel();
										mTimer.cancel();
										mTimer.purge();
										mGimbalRotationTimerTask = null;
										mTimer = null;
								}
								ce.respond ("Gimbal: dwn");
						}
						else if (args[0].equals("dc=ups")) {
								System.out.println(LOG_TAG+ " Moving Gimbal");
								if (mTimer == null) {
										mTimer = new Timer();
										mGimbalRotationTimerTask = new GimbalRotateTimerTask((float)10);
										mTimer.schedule(mGimbalRotationTimerTask, 0, 100);
								}
								try {	Thread.sleep(2000);}
								catch (Exception e) {}
								if (mTimer != null) {
										mGimbalRotationTimerTask.cancel();
										mTimer.cancel();
										mTimer.purge();
										mGimbalRotationTimerTask = null;
										mTimer = null;
								}
								ce.respond ("Gimbal: ups");
						}
						else if (args[0].equals("dc=lft")) {
								System.out.println(LOG_TAG+ " Moving Gimbal");
								if (mTimer == null) {
										mTimer = new Timer();
										mGimbalYawTimerTask = new GimbalYawTimerTask((float)10);
										mTimer.schedule(mGimbalYawTimerTask, 0, 100);
								}
								try {	Thread.sleep(1000);}
								catch (Exception e) {}
								if (mTimer != null) {
										mGimbalYawTimerTask.cancel();
										mTimer.cancel();
										mTimer.purge();
										mGimbalYawTimerTask = null;
										mTimer = null;
								}
								ce.respond ("Gimbal: lft");
						}
						else if (args[0].equals("dc=rgt")) {
								System.out.println(LOG_TAG+ " Moving Gimbal");
								if (mTimer == null) {
										mTimer = new Timer();
										mGimbalYawTimerTask = new GimbalYawTimerTask((float)10);
										mTimer.schedule(mGimbalYawTimerTask, 0, 100);
								}
								try {	Thread.sleep(1000);}
								catch (Exception e) {}
								if (mTimer != null) {
										mGimbalYawTimerTask.cancel();
										mTimer.cancel();
										mTimer.purge();
										mGimbalYawTimerTask = null;
										mTimer = null;
								}
								ce.respond ("Gimbal: rgt");
						}
						else{
								ce.respond ("No Gimbal Command " + args[0]);
						}
				}
		}

		private class AbsoluteGimbalRotationTimerTask extends TimerTask {
				float pitchAngle;

				AbsoluteGimbalRotationTimerTask(float p) {
						super();
						pitchAngle = p;
						System.out.println("Absolute Angle " +p);
				}

				@Override
				public void run() {

						DJISDKManager.getInstance().getProduct().getGimbal().
								rotate(new Rotation.Builder().pitch(pitchAngle)
											 .mode(RotationMode.ABSOLUTE_ANGLE)
											 .yaw(Rotation.NO_ROTATION)
											 .roll(Rotation.NO_ROTATION)
											 .time(0)
											 .build(), new CommonCallbacks.CompletionCallback() {

												@Override
												public void onResult(DJIError djiError) {
														if (djiError == null) {
																System.out.println("Move success");
																drvUnsetLock();
														}
														else {
																System.out.println(djiError.getDescription());
														}
												}
										});

				}
		}


		private static class GimbalRotateTimerTask extends TimerTask {
				float pitchValue;

				GimbalRotateTimerTask(float pitchValue) {
						super();
						this.pitchValue = pitchValue;
				}

				@Override
				public void run() {
						DJISDKManager.getInstance().getProduct().getGimbal().
								rotate(new Rotation.Builder().pitch(pitchValue)
											 .mode(RotationMode.SPEED)
											 .yaw(Rotation.NO_ROTATION)
											 .roll(Rotation.NO_ROTATION)
											 .time(0)
											 .build(), new CommonCallbacks.CompletionCallback() {

												@Override
												public void onResult(DJIError djiError) {
														if (djiError == null) {
																System.out.println("Move success");
														}
														else {
																System.out.println(djiError.getDescription());
														}
												}
										});
				}
		}

		private static class GimbalYawTimerTask extends TimerTask {
				float yawValue;

				GimbalYawTimerTask(float yawValue) {
						super();
						this.yawValue = yawValue;
				}

				@Override
				public void run() {
						DJISDKManager.getInstance().getProduct().getGimbal().
								rotate(new Rotation.Builder().pitch(Rotation.NO_ROTATION)
											 .mode(RotationMode.SPEED)
											 .yaw(Rotation.NO_ROTATION)
											 .roll(Rotation.NO_ROTATION)
											 .time(0)
											 .build(), new CommonCallbacks.CompletionCallback() {

												@Override
												public void onResult(DJIError djiError) {
														if (djiError == null) {
																System.out.println("Move success");
														}
														else {
																System.out.println(djiError.getDescription());
														}
												}
										});
				}
		}
		private static class GimbalGetPitchTimerTask extends TimerTask {
				float pitch;
				GimbalGetPitchTimerTask() {
						super();
				}

				@Override
				public void run() {
						Aircraft aircraft = (Aircraft)DJISDKManager.getInstance().getProduct();
						if(aircraft.getGimbal()!=null){
								aircraft.getGimbal().setStateCallback(new GimbalState.Callback() {
												@Override
												public void onUpdate(GimbalState gimbalState) {
														if (gimbalState != null) {
																String retval="";
																pitch=gimbalState.getAttitudeInDegrees().getPitch();
														}
												}
										});
						}

				}
		}

		private static class GimbalGetPositionTimerTask extends TimerTask {
				String pos;
				GimbalGetPositionTimerTask() {
						super();
				}

				@Override
				public void run() {
						Aircraft aircraft = (Aircraft)DJISDKManager.getInstance().getProduct();
						if(aircraft.getGimbal()!=null){
								aircraft.getGimbal().setStateCallback(new GimbalState.Callback() {
												@Override
												public void onUpdate(GimbalState gimbalState) {
														if (gimbalState != null) {
																String retval="";
																retval+="PitchInDegrees: ";
																retval+=gimbalState.getAttitudeInDegrees().getPitch();
																retval+="RollInDegrees: ";
																retval+=gimbalState.getAttitudeInDegrees().getRoll();
																retval+="YawInDegrees: ";
																retval+=gimbalState.getAttitudeInDegrees().getYaw();
																pos = retval;
														}
												}
										});
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

    		public static boolean isGimbalAvailable(){
			BaseProduct product = DJISDKManager.getInstance().getProduct();
			if(null != product && null != product.getGimbal())
				return true;
			return false;
		}
}
