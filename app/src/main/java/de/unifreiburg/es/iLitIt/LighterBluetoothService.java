/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.unifreiburg.es.iLitIt;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class LighterBluetoothService extends Service {
    private final static String TAG = LighterBluetoothService.class.getSimpleName();
    public static final String KEY_DEVICEADDR = "device_addr";
    private static final int PROGRESS_ID = 0;
    private static final int BATLOW_ID = 1;
    private static final String KEY_BATEMTPY = "bat_emtpy";
    private static final String KEY_BATVOLTAGE = "bat_voltage";
    private BroadcastReceiver mSmartWatchAnnotationReceiver =  null;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private Handler mHandler;
    private final CigAnnotationWriter rCigAnnotationWriter = new CigAnnotationWriter(this);
    private final CigIntentBroadcaster rCigIntentBroadcaster = new CigIntentBroadcaster(this);

    public final static UUID UUID_SERVICE =
            UUID.fromString("595403fb-f50e-4902-a99d-b39ffa4bb134");
    public final static UUID UUID_TIME_MEASUREMENT =
            UUID.fromString("595403fc-f50e-4902-a99d-b39ffa4bb134");
    public final static UUID UUID_CCC =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private ObservableLinkedList<CigaretteEvent> mEventList =null;

    private final BluetoothGattCallback rGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG,
                    String.format("onConnectionStateChanged status=%d newState=%d",status,newState));

            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                Log.d(TAG, "Connected to GATT server (" + status + "), starting discovery...");
                gatt.discoverServices();
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                Log.d(TAG, "Disconnected from GATT server.");
                gatt.close();

                mBluetoothGatt = null;
                mHandler.post(rStartLEScan);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG,
                    String.format("onServicesDiscovery status=%d", status));

            if (status != BluetoothGatt.GATT_SUCCESS)
                return;

            BluetoothGattCharacteristic c =
                    gatt.getService(UUID_SERVICE).getCharacteristic(UUID_TIME_MEASUREMENT);

            if (c == null) {
                Log.d(TAG, "onServiceDiscovered TIME characteristics UUID not found!");
                return;
            }

            // attempting to read characteristic
            boolean r = gatt.readCharacteristic(c);
            Log.d(TAG, "attempted read " + r);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharactersiticRead " + status);

            // subscribing
            boolean rw_ = gatt.setCharacteristicNotification(characteristic, true);
            Log.d(TAG, "attempting to set char notification: " + rw_);

            BluetoothGattDescriptor config = characteristic.getDescriptor(UUID_CCC);
            config.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean rw = gatt.writeDescriptor(config);
            Log.d(TAG, "attempting to subscribe characteristic " + rw);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic c) {
            Log.d(TAG, "characteristics changed ");

            long send_time = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0),
                 evnt_time = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 4),
                 diff = send_time - evnt_time;

            Date date = new Date(System.currentTimeMillis() - diff);
            Log.d(TAG, "got event at " + date + " " + Thread.currentThread().getName());

            mEventList.add( new CigaretteEvent(date, "lighter", null) );
        }
    };

    private BroadcastReceiver mBluetoothChangeReceiver;
    private LocationClient mLocationClient;
    private Notification mNotification;
    private Notification mLowBatteryWarning;

    public void clear_mac_addr() {
        mBluetoothDeviceAddress = null;
        PreferenceManager.getDefaultSharedPreferences(LighterBluetoothService.this).edit().
                putString(KEY_DEVICEADDR, mBluetoothDeviceAddress).apply();
        mEventList.fireEvent(null);
    }

    public String get_mac_addr() {
        if (mBluetoothDeviceAddress==null)
            return "none";

        return mBluetoothDeviceAddress;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mBluetoothChangeReceiver == null) {
            mBluetoothChangeReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                                               BluetoothAdapter.ERROR);

                    switch(state) {
                        case BluetoothAdapter.STATE_ON:
                            onStartCommand(null,0,0);
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            break;
                        default:
                            break;
                    }
                }
            };

            IntentFilter mif = new IntentFilter();
            mif.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBluetoothChangeReceiver, mif);
        }

        if (mSmartWatchAnnotationReceiver==null) {
            mSmartWatchAnnotationReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (mEventList == null)
                        return;

                    String via = intent.getStringExtra("ess.imu_logger.libs.data_save.extra.annotationVia");
                    mEventList.add( new CigaretteEvent(new Date(), via==null ? "intent" : via, null ) );
                }
            };

            // create watch ui intent listener
            IntentFilter filter = new IntentFilter("ess.imu_logger.libs.data_save.annotate");
            registerReceiver(mSmartWatchAnnotationReceiver, filter);
        }

        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        //if (serviceIsInitialized)
        //    return START_STICKY;

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
            return START_NOT_STICKY;
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return START_NOT_STICKY;
        }

        // for DEBUGGING only
        // PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply();

        /** check if we are already bound to a device, if not start scanning for one */
        mBluetoothDeviceAddress = PreferenceManager.getDefaultSharedPreferences(this).
                getString(KEY_DEVICEADDR, null);
        mLastBatteryVoltage = PreferenceManager.getDefaultSharedPreferences(this).
                getFloat(KEY_BATVOLTAGE, 0.0f);
        mBatteryEmpty = PreferenceManager.getDefaultSharedPreferences(this).
                getBoolean(KEY_BATEMTPY, false);

        super.onStartCommand(intent, flags, startId);

        /** load the stored events */
        if (mEventList==null) {
            mEventList = CigAnnotationWriter.readCigaretteList(this);

            mEventList.register(rCigAnnotationWriter);
            mEventList.register(rCigIntentBroadcaster);

        }

        /** set-up the location service, we need this to run here, since we need to
         *access the location whenever there is a chang to the cigarette model. */
        mLocationClient = new LocationClient(this, mLocationHandler, mLocationHandler);
        mEventList.register(new DelayedObserver(1000, mLocationHandler));

        /** start to scan for LE devices in the area */
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.post(rStartLEScan);

        /** create a notification on a pending connection */
        PendingIntent i = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        mNotification = (new NotificationCompat.Builder(this))
                .setContentText("downloading cigarette events")
                .setContentTitle("iLitIt")
                .setSmallIcon(R.drawable.ic_launcher)
                .setProgress(0,0,true)
                .setAutoCancel(true)
                .setContentIntent(i)
                .build();

        mLowBatteryWarning = (new NotificationCompat.Builder(this))
                .setContentTitle("iLitIt - battery low")
                .setContentText("replace battery as soons as possible")
                .setSmallIcon(R.drawable.ic_launcher)
                .build();

        return START_STICKY;
    }

    public ObservableLinkedList<CigaretteEvent> getModel() {
        return mEventList;
    }

    public double get_bat_voltage() {
        return mLastBatteryVoltage;
    }

    public boolean is_bat_empty() {
        return mBatteryEmpty;
    }

    public class LocalBinder extends Binder {
        LighterBluetoothService getService() {
            return LighterBluetoothService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    private final IBinder mBinder = new LocalBinder();

    private Runnable rStartLEScan = new Runnable() {
        @Override
        public void run() {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(PROGRESS_ID);
            Log.i(TAG, "starting LE/Lighter Scan.");
            mBluetoothAdapter.startLeScan(mFindLighterDevice);
        }
    };

    private double mLastBatteryVoltage = 0.0;
    private boolean mBatteryEmpty = false;
    private final BluetoothAdapter.LeScanCallback mFindLighterDevice =
        new BluetoothAdapter.LeScanCallback() {
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                // just make sure we don't hang
                //mHandler.removeCallbacks(rStartLEScan);
                //mHandler.postDelayed(rStartLEScan, 20*1000);

                if (device.getName() == null)
                    return;

                Log.d(TAG, "found device " + device.getName() + " with rssi" + rssi);

                if (!device.getName().contains("iLitIt"))
                    return; // must be something else

                if (mBluetoothDeviceAddress != null &&
                    !mBluetoothDeviceAddress.equals(device.getAddress()))
                    return;

                if (mBluetoothDeviceAddress == null) {
                    mBluetoothDeviceAddress = device.getAddress();
                    PreferenceManager.getDefaultSharedPreferences(LighterBluetoothService.this).edit().
                            putString(KEY_DEVICEADDR, mBluetoothDeviceAddress).apply();
                }

                try {
                    int offset = 0;
                    while (offset < scanRecord.length-1) {
                        int len = (int) scanRecord[offset] & 0xff,
                            typ = (int) scanRecord[offset+1] & 0xff;

                        if (typ == 0xff)
                            break; // manufacturer data

                        offset += len+1;
                    }

                    String s, ss[];
                    s = new String(Arrays.copyOfRange(scanRecord, offset+2, scanRecord.length), "utf-8");
                    ss = s.split(" ");

                    mLastBatteryVoltage = Double.parseDouble(ss[0]) / 1000.;
                    mBatteryEmpty = ss.length > 1 ? ss[1].contains("empty") : false;

                    PreferenceManager.
                            getDefaultSharedPreferences(LighterBluetoothService.this)
                            .edit()
                            .putBoolean(KEY_BATEMTPY, mBatteryEmpty)
                            .putFloat(KEY_BATVOLTAGE, (float) mLastBatteryVoltage)
                            .commit();

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mEventList.fireEvent(null);
                        }
                    });
                } catch (Exception e) {
                    Log.d(TAG, "unable to parse advertisement data", e);
                }

                mHandler.post(new Runnable() { // SAMSUNG workaround
                    @Override
                    public void run() {
                        if (mBluetoothGatt!=null)
                            Log.d(TAG, "previous connection still active?!");

                        Log.d(TAG, "stopping the scan, found connectable device " +
                                   mBluetoothDeviceAddress);
                        mBluetoothAdapter.stopLeScan(mFindLighterDevice);

                        mBluetoothGatt =
                                mBluetoothAdapter.getRemoteDevice(mBluetoothDeviceAddress)
                                .connectGatt(LighterBluetoothService.this, true, rGattCallback);

                        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).
                                notify(PROGRESS_ID, mNotification);

                        if (is_bat_empty())
                            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).
                                    notify(BATLOW_ID, mLowBatteryWarning);
                        else
                            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).
                                    cancel(BATLOW_ID);
                    }
                });
            }
    };

    private class MyLocationHandler implements
            GooglePlayServicesClient.ConnectionCallbacks,
            GooglePlayServicesClient.OnConnectionFailedListener, Runnable {

        @Override
        public void onConnected(Bundle bundle) {
            // we just go through the list of all cigarettes and fill the ones, where
            // a mock location is set. This way we don't need to get the location everywhere,
            // but just start the location service on a new event.
            boolean has_updated = false;

            if (!mLocationClient.isConnected())
                return; // this seems odd

            for (CigaretteEvent e : mEventList) {
                if (!e.hasValidLocation()) {
                    e.where = mLocationClient.getLastLocation();
                    mEventList.fireEvent(e); // let observer know about the update
                }
            }

            mLocationClient.disconnect();
        }

        @Override
        public void onDisconnected() {

        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.d(TAG, "connecting to LocationService failed " + connectionResult.toString());
        }

        @Override
        public void run() {
            // called when mEventList has changed
            for (CigaretteEvent e: mEventList) {
                if (!e.hasValidLocation()) {
                    Log.d(TAG, "connecting to LocationService");
                    mLocationClient.connect();
                    return;
                }
            }

            // make sure that there is no dangling connection
            mLocationClient.disconnect();
        }
    }

    private MyLocationHandler mLocationHandler = new MyLocationHandler();
}
