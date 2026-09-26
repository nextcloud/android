/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.test

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Disables system animations for the duration of the test.
 *
 * Espresso requires that no view keeps requesting layout while it waits for the root window. Animated components such
 * as [com.google.android.material.snackbar.Snackbar] keep animating unless the global animation scales are set to zero.
 * The Gradle `animationsDisabled` option only passes `--no-window-animation`, which leaves `animator_duration_scale`
 * untouched, so animations still run on CI. This rule zeroes all scales directly via a shell command.
 */
class DisableAnimationsRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            setAnimationScale("0.0")
            try {
                base.evaluate()
            } finally {
                setAnimationScale("1.0")
            }
        }
    }

    private fun setAnimationScale(scale: String) {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        SCALE_SETTINGS.forEach { setting ->
            val descriptor = uiAutomation.executeShellCommand("settings put global $setting $scale")
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        }
    }

    companion object {
        private val SCALE_SETTINGS = listOf(
            "window_animation_scale",
            "transition_animation_scale",
            "animator_duration_scale"
        )
    }
}
