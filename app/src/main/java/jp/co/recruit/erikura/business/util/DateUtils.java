package jp.co.recruit.erikura.business.util;

import java.util.Calendar;
import java.util.Date;

public class DateUtils extends org.apache.commons.lang.time.DateUtils {
    public static Date beginningOfDay(Date date) {
        return truncate(date, Calendar.DAY_OF_MONTH);
    }

    public static Calendar endOfDay(Calendar cal) {
        // 時、分、秒のフィールドを最大値で更新しておきます
        cal.set(Calendar.HOUR_OF_DAY, cal.getMaximum(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, cal.getMaximum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getMaximum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getMaximum(Calendar.MILLISECOND));
        return cal;
    }

    public static Date endOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return endOfDay(cal).getTime();
    }

    public static Date beginningOfMonth(Date date) {
        return truncate(date, Calendar.MONTH);
    }

    public static Date endOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(beginningOfMonth(date));
        // 翌月月初日から -1 日することで、月末日を取得します
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.DATE, -1);
        // 月末日の時、分、秒を最大値にしたものを返却します
        return endOfDay(cal).getTime();
    }

    public static Date at(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day);
        return cal.getTime();
    }

    public static int diffYears(Date d1, Date d2) {
        Calendar d1cal = calendarFrom(d1);
        Calendar d2cal = calendarFrom(d2);
        int year = d1cal.get(Calendar.YEAR) - d2cal.get(Calendar.YEAR);
        int d1month = d1cal.get(Calendar.MONTH);
        int d2month = d2cal.get(Calendar.MONTH);
        if (d1month <= d2month) {
            return year;
        }
        else {
            // d1の月まで来ていないので -1 する
            return year - 1;
        }
    }

    public static Calendar calendarFrom(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        return cal;
    }
}
