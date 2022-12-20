package com.owncloud.android.utils;

import java.util.concurrent.TimeUnit;

public class TimeUtils {

    private TimeUtils() {
        // utility class -> private constructor
    }

    public static DurationParts getDurationParts(long duration) {
        int days = (int) TimeUnit.MILLISECONDS.toDays(duration);
        int hours = (int) TimeUnit.MILLISECONDS.toHours(duration) - (days * 24);
        int minutes = (int) (TimeUnit.MILLISECONDS.toMinutes(duration) - (TimeUnit.MILLISECONDS.toHours(duration)* 60));
        return new DurationParts(days, hours, minutes);
    }

    public static class DurationParts {
        int days;
        int hours;
        int minutes;

        public DurationParts(int days, int hours, int minutes) {
            this.days = days;
            this.hours = hours;
            this.minutes = minutes;
        }

        public int getDays() {
            return days;
        }

        public int getHours() {
            return hours;
        }

        public int getMinutes() {
            return minutes;
        }
    }

}
