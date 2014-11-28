package de.unifreiburg.es.iLitIt;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.Toast;

import java.util.Date;


public class MainActivity extends FragmentActivity {
    public static final String USER_INTERACTION_TAG = "iLitIt_UI";
    private static final int ENABLE_BT_REQUEST = 1337;
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
    private ObservableLinkedList<CigaretteEvent> mModel = null;
    private final CigAnnotationWriter rCigAnnotationWriter = new CigAnnotationWriter(this);
    private final CigIntentBroadcaster rCigIntentBroadcaster = new CigIntentBroadcaster(this);

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

            // re-register AnnotationWriter and IntentBroadcaster
            if (mModel != null) {
                mModel.unregister(rCigAnnotationWriter);
                mModel.unregister(rCigIntentBroadcaster);
            }
            mModel = mBluetoothService.getModel();
            mModel.register(rCigAnnotationWriter);
            mModel.register(rCigIntentBroadcaster);

            // give fragments access to the data
            for (Fragment f : getSupportFragmentManager().getFragments()) {
                try {
                    MyFragment mf = (MyFragment) f;
                    mf.setModel(mModel);
                    mf.setBluetoothService(mBluetoothService);
                } catch (ClassCastException e) {
                    Log.d(TAG, "unable to cast " + f.toString() + " to MyFragment");
                }
            }
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

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // make sure that bluetooth is enable before trying to start our connection
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBt, ENABLE_BT_REQUEST);
        }

        // Set up the service connection for the lighter and initialize
        // UI once connected to this service.
        Intent intent = new Intent(this, LighterBluetoothService.class);
        startService(intent); // make sure it lives on
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ENABLE_BT_REQUEST && resultCode == RESULT_CANCELED) {
            Toast.makeText(this, R.string.bluetooth_not_enabled, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /** this is the interface for all fragment created down here */
    public interface MyFragment {
        public void setModel(ObservableLinkedList<CigaretteEvent> list);
        public void setBluetoothService(LighterBluetoothService service);
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
            Fragment f = null;

            try {
                f = fragments[position];
                if (mModel != null) {
                    ((MyFragment) f).setModel(mModel);
                    ((MyFragment) f).setBluetoothService(mBluetoothService);
                }
            } catch (IndexOutOfBoundsException e) {
                Log.d(TAG, "index out of bounds " + position, e);
                return null;
            } finally {
                return f;
            }
        }

        @Override
        public int getCount() {
            return fragments.length;
        }
    }
}
