package com.owncloud.android.ui.dialog

import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class DurationPickerDialogFragmentIT : AbstractIT() {

    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @Test
    fun showSyncDelayDurationDialog() {
        val activity = testActivityRule.launchActivity(null)

        val fm = activity.supportFragmentManager
        val ft = fm.beginTransaction()
        ft.addToBackStack(null)

        val dialog = DurationPickerDialogFragment.newInstance(
            TimeUnit.HOURS.toMillis(5),
            "Dialog title",
            "Hint message"
        )
        dialog.show(ft, "DURATION_DIALOG")

        waitForIdleSync()
        screenshot(dialog.requireDialog().window!!.decorView)
    }
}
