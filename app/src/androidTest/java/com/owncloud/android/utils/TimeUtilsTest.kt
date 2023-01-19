package com.owncloud.android.utils;


import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static junit.framework.TestCase.assertEquals;

@RunWith(AndroidJUnit4.class)
public class TimeUtilsTest {

    @Test
    public void shouldGetDurationParts() {
        int days = 5;
        int hours = 10;
        int minutes = 30;

        TimeUtils.DurationParts durationParts = TimeUtils.getDurationParts(
            DAYS.toMillis(days) + HOURS.toMillis(hours) + MINUTES.toMillis(minutes));

        assertEquals(days, durationParts.getDays());
        assertEquals(hours, durationParts.getHours());
        assertEquals(minutes, durationParts.getMinutes());
    }
}
