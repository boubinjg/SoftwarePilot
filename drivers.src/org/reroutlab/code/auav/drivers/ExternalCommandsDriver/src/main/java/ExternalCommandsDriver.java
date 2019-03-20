package org.reroutlab.code.auav.drivers;

import java.util.*;
import java.lang.System.*;
//import org.reroutlab.code.auav.interfaces.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.io.File;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.reroutlab.code.auav.routines.AuavRoutines;


/**
 * This driver forwards commands from a network source to any other driver.
 * It also supports a "list" command that tells of all available modules.
 * This driver runs on port 5117.
 * <br><b>URL: coap:\\localhost:port\cr?</b>
 * <br>Example: coap:\\127.0.0.1:5117\cr?dn=org.reroutlab.code.auav.drivers.DroneGimbalDriver-dc=pos
 * <br>
 * Note: all AUAV drivers have resource component "cr" for coap resource
 *
 * @author  Christopher Charles Stewart
 * @version 1.0.2
 * @since   2017-05-01
 */
public class ExternalCommandsDriver extends org.reroutlab.code.auav.drivers.AuavDrivers { //???
 		/**
		 *
		 *		<br>usageInfo += "dn=[driver name]-dc=[cmd]-dp=[option]\n"
		 *		<br>usageInfo += "dc -- driver command \n"
		 *		<br>usageInfo += "dn -- driver name    \n"
		 *		<br>usageInfo += "dp -- sequence of parameters\n"
		 *		<br>usageInfo += "dn=list -- list all drivers\n"
		 *    <br>
		 *
		 * @return usageInfo
		 *
		 */
		public String getUsageInfo() {
				String usageInfo="";
				usageInfo += "dn=[driver name]-dc=[cmd]-dp=[option]\n";
				usageInfo += "dc -- driver command \n";
				usageInfo += "dn -- driver name    \n";
				usageInfo += "dp -- sequence of parameters\n";
				usageInfo += "dn=list -- list all drivers\n";
				return usageInfo;
		}

