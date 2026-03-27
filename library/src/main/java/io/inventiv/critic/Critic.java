package io.inventiv.critic;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.core.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.squareup.seismic.ShakeDetector;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.inventiv.critic.client.PingCreator;

public final class Critic {
    public static final String INTENT_EXTRA_PRODUCT_ACCESS_TOKEN = "productAccessToken";

    private static ApplicationLifecycleTracker mApplicationLifecycleTracker = new ApplicationLifecycleTracker();
    private static Long mAppInstallId;
    private static JsonObject mBatteryJson = new JsonObject();
    private static Application mContext;
    private static String mProductAccessToken;
    private static JsonObject mProductMetadata = new JsonObject();
    private static ShakeDetector mShakeDetector;

    private Critic() {
        // stop external invocation.
    }

    public static void initialize(Application context, String productAccessToken) {

        if(productAccessToken == null || productAccessToken.length() == 0) {
            throw new AssertionError("You need to provide a Product Access Token to create a Report. See the Critic Getting Started Guide at https://inventiv.io/critic/critic-integration-getting-started/.");
        }
        else if(productAccessToken.equals("YOUR_PRODUCT_ACCESS_TOKEN")) {
            throw new AssertionError("Your Product Access Token is invalid. Please use a valid one. See the Critic Getting Started Guide at https://inventiv.io/critic/critic-integration-getting-started/.");
        }

        Critic.mContext = context;
        Critic.mProductAccessToken = productAccessToken;

        mContext.registerActivityLifecycleCallbacks(mApplicationLifecycleTracker);
        registerBatteryBroadcastReceiver();
        startShakeDetection();

        try {
            new PingCreator().create(mContext);
        }
        catch(Throwable t) {
            Log.e(PingCreator.class.getName(), "Failed to create ping.", t);
        }
    }

    public static void startShakeDetection() {

        if(mShakeDetector != null ) {
            // already detecting shakes.
            return;
        }

        SensorManager sensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mShakeDetector = new ShakeDetector(new Shakes());
        mShakeDetector.start(sensorManager);
    }

    public static void stopShakeDetection() {
        if(mShakeDetector != null) {
            mShakeDetector.stop();
        }
        mShakeDetector = null;
    }

    public static void showFeedbackReportActivity() {
        Intent intent = new Intent(mContext, FeedbackReportActivity.class);
        intent.putExtra(Critic.INTENT_EXTRA_PRODUCT_ACCESS_TOKEN, mProductAccessToken);
        mContext.startActivity(intent);
    }

    public static Long getAppInstallId() {
        return Critic.mAppInstallId;
    }

    public static void setAppInstallId(Long appInstallId) {
        Critic.mAppInstallId = appInstallId;
    }

    public static String getProductAccessToken() {
        return mProductAccessToken;
    }

    public static JsonObject getProductMetadata() {
        return Critic.mProductMetadata;
    }

    public static void setProductMetadata(JsonObject productMetadata) {
        Critic.mProductMetadata = productMetadata;
    }

    public static JsonObject getAppJson() {

        JsonObject app = new JsonObject();

        ApplicationInfo applicationInfo = mContext.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        String applicationName = stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : mContext.getString(stringId);
        app.addProperty("name", applicationName);

        String packageName = mContext.getPackageName();
        app.addProperty("package", packageName);
        app.addProperty("platform", "Android");

        try {
            PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(packageName, 0);

            JsonObject version = new JsonObject();
            version.addProperty("code", packageInfo.versionCode);
            version.addProperty("name", packageInfo.versionName);
            app.add("version", version);
        } catch (PackageManager.NameNotFoundException e) {
            // ignore.
        }

        return app;
    }

    public static JsonObject getDeviceJson() {

        JsonObject build = new JsonObject();

        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        String carrierName = telephonyManager.getNetworkOperatorName();
        if(carrierName == null || carrierName.length() == 0) {
            carrierName = "N/A";
        }

        JsonObject device = new JsonObject();
        device.addProperty("identifier", Critic.getApplicationInstanceID());
        device.addProperty("manufacturer", Build.MANUFACTURER);
        device.addProperty("model", Build.MODEL);
        device.addProperty("network_carrier", carrierName);
        device.addProperty("platform", "Android");
        device.addProperty("platform_version", Build.VERSION.RELEASE);

        return device;
    }

    public static JsonObject getDeviceStatusJson() {

        JsonObject deviceStatus = new JsonObject();
        if( mBatteryJson.get("charging_status") != null ) {
            deviceStatus.addProperty("battery_charging", mBatteryJson.get("charging_status").getAsBoolean());
        }
        if( mBatteryJson.get("health") != null ) {
            deviceStatus.addProperty("battery_health", mBatteryJson.get("health").getAsString());
        }
        if( mBatteryJson.get("percentage") != null ) {
            deviceStatus.addProperty("battery_level", mBatteryJson.get("percentage").getAsFloat());
        }

        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        deviceStatus.addProperty("disk_free", externalStorageDirectory.getFreeSpace());
        deviceStatus.addProperty("disk_total", externalStorageDirectory.getTotalSpace());
        deviceStatus.addProperty("disk_usable", externalStorageDirectory.getUsableSpace());

        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        deviceStatus.addProperty("memory_free", memoryInfo.availMem);
        deviceStatus.addProperty("memory_total", memoryInfo.totalMem);

        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if(PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_NETWORK_STATE)) {
            networkInfo = connectivityManager.getActiveNetworkInfo();
        }
        else {
            Log.w(Critic.class.getName(), "ACCESS_NETWORK_STATE is not granted. Can not retrieve NetworkInfo.");
        }
        deviceStatus.addProperty("network_cell_connected", (networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE));
        deviceStatus.addProperty("network_wifi_connected", (networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI));

