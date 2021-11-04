/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.client

import android.util.Log
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * C&p from https://stackoverflow.com/questions/45635833/how-can-i-use-flakytest-annotation-now on 18.03.2020
 */
class RetryTestRule(val retryCount: Int = 1) : TestRule {

    companion object {
        private val TAG = RetryTestRule::class.java.simpleName
    }

    override fun apply(base: Statement, description: Description): Statement {
        return statement(base, description)
    }

    @Suppress("TooGenericExceptionCaught") // and this exactly what we want here
    private fun statement(base: Statement, description: Description): Statement {
        return object : Statement() {

            override fun evaluate() {
                Log.e(TAG, "Evaluating ${description.methodName}")

                var caughtThrowable: Throwable? = null

                for (i in 0 until retryCount) {
                    try {
                        base.evaluate()
                        return
                    } catch (t: Throwable) {
                        caughtThrowable = t
                        Log.e(TAG, description.methodName + ": run " + (i + 1) + " failed")
                    }
                }

                Log.e(TAG, description.methodName + ": giving up after " + retryCount + " failures")
                if (caughtThrowable != null)
                    throw caughtThrowable
            }
        }
    }
}