		private class ecdResource extends CoapResource { //CoapResource is a basic implementation of a resource
				public ecdResource() {
						super("cr"); getAttributes().setTitle("cr");
				}
				@Override
				public void handlePUT(CoapExchange ce) { //The Class CoapExchange represents an exchange of a CoAP request and response
						// Split on & and = then on ' '
						String outLine = "";
						byte[] payload = ce.getRequestPayload();//ce.getrequest.getpayload
						String inputLine = new String(payload);

						//The line below is a hack.  We use "-" to separate
						//parameters, but we also use --> convert driver map 2 port string
						//We need to change --> to something else to parse correct.
						//CS - 6/11/2018
						inputLine = inputLine.replace("-->","BB>");
						String[] args = inputLine.split("-"); //???

						ecdLogger.log(Level.WARNING, "Send the cmd: " + inputLine );
						String driverName = args[0].substring(args[0].indexOf("=")+1,args[0].length());
						driverName = driverName.trim();//Returns a copy of the string,
						                               //with leading and trailing whitespace omitted

						//print out all activated drivers if args[0]==list
						if (driverName.equals("list")) {
								String output = "";
								if (driver2port != null) {
										Set keys = driver2port.keySet(); //HashMap<port, usageInfo>
										for (Iterator i = keys.iterator(); i.hasNext(); ) {
												String name = (String) i.next();
												String value = (String) driver2port.get(name);
												output = output + name + "-->" + value + "\n";
										}
								}
								outLine = output;
								ce.respond(outLine);
						}
						if (driverName.equals("add")) {
								String output = "";
								String[] newDrivers = args[1].split("\n");
								for (String newD : newDrivers ) {
										String[] data = newD.split("BB>");
										//System.out.println("A: " + data[0]);
										if (data.length == 2) {
												driver2port.put(data[0].trim(),data[1].trim());
												//System.out.println("dn:"+data[0]+"   port:"+data[1]);
										}
								}
								ce.respond("ExternalCommandsDriver: Added "+args[0]);
						}
						else if (driverName.equals("rtn")) {
								String routineCMD = args[1].substring(args[1].indexOf("=")+1,args[1].length());
								String routineName = args[2].substring(args[2].indexOf("=")+1,args[2].length());
								if (routineName.contains(".") == false) {
										// We were given the shortcut name
										routineName = "org.reroutlab.code.auav.routines."+routineName;
								}
								org.reroutlab.code.auav.routines.AuavRoutines ar = null;
								try {
										ar=(org.reroutlab.code.auav.routines.AuavRoutines)(Class.forName(routineName).newInstance());
								}
								catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
										System.out.println("AUAV Drivers: External Command can not instantiate class");
								}
								if (ar == null) {
										System.out.println("AUAV Drivers: External Command can not instantiate routine");
								}
								else if (routineCMD.equals("start")) {
										outLine = ar.startRoutine();
										String p = "";
										for (int pcount = 3; pcount < args.length;pcount++) {
												if (pcount > 3)
														p = p+ "-"+ args[pcount];
												else
														p = args[pcount];
										}
										ar.setParams(p);
								}
								else if (routineCMD.equals("stop")) {
										outLine = ar.stopRoutine();
								}
								else {
										outLine = "Routines currently support start/stop only";
								}

								ce.respond(outLine);
						}
						else if (driver2port.containsKey(driverName) )  {
								String driverPort = ((String)driver2port.get(driverName));

								CoapClient client = new CoapClient("coap://127.0.0.1:"+driverPort.trim()+"/cr");
								if (driverPort.contains(":")) {
										client = new CoapClient("coap://"+driverPort.trim()+"/cr");
								}

								String c = "";
								for (int i=1; i < args.length;i++) {
										c = c + args[i] + "-";
								}
								c = c.trim();
								ecdLogger.log(Level.WARNING, "Send the cmd: " + c );
								CoapResponse response = client.put(c,0);//create response
								ce.respond(response.getResponseText());
						}
						else {
								ce.respond("Error: Unable to find driver " + inputLine);
						}
				}
		}

		//-----------------------------------------------------------
		// The code below is largely templated material that won't
		// change much between drivers.  However, I have not added
		// to the interface class in case there is a need for
		// customization as the projects advance.
		//
		// Obviously, this makes updating all drivers challenging,
		// but c'est la vie
		//
		// - Christopher Stewart
		// September 18
		//-----------------------------------------------------------

		public CoapServer cs; //declare private object coapserver
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

		//implement allows to use Thread
		//indicate port number
		private static int LISTEN_PORT = 5117;
		private int driverPort = 0;

    private static Logger ecdLogger =
				Logger.getLogger(ExternalCommandsDriver.class.getName());//return the name of the entity represented by this class object
    				//get Logger object by calling getLogger receive the name of the ExternalCommandDriver.class'name
		/**
		 *
		 *
		 * This specifying which message levels will be logged by this logger
		 *
		 */
		public void setLogLevel(Level l) {
				ecdLogger.setLevel(l);
						}

		/**
		 *
		 * This function just to check if serverSocket is null, set the local port number in to -1
		 *
		 * @return local port number
		 *
		 */
		public int getLocalPort() {
						return driverPort;//???
		}

		private HashMap driver2port;  // key=drivername value={port,usageInfo}

		/**
		 *
		 * This function help construct Map
		 *
		 * @param m Hashmap connecting driver strings and port strings
		 *
		 */
		public void setDriverMap(HashMap<String, String> m) {
				if (m != null) {
						driver2port = new HashMap<String, String>(m);
				}
		}


		public ExternalCommandsDriver() {
				//ecdLogger.log(Level.FINEST, "In Constructor");
				try {
						NetworkConfig nc = new NetworkConfig();
						cs = new CoapServer(nc); //create a new coapserver
						InetSocketAddress bindToAddress = new InetSocketAddress( LISTEN_PORT);
						cs.addEndpoint(new CoapEndpoint(bindToAddress));//Adds an Endpoint to the server.
						driverPort = bindToAddress.getPort();

						cs.add(new ecdResource());//Add a resource to the server
				}
				catch(Exception e) {
				}



		}




}