        return deviceStatus;
    }

    private static void addProductMetadata(JsonObject metadata) {
        if(Critic.mProductMetadata == null || Critic.mProductMetadata.isJsonNull() || Critic.mProductMetadata.size() == 0) {
            // ignore empty product metadata.
            return;
        }

        Set<Map.Entry<String, JsonElement>> entrySet = Critic.mProductMetadata.entrySet();
        if(entrySet.size() > 0) {

            for (Map.Entry<String, JsonElement> entry : entrySet) {

                String key = entry.getKey();
                JsonElement value = entry.getValue();
                if("ic_application".equalsIgnoreCase(key) || "ic_device".equalsIgnoreCase(key)) {
                    throw new AssertionError("You specified a product metadata key of [" + key + "], which is a reserved key. Please choose something that does not start with [ic_].");
                }
                metadata.add(key, value);
            }
        }
    }

    /**
     * Make up a unique ID for the application instance. This is useful for tracking report activity across a singular application install.
     * If the user deletes the app and reinstalls later, this ID will change. It only persists as long as the app remains installed.
     *
     * @return a unique ID for the application instance.
     */
    private synchronized static String getApplicationInstanceID() {

        final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";

        SharedPreferences preferences = mContext.getSharedPreferences(PREF_UNIQUE_ID, Context.MODE_PRIVATE);
        String uniqueId = preferences.getString(PREF_UNIQUE_ID, null);
        if(uniqueId == null) {
            uniqueId = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(PREF_UNIQUE_ID, uniqueId);
            editor.commit();
        }

        return uniqueId;
    }

    private static void registerBatteryBroadcastReceiver() {
        mContext.registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context ctxt, Intent intent) {
                int healthExtra = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
                String health;
                switch(healthExtra) {
                    case BatteryManager.BATTERY_HEALTH_COLD:
                        health = "Cold";
                        break;
                    case BatteryManager.BATTERY_HEALTH_DEAD:
                        health = "Dead";
                        break;
                    case BatteryManager.BATTERY_HEALTH_GOOD:
                        health = "Good";
                        break;
                    case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                        health = "Over Voltage";
                        break;
                    case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                        health = "Overheat";
                        break;
                    case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                        health = "Unspecified Failure";
                        break;
                    default:
                        health = "Unknown";
                        break;
                }
                mBatteryJson.addProperty("health", health);

                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                mBatteryJson.addProperty("percentage", (level / (float)scale * 100));

                int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
                boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
                String chargingMethod = "N/A";
                if(acCharge) {
                    chargingMethod = "AC";
                }
                else if(usbCharge) {
                    chargingMethod = "USB";
                }
                mBatteryJson.addProperty("charging_method", chargingMethod);

                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
                mBatteryJson.addProperty("charging_status", isCharging);
            }
        }, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private static class Shakes implements ShakeDetector.Listener {

        boolean mAlreadyShaken = false;

        @Override
        public void hearShake() {

            if(mApplicationLifecycleTracker.mCurrentActivity == null) {
                Log.w(Critic.class.getSimpleName(), "No current activity! Can not handle shake.");
                return;
            }

            if(mApplicationLifecycleTracker.mCurrentDialog != null) {
                Log.w(Critic.class.getSimpleName(), "Shaken repeatedly. Ignoring.");
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(mApplicationLifecycleTracker.mCurrentActivity)
                    .setTitle("Easy, easy!")
                    .setMessage("Do you want to send us feedback?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Critic.showFeedbackReportActivity();
                            mAlreadyShaken = false;
                            mApplicationLifecycleTracker.mCurrentDialog = null;
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mAlreadyShaken = false;
                            mApplicationLifecycleTracker.mCurrentDialog = null;
                        }
                    }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            mAlreadyShaken = false;
                            mApplicationLifecycleTracker.mCurrentDialog = null;
                        }
                    });

            if(Build.VERSION.SDK_INT >= 17){
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        mAlreadyShaken = false;
                        mApplicationLifecycleTracker.mCurrentDialog = null;
                    }
                });
            }

            mApplicationLifecycleTracker.mCurrentDialog = builder.create();
            mApplicationLifecycleTracker.mCurrentDialog.show();
        }
    }

    private static class ApplicationLifecycleTracker implements Application.ActivityLifecycleCallbacks {

        Activity mCurrentActivity;
        AlertDialog mCurrentDialog;

        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {
            // do nothing.
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            // do nothing.
        }

        @Override
        public void onActivityPaused(Activity activity) {
            mCurrentActivity = null;
            mCurrentDialog = null;
        }

        @Override
        public void onActivityResumed(Activity activity) {
            mCurrentActivity = activity;
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
            // do nothing.
        }

        @Override
        public void onActivityStarted(Activity activity) {
            // do nothing.
        }

        @Override
        public void onActivityStopped(Activity activity) {
            // do nothing.
        }
    }
}
