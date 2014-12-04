package de.unifreiburg.es.iLitIt;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Date;
import java.util.List;

/**
 * Created by phil on 11/18/14.
 */
public class JournalFragment extends Fragment implements MainActivity.MyFragment{
    private ObservableLinkedList<CigaretteEvent> mModel;
    private ViewGroup mRootView;
    private JournalAdapter mAdapter;
    private DelayedObserver rUpdateFields = new DelayedObserver(DelayedObserver.DEFAULT_DELAY, new Runnable() {
        public static final String TAG = "mehe";

        @Override
        public void run() {
            if (getActivity()==null || mModel==null || mRootView==null)
                return; // init incomplete

            if (mAdapter==null) {
                mAdapter = new JournalAdapter(getActivity(), R.layout.journal_list_item, mModel);
                ListView elv = (ListView) mRootView.findViewById(R.id.journal_list_view);
                elv.setAdapter(mAdapter);

                elv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        CigaretteEvent e = (CigaretteEvent) parent.getItemAtPosition(position);
                        mAdapter.remove(e);
                        Log.e(MainActivity.USER_INTERACTION_TAG, "removed cigarette via Journal");
                        return true;
                    }
                });
            }

            mAdapter.notifyDataSetChanged();
        }
    });

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = (ViewGroup) inflater.inflate(R.layout.journal_fragment, container, false);
        mAdapter = null; // possibly recreated
        return mRootView;
    }

    private class JournalAdapter extends ArrayAdapter<CigaretteEvent> {
        public JournalAdapter(Context context, int textViewResourceId, List<CigaretteEvent> objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public CigaretteEvent getItem(int position) {
            return super.getItem(super.getCount() - 1 - position); // reverse the list
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mModel!=null) mModel.unregister(rUpdateFields);
    }

    @Override
    public void setModel(ObservableLinkedList<CigaretteEvent> list) {
        if (list != null) list.unregister(rUpdateFields);
        mModel = list;
        mModel.register(rUpdateFields);
        rUpdateFields.mAction.run();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) rUpdateFields.mAction.run();
    }

    @Override
    public void setBluetoothService(LighterBluetoothService service) {
    }
}
