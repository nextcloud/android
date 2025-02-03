/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.privacy

import android.content.Context
import javax.inject.Inject

class PrivacyPreferences @Inject constructor(
    private val context: Context,
) {
    private val sharedPreferences by lazy { context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE) }

    fun isDataProtectionProcessed(accountName: String?): Boolean =
        getAccountsWithProcessedDataProtection().contains(accountName)

    fun setDataProtectionProcessed(accountName: String?) {
        accountName?.let {
            sharedPreferences
                .edit()
                .putStringSet(DATA_PROTECTION_PROCESSED_KEY, getAccountsWithProcessedDataProtection() + accountName)
                .apply()
        }
    }

    fun removeDataProtectionProcessed(accountName: String) {
        mutableSetOf(*getAccountsWithProcessedDataProtection().toTypedArray())
            .apply { remove(accountName) }
            .let { accountsWithProcessedDataProtection ->
                sharedPreferences
                    .edit()
                    .putStringSet(DATA_PROTECTION_PROCESSED_KEY, accountsWithProcessedDataProtection)
                    .apply()
            }
    }

    private fun getAccountsWithProcessedDataProtection(): Set<String> =
        sharedPreferences
            .getStringSet(DATA_PROTECTION_PROCESSED_KEY, emptySet())
            ?: emptySet()

    fun isAnalyticsEnabled(): Boolean {
        return sharedPreferences.getBoolean(ANALYTICS_ENABLED_KEY, false)
    }

    fun setAnalyticsEnabled(value: Boolean) {
        sharedPreferences.edit().putBoolean(ANALYTICS_ENABLED_KEY, value).apply()
    }

    companion object {
        private const val FILE_NAME = "privacy_preferences"
        private const val DATA_PROTECTION_PROCESSED_KEY = "data_protection_processed"
        private const val ANALYTICS_ENABLED_KEY = "analytics_enabled"
    }
}
