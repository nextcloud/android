/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.test

import com.owncloud.android.BuildConfig
import com.owncloud.android.lib.common.utils.Log_OC
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * C&p from https://stackoverflow.com/questions/45635833/how-can-i-use-flakytest-annotation-now on 18.03.2020
 */
class RetryTestRule(val retryCount: Int = defaultRetryValue) : TestRule {

    companion object {
        private val TAG = RetryTestRule::class.java.simpleName

        @Suppress("MagicNumber")
        private val defaultRetryValue: Int = if (BuildConfig.CI) 5 else 1
    }

    override fun apply(base: Statement, description: Description): Statement = statement(base, description)

    @Suppress("TooGenericExceptionCaught") // and this exactly what we want here
    private fun statement(base: Statement, description: Description): Statement {
        return object : Statement() {

            override fun evaluate() {
                Log_OC.d(TAG, "Evaluating ${description.methodName}")

                var caughtThrowable: Throwable? = null

                for (i in 0 until retryCount) {
                    try {
                        base.evaluate()
                        return
                    } catch (t: Throwable) {
                        caughtThrowable = t
                        Log_OC.e(TAG, description.methodName + ": run " + (i + 1) + " failed")
                    }
                }

                Log_OC.e(TAG, description.methodName + ": giving up after " + retryCount + " failures")
                if (caughtThrowable != null) {
                    throw caughtThrowable
                }
            }
        }
    }
}
