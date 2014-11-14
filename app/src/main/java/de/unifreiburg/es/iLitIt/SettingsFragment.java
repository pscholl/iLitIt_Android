package de.unifreiburg.es.iLitIt;

import android.content.ServiceConnection;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Date;
import java.util.List;

/**
 * Created by phil on 11/18/14.
 */
public class SettingsFragment extends Fragment implements MainActivity.CigModelListener {
    private List<Date> mModel;
    private LighterBluetoothService mServiceconnection;
    private Button mClear;
    private TextView mMacAddr;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public static SettingsFragment newInstance(List<Date> m, LighterBluetoothService s) {
        SettingsFragment mFragment = new SettingsFragment();
        mFragment.mModel = m;
        mFragment.mServiceconnection = s;
        return mFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mRootView = inflater.inflate(R.layout.settings_fragment, container, false);

        mClear = (Button) mRootView.findViewById(R.id.cleardatabutton);
        mClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(MainActivity.USER_INTERACTION_TAG, "cleared all cigarettes from Settings");
                mModel.clear();
                ((MainActivity) getActivity()).modelChanged();
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
        return mRootView;
    }

    @Override
    public void cigModelChanged() {
        mMacAddr.setText(mServiceconnection.get_mac_addr());
    }
}
