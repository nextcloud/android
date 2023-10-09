package com.nextcloud.client.preferences

import android.content.Context
import android.content.SharedPreferences
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.preferences.AppPreferencesImpl.ListenerRegistry
import com.nextcloud.client.preferences.TestAppPreferences.ListenerRegistery
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock

@RunWith(Suite::class)
@SuiteClasses(TestAppPreferences.Preferences::class, ListenerRegistery::class)
class TestAppPreferences {
    class ListenerRegistery {
        @Mock
        private val listener1: AppPreferences.Listener? = null

        @Mock
        private val listener2: AppPreferences.Listener? = null

        @Mock
        private val listener3: AppPreferences.Listener? = null

        @Mock
        private val listener4: AppPreferences.Listener? = null

        @Mock
        var appPreferences: AppPreferences? = null
        private var registry: ListenerRegistry? = null
        @Before
        fun setUp() {
            MockitoAnnotations.initMocks(this)
            Mockito.`when`(appPreferences!!.darkThemeMode).thenReturn(DarkMode.DARK)
            registry = ListenerRegistry(appPreferences)
        }

        @Test
        fun canRemoveListenersFromCallback() {

            // GIVEN
            //      registery has few listeners
            //      one listener will try to remove itself and other listener
            registry!!.add(listener1)
            registry!!.add(listener2)
            registry!!.add(listener3)
            registry!!.add(listener4)
            Mockito.doAnswer { i: InvocationOnMock? ->
                registry!!.remove(listener2)
                registry!!.remove(listener3)
                null
            }.`when`(listener2)!!.onDarkThemeModeChanged(DarkMode.DARK)

            // WHEN
            //      callback is called twice
            registry!!.onSharedPreferenceChanged(NOT_USED_NULL, AppPreferencesImpl.PREF__DARK_THEME)
            registry!!.onSharedPreferenceChanged(NOT_USED_NULL, AppPreferencesImpl.PREF__DARK_THEME)

            // THEN
            //      no ConcurrentModificationException
            //      1st time, all listeners (including removed) are called
            //      2nd time removed callbacks are not called
            Mockito.verify(listener1, Mockito.times(2))!!.onDarkThemeModeChanged(DarkMode.DARK)
            Mockito.verify(listener2)!!.onDarkThemeModeChanged(DarkMode.DARK)
            Mockito.verify(listener3)!!.onDarkThemeModeChanged(DarkMode.DARK)
            Mockito.verify(listener4, Mockito.times(2))!!.onDarkThemeModeChanged(DarkMode.DARK)
        }

        @Test
        fun nullsAreNotAddedToRegistry() {
            // GIVEN
            //      registry has no listeners
            //      attempt to add null listener was made
            registry!!.add(null)

            // WHEN
            //      callback is called
            registry!!.onSharedPreferenceChanged(NOT_USED_NULL, AppPreferencesImpl.PREF__DARK_THEME)

            // THEN
            //      nothing happens
            //      null was not added to registry
        }

        @Test
        fun nullsAreNotRemovedFromRegistry() {
            // GIVEN
            //      registry has no listeners

            // WHEN
            //      attempt to remove null listener was made
            registry!!.remove(null)

            // THEN
            //      null is ignored
        }

        companion object {
            private val NOT_USED_NULL: SharedPreferences? = null
        }
    }

    class Preferences {
        @Mock
        private val testContext: Context? = null

        @Mock
        private val sharedPreferences: SharedPreferences? = null

        @Mock
        private val editor: SharedPreferences.Editor? = null

        @Mock
        private val userAccountManager: UserAccountManager? = null
        private var appPreferences: AppPreferencesImpl? = null
        @Before
        fun setUp() {
            MockitoAnnotations.initMocks(this)
            Mockito.`when`(editor!!.remove(ArgumentMatchers.anyString())).thenReturn(editor)
            Mockito.`when`(sharedPreferences!!.edit()).thenReturn(editor)
            appPreferences = AppPreferencesImpl(testContext, sharedPreferences, userAccountManager)
        }

        @Test
        fun removeLegacyPreferences() {
            appPreferences!!.removeLegacyPreferences()
            val inOrder = Mockito.inOrder(editor)
            inOrder.verify(editor)!!.remove("instant_uploading")
            inOrder.verify(editor)!!.remove("instant_video_uploading")
            inOrder.verify(editor)!!.remove("instant_upload_path")
            inOrder.verify(editor)!!.remove("instant_upload_path_use_subfolders")
            inOrder.verify(editor)!!.remove("instant_upload_on_wifi")
            inOrder.verify(editor)!!.remove("instant_upload_on_charging")
            inOrder.verify(editor)!!.remove("instant_video_upload_path")
            inOrder.verify(editor)!!.remove("instant_video_upload_path_use_subfolders")
            inOrder.verify(editor)!!.remove("instant_video_upload_on_wifi")
            inOrder.verify(editor)!!.remove("instant_video_uploading")
            inOrder.verify(editor)!!.remove("instant_video_upload_on_charging")
            inOrder.verify(editor)!!.remove("prefs_instant_behaviour")
            inOrder.verify(editor)!!.apply()
        }

        @Test
        fun testBruteForceDelay() {
            Assert.assertEquals(0, appPreferences!!.computeBruteForceDelay(0).toLong())
            Assert.assertEquals(0, appPreferences!!.computeBruteForceDelay(2).toLong())
            Assert.assertEquals(1, appPreferences!!.computeBruteForceDelay(3).toLong())
            Assert.assertEquals(1, appPreferences!!.computeBruteForceDelay(5).toLong())
            Assert.assertEquals(2, appPreferences!!.computeBruteForceDelay(6).toLong())
            Assert.assertEquals(3, appPreferences!!.computeBruteForceDelay(11).toLong())
            Assert.assertEquals(8, appPreferences!!.computeBruteForceDelay(25).toLong())
            Assert.assertEquals(10, appPreferences!!.computeBruteForceDelay(50).toLong())
            Assert.assertEquals(10, appPreferences!!.computeBruteForceDelay(100).toLong())
        }
    }
}