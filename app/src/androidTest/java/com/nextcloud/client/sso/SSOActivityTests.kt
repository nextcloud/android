package com.nextcloud.client.sso

import androidx.test.espresso.intent.rule.IntentsTestRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.ui.activity.SsoGrantPermissionActivity
import org.junit.Rule
import org.junit.Test

class SSOActivityTests : AbstractIT() {

    @Suppress("DEPRECATION")
    @get:Rule
    var activityRule = IntentsTestRule(SsoGrantPermissionActivity::class.java, true, false)

    @Test
    fun testActivityTheme() {
        val sut = activityRule.launchActivity(null)
        assert(sut.binding != null)
        assert(sut.materialAlertDialogBuilder != null)
    }
}
