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


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.h2.*;
import org.h2.jdbcx.JdbcConnectionPool;

/**
 * LocationDriver sets, reports and stores the physical location
 * of the AUAV (x,y,z) over time.  Note, this driver does NOT move
 * the AUAV.  Interact with this driver via CoAP (californium)
 * <br><b>URL: coap:\\localhost:port\cr?</b>
 * <br>Example: coap:\\127.0.0.1:10452\cr?dc=set-dp=7.123-dp=7.123-dp=2.123
 * <br>
 * Note: all AUAV drivers have resource component "cr" for coap resource
 *
 * @author  Christopher Charles Stewart
 * @version 1.0.1
 * @since   2017-10-01
 */
public class LocationDriver extends org.reroutlab.code.auav.drivers.AuavDrivers {

		private static int LISTEN_PORT = 0;
		private long startTime = 0;
		private int driverPort = 0;
		private CoapServer cs;
		private String Xcur="0",Ycur="0",Zcur="0";
		private String dbName="jdbc:h2:mem:LocationDriver;DB_CLOSE_DELAY=-1";

		/**
		 *		usageInfo += "dc=[cmd]-dp=[option]\n";
		 *		usageInfo += "cmd: \n";
		 *		usageInfo += "set -- Set the X,Y,Z coordinate of current location\n";
		 *		usageInfo += "get -- Get the X,Y,Z coordinates of the current location\n";
		 *		usageInfo += "qry -- Issue an SQL query against prior data\n";
		 *		usageInfo += "help -- Return this usage information.\n";
		 *		usageInfo += "option -- \n";
		 *		usageInfo += "X/Y/Z -- Coordinates\n";
		 *		usageInfo += "SQL-String -- Executed against SQLite Table: id, X, Y, Z, time\n";
		 * @author  Christopher Charles Stewart
		 * @version 1.0.2
		 * @since   2017-05-01
		 */
		public String getUsageInfo() {
				String usageInfo = "";
				usageInfo += "dc=[cmd]-dp=[option]\n";
				usageInfo += "cmd: \n";
		 		usageInfo += "set -- Set the X,Y,Z coordinate of current location\n";
		 		usageInfo += "get -- Get the X,Y,Z coordinates of the current location\n";
		 		usageInfo += "qry -- Issue an SQL query against prior data\n";
		 		usageInfo += "help -- Return this usage information.\n";
		 		usageInfo += "option -- \n";
		 		usageInfo += "X/Y/Z -- Coordinates\n";
		 		usageInfo += "SQL-String -- Executed against SQLite Table: id, X, Y, Z, time\n";
				return usageInfo;
		}

		//extends CoapResource class
		private class bdResource extends CoapResource {
				@Override
				public void handlePUT(CoapExchange ce) {
						// Split on & and = then on ' '
						String outLine = "";
						byte[] payload = ce.getRequestPayload();
						String inputLine = new String(payload);
						int AUAVsim = 0;
						if (inputLine.contains("dp=AUAVsim")) {
								AUAVsim = 1;
						}
						String[] args = inputLine.split("-");//???

						switch (args[0]) {
						case "dc=help":
								ce.respond(getUsageInfo());
								break;
						case "dc=qry":
								String qry = args[1].substring(3);
								ce.respond("Location: "+queryH2(qry));
								break;
						case "dc=tme":
								ce.respond("Location: time="+getStartTime());
								break;
						case "dc=set":
								String Xpos = args[1].substring(3);
								String Ypos = args[2].substring(3);
								String Zpos = args[3].substring(3);
								addPositionH2(Xpos,Ypos,Zpos);
								Xcur = Xpos;Ycur=Ypos; Zcur=Zpos;
								ce.respond("Location: set");
								break;
						case "dc=get":
								ce.respond("Location: x="+Xcur+"-y="+Ycur+"-z="+Zcur);
								break;
						default:
								ce.respond("Error: LocationDriver unknown command\n");
						}
				}


				public bdResource() {
						super("cr");	getAttributes().setTitle("cr");
				}

				public void addPositionH2(String X, String Y, String Z) {
						try {

								Connection conn = DriverManager.getConnection(dbName,
																															"user", "password");
								Statement stmt = conn.createStatement();
								String sql = "INSERT INTO data (time, X, Y, Z) VALUES ("
										+ (System.currentTimeMillis() - startTime) + "," + X + "," + Y + "," + Z + ")";
								stmt.executeUpdate(sql);
								conn.close();
						}
						catch (Exception e) {	e.printStackTrace();}
				}

				public String queryH2(String query) {
						String outLine = "";
						try {
								Connection conn = DriverManager.getConnection(dbName,
																															"user", "password");
								Statement stmt = conn.createStatement();
								ResultSet rs = stmt.executeQuery(query);
								while(rs.next()) {
										outLine += "key="+rs.getInt("key")+
												"-time="+rs.getLong("time")+"-X="+rs.getString("X")+
												"-Y="+rs.getString("Y")+"-Z="+rs.getString("Z")+"---";
								}
								conn.close();
						}
						catch (Exception e) {	e.printStackTrace();	}
						return outLine;
				}
		}

		public void makeH2() {
				try {
						Class.forName("org.h2.Driver");
						Connection conn = DriverManager.getConnection(dbName,
																													"user", "password");
						conn.createStatement().executeUpdate("CREATE TABLE data ("
																								 +" key  INTEGER AUTO_INCREMENT,"
																								 +" time BIGINT, "
																								 +" X VARCHAR(16), "
																								 +" Y VARCHAR(16), "
																								 +" Z VARCHAR(16) )");
						conn.close();
				}
				catch(Exception e) {e.printStackTrace();}
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

		public CoapServer getCoapServer() {
				return (cs);
		}
		public LocationDriver() throws Exception {
				bdLogger.log(Level.FINEST, "In Constructor");
				cs = new CoapServer(); //initilize the server
				InetSocketAddress bindToAddress =
						new InetSocketAddress( LISTEN_PORT);//get the address
				CoapEndpoint tmp = new CoapEndpoint(bindToAddress); //create endpoint
				cs.addEndpoint(tmp);//add endpoint to server
				startTime = System.currentTimeMillis();
				tmp.start();//Start this endpoint and all its components.
				driverPort = tmp.getAddress().getPort();
				cs.add(new bdResource());
				makeH2();
		}
		private static Logger bdLogger =
				Logger.getLogger(LocationDriver.class.getName());
		public void setLogLevel(Level l) {
				bdLogger.setLevel(l);
		}

		public int getLocalPort() {
				return driverPort;
		}
		private HashMap driver2port;  // key=drivername value={port,usageInfo}
		public void setDriverMap(HashMap<String, String> m) {
				if (m != null) {
						driver2port = new HashMap<String, String>(m);
				}
		}




}

