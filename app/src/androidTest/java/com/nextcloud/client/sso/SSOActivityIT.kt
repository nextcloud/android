package com.nextcloud.client.sso

import androidx.test.espresso.intent.rule.IntentsTestRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.ui.activity.SsoGrantPermissionActivity
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

class SSOActivityIT : AbstractIT() {

    @Suppress("DEPRECATION")
    @get:Rule
    var activityRule = IntentsTestRule(SsoGrantPermissionActivity::class.java, true, false)

    @Test
    fun testActivityStartsWithoutCrash() {
        val sut = activityRule.launchActivity(null)
        assert(sut.binding != null)

        try {
            sut.showDialog()
            assert(true)
        } catch (e: Exception) {
            fail("Wrong theme style applied")
        }
    }
}
