package de.unifreiburg.es.iLitIt;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Created by phil on 11/28/14.
 */
public class CigIntentBroadcaster implements ObservableLinkedList.Observer<CigaretteEvent> {
    private static final String TIMESTAMP = "timestamp";
    private static final String LATITUDE = "latitude";
    private static final String LONGITUDE = "longitude";
    private static final String VIA = "via";
    private static final String ACTION_ADD = "de.unifreiburg.es.iLitIt.ADD_CIG";
    private static final String ACTION_REM = "de.unifreiburg.es.iLitIt.REM_CIG";
    private static final String ACTION_CLR = "de.unifreiburg.es.iLitIt.CLR";
    private final String TAG = this.getClass().getSimpleName();
    public Context mContext;
    private int mNumCigarettes = 0;

    CigIntentBroadcaster(Context c) {
        mContext = c;
        mNumCigarettes = 0;
    }

    @Override
    public void listChanged(ObservableLinkedList<CigaretteEvent> list, CigaretteEvent object) {
        final Intent i = new Intent();


        if (object==null && list.size()==0) {
            i.setAction(ACTION_CLR);
            mNumCigarettes = 0;
        }
        else if (object==null)
        {
            // special case for no change to the list, but views need to be updated
            return;
        }
        else if (mNumCigarettes > list.size())
        {
            i.setAction(ACTION_REM);
            i.putExtra(TIMESTAMP, CigaretteEvent.dateformat.format(object.when));
            mNumCigarettes = list.size();
        }
        else if (object.hasValidLocation()) // this can only be an addition
        {
            i.setAction(ACTION_ADD);
            i.putExtra(TIMESTAMP, CigaretteEvent.dateformat.format(object.when));
            i.putExtra(LATITUDE, object.where.getLatitude());
            i.putExtra(LONGITUDE, object.where.getLongitude());
            i.putExtra(VIA, object.via);
            mNumCigarettes=list.size();
        }
        else
        { // ignore changes to a cigs inside the model
            return;
        }


        mContext.sendBroadcast(i);
        Log.d(TAG, "send intent " + i.toString());
    }
}
