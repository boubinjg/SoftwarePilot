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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.os.Bundle;
import android.app.Application;
import android.graphics.Bitmap;
/*import dji.sdk.camera.Camera;
import dji.sdk.camera.FetchMediaTask;
import dji.sdk.camera.FetchMediaTaskContent;
import dji.sdk.camera.FetchMediaTaskScheduler;
import dji.sdk.camera.MediaFile;
import dji.sdk.camera.MediaManager;
import dji.sdk.camera.FetchMediaTask;
import dji.sdk.camera.FetchMediaTaskContent;
import dji.sdk.camera.FetchMediaTaskScheduler;
import dji.sdk.camera.DownloadListener;
*/
import dji.sdk.media.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.h2.*;
import org.h2.jdbcx.JdbcConnectionPool;

/**
 * BatteryDriver connects to DJI <i>and</i> local compute.
 * It supports commands related to checking battery statuses.
 * Interact with this driver via CoAP (californium)
 * <br><b>URL: coap:\\localhost:port\cr?</b>
 * <br>Example: coap:\\127.0.0.1:10452\cr?dc=dji
 * <br>
 * Note: all AUAV drivers have resource component "cr" for coap resource
 *
 * @author  Venkata Mandadapu
 * @version 1.0.2
 * @since   2017-10-2
 */
public class CameraDriver extends org.reroutlab.code.auav.drivers.AuavDrivers {

		private static int LISTEN_PORT = 0;
		private int driverPort = 0;
		private CoapServer cs;
		private int lclLastReading = 100;
		private long startTime = 0;
		//private FileListAdapter mListAdapter;
		private List<MediaFile> mediaFileList = new ArrayList<MediaFile>();
		private MediaManager mMediaManager;
		private MediaManager.FileListState currentFileListState = MediaManager.FileListState.UNKNOWN;
		private FetchMediaTaskScheduler scheduler;

		/**
		 *		usageInfo += "dc=[cmd]-dp=[option]<br>";
		 *		usageInfo += "cmd: <br>";
		 *		usageInfo += "dji -- Grab and store battery status of DJI<br>";
		 *		usageInfo += "lcl -- Grab and store battery status of compute<br>";
		 *		usageInfo += "qry -- Issue an SQL query against prior data<br>";
		 *		usageInfo += "help -- Return this usage information.<br>";
		 *		usageInfo += "option -- <br>";
		 *		usageInfo += "AUAVsim -- Do not access DJI or local compute.  Simulate.<br>";
		 *		usageInfo += "SQL-String -- Executed against SQLite<br>";
		 * @author  Christopher Charles Stewart
		 * @version 1.0.2
		 * @since   2017-05-01
		 */
		public String getUsageInfo() {
				String usageInfo = "";
				usageInfo += "dc=[cmd]-dp=[option]\n";
				usageInfo += "cmd: \n";
				usageInfo += "dji -- Grab and store battery status of DJI\n";
				usageInfo += "lcl -- Grab and store battery status of compute\n";
				usageInfo += "qry -- Issue an SQL query against prior data\n";
				usageInfo += "help -- Return this usage information.\n";
				usageInfo += "option --> \n";
				usageInfo += "AUAVsim -- Do not access DJI or local compute.  Simulate.\n";
				usageInfo += "SQL-String -- Executed against SQLite\n";
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
								ce.respond(queryH2(qry));
								break;
						case "dc=getim":
								if (AUAVsim == 1) {
										break;
								}

								try {
										System.out.println("Camera Driver Started");
										Class<?> c = Class.forName("android.app.ActivityThread");
										android.app.Application app =
												(android.app.Application) c.getDeclaredMethod("currentApplication").invoke(null);
										android.content.Context context = app.getApplicationContext();
										final Camera camera = app.getCameraInstance();
										if (camera != null){
											SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE;
											camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback(){
												@Override
												public void onResult(DJIError djiError) {
													if (null == djiError) {
														handler.postDelayed(new Runnable() {
															@Override
															public void run() {
																camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
																	@Override
																	public void onResult(DJIError djiError) {
																		if (djiError == null) {
																			System.out.println("take photo: success");
																			ce.respond("take photo: success");
																		} else {
																			System.out.println(djiError.getDescription());
																			ce.respond(djiError.getDescription());
																		}
																	}
																});
															}
														}, 2000);
													}
												}
											});
										}

										initMediaManager(app);
