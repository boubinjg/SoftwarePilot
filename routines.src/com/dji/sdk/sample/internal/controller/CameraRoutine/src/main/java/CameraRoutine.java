package org.reroutlab.code.auav.routines;

import java.util.HashMap;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;

/**
 * BasicRoutine takes off, calibrates camera and lands
 * It does not sense from it's environment
 * Invoke this routine through external commands driver
 * <br><b>URL: coap:\\localhost:port\cr?</b>
 * <br>Example: coap:\\127.0.0.1:5117\cr?dn=rtn-dc=start-dp=BasicRoutine
 * <br>
 * Note: all AUAV drivers have resource component "cr" for coap resource
 *
 * @author  Christopher Charles Stewart
 * @version 1.0.3
 * @since   2017-10-01
 */
public class CameraRoutine extends org.reroutlab.code.auav.routines.AuavRoutines{
		/**
		 *	 Check forceStop often and safely end routine if set
		 */
		public boolean forceStop = false;

		/**
		 *	 Routines are Java Threads.  The run() function is the 
		 *	 starting point for execution. 
		 * @version 1.0.1
		 * @since   2017-10-01			 
		 */
		public void run() {

				succ = invokeDriver("org.reroutlab.code.auav.drivers.CameraDriver",
					"dc=getim", chResp );
				rtnSpin();
				System.out.println("BasicRoutine: " + chResp);
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
		private String csLock = "free";
		private String resp="";
		CoapHandler chResp = new CoapHandler() {
						@Override public void onLoad(CoapResponse response) {
								resp = response.getResponseText();
								rtnLock("barrier-1");
						}
						
						@Override public void onError() {
								System.err.println("FAILED");
								rtnLock("barrier-1");
						}};
				


		public CameraRoutine() {t = new Thread (this, "Main Thread");	}
		public String startRoutine() {
				if (t != null) {
						t.start(); return "CameraRoutine: Started";
				}
				return "BasicRoutine not Initialized";
		}
		public String stopRoutine() {
				forceStop = true;	return "BasicRoutine: Force Stop set";
		}
		synchronized void rtnLock(String value) {
				csLock = value;
		}
		public void rtnSpin() {
				while (csLock.equals("barrier-1") == false) {
						try { Thread.sleep(1000); }
						catch (Exception e) {}
				}

		}
		

		
}
