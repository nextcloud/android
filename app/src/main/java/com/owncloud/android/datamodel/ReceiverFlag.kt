/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

enum class ReceiverFlag {
    NotExported;

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun getId(): Int {
        return Context.RECEIVER_NOT_EXPORTED
    }
}
