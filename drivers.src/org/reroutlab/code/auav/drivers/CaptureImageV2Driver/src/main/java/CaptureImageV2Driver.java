package com.dji.sdk.sample.internal.controller;

import java.util.concurrent.Semaphore;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import android.provider.Settings;
import android.util.Base64;

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

import dji.sdk.camera.VideoFeeder;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import dji.sdk.codec.DJICodecManager;
import android.R.id;
import android.widget.TextView;
import android.graphics.SurfaceTexture;
import android.view.TextureView.SurfaceTextureListener;
import android.graphics.Bitmap;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;
import org.h2.*;
import org.h2.jdbcx.JdbcConnectionPool;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.ByteArrayOutputStream;
//import java.util.Base64.*;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * CaptureImageDriver connects to DJI <i>and</i> local compute.
 * It supports commands related to capturing images.
 * Interact with this driver via CoAP (californium)
 * <br><b>URL: coap:\\localhost:port\cr?</b>
 * <br>Example: coap:\\127.0.0.1:10452\cr?dc=get
 * <br>
 * Note: all AUAV drivers have resource component "cr" for coap resource
 *
 * The flow of commands in the following driver for ssm command is:
 * 1. Sets Camera Mode to Shoot Photo Mode
 * 2. Sets Shoot Photo Mode to Shoot Single Photo
 *
 * The flow of commands for get command is:
 * 1. Captures Photo
 * 2. Adds the location and temp file name to H2 database.
 *
 * The flow of commands for dld command is:
 * 1. Changes Camera Mode to Media Download Mode (Not sure why it is being done, but docs say it should be done)
 * 2. Gets the Photo index from the drone.
 * 3. Downloads the Photo.
 * 4. Updates the filename of the photo in H2 database.
 *
 * The flow of commands for dmp command is:
 * 1. Get all the data from H2 database.
 * 2. Write it to index.dat file.
 *
 * The flow of commands for del command is:
 * 1. Changes Camera Mode to Media Download Mode (Not sure why it is being done, but docs say it should be done)
 * 2. Delete all the files in the drone.
 *
 *
 * These steps will be repeated everytime the command is called.
 *
 *
 * @author  Venkata Mandadapu
 * @version 2.0.0
 * @since   2017-11-17
 */
public class CaptureImageV2Driver extends org.reroutlab.code.auav.drivers.AuavDrivers implements SurfaceTextureListener{

