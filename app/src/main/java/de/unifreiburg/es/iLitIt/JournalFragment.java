package de.unifreiburg.es.iLitIt;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
public class JournalFragment extends Fragment {
    private ObservableLinkedList<Date> mModel;
    private ViewGroup mRootView;
    private JournalAdapter mAdapter;
    private Runnable rUpdateFields = new Runnable() {
        @Override
        public void run() {
            mAdapter.notifyDataSetChanged();
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        // get the model instance from the main activity, this is ugly, but still seems
        // to be the cleanest way in Android
        mModel = ((MainActivity) getActivity()).getModel();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mRootView==null) {
            mRootView = (ViewGroup) inflater.inflate(R.layout.journal_fragment, container, false);

            mModel.register(new DelayedObserver(DelayedObserver.DEFAULT_DELAY, rUpdateFields));

            ListView elv = (ListView) mRootView.findViewById(R.id.journal_list_view);
            mAdapter = new JournalAdapter(getActivity(), R.layout.journal_list_item, mModel);
            elv.setAdapter(mAdapter);

            elv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Date d = (Date) parent.getItemAtPosition(position);
                    mAdapter.remove(d);
                    Log.e(MainActivity.USER_INTERACTION_TAG, "removed cigarette via Journal");
                }
            });
        } else { // on config (i.e. screen rotation, the view is still attached)
            ViewGroup parent = (ViewGroup) mRootView.getParent();
            parent.removeView(mRootView);
        }

        return mRootView;
    }

    private class JournalAdapter extends ArrayAdapter<Date> {
        public JournalAdapter(Context context, int textViewResourceId, List<Date> objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public Date getItem(int position) {
            return super.getItem(super.getCount() - 1 - position); // reverse the list
        }
    }
}
