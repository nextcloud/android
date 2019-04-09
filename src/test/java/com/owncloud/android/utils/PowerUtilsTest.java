package com.owncloud.android.utils;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class PowerUtilsTest {

    @Mock
    private Context mContext;

    @Mock
    private PowerManager mPowerManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }

    @Test
    public void isPowerSaveMode_assertCorrectlyReportsTrue() throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.O);
        when(mContext.getSystemService(Context.POWER_SERVICE)).thenReturn(mPowerManager);
        when(mPowerManager.isPowerSaveMode()).thenReturn(true);
        assertTrue("Incorrectly reported power saving mode on",
                   PowerUtils.isPowerSaveMode(mContext));
    }

    @Test
    public void isPowerSaveMode_assertCorrectlyReportsFalse() throws Exception {
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.O);
        when(mContext.getSystemService(Context.POWER_SERVICE)).thenReturn(mPowerManager);
        when(mPowerManager.isPowerSaveMode()).thenReturn(false);
        assertFalse("Incorrectly reported power saving mode off",
                    PowerUtils.isPowerSaveMode(mContext));
    }
}
