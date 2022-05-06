package morgan.support;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

public class Time {

    public static final long SEC  = 1000;
    public static final long MIN  = SEC * 60;
    public static final long HOUR = MIN * 60;
    public static final long DAY  = HOUR * 24;
    public static final long MON  = DAY * 30;
    public static final long YEAR = MON * 12;

    public static ThreadLocal<SimpleDateFormat> yyyyMMDD = new ThreadLocal<SimpleDateFormat>(){
        @Override
        protected SimpleDateFormat initialValue(){
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };

    public static ThreadLocal<SimpleDateFormat> MMDDyyyy = new ThreadLocal<SimpleDateFormat>(){
        @Override
        protected SimpleDateFormat initialValue(){
            return new SimpleDateFormat("MM-dd-yyyy");
        }
    };

    public static String parse(SimpleDateFormat format, long time) {
        var date = new Date();
        date.setTime(time);
        return format.format(date);
    }

    public static long parse(SimpleDateFormat format, String time) {
        long t = 0;
        try {
            t = format.parse(time).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return t;
    }

    public static boolean isSameDay(long t1, long t2) {
        var d1 = new Date();
        var d2 = new Date();
        d1.setTime(t1);
        d2.setTime(t2);
        var s1 = yyyyMMDD.get().format(d1);
        var s2 = yyyyMMDD.get().format(d2);
        return s1.equals(s2);
    }

    public static int daysBetween(long t1, long t2) {
        long diff = Math.abs(t1 - t2);
        return (int) (diff / DAY);
    }

    // return the first millisecond of the month of this date
    // date should be yyyy-mm-dd
    public static long firstMilliOfMonth(String date) {
        date = date.substring(0, date.length() - 2) + "01";
        return parse(yyyyMMDD.get(), date);
    }

    public static long getNextBirthday(String date) {
        String[] today = parse(yyyyMMDD.get(), System.currentTimeMillis()).split("-");
        String[] birthday = date.split("-");
        String nextBirthday;
        if (Integer.parseInt(today[1]) > Integer.parseInt(birthday[1])
                || ((Integer.parseInt(today[1]) == Integer.parseInt(birthday[1]))
                        && Integer.parseInt(today[2]) > Integer.parseInt(birthday[2]))) {
            // not this year
            nextBirthday = (Integer.parseInt(today[0]) + 1) + "-" + birthday[1] + "-" + birthday[2];
        } else {
            nextBirthday = today[0] + "-" + birthday[1] + "-" + birthday[2];
        }
        return parse(yyyyMMDD.get(), nextBirthday);
    }

    public static String getSixMonthLater(String date) {
        String[] today = parse(yyyyMMDD.get(), System.currentTimeMillis()).split("-");
        String[] birthday = date.split("-");
        String sixLater;
        if (Integer.parseInt(birthday[1]) + 6 > 12) {
            // not this year
            sixLater = (Integer.parseInt(today[0]) + 1) + "-" + ((Integer.parseInt(today[1]) + 6) % 12 + 1) + "-" + birthday[2];
        } else {
            sixLater = today[0] + "-" + (Integer.parseInt(today[1]) + 6) + "-" + birthday[2];
        }
        return sixLater;
    }

    public static long getTimeZoneOffset() {
        TimeZone tz = TimeZone.getDefault();
        return tz.toZoneId().getRules().getOffset(Instant.now()).getTotalSeconds() * Time.SEC;
    }
}
