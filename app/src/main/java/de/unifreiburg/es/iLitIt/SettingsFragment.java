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
public class SettingsFragment extends Fragment implements MainActivity.MyFragment {
    private ObservableLinkedList<CigaretteEvent> mModel;
    private LighterBluetoothService mServiceconnection;
    private Button mClear;
    private TextView mMacAddr;
    private View mRootView = null;
    private TextView mBatVolt = null;

    private DelayedObserver rUpdateFields = new DelayedObserver(10, new Runnable() {
        @Override
        public void run() {
            if (mRootView==null || mServiceconnection==null)
                return; // not fully initialised

            mBatVolt.setText(String.format("%.2fV%s",
                    mServiceconnection.get_bat_voltage(),
                    mServiceconnection.is_bat_empty() ? "(empty)" : ""));
            mBatVolt.postInvalidate();
            mMacAddr.setText(mServiceconnection.get_mac_addr());
        }
    });

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.settings_fragment, container, false);

        mClear = (Button) mRootView.findViewById(R.id.cleardatabutton);
        mClear.setOnClickListener(new View.OnClickListener() {
                @Override
            public void onClick(View v) {
                Log.e(MainActivity.USER_INTERACTION_TAG, "cleared all cigarettes from Settings");
                if (mModel!=null) mModel.clear();
            }
        });

        mMacAddr = (TextView) mRootView.findViewById(R.id.macaddr);
        mMacAddr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mServiceconnection!=null) mServiceconnection.clear_mac_addr();
                }
        });
        mBatVolt = (TextView) mRootView.findViewById(R.id.batteryVoltage);

        if (mServiceconnection!=null) rUpdateFields.mAction.run();
        return mRootView;
    }

    @Override
    public void setModel(ObservableLinkedList<CigaretteEvent> list) {
        if (mModel!=null) mModel.unregister(rUpdateFields);
        mModel = list;
        mModel.register(rUpdateFields);
    }

    @Override
    public void setBluetoothService(LighterBluetoothService service) {
        mServiceconnection = service;
        rUpdateFields.mAction.run();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        //rUpdateFields.mAction.run();
    }
}