	private static int LISTEN_PORT = 0;
	private int driverPort = 0;
	private CoapServer cs;
	private int lclLastReading = 100;
	private long startTime = 0;
	private List<MediaFile> mediaFileList = null;
	private MediaManager mMediaManager;
	private MediaManager.FileListState currentFileListState = MediaManager.FileListState.UNKNOWN;
	private FetchMediaTaskScheduler scheduler;
	private static BaseProduct mProduct;
	private Handler handler;
	private int numPhotos = 0;
	File destDir, tmpPic;
	String dir = "";
	private int currentProgress = -1;
	private int lastClickViewIndex =-1;
	private Thread t = null;
	private String csLock = "free";
	private String resp="";HashMap<String, String> d2p = null;
	public final String AUAVsim = "AUAVsim";
	private long cmdStartTime;
	private Semaphore checkRef = new Semaphore(1);
	private String imageEnc; //base64 encoding for images to be sent over coap
	private boolean complete;
	protected TextureView mVideoSurface = null;
    private Context context;
    protected DJICodecManager mCodecManager = null;
    /**
	 *		usageInfo += "dc=[cmd]-dp=[option]<br>";
	 *		usageInfo += "cmd: <br>";
	 *		usageInfo += "ssm -- Set the camera mode to Shoot Photo\n";
	 *		usageInfo += "get -- Take a picture\n";
	 *		usageInfo += "dld -- Download recent pictures to phone\n";
	 *		usageInfo += "del -- Delete all media files in the drone\n";
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
		usageInfo += "cmd: \n";
		usageInfo += "ssm -- Set the camera mode to Shoot Photo\n";
		usageInfo += "get -- Take a picture\n";
		usageInfo += "dld -- Download recent pictures to phone\n";
		usageInfo += "del -- Delete all media files in the drone\n";
		usageInfo += "dir -- Set directory name in which you want to save images\n";
		usageInfo += "dmp -- Get database dump of Image Name and Location\n";
		usageInfo += "qry -- Issue an SQL query against prior data\n";
		usageInfo += "help -- Return this usage information.\n";
		usageInfo += "option -- \n";
		usageInfo += "AUAVsim -- Do not access DJI or local compute.  Simulate.\n";
		usageInfo += "SQL-String -- Executed against H2\n";
		return usageInfo;
	}

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(context, surface, width, height);
        }
    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void initVideoSurface(){
        if(mVideoSurface == null){
            mVideoSurface = new TextureView(context);
            mVideoSurface.setSurfaceTextureListener(this);
        }
    }

	//extends CoapResource class
	private class bdResource extends CoapResource {
		@Override
		public void handlePUT(final CoapExchange ce) {
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
			}

			destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/AUAVTmp/FullPics/");
			tmpPic = new File(Environment.getExternalStorageDirectory().getPath() + "/AUAVTmp/fullPic.JPG");
            final Camera camera = getCameraInstance();

			switch (args[0]) {
				case "dc=help":
					ce.respond(getUsageInfo());
					break;
				case "dc=qry":
					String qry = args[1].substring(3);
					ce.respond(queryH2(qry));
					break;
				case "dc=ssm":
                    System.out.println("SSM");
					if (AUAVsim == 1) {
                        System.out.println("SSM Sim");
						break;
					}
					cmdStartTime = System.currentTimeMillis();
					try {
							if (mediaFileList == null) {
									//drvSetLock("DLD");
									//getCameraInstance().setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, new djiCC().cb);
									//drvSpin();
									//System.out.println("CaptureImageV2: Set DLD: "+timeStamp(cmdStartTime));

									mMediaManager = getCameraInstance().getMediaManager();
									if (mMediaManager == null) {
											System.out.println("CaptureImageV2: Err! mMediaManager is null ");
									}

									mediaFileList = mMediaManager.getSDCardFileListSnapshot();

									deleteFiles(ce);	// Semaphore used inside of the function

							}
							if (camera != null){
									drvSetLock("PHOTO");
									camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new djiCC().cb);
									drvSpin();
									System.out.println("CaptureImageV2: Shoot Mode" + timeStamp(cmdStartTime) );

									drvSetLock("SINGLE");
									SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE;
									camera.setShootPhotoMode(photoMode, new djiCC().cb);
									drvSpin();
									System.out.println("CaptureImageV2: shoot photo mode "+timeStamp(cmdStartTime));
							}
							ce.respond("CaptureImageV2: initialized");

					}
					catch (Exception e) {
						System.out.println("Camera: Error"+e.getMessage());
						ce.respond("Camera: Error"+e.getMessage());
					}

					break;
				case "dc=get":
                    System.out.println("Get");
					if (AUAVsim == 1) {
                        System.out.println("sim");
						break;
					}

					//mediaFileList = mMediaManager.getFileListSnapshot();
					//int curSize = mediaFileList.size();
					//System.out.println("Original media file list size: "+curSize);

					cmdStartTime = System.currentTimeMillis();
					drvSetLock("GET");
					camera.startShootPhoto(new djiCC().cb);
					drvSpin();

					numPhotos++;
					//refreshing media manager and media file list
					//makes sure the list updates
					int asynchCount = 0;

					drvSetLock("REF");

					mMediaManager.refreshFileList(new djiCC().cb);
					drvSpin();
					mediaFileList = mMediaManager.getSDCardFileListSnapshot();
					asynchCount++;

					System.out.println("Updated File Count, took this many tries: "+asynchCount);
					try{Thread.sleep(1000);}catch(Exception e){}
					mediaFileList = mMediaManager.getSDCardFileListSnapshot();
					System.out.println("Media File List Size: "+mediaFileList.size());
					//System.out.println("Cur File Name: "+mediaFileList.get(mediaFileList.size()-1).getFileName());
					drvSetLock("LocationDriver");
					String succ = invokeDriver("org.reroutlab.code.auav.drivers.LocationDriver",
																	 "dc=get", chResp );
					drvSpin();
					String[] location = resp.split("=");
					addReadingH2(location[1].split("-")[0], location[2].split("-")[0], location[3], mediaFileList.get(mediaFileList.size()-1).getFileName());

					System.out.println("Capture Image Driver V2: Photo Delay "+timeStamp(cmdStartTime));
					ce.respond("CaptureImageV2: Success");

					break;
				case "dc=dir":
					dir = args[1].substring(3);
					destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/DroneImages/" + dir + "/");
					System.out.println("Directory is set to :"+ destDir.getAbsolutePath());
					ce.respond("Directory is set to :"+destDir.getAbsolutePath());
					break;
				case "dc=dld":
					System.out.println("In DLD");
					cmdStartTime = System.currentTimeMillis();
					try {
						System.out.println("DLD: Trying to init media manager");
						initMediaManager(ce,"dld");
						System.out.println("DLD: success");
						ce.respond("DLD: Success");
					} catch(Exception e){
						System.out.println("DLD: Error");
						e.printStackTrace();
						ce.respond(e.getMessage());
					}
					break;
				case "dc=dldFull":
                    System.out.println("DLDFull");
                    try{
                        initMediaManager(ce,"dldFull");
                    } catch(Exception e){
                        System.out.println("DLDFull: Error");
                        e.printStackTrace();
                        ce.respond(e.getMessage());
                    }
                    break;
                case "dc=del":
					cmdStartTime = System.currentTimeMillis();
					try {
						initMediaManager(ce,"del");
					} catch(Exception e){
						ce.respond(e.getMessage());
					}
					break;
                case "dc=dmp":
					cmdStartTime = System.currentTimeMillis();
					String alldata = queryH2("SELECT * FROM data");
					System.out.println("Database data: "+ alldata);
					BufferedWriter out = null;
					try {
						FileWriter filestream = new FileWriter(destDir.getAbsolutePath() + "/index.dat");
						out = new BufferedWriter(filestream);
						out.write(alldata);
						System.out.println("Capture Image Driver V2: Time Taken to create dump "+Long.toString(System.currentTimeMillis() - cmdStartTime));
						ce.respond("Dump successful");
					}
					catch (IOException e)
					{
						System.err.println("Error: " + e.getMessage());
						ce.respond(e.getMessage());
					}
					finally
					{
						try {
							if (out != null) out.close();

						} catch (IOException e){
							System.err.println("Error: " + e.getMessage());
							ce.respond(e.getMessage());
						}
					}
					break;
                case "dc=vid":
                    System.out.println("In Vid");
                    ce.respond("Vid Returned");
                    break;
                case "dc=vpc":
                    Bitmap pic = mVideoSurface.getBitmap();
                    String fname = Environment.getExternalStorageDirectory().getPath()+"/test.png";
                    try(FileOutputStream fout = new FileOutputStream(fname)){
                        pic.compress(Bitmap.CompressFormat.PNG, 100, fout);
                    } catch(IOException e){
                        System.out.println("IO Exception");
                        e.printStackTrace();
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

	}

	public CoapServer getCoapServer() {
		return (cs);
	}
	public CaptureImageV2Driver() throws Exception {
		//NetworkConfig.getStandard().set(NetworkConfig.Keys.MAX_RESOURCE_BODY_SIZE,10000000);
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
			Connection conn = DriverManager.getConnection("jdbc:h2:mem:CaptureImageV2Driver;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
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
			Logger.getLogger(CaptureImageV2Driver.class.getName());
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

	private void initMediaManager(final CoapExchange ce, final String option) {
			getFileList(ce,option);

			/*			mMediaManager = getCameraInstance().getMediaManager();
			if (null != mMediaManager) {
					//mMediaManager.addUpdateFileListStateListener(this.updateFileListStateListener);
				if (mediaFileList == null) {
						getCameraInstance().setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, new CommonCallbacks.CompletionCallback() {
										@}Override
										public void onResult(DJIError error) {
												if (error == null) {
														System.out.println("Capture Image Driver V2: Set Camera DLD mode "+Long.toString(System.currentTimeMillis() - cmdStartTime));
														System.out.println("Set cameramode success");

														mMediaManager.refreshFileList(new CommonCallbacks.CompletionCallback() {
																		@Override
																		public void onResult(DJIError error) {
																				System.out.println("Refreshed: "+Long.toString(System.currentTimeMillis() - cmdStartTime));
																				if (error == null) {
																						mediaFileList = mMediaManager.getFileListSnapshot();
																						System.out.println("Capture Image Driver V2: Time Taken to sort media file list "+Long.toString(System.currentTimeMillis() - cmdStartTime));
																						getFileList(ce,option);
																				}
																		}
																});
												}
												else {
														System.out.println("Set cameramode failed");
												}
										}});
				}
				else {
						getFileList(ce,option);
						System.out.println("Capture Image Driver V2: Time Taken to sort media file list--b "+Long.toString(System.currentTimeMillis() - cmdStartTime));
				}
			}a
		} else if (null != getCameraInstance()
				&& !getCameraInstance().isMediaD/ownloadModeSupported()) {
			System.out.println("Media download not supported");
			}*/
		return;
	}

	private void getFileList(final CoapExchange ce,final String option) {
			if (mMediaManager == null) {
					return;
			}

			drvSetLock("REF");
			mMediaManager.refreshFileList(new djiCC().cb);
			drvSpin();
			//System.out.println("CaptureImageV2: Ref "+timeStamp(cmdStartTime));

			//System.out.println("CaptureImageV2: GetFileList "+timeStamp(cmdStartTime));
			switch (option){
			case "dld":
					int size = dbSize();
					System.out.println(size);

					System.out.println("In getFileList in DLD");
					int counter = 1;
					System.out.println("DLD: Num Photos: "+numPhotos+" mediafile list size: "+mediaFileList.size());
					String ret = ""; //images downloaded, delimited by |
					for(int i = (mediaFileList.size() - numPhotos) ;i < (mediaFileList.size());i++){
							System.out.println("File Name before download "+mediaFileList.get(i).getFileName());
							System.out.println("Downloading file with Index "+Integer.toString(i));
							if (i == (mediaFileList.size() - 1) ){
									ret += downloadFileByIndex(ce,i,true);
							}else{
									ret += downloadFileByIndex(ce,i,false)+ "|";
							}
							int rowOffset = mediaFileList.size() - i;
							System.out.println("Set filename :"+mediaFileList.get(i).getFileName()+" filenum :"+rowOffset);
							updateReadingH2(mediaFileList.get(i).getFileName(),Integer.toString(size-rowOffset + 1));
							counter++;
							System.out.println("CaptureImageV2: sending image");
							System.out.println("CaptureImageV2: Size of Payload: "+ret.length());
							/*try{
								sendToPort(ret);
							}catch(Exception e){
								e.printStackTrace();
							}*/
							try{
								File f = new File(Environment.getExternalStorageDirectory().getPath()+"/tmp.dat");
								f.delete();
								MappedByteBuffer out = new RandomAccessFile(Environment.getExternalStorageDirectory().getPath()+"/AUAVtmp/pictmp.dat","rw").getChannel().map(FileChannel.MapMode.READ_WRITE, 0, ret.length());

								for(int j = 0; j<ret.length(); j++){
									out.put((byte) ret.charAt(j));
								}
								System.out.println("Wrote to tmp.dat");
								ce.respond("CaptureImageV2: DLD Success");
							} catch(Exception e){
								e.printStackTrace();
							}
							ce.respond("CaptureImageV2: DLD Fail");
					}
					break;
            case "dldFull":
                downloadFileFull(ce, mediaFileList.size()-1);
			    ce.respond("CaptureImageV2: DLD Full Success");
            case "del":
					break;
			}
			numPhotos = 0;
	}
	public int dbSize() {
		int size = 0;
		try {
			Connection conn = DriverManager.getConnection("jdbc:h2:mem:CaptureImageV2Driver;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
					"user", "password");
			Statement stmt = conn.createStatement();
			stmt.execute("select * from data");
			ResultSet rs = stmt.executeQuery("select * from data");

			while(rs.next()) {
				++size;
			}
			conn.close();

		} catch(Exception e){
			e.printStackTrace();
		}
		return size;
	}
	public void addReadingH2(String X, String Y, String Z, String filename) {
		try {

			Connection conn = DriverManager.getConnection("jdbc:h2:mem:CaptureImageV2Driver;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
					"user", "password");
			Statement stmt = conn.createStatement();

			String sql = "INSERT INTO data (time, X, Y, Z, Filename) VALUES ("
					+ (System.currentTimeMillis()- startTime) + "," + X + "," + Y + "," + Z + ",'" + filename + "')";
			stmt.executeUpdate(sql);
			conn.close();
			System.out.println("Added file to H2 with location :" +X+","+Y+","+Z+" filename: "+filename);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized void updateReadingH2(String filename, String filenum) {
		System.out.println("filename :"+filename+"filenum :"+filenum);
		try {

			Connection conn = DriverManager.getConnection("jdbc:h2:mem:CaptureImageV2Driver;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
					"user", "password");
			Statement stmt = conn.createStatement();
			//String sql = "update data set filename = 'wow' where rownum() = 1";
			String sql = "UPDATE data SET Filename='" + filename + "' WHERE key ="+filenum;
			stmt.executeUpdate(sql);
			conn.close();
			System.out.println("Updated "+ filenum + "to "+filename);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
    private synchronized void downloadFileFull(final CoapExchange ce, final int index) {
        drvSetLock("DownloadFull");
        mediaFileList.get(index).fetchFileData(destDir, null, new DownloadListener<String>(){
            @Override
            public void onFailure(DJIError error){
                System.out.println("Download File Error"+error.getDescription());
                drvUnsetLock();
            }

            @Override
            public void onSuccess(String filePath){
                System.out.println("Download Success "+filePath);
                try{
                    File folder = new File(filePath);
                    File[] listOfFiles = folder.listFiles();
                    for(int i = 0; i<listOfFiles.length; i++){
                        if(listOfFiles[i].isFile()){
                            listOfFiles[i].renameTo(tmpPic);
                            listOfFiles[i].delete();
                        }
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
                drvUnsetLock();
            }
            @Override
            public void onProgress(long total, long current){}

            @Override
            public void onRateUpdate(long total, long current, long persize){}

            @Override
            public void onStart(){}
        });
        drvSpin();
    }
	private synchronized String downloadFileByIndex(final CoapExchange ce, final int index,final boolean isLastFile){

		if ((mediaFileList.get(index).getMediaType() == MediaFile.MediaType.PANORAMA)
				|| (mediaFileList.get(index).getMediaType() == MediaFile.MediaType.SHALLOW_FOCUS)) {
			return "";
		}
		complete = false;
		imageEnc = "";
		drvSetLock("encode");
		mediaFileList.get(index).fetchPreview(new CommonCallbacks.CompletionCallback() {
						@Override
						public void onResult(DJIError message){
								if (message == null) {
										System.out.println("Time take to fetch preview " + Long.toString(System.currentTimeMillis() - cmdStartTime));
										System.out.println("Preview success");
										//try {
												//FileOutputStream out = new FileOutputStream(destDir.getAbsolutePath() + "/" + mediaFileList.get(index).getFileName());
												//mediaFileList.get(index).getPreview().compress(Bitmap.CompressFormat.JPEG, 100, out);
												Bitmap image = mediaFileList.get(index).getPreview();
												imageEnc = BitMapToString(image);

										/*} catch (FileNotFoundException e){
												System.out.println(e.toString());
										}*/

										if(isLastFile){
												//ce.respond("Download Preview Success");
										}
								} else {
										System.out.println(message.toString());
								}
								drvUnsetLock();
						}
				});
		/*while(!complete){
			try{Thread.sleep(50);}catch(Exception e){}
		}*/
		drvSpin();
		//System.out.println("Image Index: "+index+" Image Encoding: "+imageEnc);
		return imageEnc;
	}
	//https://stackoverflow.com/questions/13562429/how-many-ways-to-convert-bitmap-to-string-and-vice-versa
	public String BitMapToString(Bitmap bitmap){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
		byte [] b = baos.toByteArray();
		//String temp = Base64.getEncoder().encodeToString(b);
		String temp = Base64.encodeToString(b, Base64.DEFAULT);
		return temp;
	}

	private void deleteFiles(final CoapExchange ce) {
		drvSetLock("DEL");
		mMediaManager.deleteFiles(mediaFileList, new CommonCallbacks.CompletionCallbackWithTwoParam<List<MediaFile>, DJICameraError>() {
			@Override
			public void onSuccess(List<MediaFile> x, DJICameraError y) {
				ce.respond("Delete file success");
				drvUnsetLock();
			}

			@Override
			public void onFailure(DJIError error) {
				ce.respond(error.getDescription());
				drvUnsetLock();
			}
		});
		drvSpin();
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

/*	private MediaManager.FileListStateListener updateFileListStateListener = new MediaManager.FileListStateListener() {
		@Override
		public void onFileListStateChange(MediaManager.FileListState state) {
			currentFileListState = state;
		}
	};
		*/
	public void sendToPort(String str) throws IOException {
    		ServerSocket ss = new ServerSocket(44044);
        	Socket socket = ss.accept();
	       	OutputStream out = socket.getOutputStream();
      		ObjectOutputStream oout = new ObjectOutputStream(out);
		oout.writeObject(str);
        	oout.close();
		socket.close();
		ss.close();
	}

	CoapHandler chResp = new CoapHandler() {
		@Override public void onLoad(CoapResponse response) {
				resp = response.getResponseText();
				drvUnsetLock();
		}

		@Override public void onError() {
				resp ="CaptureImageV2: Coap Error";
				drvUnsetLock();
		}};


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

	public String queryH2(String query) {
		String outLine = "";
		try {
			Connection conn = DriverManager.getConnection("jdbc:h2:mem:CaptureImageV2Driver;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
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

    public String timeStamp(long start) {
				return( Long.toString(System.currentTimeMillis() - start));
		}

		private class djiCC {
				public CommonCallbacks.CompletionCallback cb;
				djiCC() {
						cb = new CommonCallbacks.CompletionCallback() {
										@Override
										public void onResult(DJIError e) {
												drvUnsetLock();
										}
								};
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

