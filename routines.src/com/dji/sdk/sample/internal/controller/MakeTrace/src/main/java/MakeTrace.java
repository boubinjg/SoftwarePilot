package org.reroutlab.code.auav.routines;

import java.util.HashMap;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.FileOutputStream;

/**
 * MakeTrace is a routine that explores all possible sensing situations 
 * for a AUAV and dumps them to a directory for PicTraceDriver
 * Invoke this routine through external commands driver
 * <br><b>URL: coap:\\localhost:port\cr?</b>
 * <br>Example: coap:\\127.0.0.1:5117\cr?dn=rtn-dc=start-dp=MakeTrace-dp=/dirname-dp=Duration-dp=SafetyTime
 * <br>
 * Note: all AUAV drivers have resource component "cr" for coap resource
 *
 * @author  Christopher Charles Stewart
 * @version 1.0.1
 * @since   2017-10-13
 */

public class MakeTrace extends org.reroutlab.code.auav.routines.AuavRoutines{
		/**
		 *	 Check forceStop often and safely end routine if set
		 */
		public boolean forceStop = false;

		/**
		 *	 Routines are Java Threads.  
		 *   The run() function is the 
		 *	 starting point for execution. 
		 * @version 1.0.1
		 * @since   2017-10-01			 
		 */
		public void run() {
				String succ = "";

				String args[] = params.split("-");
				String fileName = args[0].substring(3);
				long safetyT = Long.valueOf(args[2].substring(3));
				long duration = Long.valueOf(args[1].substring(3));				

				succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageDriver",
														"dc=dir-dp="+fileName, chVoid );
				
				System.out.println("MakeTrace: Starting");
				long unitT = 0;
				long unitTStart = System.currentTimeMillis();
				captureTimeSlice();
				long unitTStop = System.currentTimeMillis();
				unitT = unitTStop - unitTStart;
				
				long curUnitT = 0;
				long tST = System.currentTimeMillis();
				boolean failure = false;
				System.out.println("MakeTrace: UnitT:" + unitT);
				while ((curUnitT < duration) && (failure == false) ) {
						long nextSliceTime =  tST + (unitT + safetyT)* curUnitT;

						if (nextSliceTime > System.currentTimeMillis()) {
								try {Thread.sleep(nextSliceTime - System.currentTimeMillis()); }
								catch (Exception e) {e.printStackTrace();}
						}
						
						captureTimeSlice();

						// detect failures
						if (System.currentTimeMillis() > (tST + (unitT + safetyT)* (curUnitT + 1)) ) {
								failure = true;
						}						
						curUnitT++;
						System.out.println("MakeTrace: Explore loop variables --- curUnitT:" + curUnitT
															 + "  failure: " + failure + " duration:" + duration);						
				}

				if (failure ==false) {
						auavLock("DumpCaptureImage");
						succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageDriver",
																"dc=dmp", auavResp.ch );
						auavSpin();
						System.out.println("MakeTrace: "+auavResp.getResponse());
				}
				else {
						System.out.println("MakeTrace: Failed");
				}
				
		}


		/**
		 *	 The captureTimeSlice function executes all possible 
		 *   drone activities for the trace file
		 * @version 1.0.1
		 * @since   2017-10-15
		 */
		public void  captureTimeSlice() {
				String succ = "";
				// set location 0,0,0 -- snap pic
				auavLock("GetFirstPicture");
				succ = invokeDriver("org.reroutlab.code.auav.drivers.LocationDriver",
														"dc=set-dp=0.00-dp=0.00-dp=0.00", chVoid );				
				succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageDriver",
														"dc=null", auavResp.ch );
				auavSpin();
				System.out.println("MakeTrace: FirstPicDone - " + auavResp.getResponse() );
				
				
				// set location 0,0,4 -- snap pic				
				auavLock("ConfigFlight");
				succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver",
														"dc=cfg", auavResp.ch );
				auavSpin();
				auavLock("TakeOffGetPic");
				System.out.println("MakeTrace: Taking off" );
				
				succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver",
														"dc=lft", auavResp.ch );
				auavSpin();
				System.out.println("MakeTrace: " + auavResp.getResponse() );								
				succ = invokeDriver("org.reroutlab.code.auav.drivers.LocationDriver",
														"dc=set-dp=0.00-dp=0.00-dp=4.00", chVoid );
				succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageDriver",
														"dc=null", auavResp.ch );
				auavSpin();
				System.out.println("MakeTrace: " + auavResp.getResponse() );				

				
				// set location 0,0,5 -- snap pic				
				auavLock("Up1ft");
				System.out.println("MakeTrace: Up 1 ft" );								
				succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver",
														"dc=ups", auavResp.ch );
				auavSpin();
				System.out.println("MakeTrace: " + auavResp.getResponse() );								
				auavLock("Snap2ndPic");				
				succ = invokeDriver("org.reroutlab.code.auav.drivers.LocationDriver",
														"dc=set-dp=0.00-dp=0.00-dp=5.00", chVoid );				
				succ = invokeDriver("org.reroutlab.code.auav.drivers.CaptureImageDriver",
														"dc=null", auavResp.ch );
				auavSpin();
				System.out.println("MakeTrace: " + auavResp.getResponse() );



				// land for next sequence
				System.out.println("MakeTrace: Landing" );
				auavLock("Landing ");
				succ = invokeDriver("org.reroutlab.code.auav.drivers.FlyDroneDriver",
														"dc=lnd", auavResp.ch );
				auavSpin();
				System.out.println("MakeTrace: " + auavResp.getResponse() );
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

		CoapHandler chVoid = new CoapHandler() { // Doesn't reset lock or get response
						@Override public void onLoad(CoapResponse response) {
						}						
						@Override public void onError() {	System.err.println("FAILED");	}};


		public MakeTrace() {t = new Thread (this, "Main Thread");	}
		public String startRoutine() {
				if (t != null) {
						t.start(); return "MakeTrace: Started";
				}
				return "MakeTrace not Initialized";
		}
		public String stopRoutine() {
				forceStop = true;	return "BasicRoutine: Force Stop set";
		}

		

		
}
