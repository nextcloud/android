package com.nextcloud.client.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.nextcloud.client.account.CurrentAccountProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.mockito.InOrder;
import org.mockito.Mock;
import static org.mockito.Mockito.*;

import org.mockito.MockitoAnnotations;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    TestAppPreferences.Preferences.class,
    TestAppPreferences.ListenerRegistery.class
})
public class TestAppPreferences {

    public static class ListenerRegistery {
        private static final SharedPreferences NOT_USED_NULL = null;

        @Mock
        private AppPreferences.Listener listener1;

        @Mock
        private AppPreferences.Listener listener2;

        @Mock
        private AppPreferences.Listener listener3;

        @Mock
        private AppPreferences.Listener listener4;

        @Mock
        AppPreferences appPreferences;

        private AppPreferencesImpl.ListenerRegistry registry;

        @Before
        public void setUp() {
            MockitoAnnotations.initMocks(this);
            when(appPreferences.isDarkThemeEnabled()).thenReturn(true);
            registry = new AppPreferencesImpl.ListenerRegistry(appPreferences);
        }

        @Test
        public void canRemoveListenersFromCallback() {

            // GIVEN
            //      registery has few listeners
            //      one listener will try to remove itself and other listener
            registry.add(listener1);
            registry.add(listener2);
            registry.add(listener3);
            registry.add(listener4);

            doAnswer((i) -> {
                registry.remove(listener2);
                registry.remove(listener3);
                return null;
            }).when(listener2).onDarkThemeEnabledChanged(anyBoolean());

            // WHEN
            //      callback is called twice
            registry.onSharedPreferenceChanged(NOT_USED_NULL, AppPreferencesImpl.PREF__DARK_THEME_ENABLED);
            registry.onSharedPreferenceChanged(NOT_USED_NULL, AppPreferencesImpl.PREF__DARK_THEME_ENABLED);

            // THEN
            //      no ConcurrentModificationException
            //      1st time, all listeners (including removed) are called
            //      2nd time removed callbacks are not called
            verify(listener1, times(2)).onDarkThemeEnabledChanged(anyBoolean());
            verify(listener2).onDarkThemeEnabledChanged(anyBoolean());
            verify(listener3).onDarkThemeEnabledChanged(anyBoolean());
            verify(listener4, times(2)).onDarkThemeEnabledChanged(anyBoolean());
        }

        @Test
        public void nullsAreNotAddedToRegistry() {
            // GIVEN
            //      registry has no listeners
            //      attempt to add null listener was made
            registry.add(null);

            // WHEN
            //      callback is called
            registry.onSharedPreferenceChanged(NOT_USED_NULL, AppPreferencesImpl.PREF__DARK_THEME_ENABLED);

            // THEN
            //      nothing happens
            //      null was not added to registry
        }

        @Test
        public void nullsAreNotRemovedFromRegistry() {
            // GIVEN
            //      registry has no listeners

            // WHEN
            //      attempt to remove null listener was made
            registry.remove(null);

            // THEN
            //      null is ignored
        }
    }

    public static class Preferences {
        @Mock
        private Context testContext;

        @Mock
        private SharedPreferences sharedPreferences;

        @Mock
        private SharedPreferences.Editor editor;

        @Mock
        private CurrentAccountProvider accountProvider;

        private AppPreferencesImpl appPreferences;

        @Before
        public void setUp() {
            MockitoAnnotations.initMocks(this);
            when(editor.remove(anyString())).thenReturn(editor);
            when(sharedPreferences.edit()).thenReturn(editor);
            appPreferences = new AppPreferencesImpl(testContext, sharedPreferences, accountProvider);
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
    }
}
