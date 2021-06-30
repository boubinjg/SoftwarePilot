package com.dji.sdk.sample.internal.controller;

import com.dji.sdk.sample.internal.controller.AuavDrivers;
import android.animation.AnimatorInflater;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.model.ViewWrapper;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.view.DemoListView;
import com.dji.sdk.sample.internal.view.PresentableView;
import com.squareup.otto.Subscribe;

import java.util.Stack;

import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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



public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private FrameLayout contentFrameLayout;
    private ObjectAnimator pushInAnimator;
    private ObjectAnimator pushOutAnimator;
    private ObjectAnimator popInAnimator;
    private LayoutTransition popOutTransition;
    private ProgressBar progressBar;
    private Stack<ViewWrapper> stack;
    private TextView titleTextView;
    private SearchView searchView;
    private MenuItem searchViewItem;
    private MenuItem hintItem;

    public boolean hasRegistered = false;

    Level AUAVLEVEL = Level.ALL;

    HashMap n2p = new HashMap<String, String>();
    AuavDrivers[] ad = new AuavDrivers[128];

    /*public MainActivity(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }*/


    //region Life-cycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DJISampleApplication.getEventBus().register(this);
        setContentView(R.layout.activity_main);
        setupActionBar();
        contentFrameLayout = (FrameLayout) findViewById(R.id.framelayout_content);
        initParams();

        //new MainActivity.ConnectivityChangeEvent();

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
                    System.out.println(e.toString());
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
                    ad[x] = instantiate(jarNames[x], com.dji.sdk.sample.internal.controller.AuavDrivers.class);
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
                System.out.println(mapAsString);

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

    @Override
    protected void onDestroy() {
        DJISampleApplication.getEventBus().unregister(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu from XML
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchViewItem = menu.findItem(R.id.action_search);
        hintItem = menu.findItem(R.id.action_hint);
        searchView = (SearchView) searchViewItem.getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                DJISampleApplication.getEventBus().post(new SearchQueryEvent(""));
                return false;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                DJISampleApplication.getEventBus().post(new SearchQueryEvent(query));
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                DJISampleApplication.getEventBus().post(new SearchQueryEvent(newText));
                return false;
            }
        });

        // Hint click
        hintItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                showHint();
                return false;
            }
        });
        return true;
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
            Intent attachedIntent = new Intent();
            attachedIntent.setAction(DJISDKManager.USB_ACCESSORY_ATTACHED);
            sendBroadcast(attachedIntent);
        }
    }

    @Override
    public void onBackPressed() {
        if (stack.size() > 1) {
            popView();
        } else {
            super.onBackPressed();
        }
    }

    //endregion

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (null != actionBar) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.actionbar_custom);

            titleTextView = (TextView) (actionBar.getCustomView().findViewById(R.id.title_tv));
        }
    }


    private void setupInAnimations() {
        pushInAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(this, R.animator.slide_in_right);
        pushOutAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(this, R.animator.fade_out);
        popInAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(this, R.animator.fade_in);
        ObjectAnimator popOutAnimator =
                (ObjectAnimator) AnimatorInflater.loadAnimator(this, R.animator.slide_out_right);

        pushOutAnimator.setStartDelay(100);

        popOutTransition = new LayoutTransition();
        popOutTransition.setAnimator(LayoutTransition.DISAPPEARING, popOutAnimator);
        popOutTransition.setDuration(popOutAnimator.getDuration());
    }

    private void initParams() {
        setupInAnimations();

        stack = new Stack<ViewWrapper>();
        View view = contentFrameLayout.getChildAt(0);
        stack.push(new ViewWrapper(view, R.string.activity_component_list));
    }

    private void pushView(ViewWrapper wrapper) {
        if (stack.size() <= 0) {
            return;
        }

        contentFrameLayout.setLayoutTransition(null);

        View showView = wrapper.getView();

        View preView = stack.peek().getView();

        stack.push(wrapper);

        if (showView.getParent() != null) {
            ((ViewGroup) showView.getParent()).removeView(showView);
        }
        contentFrameLayout.addView(showView);

        pushOutAnimator.setTarget(preView);
        pushOutAnimator.start();

        pushInAnimator.setTarget(showView);
        pushInAnimator.setFloatValues(contentFrameLayout.getWidth(), 0);
        pushInAnimator.start();

        refreshTitle();
        refreshOptionsMenu();
    }

    private void refreshTitle() {
        if (stack.size() > 1) {
            ViewWrapper wrapper = stack.peek();
            titleTextView.setText(wrapper.getTitleId());
        } else if (stack.size() == 1) {
            BaseProduct product = DJISampleApplication.getProductInstance();
            if (product != null && product.getModel() != null) {
                titleTextView.setText("" + product.getModel().getDisplayName());
            } else {
                titleTextView.setText(R.string.sample_app_name);
            }
        }
    }

    private void popView() {

        if (stack.size() <= 1) {
            finish();
            return;
        }

        ViewWrapper removeWrapper = stack.pop();

        View showView = stack.peek().getView();
        View removeView = removeWrapper.getView();

        contentFrameLayout.setLayoutTransition(popOutTransition);
        contentFrameLayout.removeView(removeView);

        popInAnimator.setTarget(showView);
        popInAnimator.start();

        refreshTitle();
        refreshOptionsMenu();
    }

    private void refreshOptionsMenu() {
        if (stack.size() == 2 && stack.peek().getView() instanceof DemoListView) {
            searchViewItem.setVisible(true);
        } else {
            searchViewItem.setVisible(false);
            searchViewItem.collapseActionView();
        }
        if (stack.size() == 3 && stack.peek().getView() instanceof PresentableView) {
            hintItem.setVisible(true);
        } else {
            hintItem.setVisible(false);
        }
    }


    private void showHint() {
        if (stack.size() != 0 && stack.peek().getView() instanceof PresentableView) {
            ToastUtils.setResultToToast(((PresentableView) stack.peek().getView()).getHint());
        }
    }


    //region Event-Bus
    @Subscribe
    public void onReceiveStartFullScreenRequest(RequestStartFullScreenEvent event) {
        getSupportActionBar().hide();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Subscribe
    public void onReceiveEndFullScreenRequest(RequestEndFullScreenEvent event) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getSupportActionBar().show();
    }

    @Subscribe
    public void onPushView(final ViewWrapper wrapper) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pushView(wrapper);
            }
        });
    }

    @Subscribe
    public void onConnectivityChange(ConnectivityChangeEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshTitle();
            }
        });
    }

    public static class SearchQueryEvent {
        private final String query;

        public SearchQueryEvent(String query) {
            this.query = query;
        }

        public String getQuery() {
            return query;
        }
    }

    public static class RequestStartFullScreenEvent {
    }

    public static class RequestEndFullScreenEvent {
    }

    public static class ConnectivityChangeEvent {
    }
    //endregion
    public <T> T instantiate(final String className, final Class<T> type) {
        try {
            System.out.println("Loading driver: " + className);
            return type.cast(Class.forName(className).newInstance());
        } catch (InstantiationException
                | IllegalAccessException
                | ClassNotFoundException e) {
            System.out.println("Error:" + e.toString() + "\nStack" + e.getStackTrace().toString());
            throw new IllegalStateException(e);
        }
    }

}
