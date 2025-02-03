/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.license

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.ionos.scanbot.license.oath.settings.SecureEncryptor
import com.ionos.scanbot.license.oath.settings.SecureSharedPreferences
import javax.inject.Inject

class PreferencesKeyStore @Inject constructor(
	context: Context,
	secureEncryptor: SecureEncryptor,
) : KeyStore {
	private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
	private val securedPreferences: SharedPreferences =
        SecureSharedPreferences(
            preferences,
            secureEncryptor
        )

	override fun get(keyId: String): String? {
		return securedPreferences.getString(keyId, null)
	}

	override fun set(keyId: String, keyValue: String?) {
		securedPreferences.edit().putString(keyId, keyValue).apply()
	}
}
