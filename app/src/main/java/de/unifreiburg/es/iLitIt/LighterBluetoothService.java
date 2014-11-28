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
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class LighterBluetoothService extends Service {
    private final static String TAG = LighterBluetoothService.class.getSimpleName();
    public static final String KEY_DEVICEADDR = "device_addr";
    public static final String FILENAME = "cigarettes.csv";
    public String KEY_SCANSTARTDELAY = "scan_timeout";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private Handler mHandler;
    private long mScanStartDelay;

    public final static UUID UUID_SERVICE =
            UUID.fromString("595403fb-f50e-4902-a99d-b39ffa4bb134");
    public final static UUID UUID_TIME_MEASUREMENT =
            UUID.fromString("595403fc-f50e-4902-a99d-b39ffa4bb134");
    public final static UUID UUID_CCC =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private ObservableLinkedList<CigaretteEvent> mEventList =null;

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (mBluetoothGatt==null) {
                Log.e(TAG, "problem problem");
                mBluetoothGatt = gatt;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server. " + status);
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                close();
                mHandler.postDelayed(rStartLEScan, mScanStartDelay);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "onServicesDiscovered received: " + status);
                return;
            }

            BluetoothGattCharacteristic c =
                    gatt.getService(UUID_SERVICE).getCharacteristic(UUID_TIME_MEASUREMENT);

            if (c == null) {
                Log.w(TAG, "onServiceDiscovered TIME characteristics UUID not found!");
                return;
            }

            // subscribing
            gatt.setCharacteristicNotification(c, true);
            BluetoothGattDescriptor config = c.getDescriptor(UUID_CCC);
            config.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean rw = gatt.writeDescriptor(config);
            Log.d(TAG, "attempting to subscribe characteristic " + rw);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic c) {
            Log.w(TAG, "characteristics changed ");

            long send_time = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0),
                 evnt_time = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 4),
                 diff = send_time - evnt_time;

            Date date = new Date(System.currentTimeMillis() - diff);
            Log.w(TAG, "got event at " + date + " " + Thread.currentThread().getName());

            Location location = null; // XXX
            mEventList.add( new CigaretteEvent(date, location) );
        }
    };

    private BroadcastReceiver mBluetoothChangeReceiver;
    private LocationClient mLocationClient;

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

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
                    close();
                }
            };

            IntentFilter mif = new IntentFilter();
            mif.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBluetoothChangeReceiver, mif);
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

        super.onStartCommand(intent, flags, startId);

        /** load the stored events */
        if (mEventList==null) {
            mEventList = CigAnnotationWriter.readCigaretteList(this);
        }

        /** set-up the location service, we need this to run here, since we need to
         *access the location whenever there is a chang to the cigarette model. */
        mLocationClient = new LocationClient(this, mLocationHandler, mLocationHandler);
        mEventList.register(new DelayedObserver(1000, mLocationHandler));

        /** start to scan for LE devices in the area */
        mHandler = new Handler(Looper.getMainLooper());
        mScanStartDelay = PreferenceManager.getDefaultSharedPreferences(this).getLong(KEY_SCANSTARTDELAY, 20 * 1000);
        mHandler.postDelayed(rStartLEScan, 10);

        return START_STICKY;
    }

    public ObservableLinkedList<CigaretteEvent> getModel() {
        return mEventList;
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

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null &&
            address.equals(mBluetoothDeviceAddress) &&
            mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            return true;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // use auto-connect, whenever possible
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);

        Log.d(TAG, "Trying to create a new connection. ");
        mBluetoothDeviceAddress = address;
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void close() {
        Log.e(TAG, "connection closed.");

        if (mBluetoothGatt == null) {
            return;
        }

        mBluetoothGatt.close();
        mBluetoothGatt = null;

        // start a new scan immediately
        mHandler.postDelayed(rStartLEScan, 10);
    }

    private Runnable rStartLEScan = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "starting LE/Lighter Scan.");
            mBluetoothAdapter.startLeScan(mFindLighterDevice);
        }
    };

    private final BluetoothAdapter.LeScanCallback mFindLighterDevice =
        new BluetoothAdapter.LeScanCallback() {
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                // just make sure we don't hang
                mHandler.removeCallbacks(rStartLEScan);
                mHandler.postDelayed(rStartLEScan, mScanStartDelay);

                if (device.getName() == null)
                    return;

                Log.i(TAG, "found device " + device.getName() + " with rssi" + rssi);

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

                final String addr = device.getAddress();
                mHandler.post(new Runnable() { // SAMSUNG workaround
                    @Override
                    public void run() {
                        if ( connect(mBluetoothDeviceAddress) ) {
                            Log.w(TAG, "stopping the scan, found connectable device " + addr);
                            mBluetoothAdapter.stopLeScan(mFindLighterDevice);
                        }
                    }
                });


                //mHandler.postDelayed(stopLEScan, timeout_ms)
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
