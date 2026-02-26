/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Philipp Hasper <vcs@hasper.info>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.test

import android.content.Context
import android.view.View
import androidx.test.espresso.FailureHandler
import androidx.test.espresso.base.DefaultFailureHandler
import org.hamcrest.Matcher

/**
 * When testing inside of a loop, test failures are hard to attribute. For that, wrap them in an outer
 * exception detailing more about the context.
 *
 * Set the failure handler via
 * ```
 * Espresso.setFailureHandler(
 *     LoopFailureHandler(targetContext, "Test failed in iteration $yourTestIterationCounter")
 * )
 * ```
 * and set it back to the default afterwards via
 * ```
 * Espresso.setFailureHandler(DefaultFailureHandler(targetContext))
 * ```
 */
class LoopFailureHandler(targetContext: Context, private val loopMessage: String) : FailureHandler {
    private val delegate: FailureHandler = DefaultFailureHandler(targetContext)

    override fun handle(error: Throwable?, viewMatcher: Matcher<View?>?) {
        // Wrap in additional Exception
        delegate.handle(Exception(loopMessage, error), viewMatcher)
    }
}
