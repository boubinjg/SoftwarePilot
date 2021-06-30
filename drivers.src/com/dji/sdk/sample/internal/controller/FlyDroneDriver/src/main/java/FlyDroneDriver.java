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
public class FlyDroneDriver extends com.dji.sdk.sample.internal.controller.AuavDrivers { //implements???

		private Timer mTimer;
		private String LOG_TAG="FlyDroneDriver ";
		private CoapServer cs;
		private static int LISTEN_PORT = 0;
		private int driverPort = 0;
		private static boolean fin;
		private static Logger fddLogger =
				Logger.getLogger(FlyDroneDriver.class.getName());

        private Timer mSendVirtualStickDataTimer;
        private SendVirtualStickDataTask mSendVirtualStickDataTask;
        private float mPitch = 0;
        private float mRoll = 0;
        private float mYaw = 0;
        private float mThrottle = 0;
        private Aircraft aircraft;
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
		 * @author  Christopher Charles Stewart
		 * @version 1.0.2
		 * @since   2017-09-01
		 */
		public String getUsageInfo() {
				String usageInfo="";
				usageInfo += "lft -- Lift off and hover\n";
				usageInfo += "lnd -- Land as soon as possible\n";
				usageInfo += "ups -- Go up roughly 0.2 meters";
				usageInfo += "dwn -- Go down roughly 0.2 meters";
				usageInfo += "rcw -- rotate the drone clockwise by some amount of degrees passed as the subsequent parameter";
				usageInfo += "rcc -- rotate the drone counterclockwise by some amount of degrees passed as the subsequent parameter";
				usageInfo += "cfg -- Config intelligent flight ass";
				usageInfo += "help - Return this info\n";
				usageInfo += "AUAVsim -- Global flag. Do not access DJI.  Just simulate.\n";
				return usageInfo;
		}



