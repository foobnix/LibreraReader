package com.foobnix.ext;

import org.xmlpull.v1.XmlPullParser;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The date a book was published, read out of whatever a file writes it as. */
public class PubDate {

    /** Calibre's year for a date that is not known. */
    public static final int UNDEFINED_YEAR = 101;

    private static final Pattern DATE = Pattern.compile("(\\d{4})(?:-?(\\d{2})(?:-?(\\d{2}))?)?");

    /**
     * A date out of "2004-03-15T00:00:00+00:00" from Calibre, "D:20040315" from a PDF, "2004"
     * from a FictionBook, as "yyyy", "yyyy-MM" or "yyyy-MM-dd". Null where there is none.
     * <p>
     * Calibre's own "no date" is the year 101, written 0101-01-01 — or 0100-12-31 once a time
     * zone has had it — and it was being read as a book published in the year 100.
     */
    public static String dateOf(String s) {
        if (s == null) {
            return null;
        }
        Matcher m = DATE.matcher(s);
        if (!m.find()) {
            return null;
        }
        int year = Integer.parseInt(m.group(1));
        if (year <= UNDEFINED_YEAR) {
            return null;
        }
        int month = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
        if (month < 1 || month > 12) {
            return String.format(Locale.US, "%04d", year);
        }
        int day = m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
        if (day < 1 || day > 31) {
            return String.format(Locale.US, "%04d-%02d", year, month);
        }
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
    }

    private static final Pattern YEAR = Pattern.compile("(19|20)[0-9]{2}");

    /**
     * The year a date is of — "03/16/2010 11:19:32 PM" from a MOBI as much as an ISO date —
     * or -1 where there is none, Calibre's "no date" among them.
     */
    public static int yearOf(String s) {
        if (s == null) {
            return -1;
        }
        String input = s.trim();
        if (input.length() > 4) {
            Matcher m = YEAR.matcher(input);
            if (m.find()) {
                input = m.group();
            } else {
                String date = dateOf(input);
                input = date == null ? "" : date.substring(0, 4);
            }
        }
        try {
            int year = Integer.parseInt(input);
            return year > UNDEFINED_YEAR ? year : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * The date a book was published, out of the dates its package gives: the earliest real
     * one, as Calibre takes it. A date the file says is when it was modified is not when the
     * book came out, and Calibre's "no date" is not a date at all.
     *
     * @param dates pairs of the opf:event attribute (or null) and the date's text
     */
    public static String published(List<String[]> dates) {
        String result = null;
        for (String[] pair : dates) {
            if ("modification".equalsIgnoreCase(pair[0])) {
                continue;
            }
            String date = dateOf(pair[1]);
            if (date != null && (result == null || date.compareTo(result) < 0)) {
                result = date;
            }
        }
        return result;
    }

    /** The event a dc:date says it is the date of — opf:event, however it is prefixed. */
    public static String event(XmlPullParser xpp) {
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            String name = xpp.getAttributeName(i);
            if (name != null && name.substring(name.indexOf(':') + 1).equals("event")) {
                return xpp.getAttributeValue(i);
            }
        }
        return null;
    }
}
