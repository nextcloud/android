package com.owncloud.android.ui.dialog

import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES

class DurationPickerDialogFragmentIT : AbstractIT() {

    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @Test
    fun showSyncDelayDurationDialog() {
        val initialDuration = DAYS.toMillis(2) + HOURS.toMillis(8) + MINUTES.toMillis(15)
        val activity = testActivityRule.launchActivity(null)

        val fm = activity.supportFragmentManager
        val ft = fm.beginTransaction()
        ft.addToBackStack(null)

        val dialog = DurationPickerDialogFragment.newInstance(
            initialDuration,
            "Dialog title",
            "Hint message"
        )
        dialog.show(ft, "DURATION_DIALOG")

        waitForIdleSync()
        screenshot(dialog.requireDialog().window!!.decorView)
    }
}
