package de.unifreiburg.es.iLitIt;

import android.os.Handler;
import android.os.Looper;

/**
* Created by phil on 11/19/14.
*/
public class DelayedObserver<E> implements ObservableLinkedList.Observer<E> {
    protected static Handler mHandler;
    protected Runnable mAction;
    private long mDelay;

    public final static long DEFAULT_DELAY = 500;
    public E mObject;
    public ObservableLinkedList<E> mList;

    public DelayedObserver(long delay, Runnable r) {
        if (mHandler==null)
            mHandler = new Handler(Looper.getMainLooper());
        mAction = r;
        mDelay = delay;
    }

    @Override
    public void listChanged(ObservableLinkedList<E> list, E object) {
        mList = list;
        mObject = object;

        mHandler.removeCallbacks(mAction, null);
        mHandler.postDelayed(mAction, mDelay);
    }
}
