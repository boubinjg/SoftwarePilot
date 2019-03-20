package org.reroutlab.code.auav.drivers;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.lang.System.*;
//import org.reroutlab.code.auav.interfaces.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.InetSocketAddress;
import java.net.SocketException;
import org.reroutlab.code.auav.routines.AuavRoutines;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Environment;
import android.app.Application;
import android.graphics.Bitmap;
import dji.sdk.camera.Camera;
import dji.sdk.camera.FetchMediaTask;
import dji.sdk.camera.FetchMediaTaskContent;
import dji.sdk.camera.FetchMediaTaskScheduler;
import dji.sdk.camera.MediaFile;
import dji.sdk.camera.MediaManager;
import dji.sdk.camera.FetchMediaTask;
import dji.sdk.camera.FetchMediaTaskContent;
import dji.sdk.camera.FetchMediaTaskScheduler;
import dji.sdk.camera.DownloadListener;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.common.camera.SettingsDefinitions;
import dji.common.util.CommonCallbacks;
import dji.common.error.DJIError;
import dji.common.error.DJICameraError;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;
import org.h2.*;
import org.h2.jdbcx.JdbcConnectionPool;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.concurrent.locks.*;
import java.util.concurrent.*;
/**
 * CaptureImageDriver connects to DJI <i>and</i> local compute.
 * It supports commands related to capturing images.
 * Interact with this driver via CoAP (californium)
 * <br><b>URL: coap:\\localhost:port\cr?</b>
 * <br>Example: coap:\\127.0.0.1:10452\cr?dc=get
 * <br>
 * Note: all AUAV drivers have resource component "cr" for coap resource
 *
 * The flow of commands in the following driver for getim command is:
 * 1. Sets Camera Mode to Shoot Photo Mode
 * 2. Sets Shoot Photo Mode to Shoot Single Photo
 * 3. Captures Photo
 * 4. Changes Camera Mode to Media Download Mode (Not sure why it is being done, but docs say it should be done)
 * 5. Gets the Photo index from the drone.
 * 6. Downloads the Photo.
 *
 * These steps will be repeated everytime the command is called.
 *
 *
 * @author  Venkata Mandadapu
 * @version 1.0.2
 * @since   2017-10-2
 */
public class CaptureImageDriver extends org.reroutlab.code.auav.drivers.AuavDrivers {

	private static int LISTEN_PORT = 0;
	private int driverPort = 0;
	private CoapServer cs;
	private int lclLastReading = 100;
	private long startTime = 0;
	private List<MediaFile> mediaFileList = new ArrayList<MediaFile>();
	private MediaManager mMediaManager;
	private MediaManager.FileListState currentFileListState = MediaManager.FileListState.UNKNOWN;
	private FetchMediaTaskScheduler scheduler;
	private static BaseProduct mProduct;
	private Handler handler;
	File destDir;
	String dir = "";
	private int currentProgress = -1;
	private int lastClickViewIndex =-1;
	private int lastIndex = -1;
	private Thread t = null;
	private String csLock = "free";
	private String resp="";HashMap<String, String> d2p = null;
	public final String AUAVsim = "AUAVsim";
	private static boolean dbDone = false;

