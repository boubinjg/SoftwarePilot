package org.reroutlab.code.auav.routines;


import java.util.HashMap;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.CoapHandler;


abstract public class AuavRoutines implements Runnable {
		HashMap<String, String> d2p = null;
		public String params = "";
		final int TEXT_PLAIN=0;
    /**
     * Set Linux simulator -- AUAVsim; Android/DJI -- No-AUAVsim
     */
		public String AUAVsim= "AUAVsim"; // No-AUAVsim or AUAVsim
		public int TIMEOUT=180000; // 3xsixty second timeout
		public String IP = "127.0.0.1";
		public void setSimOn() {
				AUAVsim="AUAVsim";
		}
		public void setSimOff() {
				AUAVsim="No-AUAVsim";
		}

		public String getSim() {
				return AUAVsim;
		}
		public void setParams(String p) {
				params = p;
				return;
		}
		public String invokeHostDriver(String command, String IP, CoapHandler ch, boolean gov){
				if(gov) {
						try {
								//System.out.println("URI = "+IP+":"+portStr);
								URI uri = new URI("coap://"+IP+":5117/cr");
								CoapClient client = new CoapClient(uri);
								client.setTimeout(TIMEOUT);

								client.put(ch,command,TEXT_PLAIN);
								return("Success");
								//return(response.getResponseText());
						}
						catch (Exception e) {
								return("Unable to reach driver host");
						}
				}
				return "InvokeDriver with Governor Executed";
		}
		public String invokeDriver(String dn, String params, CoapHandler ch) {
        params = params + "-dp="+AUAVsim;

        if (d2p == null) {
						try {
								URI uri = new URI("coap://127.0.0.1:5117/cr");
								CoapClient client = new CoapClient(uri);
								CoapResponse response = client.put("dn=list",TEXT_PLAIN);
								String ls = response.getResponseText();
								d2p = new HashMap<String,String>();
								String[] lines = ls.split("\n");
								for (int x=0;x<lines.length; x++) {
										String[] data = lines[x].split("-->");
										if (data.length == 2) {
												d2p.put(data[0].trim(),data[1].trim());
										}
								}

						}
						catch (Exception e) {
								d2p = null;
								System.out.println("AUAVRoutine invokeDriver error");
								e.printStackTrace();
								return "Invoke Error";
						}
				}
				//System.out.println("After lst");
				if (d2p != null) {
						String portStr = d2p.get(dn);
						if (portStr != null) {
								try {
										//System.out.println("URI = "+IP+":"+portStr);
										URI uri = new URI("coap://127.0.0.1:"+portStr+"/cr");
										if (dn.contains(":") == false ) {
												String[] hostArry = dn.split(":");
												if (hostArry.length == 2) {
														uri = new URI("coap://"+portStr+"/cr");
												}
										}
										CoapClient client = new CoapClient(uri);
										client.setTimeout(TIMEOUT);

										client.put(ch,params,TEXT_PLAIN);
										return("Success");
										//return(response.getResponseText());
								}
								catch (Exception e) {
										return("Unable to reach driver " + dn + "  at port: " + portStr);
								}
						}
						else {
								return ("Unable to find driver: "+ dn);
						}
				}
				return("InvokeDriver: Unreachable code touched");
		}

		String auavLock = "continue";
    synchronized void auavLock(String value) {
				auavLock = value;
		}
		public void auavSpin() {
				while (auavLock.equals("continue") == false) {
						try { Thread.sleep(1000); }
						catch (Exception e) {}
				}
		}

		public AUAVHandler auavResp = new AUAVHandler();
		class AUAVHandler {
				private String resp = "Unset";
				public String getResponse() {return resp;}

				public CoapHandler ch;
				AUAVHandler () {
						ch = new CoapHandler() {
										@Override public void onLoad(CoapResponse response) {
												resp = response.getResponseText();
												auavLock("continue");
										}
										@Override public void onError() {
												resp = "CoapHandler Error";
												System.out.println("RoutineLock: " +auavLock+"-FAILED");
												auavLock("continue");
										}};
				}
		}

		abstract public String startRoutine();
		abstract public String stopRoutine();

}

