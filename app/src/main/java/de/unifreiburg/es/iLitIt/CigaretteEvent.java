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
    public static final char CSV_SEPARATOR = '\t';

    public Date when;
    public Location where;
    public String via;

    public CigaretteEvent(Date date, String v, Location location) {
        when = date;
        via = v;
        where = location;
    }

    public static CigaretteEvent fromString(String line) throws ParseException {
        Location l = null;
        Date d;
        String v;

        String[] dateandviaandloc = line.split(Character.toString(CSV_SEPARATOR));
        if (dateandviaandloc.length < 2) { throw new ParseException("line too short", 0); }
        d = dateformat.parse(dateandviaandloc[0]);
        v = dateandviaandloc[1];

        String[] latlon = dateandviaandloc[2].split(" ");
        if (latlon.length > 1) { // if we can't parse -> set l == null
            l = new Location("file");
            l.setLatitude( NumberFormat.getInstance().parse(latlon[0]).doubleValue() );
            l.setLongitude( NumberFormat.getInstance().parse(latlon[1]).doubleValue() );
        }

        return new CigaretteEvent(d,v,l);
    }

    @Override
    public String toString() {
        if (where == null)
            return String.format("%s%c%s",dateformat.format(when), CSV_SEPARATOR, via);
        else
            return String.format("%s%c%s%c%f %f",
                dateformat.format(when),
                CSV_SEPARATOR,
                via.replace(CSV_SEPARATOR, ' '),
                CSV_SEPARATOR,
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
