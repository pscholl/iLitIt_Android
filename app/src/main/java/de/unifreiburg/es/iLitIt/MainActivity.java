package de.unifreiburg.es.iLitIt;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;


public class MainActivity extends FragmentActivity {
    public static final String USER_INTERACTION_TAG = "iLitIt_UI";
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    final String TAG = MainActivity.class.toString();
    LighterBluetoothService mBluetoothService;
    String mDeviceAddress;

    private ObservableLinkedList<CigaretteEvent> mModel = null; //new ObservableLinkedList<CigaretteEvent>();
    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mServiceConnection);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            // get the bluetooth service and its attached model, all fragments
            // informed about model changes by attaching an observer to this model
            mBluetoothService = ((LighterBluetoothService.LocalBinder) service).getService();
            mModel = mBluetoothService.getModel();

            // Create the adapter that will return a fragment for each of the three
            // primary sections of the activity.
            mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

            // Set up the ViewPager with the sections adapter.
            mViewPager = (ViewPager) findViewById(R.id.pager);
            mViewPager.setAdapter(mSectionsPagerAdapter);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            mBluetoothService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // make sure that bluetooth is enable before trying to start our connection
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBt, 0);
        }

        // Set up the service connection for the lighter and initialize
        // UI once connected to this service.
        Intent intent = new Intent(this, LighterBluetoothService.class);
        startService(intent); // make sure it lives on
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    public ObservableLinkedList<CigaretteEvent> getModel() {
        return mModel;
    }
    public LighterBluetoothService getServiceConnection() {
        return mBluetoothService;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private Fragment[] fragments = new Fragment[] {
                new HomescreenFragment(),
                new HeatMapFragment(),
                new JournalFragment(),
                new SettingsFragment(),
        };

        public SectionsPagerAdapter(FragmentManager fm) { super(fm); }

        @Override
        public Fragment getItem(int position) {
            try {
                return fragments[position];
            } catch (IndexOutOfBoundsException e) {
                Log.d(TAG, "index out of bounds " + position, e);
                return null;
            }
        }

        @Override
        public int getCount() {
            return fragments.length;
        }
    }
}
