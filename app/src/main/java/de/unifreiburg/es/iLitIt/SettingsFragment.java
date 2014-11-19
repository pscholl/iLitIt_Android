package de.unifreiburg.es.iLitIt;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Date;

/**
 * Created by phil on 11/18/14.
 */
public class SettingsFragment extends Fragment {
    private ObservableLinkedList<Date> mModel;
    private LighterBluetoothService mServiceconnection;
    private Button mClear;
    private TextView mMacAddr;
    private Runnable rUpdateFields = new Runnable() {
        @Override
        public void run() {
            mMacAddr.setText(mServiceconnection.get_mac_addr());
        }
    };
    private View mRootView = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public static SettingsFragment newInstance(ObservableLinkedList<Date> m, LighterBluetoothService s) {
        SettingsFragment mFragment = new SettingsFragment();
        mFragment.mModel = m;
        mFragment.mServiceconnection = s;
        return mFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mRootView==null) {
            mRootView = inflater.inflate(R.layout.settings_fragment, container, false);

            mModel.register(new DelayedObserver(10, rUpdateFields));

            mClear = (Button) mRootView.findViewById(R.id.cleardatabutton);
            mClear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e(MainActivity.USER_INTERACTION_TAG, "cleared all cigarettes from Settings");
                    mModel.clear();
                }
            });

            mMacAddr = (TextView) mRootView.findViewById(R.id.macaddr);
            mMacAddr.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mServiceconnection.clear_mac_addr();
                }
            });
            mMacAddr.setText(mServiceconnection.get_mac_addr());
        }  else { // on config (i.e. screen rotation, the view is still attached)
            ViewGroup parent = (ViewGroup) mRootView.getParent();
            parent.removeView(mRootView);
        }

        return mRootView;
    }
}
