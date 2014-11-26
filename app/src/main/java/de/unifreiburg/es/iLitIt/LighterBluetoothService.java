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
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class LighterBluetoothService extends Service {
    private final static String TAG = LighterBluetoothService.class.getSimpleName();
    public static final String KEY_DEVICEADDR = "device_addr";
    public static final String FILENAME = "cigarettes.csv";
    public static final String EXTRA_ARRAY_OF_10_CIGARETTES = "last10cigs";
    public static final String EXTRA_FILE_URI = "fileuri";
    public static final String IACTION = "de.unifreiburg.es.iLitIt.CIGARETTES";
    private static final long LIST_WRITE_DELAY = 1500;
    public String KEY_SCANSTARTDELAY = "scan_timeout";
    public static final DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

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

    private ObservableLinkedList<Date> mEventList = new ObservableLinkedList<Date>();

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
            Log.w(TAG, "got event at " + date);

            mEventList.add(date);
        }
    };

    public java.lang.Runnable rListWrite = new Runnable() {
        @Override
        public void run() {
            try {
                FileOutputStream fos = openFileOutput(FILENAME, MODE_PRIVATE);

                for (Date d : mEventList) {
                    fos.write(dateformat.format(d).getBytes("utf-8"));
                    fos.write("\n".getBytes("utf-8"));
                }

                fos.close();
            } catch (Exception e) {
                Log.e(TAG, "unable to write ", e);
            }

            Log.i(TAG, "succesfully written cigs to " + FILENAME);
        }
    };
    private static boolean serviceIsInitialized = false;
    private BroadcastReceiver mBluetoothChangeReceiver;

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public void clear_mac_addr() {
        mBluetoothDeviceAddress = null;
        PreferenceManager.getDefaultSharedPreferences(LighterBluetoothService.this).edit().
                putString(KEY_DEVICEADDR, mBluetoothDeviceAddress).apply();
        mEventList.fireEvent();
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
                    close();
                    onStartCommand(null, 0, 0);
                }
            };

            IntentFilter mif = new IntentFilter();
            mif.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBluetoothChangeReceiver, mif);
        }

        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (serviceIsInitialized)
            return START_STICKY;

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return START_NOT_STICKY;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return START_NOT_STICKY;
        }

        serviceIsInitialized = true;

        // for DEBUGGING only
        // PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply();

        /** check if we are already bound to a device, if not start scanning for one */
        mBluetoothDeviceAddress = PreferenceManager.getDefaultSharedPreferences(this).
                getString(KEY_DEVICEADDR, null);

        /** create a handler on the UI thread */
        mHandler = new Handler(Looper.getMainLooper());
        mScanStartDelay = PreferenceManager.getDefaultSharedPreferences(this).getLong(KEY_SCANSTARTDELAY, 20 * 1000);
        mHandler.postDelayed(rStartLEScan, 10);

        super.onStartCommand(intent, flags, startId);

        /** load the stored events */
        try {
            FileInputStream fis = openFileInput(FILENAME);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;

            while ((line=br.readLine())!=null) {
                Date d = dateformat.parse(line);
                mEventList.add(d);
            }
        } catch(Exception e) {
            Log.e(TAG, "file load failed",e);
        }

        /** add an observer to the model that store the list after a change has occured,
         * and after a certain delay to avoid to much delay for event handling. */
        mEventList.register(new DelayedObserver(1500,rListWrite));

        return START_STICKY;
    }

    public ObservableLinkedList<Date> getModel() {
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

    @Override
    public void onDestroy() {
        Log.e(TAG, "service destroyed");
        serviceIsInitialized=false;
        super.onDestroy();
    }

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

        if (mEventList.size() == 0)
            return;

        LinkedList<String> li = new LinkedList<String>();
        for (int i = mEventList.size() > 10 ? 10 : mEventList.size(); i > 0; i--)
            li.add(mEventList.get(mEventList.size() - i).toString());

        Intent info = new Intent(IACTION);
        info.putExtra(EXTRA_ARRAY_OF_10_CIGARETTES, li.toArray(new String[li.size()]));
        info.putExtra(EXTRA_FILE_URI, FILENAME);
        info.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        sendBroadcast(info);

        Log.e(TAG, info.toString());

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

                if ( connect(mBluetoothDeviceAddress) ) {
                    Log.w(TAG, "stopping the scan, found connectable device " + device.getAddress() );
                    mBluetoothAdapter.stopLeScan(this);
                }

                //mHandler.postDelayed(stopLEScan, timeout_ms)
            }
    };
}
