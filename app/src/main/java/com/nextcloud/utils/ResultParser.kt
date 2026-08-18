/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.owncloud.android.lib.common.operations.RemoteOperationResult

object ResultParser {
    @Suppress("DEPRECATION", "ReturnCount")
    @JvmStatic
    fun <T : Any> RemoteOperationResult<*>.list(type: Class<T>): List<T> {
        if (!isSuccess) return emptyList()
        val data = data ?: return emptyList()
        return data.filterIsInstance(type)
    }

    inline fun <reified T : Any> RemoteOperationResult<*>.list(): List<T> = list(T::class.java)

    @JvmStatic
    fun <T : Any> RemoteOperationResult<*>.data(type: Class<T>): T? = list(type).firstOrNull()
}