	/**
	 *		usageInfo += "dc=[cmd]-dp=[option]<br>";
	 *		usageInfo += "cmd: <br>";
	 *		usageInfo += "get -- Take a picture and download it to phone<br>";
	 *		usageInfo += "dir -- Set directory name in which you want to save images\n";
	 *		usageInfo += "dmp -- Get database dump of Image Name and Location\n";
	 *		usageInfo += "qry -- Issue an SQL query against prior data<br>";
	 *		usageInfo += "help -- Return this usage information.<br>";
	 *		usageInfo += "option -- <br>";
	 *		usageInfo += "AUAVsim -- Do not access DJI or local compute.  Simulate.<br>";
	 *		usageInfo += "SQL-String -- Executed against SQLite<br>";
	 * @author  Venkata Sai Mandadapu
	 * @version 1.0.2
	 * @since   2017-10-02
	 */
	public String getUsageInfo() {
		String usageInfo = "";
		usageInfo += "dc=[cmd]-dp=[option]\n";
		usageInfo += "cmd --> \n";
		usageInfo += "get -- Take a picture and download it to phone\n";
		usageInfo += "dir -- Set directory name in which you want to save images\n";
		usageInfo += "dmp -- Get database dump of Image Name and Location\n";
		usageInfo += "qry -- Issue an SQL query against prior data\n";
		usageInfo += "help -- Return this usage information.\n";
		usageInfo += "option: \n";
		usageInfo += "AUAVsim -- Do not access DJI or local compute.  Simulate.\n";
		usageInfo += "SQL-String -- Executed against H2\n";
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

			if (AUAVsim == 1){
				ce.respond("Cannot execute the command in simulator mode.");
				return;
			}

			destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/AUAVTmp/FullPics/");

			switch (args[0]) {
				case "dc=help":
					ce.respond(getUsageInfo());
					break;
				case "dc=qry":
					String qry = args[1].substring(3);
					ce.respond(queryH2(qry));
					break;
				case "dc=get":
					System.out.println("CaptureImageDriver: in get");
					if (AUAVsim == 1) {
						System.out.println("CaptureImageDriver: AUAVsim detected, abort");
						break;
					}
					System.out.println("");
					try {
						System.out.println("CaptureImageDriver: Trying to take picture ---------------------------------------");
						handler = new Handler(Looper.getMainLooper());
						final Camera camera = getCameraInstance();
						if (camera != null){
							System.out.println("CaptureImageDriver: Camera's not null");
							camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
								@Override
								public void onResult(DJIError error) {
									if (error == null) {
										System.out.println("Switch Camera Mode Succeeded");
										SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE;
										camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback(){
											@Override
											public void onResult(DJIError djiError) {
												if (null == djiError) {
													System.out.println("Set Shoot Photo Mode Succeded");
													handler.postDelayed(new Runnable() {
														@Override
														public void run() {
															camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
																@Override
																public void onResult(DJIError djiError) {
																	if (djiError == null) {
																		System.out.println("take photo: success");
																		initMediaManager();
																	} else {

																		System.out.println("CaptureImageDriver: Error when shooting photo"+djiError.getDescription());
																	}
																}
															});
														}
													}, 2000);
												} else{
													System.out.println(djiError.toString());
												}
											}
										});

									} else {
										System.out.println(error.getDescription());
									}
								}
						  });

						} else {
							System.out.println("Camera was null");
						}
						System.out.println("Wait until Database update signal");

						try{
							System.out.println("Waiting for image to be written to DB");
							while(!dbDone){
								//System.out.println("DB Not Done");
								Thread.sleep(1000);
							}
							dbDone = false; //reset bool
							System.out.println("DB Done, Thread Awake");

						}catch(Exception e){System.out.println("CaptureImageDriver: Exception in wait on sleep");}


						System.out.println("CaptureImageDriver in get: Last Index = "+lastIndex);
						if (lastIndex != -1) {
								System.out.println("CaptureImageDriver: Querying for Image");
								byte[] b = queryH2vb("select * from data order by key desc;"); //"where key = lastIndex" should be included
								ServerSocket ss = null;
								System.out.println("CaptureImageDriver: Attempting to make Socket");
								try {
										System.out.println("Sending Bytes");
										ss = new ServerSocket(44044,1);
										if (ss == null) {throw new Exception("CaptureImageDriver: null serversocket");}
										ss.setSoTimeout(5000);
										ce.respond("Camera: Ready to send data");
								}
								catch (Exception e) {
									System.out.println("CaptureImageDriver: Error Creating Socket");
									e.printStackTrace();
								}
								try {
									if (ss != null) {
										System.out.println("CaptureImageDriver: Sending Image as Byte Array");
										Socket cli = ss.accept();
										cli.getOutputStream().write(b);
										cli.close();
										ss.close();
									}
								} catch (Exception e) {
									System.out.println("CaptureImageDriver: Error Writing to Socker");
									e.printStackTrace();
								}

						}

					}
					catch (Exception e) {
						System.out.println("Camera: Error"+e.getMessage());
						ce.respond("Camera: Error"+e.getMessage());
					}
					System.out.println("CaptureImageDriver: Complete");
					break;
				case "dc=dir":
					dir = args[1].substring(3);
					destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/DroneImages/" + dir + "/");
					System.out.println("Directory is set to :"+ destDir.getAbsolutePath());
					ce.respond("Directory is set to :"+destDir.getAbsolutePath());
					break;

				case "dc=dmp":
					String alldata = queryH2("SELECT * FROM data");
					System.out.println("Database data: "+ alldata);
					BufferedWriter out = null;
					try {
						FileWriter filestream = new FileWriter(destDir.getAbsolutePath() + "/index.dat");
						out = new BufferedWriter(filestream);
						out.write(alldata);
						ce.respond("Dump successful");
					}
					catch (IOException e)
					{
						System.err.println("Error: " + e.getMessage());
					}
					finally
					{
						try {
							if (out != null) out.close();

						} catch (IOException e){
							System.err.println("Error: " + e.getMessage());
						}
					}
					break;
				default:
					System.out.println("Camera unknow command");
					ce.respond("Error: CameraDriver unknown command\n");
			}
		}


		public bdResource() {
			super("cr");	getAttributes().setTitle("cr");
		}

		public String queryH2(String query) {
			String outLine = "";
			try {
				Connection conn = DriverManager.getConnection("jdbc:h2:mem:CaptureImageDriver;DB_CLOSE_DELAY=-1",
						"user", "password");
				Statement stmt = conn.createStatement();

				ResultSet rs = stmt.executeQuery(query);

				while(rs.next()) {
					outLine += "key="+rs.getInt("key")+
							"-time="+rs.getLong("time")+"-X="+rs.getString("X")+"-Y="+rs.getString("Y")+"-Z="+rs.getString("Z")+
							"-filename="+rs.getString("Filename")+"\n";
				}

				conn.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			return outLine;
		}



		public byte[] queryH2vb(String query) {
			try {
				System.out.println("CaptureImageDriver: Querying H2vb");

				Connection conn = DriverManager.getConnection("jdbc:h2:mem:CaptureImageDriver;DB_CLOSE_DELAY=-1",
						"user", "password");
				Statement stmt = conn.createStatement();

				ResultSet rs = stmt.executeQuery(query);

				System.out.println("CaptureImageDriver: DB Connection Success");
				if (rs.next()) {
						File file = new File(rs.getString("Filename"));
						System.out.println("Filename: "+file.getAbsolutePath());
						byte[] data = new byte[(int) file.length()];
						InputStream is = new FileInputStream(file);
						System.out.println("Reading data from file: " + file.getAbsolutePath());
						is.read(data);
						is.close();
						System.out.println("CaptureImageDriver: Sending Data from h2vb");
						System.out.println("CaptureImageDriver: Sending data of size: " + data.length);
						return(data);
				} else {
					System.out.println("DB Doesn't have image for that query");
				}
				conn.close();


			}
			catch (Exception e) {
				System.out.println("Capture Image Driver: Error Querying h2vm");
				e.printStackTrace();
			}
			return null;
		}
	}
	public CoapServer getCoapServer() {
		return (cs);
	}
	public CaptureImageDriver() throws Exception {
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

		try {
			Class.forName("org.h2.Driver");
			Connection conn = DriverManager.getConnection("jdbc:h2:mem:CaptureImageDriver;DB_CLOSE_DELAY=-1",
					"user", "password");
			conn.createStatement().executeUpdate("CREATE TABLE data (" +" key  INTEGER AUTO_INCREMENT,"
					+" time BIGINT, "
					+" X VARCHAR(16), "
					+" Y VARCHAR(16), "
					+" Z VARCHAR(16), "
					+" Filename VARCHAR(1023) )");
			conn.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}

	}
	private static Logger bdLogger =
			Logger.getLogger(CaptureImageDriver.class.getName());
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

	private void initMediaManager() {

		if (null != getCameraInstance() && getCameraInstance().isMediaDownloadModeSupported()) {
			mMediaManager = getCameraInstance().getMediaManager();
			if (null != mMediaManager) {
				mMediaManager.addUpdateFileListStateListener(this.updateFileListStateListener);
				getCameraInstance().setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, new CommonCallbacks.CompletionCallback() {
					@Override
					public void onResult(DJIError error) {
						if (error == null) {
							System.out.println("Set cameramode success");
							getFileList();
							System.out.println("GetImageDriver: Done getting file List");
						} else {
							System.out.println("Set cameramode failed");
						}
					}
				});
				System.out.println("CaptureImageDriver: getting scheduler");
				scheduler = mMediaManager.getScheduler();
				System.out.println("CaptureImageDriver: Scheruler retrieved");
			}

		} else if (null != getCameraInstance()
				&& !getCameraInstance().isMediaDownloadModeSupported()) {
			System.out.println("Media download not supported");
		}

		return;
	}

	private void getFileList() {
		mMediaManager = getCameraInstance().getMediaManager();
		if (mMediaManager != null) {

			if ((currentFileListState == MediaManager.FileListState.SYNCING) || (currentFileListState == MediaManager.FileListState.DELETING)){
				System.out.println("Media Manager is busy.");
			}else{
				mMediaManager.refreshFileList(new CommonCallbacks.CompletionCallback() {
					@Override
					public void onResult(DJIError error) {
						if (error == null) {
							System.out.println("CaptureImageDriver: Media File List Not Null");
							if (currentFileListState != MediaManager.FileListState.INCOMPLETE) {
								mediaFileList.clear();
							}

							mediaFileList = mMediaManager.getFileListSnapshot();
							Collections.sort(mediaFileList, new Comparator<MediaFile>() {
								@Override
								public int compare(MediaFile lhs, MediaFile rhs) {
									if (lhs.getTimeCreated() > rhs.getTimeCreated()) {
										return 1;
									} else if (lhs.getTimeCreated() < rhs.getTimeCreated()) {
										return -1;
									}
									return 0;
								}
							});
							scheduler.resume(new CommonCallbacks.CompletionCallback() {
								@Override
								public void onResult(DJIError error) {
									if (error == null) {
										System.out.println("CaptureImageDriver: Downloading file by index");
										downloadFileByIndex(mediaFileList.size()-1);
										System.out.println("CaptureImageDriver: Getting lastIndex");
										lastIndex = mediaFileList.size()-1;
										System.out.println("CaptureImageDriver - in getFileList: done downloading file by index, lastIndex = " + lastIndex);
									}
								}
							});
						} else {
							System.out.println("Get media file list failed:"+error.getDescription());
						}
					}
				});
			}
		}
		return;
	}

	public void addReadingH2(String X, String Y, String Z, String filename) {
		try {

			Connection conn = DriverManager.getConnection("jdbc:h2:mem:CaptureImageDriver;DB_CLOSE_DELAY=-1",
					"user", "password");
			Statement stmt = conn.createStatement();

			String sql = "INSERT INTO data (time, X, Y, Z, Filename) VALUES ("
					+ (System.currentTimeMillis()- startTime) + "," + X + "," + Y + "," + Z + ",'" + Environment.getExternalStorageDirectory().getPath() + "/DroneImages/"+filename + "')";
			stmt.executeUpdate(sql);
			conn.close();
			System.out.println("Added file to H2 with location :" +X+","+Y+","+Z+" filename: "+Environment.getExternalStorageDirectory().getPath() + "/DroneImages/" + filename);
			dbDone = true;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void downloadFileByIndex(final int index){
		if ((mediaFileList.get(index).getMediaType() == MediaFile.MediaType.PANORAMA)
				|| (mediaFileList.get(index).getMediaType() == MediaFile.MediaType.SHALLOW_FOCUS)) {
			return;
		}

		mediaFileList.get(index).fetchFileData(destDir, null, new DownloadListener<String>() {
			@Override
			public void onFailure(DJIError error) {
				System.out.println("Download File Failed" + error.getDescription());
				currentProgress = -1;
			}

			@Override
			public void onProgress(long total, long current) {
			}

			@Override
			public void onRateUpdate(long total, long current, long persize) {
				int tmpProgress = (int) (1.0 * current / total * 100);
				if (tmpProgress != currentProgress) {
					System.out.println("Download Progress: "+ Integer.toString(tmpProgress));
					currentProgress = tmpProgress;
				}
			}

			@Override
			public void onStart() {
			}

			@Override
			public void onSuccess(String filePath) {
				System.out.println("Download File Success" + ":" + filePath);
				String succ = "";
				succ = invokeDriver("org.reroutlab.code.auav.drivers.LocationDriver",
						"dc=get", chResp );
				rtnSpin();
				rtnLock("free");
				System.out.println("Response: "+resp);
				String[] location = resp.split("=");
				addReadingH2(location[1].split("-")[0], location[2].split("-")[0], location[3], mediaFileList.get(index).getFileName());
				System.out.println("CaptureImageDriver: Done adding image to H2");
				currentProgress = -1;
			}
		});
	}

	public static synchronized BaseProduct getProductInstance() {
		if (null == mProduct) {
			mProduct = DJISDKManager.getInstance().getProduct();
		}
		return mProduct;
	}

	public static synchronized Camera getCameraInstance() {

		if (getProductInstance() == null) return null;

		Camera camera = null;

		if (getProductInstance() instanceof Aircraft){
			camera = ((Aircraft) getProductInstance()).getCamera();

		} else if (getProductInstance() instanceof HandHeld) {
			camera = ((HandHeld) getProductInstance()).getCamera();
		}

		return camera;
	}

	private MediaManager.FileListStateListener updateFileListStateListener = new MediaManager.FileListStateListener() {
		@Override
		public void onFileListStateChange(MediaManager.FileListState state) {
			currentFileListState = state;
		}
	};

	CoapHandler chResp = new CoapHandler() {
		@Override public void onLoad(CoapResponse response) {
			resp = response.getResponseText();
			rtnLock("barrier-1");
		}

		@Override public void onError() {
			System.err.println("FAILED");
			rtnLock("barrier-1");
		}};

	synchronized void rtnLock(String value) {
		csLock = value;
	}
	public void rtnSpin() {
		while (csLock.equals("barrier-1") == false) {
			try { Thread.sleep(1000); }
			catch (Exception e) {}
		}

	}

	public String invokeDriver(String dn, String params, CoapHandler ch) {
		params = params + "-dp=" + "AUAVsim";
		if(this.d2p == null) {
			try {
				URI portStr = new URI("coap://127.0.0.1:5117/cr");
				CoapClient e = new CoapClient(portStr);
				CoapResponse client = e.put("dn=list", 0);
				String ls = client.getResponseText();
				this.d2p = new HashMap();
				String[] lines = ls.split("\n");

				for(int x = 0; x < lines.length; ++x) {
					String[] data = lines[x].split("-->");
					if(data.length == 2) {
						this.d2p.put(data[0].trim(), data[1].trim());
					}
				}
			} catch (Exception var12) {
				this.d2p = null;
				System.out.println("AUAVRoutine invokeDriver error");
				var12.printStackTrace();
				return "Invoke Error";
			}
		}

		if(this.d2p != null) {
			String var13 = (String)this.d2p.get(dn);
			if(var13 != null) {
				try {
					URI var14 = new URI("coap://127.0.0.1:" + var13 + "/cr");
					CoapClient var15 = new CoapClient(var14);
					var15.put(ch, params, 0);
					return "Success";
				} catch (Exception var11) {
					return "Unable to reach driver " + dn + "  at port: " + var13;
				}
			} else {
				return "Unable to find driver: " + dn;
			}
		} else {
			return "InvokeDriver: Unreachable code touched";
		}
	}
}
