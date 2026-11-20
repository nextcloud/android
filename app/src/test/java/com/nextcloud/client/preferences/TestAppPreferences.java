/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.datamodel.ArbitraryDataProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    TestAppPreferences.Preferences.class
})
public class TestAppPreferences {

    public static class Preferences {
        @Mock
        private Context testContext;

        @Mock
        private SharedPreferences sharedPreferences;

        @Mock
        private SharedPreferences.Editor editor;

        @Mock
        private UserAccountManager userAccountManager;

        @Mock
        private ArbitraryDataProvider arbitraryDataProvider;

        private AppPreferencesImpl appPreferences;

        @Before
        public void setUp() {
            MockitoAnnotations.initMocks(this);
            when(editor.remove(anyString())).thenReturn(editor);
            when(sharedPreferences.edit()).thenReturn(editor);
            appPreferences = new AppPreferencesImpl(testContext, sharedPreferences, userAccountManager, arbitraryDataProvider);
        }

        @Test
        public void removeLegacyPreferences() {
            appPreferences.removeLegacyPreferences();
            InOrder inOrder = inOrder(editor);
            inOrder.verify(editor).remove("instant_uploading");
            inOrder.verify(editor).remove("instant_video_uploading");
            inOrder.verify(editor).remove("instant_upload_path");
            inOrder.verify(editor).remove("instant_upload_path_use_subfolders");
            inOrder.verify(editor).remove("instant_upload_on_wifi");
            inOrder.verify(editor).remove("instant_upload_on_charging");
            inOrder.verify(editor).remove("instant_video_upload_path");
            inOrder.verify(editor).remove("instant_video_upload_path_use_subfolders");
            inOrder.verify(editor).remove("instant_video_upload_on_wifi");
            inOrder.verify(editor).remove("instant_video_uploading");
            inOrder.verify(editor).remove("instant_video_upload_on_charging");
            inOrder.verify(editor).remove("prefs_instant_behaviour");
            inOrder.verify(editor).apply();
        }

        @Test
        public void testBruteForceDelay() {
            assertEquals(0, appPreferences.computeBruteForceDelay(0));
            assertEquals(0, appPreferences.computeBruteForceDelay(2));
            assertEquals(1, appPreferences.computeBruteForceDelay(3));
            assertEquals(1, appPreferences.computeBruteForceDelay(5));
            assertEquals(2, appPreferences.computeBruteForceDelay(6));
            assertEquals(3, appPreferences.computeBruteForceDelay(11));
            assertEquals(8, appPreferences.computeBruteForceDelay(25));
            assertEquals(10, appPreferences.computeBruteForceDelay(50));
            assertEquals(10, appPreferences.computeBruteForceDelay(100));
        }
    }
}
