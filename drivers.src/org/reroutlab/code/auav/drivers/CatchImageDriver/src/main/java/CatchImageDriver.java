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




public class CatchImageDriver extends org.reroutlab.code.auav.drivers.AuavDrivers {

		private int PIC_LABEL = 0;



		private CoapServer cs;
		public CoapServer getCoapServer() {
				return (cs);
		}
		//start a server
		public static void main(String[] args) {
				try { 
						CatchImageDriver cid = new CatchImageDriver();
						cid.getCoapServer().start();
				}
				catch (Exception e) {
						cidLogger.log(Level.WARNING, "Unable to start server" + e.getMessage());
				}
				
		}

		//implement allows to use Thread
		//indicate port number
		private static int LISTEN_PORT = 0;
		private int driverPort = 0;


		
    		private static Logger cidLogger =
				Logger.getLogger(CatchImageDriver.class.getName());

		public void setLogLevel(Level l) {
				cidLogger.setLevel(l);
		}		
		
		public int getLocalPort() {
				return driverPort;
		}
		
		private String usageInfo=";dc=getImage PIC_LABEL=##; ";

		public String getUsageInfo() {
				return usageInfo;
		}

		

		private HashMap driver2port;  // key=drivername value={port,usageInfo}

		public void setDriverMap(HashMap<String, String> m) {
				if (m != null) {
						driver2port = new HashMap<String, String>(m);
				}
		}


		//constructor
		public CatchImageDriver() throws Exception {
				cidLogger.log(Level.FINEST, "In Constructor");
				cs = new CoapServer(); //initilize the server
				InetSocketAddress bindToAddress = new InetSocketAddress( LISTEN_PORT);//get the address
				CoapEndpoint tmp = new CoapEndpoint(bindToAddress); //create endpoint
				cs.addEndpoint(tmp);//add endpoint to server				
				tmp.start();//Start this endpoint and all its components.
				driverPort = tmp.getAddress().getPort();
				
				cs.add(new cidResource());
				
		}
		private class cidResource extends CoapResource {
				public cidResource() {
						super("cr");//???
						getAttributes().setTitle("cr");//???
				}
				/**
				 *
				 * This function process the input commands, if the there is list, append to 
				 * the string, if not call sendTo methond find out the matching information
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
						
						// Format: dc=driver_cmd [driver_prm=driver_arg]*						
						if (args[0].equals("dc=help")) {
								ce.respond(getUsageInfo());
						}
						else if (args[0].equals("dc=getImage")) {
								PIC_LABEL=Integer.parseInt(args[1].substring(10));
								ce.respond ("OK");
						}
						else {
								ce.respond("Error: CatchImageDriver unknown command\n");
						}
						
						
				}
		}


		
	
}



