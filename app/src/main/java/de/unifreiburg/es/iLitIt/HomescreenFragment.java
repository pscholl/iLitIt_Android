package de.unifreiburg.es.iLitIt;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Date;
import java.util.List;

/**
 * Created by phil on 11/17/14.
 */
public class HomescreenFragment extends Fragment {

    private static final long FIELD_DELAY = 100;
    private final Handler mHandler;
    private ObservableLinkedList<Date> mModel;
    private int mTimeAgo = 0;
    private Runnable rUpdateFields = new Runnable() {
        @Override
        public void run() {
            TextView timeago = (TextView) mRootView.findViewById(R.id.timeago),
                    total = (TextView) mRootView.findViewById(R.id.total_amount),
                    since = (TextView) mRootView.findViewById(R.id.trackingsince),
                    nicotine = (TextView) mRootView.findViewById(R.id.estimation),
                    mean = (TextView) mRootView.findViewById(R.id.meandaily);

            mHandler.postDelayed(rUpdateFields, FIELD_DELAY);

            if (timeago == null || total == null || since == null || nicotine == null || mean == null ||
                    mModel == null) {
                return;
            }

            total.setText(
                    String.format("%d cigarettes%s", mModel.size(), mModel.size() > 1 ? "s" : ""));

            since.setText(StatsHelper.tracking_since(mModel));
            timeago.setText(StatsHelper.last_cigarette_at(mModel));

            nicotine.setText(String.format("%.2f", StatsHelper.current_nicotine(mModel)));
            mean.setText(String.format("%.2f", StatsHelper.mean_cigs_per_day(mModel)));

        }
    };

    private ViewGroup mRootView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        // get the model instance from the main activity, this is ugly, but still seems
        // to be the cleanest way in Android
        mModel = ((MainActivity) getActivity()).getModel();
    }

    public HomescreenFragment() {
        super();
        mHandler = new Handler();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mRootView == null) {
            mRootView = (ViewGroup) inflater.inflate(R.layout.homescreen_fragment, container, false);;

            Button button = (Button) mRootView.findViewById(R.id.justhadonebutton);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mModel.add(new Date());
                    Log.e(MainActivity.USER_INTERACTION_TAG, "added cigarette via HomeScreen");
                }
            });

            button = (Button) mRootView.findViewById(R.id.forget_lastone);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mModel.size() > 0)
                        mModel.remove( mModel.getLast() );
                    Log.e(MainActivity.USER_INTERACTION_TAG, "removed cigarette via HomeScreen");
                }
            });
        } else { // on config (i.e. screen rotation, the view is still attached)
            ViewGroup parent = (ViewGroup) mRootView.getParent();
            parent.removeView(mRootView);
        }

        return mRootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser)
            mHandler.postDelayed(rUpdateFields, FIELD_DELAY);
        else
            mHandler.removeCallbacks(rUpdateFields);
    }

}
