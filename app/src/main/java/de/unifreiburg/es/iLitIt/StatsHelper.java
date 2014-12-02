package de.unifreiburg.es.iLitIt;

import android.content.Intent;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by phil on 11/18/14.
 */
public class StatsHelper {

    public static String time_diff(Date date, Date date1) {
        long diff = date.getTime() - date1.getTime(),
                DAY = 24*60*60*1000,
                HOUR = DAY/24,
                MINUTE = HOUR/60,
                SECOND = MINUTE/60;

        if (diff > DAY)
            return String.format("%d day%s ago", diff/DAY, diff/DAY>1 ? "s":"");
        else if (diff > HOUR)
            return String.format("%d hour%s ago", diff/HOUR, diff/HOUR>1 ? "s":"");
        else if (diff > MINUTE)
            return String.format("%d minute%s ago", diff/MINUTE, diff/MINUTE>1 ? "s":"");
        else if (diff > SECOND)
            return String.format("%d second%s ago", diff/SECOND, diff/SECOND>1 ? "s":"");
        else
            return "just now";
    }

    public static String tracking_since(List<CigaretteEvent> mModel) {
        try {
            return time_diff(new Date(), mModel.get(0).when);
        } catch (Exception e) {
            return "never";
        }
    }


    public static String last_cigarette_at(List<CigaretteEvent> mModel) {
        try {
            return time_diff(new Date(), mModel.get(mModel.size()-1).when);
        } catch (Exception e) {
            return "never";
        }
    }

    public static double current_nicotine(List<CigaretteEvent> mModel) {
        long HALFDAY = new Date().getTime() - 12*60*60*1000;
        LinkedList<Date> last_12_hours = new LinkedList<Date>();

        for (CigaretteEvent e : mModel) {
            if (e.when.getTime() < HALFDAY)
                continue;

            last_12_hours.add(e.when);
        }

        double multidose = 0.;

        for (Date d : last_12_hours) {
            multidose += dose(d);
        }

        return multidose;
    }

    public static double dose(Date t0) {
        /** Nicotine multi-dose estimation: DHillon, Basic pharmacokinetics. Parameters for
         * Nicotine taken from Benowitz, 1991, Stable Isotope Studies of Nicotine Kinetics and
         * Bioavailability, Table 1 */
        double N0=40., k=1./140., ka=1./8.1;

        if (new Date().getTime() < t0.getTime())
            return 0; // not in the past

        double minutes_since_dose = (new Date().getTime() - t0.getTime()) / 1000. / 60.;
        return N0*ka * (Math.pow(2, -k*minutes_since_dose) - Math.pow(2, -ka*minutes_since_dose)) / (ka-k);
    }

    public static double mean_cigs_per_day(List<CigaretteEvent> mModel) {
        long DAY = 12*60*60*1000;
        if (mModel.size()==0)
            return 0.;
        else if (mModel.size()==1)
            return 1.;

        double days = (long) Math.ceil((mModel.get(mModel.size() - 1).when.getTime() - mModel.get(0).when.getTime())/ (double) DAY);
        return mModel.size() / (double) days;
    }
}
