/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee.model

import androidx.annotation.StringRes

data class E2EEKeyCheckResult(val state: E2EEKeyCheck, @param:StringRes val errorMessageId: Int? = null) {
    @StringRes
    fun getMessageId(action: E2EEAction): Int? = errorMessageId ?: state.getMessageId(action)
}
