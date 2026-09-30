/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.test

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.Description
import org.junit.runner.manipulation.Filter

class ServerVersionFilter : Filter() {

    private val serverVersion: Int? by lazy {
        InstrumentationRegistry
            .getArguments()
            .getString(SERVER_VERSION_ARGUMENT)
            ?.toIntOrNull()
    }

    override fun shouldRun(description: Description): Boolean {
        val currentVersion = serverVersion ?: return true

        return when {
            description.isTest -> description.requiredServerVersion() <= currentVersion
            else -> description.children.any { shouldRun(it) }
        }
    }

    override fun describe(): String = "skip tests annotated with @SinceServer above server version $serverVersion"

    private fun Description.requiredServerVersion(): Int {
        val onMethod = getAnnotation(SinceServer::class.java)?.majorVersion ?: ANY_SERVER
        val onClass = testClass?.getAnnotation(SinceServer::class.java)?.majorVersion ?: ANY_SERVER
        return maxOf(onMethod, onClass)
    }

    companion object {
        private const val SERVER_VERSION_ARGUMENT = "TEST_SERVER_VERSION"
        private const val ANY_SERVER = 0
    }
}
