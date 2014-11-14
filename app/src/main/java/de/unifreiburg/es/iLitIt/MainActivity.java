package de.unifreiburg.es.iLitIt;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.lang.Override;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


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

    private List<Date> mModel = new LinkedList<Date>();
    private final BroadcastReceiver mBleUpdates = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateModel();
        }
    };

    private void updateModel() {
        try {
            String filename = LighterBluetoothService.FILENAME;
            FileInputStream fis = openFileInput (filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            mModel.clear();
            for (String line=br.readLine(); line!=null; line=br.readLine()) {
                DateFormat df = DateFormat.getDateInstance();
                mModel.add(LighterBluetoothService.dateformat.parse(line));
            }
            Log.e(USER_INTERACTION_TAG, "added cigarettes via Lighter");
            modelChanged();
        } catch(Exception e) {
            Log.e(TAG, "file read failed", e);
        }
    }

    private IntentFilter updateBleFilter() {
        IntentFilter blaa = new IntentFilter();
        blaa.addAction(LighterBluetoothService.IACTION);
        return blaa;
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBleUpdates);
        unbindService(mServiceConnection);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothService = ((LighterBluetoothService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
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

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // Set up the service connection for the lighter.
        Intent intent = new Intent(this, LighterBluetoothService.class);
        startService(intent); // make sure it lives on
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

        // and wire the intent updates
        registerReceiver(mBleUpdates, updateBleFilter());
        updateModel();
    }

    public void modelChanged() {
        // let all active fragments know that something happened, this is crazy
        if (getSupportFragmentManager()==null || getSupportFragmentManager().getFragments()==null)
            return;

        for(Fragment f : getSupportFragmentManager().getFragments()) {
            ((CigModelListener) f).cigModelChanged();
        }
    }

    public interface CigModelListener {
        public void cigModelChanged();
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch(position) {
                case 0:
                    return HomescreenFragment.newInstance(mModel);
                case 1:
                    return JournalFragment.newInstance(mModel);
                case 2:
                    return SettingsFragment.newInstance(mModel, mBluetoothService);
                default:
                    return PlaceholderFragment.newInstance(position + 1);
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return "Section 1".toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements CigModelListener{
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void cigModelChanged() {

        }
    }

}
