package de.unifreiburg.es.iLitIt;

import android.location.Location;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by phil on 11/26/14.
 */
public class CigaretteEvent {
    public static final DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    public static final java.lang.String CSV_SEPARATOR = "\t";

    public Date when;
    public Location where;

    public CigaretteEvent(Date date, Location location) {
        when = date;
        where = location;
    }

    public static CigaretteEvent fromString(String line) throws ParseException {
        Location l;
        Date d;

        String[] dateandloc = line.split(CSV_SEPARATOR);
        if (dateandloc.length < 1) { throw new ParseException("line too short", 0); }
        d = dateformat.parse(dateandloc[0]);

        String[] latlon = dateandloc[1].split(" ");
        if (latlon.length < 1) { throw new ParseException("locatioin too short", 0); }

        l = new Location("file");
        l.setLatitude(Double.parseDouble(latlon[0]));
        l.setLongitude(Double.parseDouble(latlon[1]));

        return new CigaretteEvent(d,l);
    }

    @Override
    public String toString() {
        return String.format("%s\t%f %f",
                dateformat.format(when),
                where.getLatitude(),
                where.getLongitude());
    }
}
