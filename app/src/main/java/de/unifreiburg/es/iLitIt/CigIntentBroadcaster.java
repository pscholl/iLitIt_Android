package de.unifreiburg.es.iLitIt;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;

/**
 * Created by phil on 11/28/14.
 */
public class CigIntentBroadcaster extends DelayedObserver<CigaretteEvent> {
    private static final String TIMESTAMP = "timestamp";
    private static final String LATITUDE = "latitude";
    private static final String LONGITUDE = "longitude";
    private static final String ACTION_ADD = "de.unifreiburg.es.iLitIt.ADD_CIG";
    private static final String ACTION_REM = "de.unifreiburg.es.iLitIt.REM_CIG";
    private static final String ACTION_CLR = "de.unifreiburg.es.iLitIt.CLR";
    private final String TAG = this.getClass().getSimpleName();
    private final Context mContext;
    private int mNumCigarettes = 0;

    public final Runnable rBroadcastIntent = new Runnable() {
        @Override
        public void run() {
            Intent i;

            Log.d(TAG, "run " + mList);

            if (mObject == null) {
                i = new Intent(ACTION_CLR);
            } else if (mNumCigarettes < mList.size() &&
                      (mObject.where != null && (!mObject.where.getProvider().equals("test") ||
                                                 !mObject.where.getProvider().equals("mock")))) {
                i = new Intent(ACTION_ADD);
                i.putExtra(TIMESTAMP, mObject.when.toString());
                i.putExtra(LATITUDE, mObject.where.getLatitude());
                i.putExtra(LONGITUDE, mObject.where.getLongitude());
                mNumCigarettes = mList.size();
            } else if (mNumCigarettes > mList.size()) {
                i = new Intent(ACTION_REM);
                i.putExtra(TIMESTAMP, mObject.when.toString());
                mNumCigarettes = mList.size();
            } else { // ignore changes to a cigs inside the model
                return;
            }

            i.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            mContext.sendBroadcast(i);

            Log.d(TAG, "send intent " + i.toString());
        }
    };

    CigIntentBroadcaster(Context c) {
        super(DelayedObserver.DEFAULT_DELAY, null);
        mAction = rBroadcastIntent;
        mContext = c;
    }

}
