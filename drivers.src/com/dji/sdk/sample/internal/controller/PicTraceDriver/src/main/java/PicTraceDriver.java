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


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.h2.*;
import org.h2.jdbcx.JdbcConnectionPool;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * PicTraceDriver pulls images from a directory and labels them as
 * as a corresponding X,Y,Z and T
 *
 * <br><b>URL: coap:\\localhost:port\cr?</b>
 * <br>Example: coap:\\127.0.0.1:10452\cr?dc=get-dp=/data/HelloWorld
 * <br>
 * Note: all AUAV drivers have resource component "cr" for coap resource
 *
 * @author  Christopher Charles Stewart
 * @version 1.0.1
 * @since   2017-10-01
 */
public class PicTraceDriver extends com.dji.sdk.sample.internal.controller.AuavDrivers {

		private static int LISTEN_PORT = 0;
		private int driverPort = 0;
		private CoapServer cs;
		private Byte[] pic;
		private long startTime=0;
		private int  grn=1000; //1000 = 1 sec
		private String dbName="jdbc:h2:mem:PicTraceDriver;DB_CLOSE_DELAY=-1";

		/**
		 *		usageInfo += "dc=[cmd]-dp=[option]<br>"
		 *		usageInfo += "dir -- <br>"
		 *		usageInfo += "set -- Set the directory of trace contents<br>"
		 *		usageInfo += "qry -- Issue an SQL query for meta data<br>"
		 *		usageInfo += "qrb -- Issue an SQL query for image data<br>"
		 *		usageInfo += "help -- Return this usage information.<br>"
		 *		usageInfo += "option -- <br>"
		 *		usageInfo += "Directoy name (dir) -- <br>"
		 *		usageInfo += "Granularity as int in millis -- <br>"
		 *		usageInfo += "SQL-String -- Executed against SQLite Table: id, X, Y, Z, time<br>"
		 * @author  Christopher Charles Stewart
		 * @version 1.0.2
		 * @since   2017-05-01
		 */
		public String getUsageInfo() {
				String usageInfo = "";
				usageInfo += "dc=[cmd]-dp=[option]\n";
				usageInfo += "cmd: \n";
		 		usageInfo += "set -- Set the directory of trace contents\n";
		 		usageInfo += "qry -- Issue an SQL query for meta data\n";
		 		usageInfo += "qrb -- Issue an SQL query for image data\n";
		 		usageInfo += "help -- Return this usage information.\n";
		 		usageInfo += "option -- \n";
		 		usageInfo += "Directoy name (dir) -- \n";
		 		usageInfo += "Granularity as int in millis -- \n";
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
								ce.respond("PicTrace: " + queryH2meta(qry));
								break;
						case "dc=qrb":
								String qrb = args[1].substring(3);
								byte[] b = queryH2vb(qrb);
								if (b == null ) {
										ce.respond("PicTrace: Error B is null");
										break;
								}
								ServerSocket ss = null;
								try {
										ss = new ServerSocket(44044,1);
										if (ss == null) {throw new Exception("PicTrace: null serversocket");}
										ss.setSoTimeout(5000);
										ce.respond("PicTrace: port="+ss.getLocalPort());
								}
								catch (Exception e) {e.printStackTrace(); break;}

								try {
										if (ss != null) {
												Socket cli = ss.accept();
												cli.getOutputStream().write(b);
												cli.close();
												ss.close();
										}
								}
								catch (Exception e) {	e.printStackTrace();break;}

								break;
						case "dc=dir":
								String dir = args[1].substring(3);
								String[] index = new String[1024];
								int count = 0;
								// open and mem-map index.dat
								System.out.println(dir+"dirrreee");
								try {
										BufferedReader br = new BufferedReader(new FileReader(dir+"/index.dat"));
										while ((index[count] = br.readLine()) != null) {
												if (index[count].startsWith("#") == false) {
														count++;
												}
										}
								}
								catch (Exception e) {
										e.printStackTrace();
										ce.respond("PicTrace: Unable to open and load index.dat in " + dir);
										break;
								}
								// Now try to load images and put into database
								try {
										for (int i = 0; i < count; i++) {
												String entry = index[i];
												String[] element = entry.split("-");
												Path path = Paths.get(dir+"/"+element[5].substring(9));
												byte[] vb = Files.readAllBytes(path);
												addPicH2(vb,element[1].trim().substring(5),
													    element[2].trim().substring(2),
													    element[3].trim().substring(2),
													    element[4].trim().substring(2));

										}
								}
								catch (Exception e) {
										e.printStackTrace();
										ce.respond("PicTrace: Couldn't load images");
										break;
								}

								ce.respond("PicTrace: set");
								break;
						default:
								ce.respond("Error: PicTraceDriver unknown command\n");
						}
				}


				public bdResource() {
						super("cr");	getAttributes().setTitle("cr");
				}

				public void addPicH2(byte[] vb, String time, String X, String Y, String Z) {
						try {

								Connection conn = DriverManager.getConnection(dbName,
																															"user", "password");
								String sql = "INSERT INTO data (pic, time, X, Y, Z) VALUES ( ?, "
										+ Long.parseLong(time.trim()) + ",\'" + X + "\',\'" + Y + "\',\'" + Z + "\')";
								PreparedStatement stmt = conn.prepareStatement(sql);
								stmt.setBytes(1,vb);
								stmt.execute();
								conn.close();
						}
						catch (Exception e) {	e.printStackTrace();}
				}

				public byte[]queryH2vb(String query) {
						String outLine = "";
						try {
								Connection conn = DriverManager.getConnection(dbName,
																															"user", "password");
								Statement stmt = conn.createStatement();
								ResultSet rs = stmt.executeQuery(query);
								if (rs.next()) {
										return (rs.getBytes("pic"));
								}
								conn.close();
						}
						catch (Exception e) {	e.printStackTrace();	}
						return null;
				}

				public String queryH2meta(String query) {
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
						return (outLine);
				}
		}

		public void makeH2() {
				try {
						Class.forName("org.h2.Driver");
						Connection conn = DriverManager.getConnection(dbName,
																													"user", "password");
						conn.createStatement().executeUpdate("CREATE TABLE data ("
																								 +" key  INTEGER AUTO_INCREMENT,"
																								 +" pic  VARBINARY, "
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
		public PicTraceDriver() throws Exception {
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
				Logger.getLogger(PicTraceDriver.class.getName());
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