		public FlyDroneDriver() throws Exception {
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
						}
						if (args[0].equals("dc=hdg")) {
                            Aircraft aircraft = (Aircraft)DJISDKManager.getInstance().getProduct();
							fc=aircraft.getFlightController();
                            float curYaw = (float)fc.getState().getAttitude().yaw;
                            ce.respond(Float.toString(curYaw));
                        }
						else if (args[0].equals("dc=cfgL")) {
								if (AUAVsim == false) {
										Aircraft aircraft = (Aircraft)DJISDKManager.getInstance().getProduct();
										FlightAssistant fa = aircraft.getFlightController().getFlightAssistant();

										drvSetLock("Disable Landing Protect");
										fa.setLandingProtectionEnabled(false, fddHandler	);
										drvSpin();

										drvSetLock("HoverOnConnectFail");
										ConnectionFailSafeBehavior cfsb = ConnectionFailSafeBehavior.HOVER;
										FlightController fc = aircraft.getFlightController();
										fc.setConnectionFailSafeBehavior(cfsb,fddHandler	);
										drvSpin();

										drvSetLock("setVirtualStick");
										fc.setControlMode(ControlMode.MANUAL, fddHandler);
										fc.setVirtualStickModeEnabled(true, fddHandler);
										fc.setVerticalControlMode(VerticalControlMode.VELOCITY);
										fc.setYawControlMode(YawControlMode.ANGLE);
										fc.setRollPitchControlMode(RollPitchControlMode.ANGLE);
										drvSpin();
								}
								ce.respond ("FlyDroneDriver: cfg complete");
                        } else if(args[0].equals("dc=cfg")) {
                            if(AUAVsim == false){
                                aircraft = (Aircraft)DJISDKManager.getInstance().getProduct();
								FlightAssistant fa = aircraft.getFlightController().getFlightAssistant();

								drvSetLock("Disable Landing Protect");
								fa.setLandingProtectionEnabled(false, fddHandler	);
								drvSpin();

								drvSetLock("HoverOnConnectFail");
								ConnectionFailSafeBehavior cfsb = ConnectionFailSafeBehavior.HOVER;
						        fc = aircraft.getFlightController();
								fc.setConnectionFailSafeBehavior(cfsb,fddHandler	);
								drvSpin();

								drvSetLock("setVirtualStick");
                                fc.setControlMode(ControlMode.MANUAL, fddHandler);
                                fc.setVirtualStickModeEnabled(true, fddHandler);
                                fc.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                                fc.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                                fc.setVerticalControlMode(VerticalControlMode.VELOCITY);
                                fc.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
                                drvSpin();

                                if (null == mSendVirtualStickDataTimer) {
                                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                                    mSendVirtualStickDataTimer = new Timer();
                                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
                                }
                            }
                            ce.respond("FlyDroneDriver: cfg complete");
                        }
			else if (args[0].equals("dc=lft")) {
				System.out.println(LOG_TAG+ " Taking Off");
				if (AUAVsim == false) {
					drvSetLock("NoAUAVSIM-Takeoff");
					Aircraft aircraft = (Aircraft)DJISDKManager.getInstance().getProduct();
					aircraft.getFlightController().startTakeoff(fddHandler);
					drvSpin();
				}
				//try {Thread.sleep(7500);}
				//catch (Exception e) {e.printStackTrace();}

				ce.respond ("FlyDroneDriver: startTakeoff Complete");
			}
                        else if (args[0].equals("dc=rca")){
                            Aircraft aircraft = (Aircraft)DJISDKManager.getInstance().getProduct();
							fc=aircraft.getFlightController();

				            try{
                                Thread.sleep(1000);
                            } catch(Exception e){}
							System.out.println(LOG_TAG+ " Rotating clockwise");
							float deg = 0;

							try{
								deg = Float.parseFloat(args[1].substring(3));
							} catch(Exception e){
								e.printStackTrace();
								ce.respond("invalid parameter");
								return;
							} if(deg > 360 || deg < 0) {
								ce.respond("Please input degrees between 0 and 360");
								return;
							} else if (AUAVsim == false) {

								//keep yaw values between -180 and 180
								if(deg > 180)
									deg -= 360;

                                FlightControlData fcd = new FlightControlData(0,0,deg,0);
								mTimer = new Timer();
								FlightTimerTask ftt= new FlightTimerTask(fcd, mTimer, 5, ce, true);
								mTimer.schedule(ftt, 0, 100);

                                try{
                                    Thread.sleep(300);
                                } catch(Exception e){}
								return;
							}
                            ce.respond ("FlyDroneDriver: rcw complete");
                        }
						else if (args[0].equals("dc=rcw")) {
				            Aircraft aircraft = (Aircraft)DJISDKManager.getInstance().getProduct();
							fc=aircraft.getFlightController();

				            try{
                                Thread.sleep(1000);
                            } catch(Exception e){}
							System.out.println(LOG_TAG+ " Rotating clockwise");
							float deg = 0;

							try{
								deg = Float.parseFloat(args[1].substring(3));
							} catch(Exception e){
								e.printStackTrace();
								ce.respond("invalid parameter");
								return;
							} if(deg > 360 || deg < 0) {
								ce.respond("Please input degrees between 0 and 360");
								return;
							} else if (AUAVsim == false) {

						    	float curYaw = (float)fc.getState().getAttitude().yaw;

								float newYaw = curYaw + deg;
								//keep yaw values between -180 and 180
								if(newYaw > 180)
									newYaw -= 360;

                                FlightControlData fcd = new FlightControlData(0,0,newYaw,0);
								mTimer = new Timer();
								FlightTimerTask ftt= new FlightTimerTask(fcd, mTimer, 5, ce, true);
								mTimer.schedule(ftt, 0, 100);

                                try{
                                    Thread.sleep(300);
                                } catch(Exception e){}
                                System.out.println("curYaw: "+curYaw+" NewYaw: "+newYaw);

								return;
				            }

							ce.respond ("FlyDroneDriver: rcw complete");
						}
						else if (args[0].equals("dc=rcc")) {
							Aircraft aircraft = (Aircraft)DJISDKManager.getInstance().getProduct();
							fc=aircraft.getFlightController();

							try{
                                Thread.sleep(1000);
                            } catch(Exception e){}

							System.out.println(LOG_TAG+ " Rotating clockwise");
							float deg = 0;
							try{
								deg = Float.parseFloat(args[1].substring(3));
							} catch(Exception e){
								e.printStackTrace();
								ce.respond("invalid parameter");
								return;
							} if(deg > 360 || deg < 0) {
								ce.respond("Please input degrees between 0 and 360");
								return;
							} else if (AUAVsim == false) {

								float curYaw = (float)fc.getState().getAttitude().yaw;

                                float newYaw = curYaw - deg;
								//keep yaw values between -180 and 180
								if(newYaw < -180)
									newYaw += 360;

                                FlightControlData fcd = new FlightControlData(0,0,newYaw,0);
                                mTimer = new Timer();
                                FlightTimerTask ftt= new FlightTimerTask(fcd,mTimer, 5, ce, true);
                                mTimer.schedule(ftt, 0, 100);

                                try {
                                    Thread.sleep(400);
                                } catch(Exception e){}
                                System.out.println("curYaw: "+curYaw+" NewYaw: "+newYaw);

								return;
                            }
							System.out.println("respond");
							ce.respond ("FlyDroneDriver: rcc complete");
						}
                        else if (args[0].equals("dc=rgh")){
                            System.out.println("Moved Right");
			    if(AUAVsim == false) {
			    	mPitch = 0.5f;
                            	mYaw = 0;
                            	mRoll = 0;
                            	mThrottle = 0;
                            	try{
                                	Thread.sleep(1000);
                            	} catch(Exception e){}
                            	mPitch = 0;
                            }
			    ce.respond("Moved Right");
			    return;
                        }
                        else if (args[0].equals("dc=lef")){
                            System.out.println("Moved Left");
			    if (AUAVsim == false) {
                            	mPitch = -0.5f;
                            	mYaw = 0;
                            	mRoll = 0;
                            	mThrottle = 0;
                            	try{
                                	Thread.sleep(1000);
                            	} catch(Exception e){}

                            	mPitch = 0;
                            }
			    ce.respond("Moved Left");

    			    return;

                        }
                        else if (args[0].equals("dc=fwd")){
                            if(AUAVsim == false) {
			    	mPitch = 0;
                            	mYaw = 0;
                            	mRoll = 0.5f;
                            	mThrottle = 0;
                            	try{
                                	Thread.sleep(1000);
                            	} catch(Exception e){}

                            	mRoll = 0;

                            }
			    System.out.println("Moved Forward");
			    ce.respond("forward");
			    return;
                        }
                        else if (args[0].equals("dc=bck")){
                            if (AUAVsim == false) {
                            	mPitch = 0;
                            	mYaw = 0;
                            	mRoll = -0.5f;
                            	mThrottle = 0;
                            	try{
                                	Thread.sleep(1000);
                            	} catch(Exception e){}

                            	mRoll = 0;

                            }
			    ce.respond("backward");
			    System.out.println("Moved Backward");
			    return;
                        }
			else if (args[0].equals("dc=ups")) {
			    if (AUAVsim == false) {
		    	    	mPitch = 0;
                            	mYaw = 0;
                            	mRoll = 0;
                            	mThrottle = 0.2f;
                            	try{
                                	Thread.sleep(1000);
                            	} catch(Exception e){}

                            	mThrottle = 0;

                            }
			    ce.respond("Up");
			    System.out.println("Moved Upward");
			    return;

			}
			else if (args[0].equals("dc=dwn")) {
		    	    if (AUAVsim == false) {
			    	mPitch = 0;
                            	mYaw = 0;
                            	mRoll = 0;
                            	mThrottle = -0.2f;
                            	try{
                                	Thread.sleep(1000);
                            	} catch(Exception e){}
                            	mThrottle = 0;
                            }
			    ce.respond("Down");
			    System.out.println("Moved Downward");
			    return;
			}
			else if (args[0].equals("dc=lnd")) {
				System.out.println(LOG_TAG+ " Landing");
				if (AUAVsim == false) {
						drvSetLock("NoAUAVSIM-Landing");
						Aircraft aircraft = (Aircraft)DJISDKManager.getInstance().getProduct();
						aircraft.getFlightController().startLanding(fddHandler);
						drvSpin();
				}
				//try {Thread.sleep(4000);}
				//catch (Exception e) {e.printStackTrace();}

				ce.respond ("startLanding Complete");
			}
			//virtual stick test
                        else if(args[0].equals("dc=vst")){
                            float pitch = 0, roll = 0, yaw = 0, vel = 0;
                            try{
								pitch = Float.parseFloat(args[1].substring(3));
                                if(args[2].substring(3).equals("m"))
                                        pitch = -pitch;
                                roll = Float.parseFloat(args[3].substring(3));
                                if(args[4].substring(3).equals("m"))
                                        roll = -roll;
                                yaw = Float.parseFloat(args[5].substring(3));
                                if(args[6].substring(3).equals("m"))
                                        yaw = -yaw;
                                vel = Float.parseFloat(args[7].substring(3));
                                if(args[8].substring(3).equals("m"))
                                        vel = -vel;

                            System.out.println(pitch + " " + roll + " " + yaw + " " + vel);
							} catch(Exception e) {
								e.printStackTrace();
								ce.respond("invalid parameter");
								return;
							}
                            try{
                                Thread.sleep(1000);
                            } catch(Exception e){}

							Aircraft aircraft = (Aircraft)DJISDKManager.getInstance().getProduct();
							fc=aircraft.getFlightController();
							fc.setRollPitchControlMode(RollPitchControlMode.ANGLE);

							float origPitch = (float)fc.getState().getAttitude().pitch;
							float originalYaw = (float)fc.getState().getAttitude().yaw;
                            float origRoll = (float)fc.getState().getAttitude().roll;

                            FlightControlData fcd = new FlightControlData(origPitch+pitch,origRoll+roll,originalYaw+yaw,0.1f);
							mTimer = new Timer();
							FlightTimerTask ftt= new FlightTimerTask(fcd,mTimer,3, ce, false);
							mTimer.schedule(ftt, 0, 100);

                            try{
                                Thread.sleep(400);
                            } catch(Exception e){}

                            FlightControlData fcd2 = new FlightControlData(origPitch,origRoll, originalYaw, 0);
                            mTimer = new Timer();
                            FlightTimerTask ftt2 = new FlightTimerTask(fcd2, mTimer, 3, ce, true);
                            mTimer.schedule(ftt2, 0, 100);

							return;
                        }
                        else if(args[0].equals("dc=vec")){
                            System.out.println("Inside Vec");
                            int time = 0;
                            String[] dirs;
                            try{
                                System.out.println(args[1].substring(3));
                                dirs = args[1].substring(3).replace('n','-').split(",");
                                mPitch = Float.parseFloat(dirs[0]);
                                mRoll = Float.parseFloat(dirs[1]);
                                mYaw = Float.parseFloat(dirs[2]);
                                mThrottle = Float.parseFloat(dirs[3]);
                                time = Integer.parseInt(dirs[4]);
                            } catch(Exception e){
                                e.printStackTrace();
                                ce.respond("invalid parameter");
                            }
                            System.out.println();
                            /*
                            try{
								mPitch = Float.parseFloat(args[1].substring(3));
                                if(args[2].substring(3).equals("m"))
                                        mPitch = -mPitch;
                                mRoll = Float.parseFloat(args[3].substring(3));
                                if(args[4].substring(3).equals("m"))
                                        mRoll = -mRoll;
                                mYaw = Float.parseFloat(args[5].substring(3));
                                if(args[6].substring(3).equals("m"))
                                        mYaw = -mYaw;
                                mThrottle = Float.parseFloat(args[7].substring(3));
                                if(args[8].substring(3).equals("m"))
                                        mThrottle = -mThrottle;
                                time =Integer.parseInt(args[9].substring(3));

                            System.out.println(mPitch + " " + mRoll + " " + mYaw + " " + mThrottle);
							} catch(Exception e){
								e.printStackTrace();
								ce.respond("invalid parameter");
								return;
							}*/

                            if (null == mSendVirtualStickDataTimer) {
                                mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                                mSendVirtualStickDataTimer = new Timer();
                                mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
                            }
                            try{
                                Thread.sleep(time);
                            } catch(Exception e){

                            }
                            mPitch = 0;
                            mRoll = 0;
                            mYaw = 0;
                            mThrottle = 0;
                            ce.respond("VEC: did it");
                        }
						else {
							ce.respond("Error: FlyDroneDriver unknown command\n");
						}
			}
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

        class SendVirtualStickDataTask extends TimerTask {
            @Override
            public void run() {
                System.out.println("Sending Data: "+mPitch+" "+mRoll+" "+mYaw+" "+mThrottle);
                if (fc != null) {
                    fc.sendVirtualStickFlightControlData(
                        new FlightControlData(
                                mPitch, mRoll, mYaw, mThrottle
                        ), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                            }
                        }
                    );
                }
            }
        }

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

