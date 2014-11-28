package de.unifreiburg.es.iLitIt;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

/**
 * Created by phil on 11/28/14.
 */
public class CigAnnotationWriter extends  DelayedObserver<CigaretteEvent> {
    private static final String TAG = CigAnnotationWriter.class.getSimpleName();
    private final Context mContext;
    public static final String FILENAME = "cigarettes.csv";

    private final Runnable rWriteFile = new Runnable() {
        @Override
        public void run() {
            try {
                FileOutputStream fos = mContext.openFileOutput(FILENAME, mContext.MODE_PRIVATE);

                for (CigaretteEvent ev : mList) {
                    fos.write(ev.toString().getBytes("utf-8"));
                    fos.write("\n".getBytes("utf-8"));
                }

                fos.close();
            } catch (Exception e) {
                Log.e(TAG, "unable to write ", e);
            }

            Log.i(TAG, "succesfully written cigs to " + FILENAME);
        }
    };

    CigAnnotationWriter(Context c) {
        super(DelayedObserver.DEFAULT_DELAY, null);
        mContext = c;
        mAction = rWriteFile;
    }

    public static ObservableLinkedList<CigaretteEvent> readCigaretteList(Context c) {
        ObservableLinkedList<CigaretteEvent> cigList = new ObservableLinkedList<CigaretteEvent>();

        try {
            FileInputStream fis = c.openFileInput(FILENAME);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;

            while ((line = br.readLine()) != null)
                cigList.add(CigaretteEvent.fromString(line));

        } catch (Exception e) {
            Log.e(TAG, "file load failed", e);
        }

        return cigList;
    }
}
