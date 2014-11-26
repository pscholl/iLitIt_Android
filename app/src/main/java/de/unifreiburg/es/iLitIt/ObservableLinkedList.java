package de.unifreiburg.es.iLitIt;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.LinkedList;
import java.util.Observer;

/**
 * Created by phil on 11/19/14.
 */
public class ObservableLinkedList<E> extends LinkedList<E> {

    private final LinkedList<Observer> observers = new LinkedList<Observer>();

    public void register(ObservableLinkedList.Observer o) {
        observers.add(o);
        String klass = o.getClass().getSimpleName();

        if ((klass.contains("Runnable") || klass.contains("DelayedObserver")) && o.getClass().getDeclaringClass()!=null)
            klass = o.getClass().getDeclaringClass().getName();

        Log.d(ObservableLinkedList.class.getName(),
                "new observer registered: " + klass);
    }
    public void unregister(ObservableLinkedList.Observer o)
    {
        observers.remove(o);
    }



    @Override
    public void clear() {
        super.clear();
        fireEvent();
    }

    @Override
    public boolean add(E object) {
        boolean b = super.add(object);
        fireEvent();
        return b;
    }

    @Override
    public boolean remove(Object object) {
        boolean b = super.remove(object);
        fireEvent();
        return b;
    }

    public void fireEvent() {
        for (Observer o : observers)
            o.listChanged();

        Log.d(ObservableLinkedList.class.getName(),
              "fired event from Thread " + Thread.currentThread().getName());
    }

    public interface Observer {
        public void listChanged();
    }
}
