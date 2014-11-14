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
public class JournalFragment extends Fragment implements MainActivity.CigModelListener {
    private static JournalFragment mFragment;
    private List<Date> mModel;
    private ViewGroup mRootView;
    private JournalAdapter mAdapter;

    public static JournalFragment newInstance(List<Date> mModel) {
        mFragment = new JournalFragment();
        mFragment.mModel = mModel;
        return mFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mRootView==null) {
            mRootView = (ViewGroup) inflater.inflate(R.layout.journal_fragment, container, false);

            ListView elv = (ListView) mRootView.findViewById(R.id.journal_list_view);
            mAdapter = new JournalAdapter(getActivity(), R.layout.journal_list_item, mModel);
            elv.setAdapter(mAdapter);

            elv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Date d = (Date) parent.getItemAtPosition(position);
                    mAdapter.remove(d);
                    ((MainActivity) getActivity()).modelChanged(); // horrible and necessary

                    Log.e(MainActivity.USER_INTERACTION_TAG, "removed cigarette via Journal");
                }
            });
        } else { // on config (i.e. screen rotation, the view is still attached)
            ViewGroup parent = (ViewGroup) mRootView.getParent();
            parent.removeView(mRootView);
        }

        return mRootView;
    }

    @Override
    public void cigModelChanged() {
        mAdapter.notifyDataSetChanged();
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
