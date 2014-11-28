package de.unifreiburg.es.iLitIt;

import android.util.Log;

import java.util.LinkedList;

/**
 * Created by phil on 11/19/14.
 */
public class ObservableLinkedList<E> extends LinkedList<E> {

    private final LinkedList<Observer> observers = new LinkedList<Observer>();

    public void register(ObservableLinkedList.Observer<E> o) {
        observers.add(o);
        String klass = o.getClass().getSimpleName();

        if ((klass.contains("Runnable") || klass.contains("DelayedObserver")) && o.getClass().getDeclaringClass()!=null)
            klass = o.getClass().getDeclaringClass().getName();

        Log.d(ObservableLinkedList.class.getName(),
                "new observer registered: " + klass);
    }
    public void unregister(ObservableLinkedList.Observer<E> o)
    {
        observers.remove(o);
    }

    @Override
    public void clear() {
        super.clear();
        fireEvent(null);
    }

    @Override
    public boolean add(E object) {
        boolean b = super.add(object);
        fireEvent(object);
        return b;
    }

    @Override
    public boolean remove(Object object) {
        boolean b = super.remove(object);
        fireEvent((E) object);
        return b;
    }

    public void fireEvent(E object) {
        Log.d(ObservableLinkedList.class.getName(),
                "fired event from Thread " + Thread.currentThread().getName());

        for (Observer o : observers)
            o.listChanged(this, object);
    }

    public interface Observer<E> {
        public void listChanged(ObservableLinkedList<E> list, E object);
    }
}
