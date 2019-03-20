package org.reroutlab.code.auav.kernels.auavandroid;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.Manifest;
import android.util.Log;
import android.support.v4.app.ActivityCompat;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.support.v4.content.ContextCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;
import org.reroutlab.code.auav.drivers.AuavDrivers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.log.DJILog;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.base.BaseProduct.ComponentKey;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.BluetoothProductConnector;
import dji.sdk.sdkmanager.DJISDKManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opencv.android.OpenCVLoader;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AUAVAndroid";
    private static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    public boolean hasRegistered = false;
    private Handler mHandler;
    private static BaseProduct mProduct;
    Level AUAVLEVEL = Level.ALL;

    HashMap n2p = new HashMap<String, String>();
    AuavDrivers[] ad = new AuavDrivers[128];

    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };
    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private static final int REQUEST_PERMISSION_CODE = 12345;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions();
        }

        setContentView(R.layout.activity_main);

        mHandler = new Handler(Looper.getMainLooper());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        /*
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.INTERNET, Manifest.permission.VIBRATE,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                        Manifest.permission.READ_PHONE_STATE,
                }
                , 1);
        */

        if(!OpenCVLoader.initDebug()){
            Log.v(TAG, "OpenCV not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this,
                    new BaseLoaderCallback(this) {
                @Override
                public void onManagerConnected(int status) {
                    switch (status) {
                        case LoaderCallbackInterface.SUCCESS:
                        {
                            Log.i("OpenCV", "OpenCV loaded successfully");
                            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
                            CascadeClassifier tt = new CascadeClassifier();

                            Mat imageMat=new Mat();
                        } break;
                        default:
                        {
                            super.onManagerConnected(status);
                        } break;
                    }
                }
            });
        } else {
            Log.v(TAG, "OpenCV loaded");
        }


        /*DJISDKManager.getInstance().registerApp(MainActivity.this.getApplicationContext(), mDJISDKManagerCallback);

        while (hasRegistered == false) {
            System.out.println("No Registration Loop");
            try {
                Thread.sleep(10000);
            } catch (Exception e) {
            }
            DJISDKManager.getInstance().registerApp(MainActivity.this.getApplicationContext(), mDJISDKManagerCallback);
        }*/
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            showToast("Need to grant the permissions!");
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            showToast("Missing permissions!!!");
        }

        /*
        boolean b = true;
        for (int x = 0; x < grantResults.length; x++) {
            if (grantResults[x] != PackageManager.PERMISSION_GRANTED) {
                b = false;
            }
        }
        if (b == true) {
        }*/
    }

    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    DJISDKManager.getInstance().registerApp(MainActivity.this.getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                System.out.println("Registration success");
                                boolean connected2DJI = false;
                                DJISDKManager.getInstance().startConnectionToProduct();
                                //System.out.println("Can you See Me?");
                                /*while(!connected2DJI) {
                                    System.out.println("Trying to connect");
                                    try {Thread.sleep(2000);}catch(Exception e){
                                        e.printStackTrace(); };

                                    System.out.println("Attempting to start connection to product");
                                    boolean b = DJISDKManager.getInstance().startConnectionToProduct();
                                    System.out.println("Connection returned: "+b);

                                    if (DJISDKManager.getInstance().getProduct() != null) {
                                        connected2DJI = true;
                                        reroutProductConnect(DJISDKManager.getInstance().getProduct());
                                    }
                                }*/
                            } else {
                                showToast("Register sdk fails, please check the bundle id and network connection!");
                            }
                            Log.v(TAG, djiError.getDescription());
                        }
                        /*
                        @Override
                        public void onProductDisconnect() {
                            Log.d(TAG, "onProductDisconnect");
                            showToast("Product Disconnected");
                            notifyStatusChange();

                        }*/
                        @Override
                        public void onProductChange(BaseProduct op, BaseProduct baseProduct) {
                            int productChanged = 0;
                            System.out.println("Product Connected");
                            Log.d(TAG, String.format("onProductConnect newProduct:%s", baseProduct));
                            showToast("Product Connected");
                            notifyStatusChange();

                            if ( (baseProduct != null) && (productChanged== 0)) {
                                productChanged = 1;
                                hasRegistered =true;
                                Thread t = new Thread() {
                                    public void run() {
                                        String jarList = "";
                                        long st = System.currentTimeMillis();
                                        try {
                                            String line = "";
                                            InputStream is = getAssets().open("jarList");
                                            BufferedReader br = new BufferedReader(new InputStreamReader(is));
                                            while ((line = br.readLine()) != null) {
                                                if (line.startsWith("#") == false) {
                                                    jarList = jarList + line.trim() + ":";
                                                }
                                            }
                                            br.close();
                                        } catch (Exception e) {
                                            Log.v(TAG, e.toString());
                                        }

                                        String[] fullPath = jarList.split(".jar:");
                                        String[] jarNames = new String[fullPath.length];
                                        int countDrivers = 0;
                                        for (int x = 0; x < fullPath.length; x++) {
                                            String[] seps = fullPath[x].split("/");
                                            if (seps[seps.length - 1].endsWith("Driver") == true) {
                                                jarNames[countDrivers] = seps[seps.length - 1];
                                                countDrivers++;
                                            }
                                        }

                                        for (int x = 0; x < countDrivers; x++) {
                                            System.out.println("Jar: " + jarNames[x]);
                                            ad[x] = instantiate(jarNames[x], org.reroutlab.code.auav.drivers.AuavDrivers.class);
                                            String canon = ad[x].getClass().getCanonicalName();
                                            n2p.put(canon,
                                                    new String(""+ad[x].getLocalPort()+"\n" ) );
                                            String nick = canon.substring(canon.lastIndexOf(".")+1);
                                            if (n2p.containsKey(nick) == false) {
                                                n2p.put(nick,
                                                        new String(""+ad[x].getLocalPort()+"\n" ) );
                                            }
                                            else {
                                                n2p.remove(nick);
                                            }
                                        }

                                        // Printing the map object locally for logging
                                        String mapAsString = "Active Drivers\n";
                                        Set keys = n2p.keySet();
                                        for (Iterator i = keys.iterator(); i.hasNext(); ) {
                                            String name = (String) i.next();
                                            String value = (String) n2p.get(name);
                                            mapAsString = mapAsString + name + " --> " + value + "\n";
                                        }
                                        Log.v(TAG, mapAsString);

                                        for (int x = 0; x < countDrivers; x++) {
                                            // Send the map back to each object
                                            ad[x].setDriverMap(n2p);
                                            ad[x].setLogLevel(AUAVLEVEL);
                                            ad[x].setStartTime(st);
                                            ad[x].getCoapServer().start();
                                        }
                                    }
                                };
                                try {
                                    File newAssetDir = new File("/sdcard/AUAVassets/");
                                    if (newAssetDir.exists() == false ) {
                                        newAssetDir.mkdirs();
                                    }
                                    String[] myAssetsList = getAssets().list("AUAVassets");
                                    for (String myAsset : myAssetsList) {
                                        try {
                                            File newAssetFile = new File(newAssetDir, myAsset);
                                            InputStream is = getAssets().open("AUAVassets/"+myAsset);
                                            FileOutputStream os = new FileOutputStream(newAssetFile);

                                            byte[] buffer = new byte[4096];
                                            int bytesRead;
                                            while ((bytesRead = is.read(buffer)) != -1) {
                                                os.write(buffer, 0, bytesRead);
                                            }
                                            is.close();
                                            os.close();
                                        }
                                        catch (Exception e) {
                                            System.out.println("Kernel: Error moving asset to file " + myAsset);
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                catch(IOException err) {
                                    System.out.println("Kernel: Unable to mkdir" );
                                    err.printStackTrace();
                                }


                                t.start();


                            }
                            /*else {
                                try {
                                    Thread.sleep(10000);
                                }
                                catch(Exception e) {

                                }
                                if (hasRegistered == false) {
                                    Log.w("onProduct note", "Connect returns" + DJISDKManager.getInstance().startConnectionToProduct());
                                }
                            }*/
                        }
                        /*
                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent,
                                                      BaseComponent newComponent) {

                            if (newComponent != null) {
                                newComponent.setComponentListener(new BaseComponent.ComponentListener() {

                                    @Override
                                    public void onConnectivityChange(boolean isConnected) {
                                        Log.d(TAG, "onComponentConnectivityChanged: " + isConnected);
                                        notifyStatusChange();
                                    }
                                });
                            }
                            Log.d(TAG,
                                    String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                            componentKey,
                                            oldComponent,
                                            newComponent));

                        }*/

                    });
                }
            });
        }
    }

    public <T> T instantiate(final String className, final Class<T> type) {
        try {
            Log.v(TAG, "Loading driver: " + className);
            return type.cast(Class.forName(className).newInstance());
        } catch (InstantiationException
                | IllegalAccessException
                | ClassNotFoundException e) {
            Log.e(TAG, "Error:" + e.toString() + "\nStack" + e.getStackTrace().toString());
            throw new IllegalStateException(e);
        }
    }

    public void reroutProductConnect(BaseProduct baseProduct) {
        int productChanged = 0;
        System.out.println("Product Connected");
        Log.d(TAG, String.format("onProductConnect newProduct:%s", baseProduct));
        showToast("Product Connected");
        notifyStatusChange();

        if ( (baseProduct != null) && (productChanged== 0)) {
            productChanged = 1;
            hasRegistered =true;
            Thread t = new Thread() {
                public void run() {
                    String jarList = "";
                    long st = System.currentTimeMillis();
                    try {
                        String line = "";
                        InputStream is = getAssets().open("jarList");
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        while ((line = br.readLine()) != null) {
                            if (line.startsWith("#") == false) {
                                jarList = jarList + line.trim() + ":";
                            }
                        }
                        br.close();
                    } catch (Exception e) {
                        Log.v(TAG, e.toString());
                    }

                    String[] fullPath = jarList.split(".jar:");
                    String[] jarNames = new String[fullPath.length];
                    int countDrivers = 0;
                    for (int x = 0; x < fullPath.length; x++) {
                        String[] seps = fullPath[x].split("/");
                        if (seps[seps.length - 1].endsWith("Driver") == true) {
                            jarNames[countDrivers] = seps[seps.length - 1];
                            countDrivers++;
                        }
                    }

                    for (int x = 0; x < countDrivers; x++) {
                        System.out.println("Jar: " + jarNames[x]);
                        ad[x] = instantiate(jarNames[x], org.reroutlab.code.auav.drivers.AuavDrivers.class);
                        String canon = ad[x].getClass().getCanonicalName();
                        n2p.put(canon,
                                new String(""+ad[x].getLocalPort()+"\n" ) );
                        String nick = canon.substring(canon.lastIndexOf(".")+1);
                        if (n2p.containsKey(nick) == false) {
                            n2p.put(nick,
                                    new String(""+ad[x].getLocalPort()+"\n" ) );
                        }
                        else {
                            n2p.remove(nick);
                        }
                    }

                    // Printing the map object locally for logging
                    String mapAsString = "Active Drivers\n";
                    Set keys = n2p.keySet();
                    for (Iterator i = keys.iterator(); i.hasNext(); ) {
                        String name = (String) i.next();
                        String value = (String) n2p.get(name);
                        mapAsString = mapAsString + name + " --> " + value + "\n";
                    }
                    Log.v(TAG, mapAsString);

                    for (int x = 0; x < countDrivers; x++) {
                        // Send the map back to each object
                        ad[x].setDriverMap(n2p);
                        ad[x].setLogLevel(AUAVLEVEL);
                        ad[x].setStartTime(st);
                        ad[x].getCoapServer().start();
                    }
                }
            };
            try {
                File newAssetDir = new File("/sdcard/AUAVassets/");
                if (newAssetDir.exists() == false ) {
                    newAssetDir.mkdirs();
                }
                String[] myAssetsList = getAssets().list("AUAVassets");
                for (String myAsset : myAssetsList) {
                    try {
                        File newAssetFile = new File(newAssetDir, myAsset);
                        InputStream is = getAssets().open("AUAVassets/"+myAsset);
                        FileOutputStream os = new FileOutputStream(newAssetFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();
                    }
                    catch (Exception e) {
                        System.out.println("Kernel: Error moving asset to file " + myAsset);
                        e.printStackTrace();
                    }
                }
            }
            catch(IOException err) {
                System.out.println("Kernel: Unable to mkdir" );
                err.printStackTrace();
            }


            t.start();


        }
        else {
            try {
                Thread.sleep(10000);
            }
            catch(Exception e) {

            }
            if (hasRegistered == false) {
                Log.w("onProduct note", "Connect returns" + DJISDKManager.getInstance().startConnectionToProduct());
            }
        }
    }

    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = new Runnable() {

        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            sendBroadcast(intent);
        }
    };

    private void showToast(final String toastMsg) {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
            }
        });

    }

    /*@Override
    public boolean o/nCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/

}
