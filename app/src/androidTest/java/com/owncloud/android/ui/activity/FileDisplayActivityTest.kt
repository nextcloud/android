package com.owncloud.android.ui.activity

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.nextcloud.client.onboarding.WhatsNewActivity
import com.owncloud.android.AbstractIT
import org.junit.Test

class FileDisplayActivityTest : AbstractIT() {
    @Test
    fun testSetupToolbar() {
        ActivityScenario.launch(FileDisplayActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val activity =
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).iterator().next()
                (activity as? WhatsNewActivity)?.onBackPressed()
            }
            scenario.recreate()
        }
    }
}