//										BatteryManager bm = (BatteryManager)context.getSystemService("batterymanager");
//										int batLevel = bm.getIntProperty(4);
//										lclLastReading = batLevel;
//										addReadingH2(batLevel, "lcl");
//										ce.respond("Battery: " + Integer.toString(batLevel));
								}
								catch (Exception e) {
									System.out.println("Camera: Error");
									ce.respond("Camera: Error");
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

				public void addReadingH2(int reading, String type) {
						try {

								Connection conn = DriverManager.getConnection("jdbc:h2:mem:CameraDriver;DB_CLOSE_DELAY=-1",
																											 "user", "password");
								Statement stmt = conn.createStatement();

								String sql = "INSERT INTO data (time, type, value) VALUES ("
									+ (System.currentTimeMillis()- startTime) + "," + type + "," + reading + ")";
						stmt.executeUpdate(sql);
						conn.close();
						}
						catch (Exception e) {
														e.printStackTrace();
						}



				}

				public String queryH2(String query) {
						String outLine = "";
						try {
								Connection conn = DriverManager.getConnection("jdbc:h2:mem:CameraDriver;DB_CLOSE_DELAY=-1",
																											 "user", "password");
								Statement stmt = conn.createStatement();

								ResultSet rs = stmt.executeQuery(query);

								while(rs.next()) {
										outLine += "key="+rs.getInt("key")+
												"-time="+rs.getLong("time")+"-type="+rs.getString("type")+
												"-data="+rs.getString("value")+"---";
								}

								conn.close();
						}
						catch (Exception e) {
								e.printStackTrace();
						}
						return outLine;
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

		public CoapServer getCoapServer() {
				return (cs);
		}
		public CameraDriver() throws Exception {
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
						Connection conn = DriverManager.getConnection("jdbc:h2:mem:CameraDriver;DB_CLOSE_DELAY=-1",
																									 "user", "password");
						conn.createStatement().executeUpdate("CREATE TABLE data (" +" key  INTEGER AUTO_INCREMENT,"
																								 +" time BIGINT, "
																								 +" type VARCHAR(16), "
																								 +" value VARCHAR(1023) )");
						conn.close();
				}
				catch(Exception e) {
					e.printStackTrace();
				}

		}
		private static Logger bdLogger =
				Logger.getLogger(CameraDriver.class.getName());
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

	private void initMediaManager(android.app.Application app) {
		if (null != app.getCameraInstance() && app.getCameraInstance().isMediaDownloadModeSupported()) {
			mMediaManager = app.getCameraInstance().getMediaManager();
			if (null != mMediaManager) {
				mMediaManager.addUpdateFileListStateListener(this.updateFileListStateListener);
				mMediaManager.addMediaUpdatedVideoPlaybackStateListener(this.updatedVideoPlaybackStateListener);
				app.getCameraInstance().setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, new CommonCallbacks.CompletionCallback() {
					@Override
					public void onResult(DJIError error) {
						if (error == null) {
							System.out.println("Set cameramode success");
							ce.respond("Set cameraMode success");
							getFileList(app);
						} else {
							System.out.println("Set cameramode failed");
							ce.respond("Set cameraMode failed");
						}
					}
				});
				scheduler = mMediaManager.getScheduler();
			}

		} else if (null != app.getCameraInstance()
				&& !app.getCameraInstance().isMediaDownloadModeSupported()) {
			System.out.println("Media download not supported");
			ce.respond("Media Download Mode not Supported");
		}
		return;
	}

	private void getFileList(android.app.Application app) {
		mMediaManager = app.getCameraInstance().getMediaManager();
		if (mMediaManager != null) {

			if ((currentFileListState == MediaManager.FileListState.SYNCING) || (currentFileListState == MediaManager.FileListState.DELETING)){
				ce.respond("Media Manager is busy.");
			}else{
				mMediaManager.refreshFileList(new CommonCallbacks.CompletionCallback() {
					@Override
					public void onResult(DJIError error) {
						if (null == error) {

							if (currentFileListState != MediaManager.FileListState.INCOMPLETE) {
								mediaFileList.clear();
							}

							mediaFileList = mMediaManager.getFileListSnapshot();
							Collections.sort(mediaFileList, new Comparator<MediaFile>() {
								@Override
								public int compare(MediaFile lhs, MediaFile rhs) {
									if (lhs.getTimeCreated() < rhs.getTimeCreated()) {
										return 1;
									} else if (lhs.getTimeCreated() > rhs.getTimeCreated()) {
										return -1;
									}
									return 0;
								}
							});
							scheduler.resume(new CommonCallbacks.CompletionCallback() {
								@Override
								public void onResult(DJIError error) {
									if (error == null) {
										downloadFileByIndex(mediaFileList.size()-1);
									}
								}
							});
						} else {
							System.out.println("Get media file list failed:"+error.getDescription());
							ce.respond("Get Media File List Failed:" + error.getDescription());
						}
					}
				});
			}
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
				ce.respond("Download File Failed" + error.getDescription());
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
					ce.respond("Download Progress: "+ Integer.toString(tmpProgress));
					currentProgress = tmpProgress;
				}
			}

			@Override
			public void onStart() {
			}

			@Override
			public void onSuccess(String filePath) {
				System.out.println("Download File Success" + ":" + filePath);
				ce.respond("Download File Success" + ":" + filePath);
				currentProgress = -1;
				deleteFileByIndex(index);
			}
		});
	}

	private void deleteFileByIndex(final int index) {
		ArrayList<MediaFile> fileToDelete = new ArrayList<MediaFile>();
		if (mediaFileList.size() > index) {
			fileToDelete.add(mediaFileList.get(index));
			mMediaManager.deleteFiles(fileToDelete, new CommonCallbacks.CompletionCallbackWithTwoParam<List<MediaFile>, DJICameraError>() {
				@Override
				public void onSuccess(List<MediaFile> x, DJICameraError y) {
					DJILog.e(TAG, "Delete file success");
					runOnUiThread(new Runnable() {
						public void run() {
							MediaFile file = mediaFileList.remove(index);

							//Reset select view
							lastClickViewIndex = -1;
							lastClickView = null;

							//Update recyclerView
							//mListAdapter.notifyItemRemoved(index);
						}
					});
				}

				@Override
				public void onFailure(DJIError error) {
					System.out.println("Delete file failed");
					ce.respond("Delete file failed");
				}
			});
		}
	}

}

