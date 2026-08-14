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
    fun <T : Any> RemoteOperationResult<*>.dataOfType(type: Class<T>): List<T> {
        if (!isSuccess) return emptyList()
        val data = data ?: return emptyList()
        return data.filterIsInstance(type)
    }

    inline fun <reified T : Any> RemoteOperationResult<*>.dataOfType(): List<T> = dataOfType(T::class.java)

    @JvmStatic
    fun <T : Any> RemoteOperationResult<*>.firstDataOfTypeOrNull(type: Class<T>): T? = dataOfType(type).firstOrNull()
}
