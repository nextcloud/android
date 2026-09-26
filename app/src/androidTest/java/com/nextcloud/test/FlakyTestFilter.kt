/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.test

import org.junit.runner.Description
import org.junit.runner.manipulation.Filter

class FlakyTestFilter : Filter() {
    override fun shouldRun(description: Description): Boolean = when {
        description.isTest -> !description.isFlaky()
        else -> description.children.any { shouldRun(it) }
    }

    override fun describe(): String = "skip tests annotated with @Flaky"

    private fun Description.isFlaky(): Boolean {
        val onMethod = getAnnotation(Flaky::class.java) != null
        val onClass = testClass?.isAnnotationPresent(Flaky::class.java) == true
        return onMethod || onClass
    }
}
