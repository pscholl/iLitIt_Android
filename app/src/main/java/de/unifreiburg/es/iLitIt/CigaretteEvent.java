package de.unifreiburg.es.iLitIt;

import android.location.Location;
import android.util.Log;

import java.text.DateFormat;
import java.text.NumberFormat;
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
        Location l = null;
        Date d;

        String[] dateandloc = line.split(CSV_SEPARATOR);
        if (dateandloc.length < 1) { throw new ParseException("line too short", 0); }
        d = dateformat.parse(dateandloc[0]);

        String[] latlon = dateandloc[1].split(" ");
        if (latlon.length > 1) { // if we can't parse -> set l == null
            l = new Location("file");
            l.setLatitude( NumberFormat.getInstance().parse(latlon[0]).doubleValue() );
            l.setLongitude( NumberFormat.getInstance().parse(latlon[1]).doubleValue() );
        }

        return new CigaretteEvent(d,l);
    }

    @Override
    public String toString() {
        if (where == null)
            return dateformat.format(when);
        else
            return String.format("%s\t%f %f",
                dateformat.format(when),
                where.getLatitude(),
                where.getLongitude());
    }

    public boolean hasValidLocation() {
        return !(where==null ||
                where.getProvider()==null ||
                where.getProvider().equals("test") ||
                where.getProvider().equals("mock"));
    }
}
