package de.unifreiburg.es.iLitIt;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static de.unifreiburg.es.iLitIt.LighterBluetoothService.*;

public class BluetoothScanActivity extends ListActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private LighterListAdapter mDeviceListAdapter;
    private BluetoothAdapter.LeScanCallback mLeScanCallBack = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
            mDeviceListAdapter.addDevice(bluetoothDevice);
            mDeviceListAdapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
           Toast.makeText(this, R.string.ble_notavailable, Toast.LENGTH_SHORT).show();
           finish();
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.ble_notavailable, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent blenable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(blenable, REQUEST_ENABLE_BT);
        }

        mDeviceListAdapter = new LighterListAdapter();
        setListAdapter(mDeviceListAdapter);
        scanLighterDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode==REQUEST_ENABLE_BT && resultCode==Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDeviceListAdapter.clear();
        scanLighterDevice(false);
    }

    private void scanLighterDevice(boolean enable) {
        if (enable) {
            UUID[] arr = new UUID[]{LighterBluetoothService.UUID_TIME_MEASUREMENT};
            mBluetoothAdapter.startLeScan(arr, mLeScanCallBack);
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallBack);
        }
    }

    private class LighterListAdapter extends BaseAdapter {

        private final ArrayList<BluetoothDevice> mList;

        LighterListAdapter() {
            mList = new ArrayList<BluetoothDevice>();
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }

        public void addDevice(BluetoothDevice result) {
            mList.add(result);
        }

        public void clear() {
            mList.clear();
        }
    }
}
