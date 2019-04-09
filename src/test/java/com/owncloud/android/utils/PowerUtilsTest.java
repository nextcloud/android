/*
 * Nextcloud Android client application
 *
 * @author Edvard Holst
 * Copyright (C) 2019 Edvard Holst
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
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
