/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.license.oath.security.key;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.ionos.scanbot.R;
import com.ionos.scanbot.util.logger.LoggerUtil;

import androidx.preference.PreferenceManager;


public class AesGcmKeyRepository {
	private final SharedPreferences preference;
	private final Context context;

	public AesGcmKeyRepository(Context context) {
		this.context = context;
		this.preference = PreferenceManager.getDefaultSharedPreferences(this.context);
	}

	public AesGcmKey getKey() {
		if (hasKey()) {
			try {
				return new AesGcmKey(
						Base64.decode(getAesGsmKey(), Base64.DEFAULT),
						Base64.decode(getAesGsmInitializationVector(), Base64.DEFAULT));
			} catch (Exception e) {
				LoggerUtil.logE(getClass().getSimpleName(), e);
			}
		}

		AesGcmKey newKey = new AesGcmKeyGenerator().generate();
		saveKey(newKey);
		return newKey;
	}

	private void saveKey(AesGcmKey aesGsmKey) {
		setAesGsmKey(Base64.encodeToString(aesGsmKey.key, Base64.DEFAULT));
		setAesGsmInitializationVector(Base64.encodeToString(aesGsmKey.iv, Base64.DEFAULT));
	}

	private boolean hasKey() {
		return hasAesGsmKey() && hasAesGsmInitializationVector();
	}

	private String getAesGsmKey() {
		return preference.getString(getString(R.string.scanbot_preference_aes_gsm_key), "");
	}

	private void setAesGsmKey(String value) {
		preference.edit().putString(getString(R.string.scanbot_preference_aes_gsm_key), value).apply();
	}

	private boolean hasAesGsmKey() {
		return preference.contains(getString(R.string.scanbot_preference_aes_gsm_key));
	}

	private String getAesGsmInitializationVector() {
		return preference.getString(getString(R.string.scanbot_preference_aes_gsm_initialization_vector), "");
	}

	private void setAesGsmInitializationVector(String value) {
		preference.edit().putString(getString(R.string.scanbot_preference_aes_gsm_initialization_vector), value).apply();
	}

	private boolean hasAesGsmInitializationVector() {
		return preference.contains(getString(R.string.scanbot_preference_aes_gsm_initialization_vector));
	}

	private String getString(int resId) {
		return this.context.getString(resId);
	}
}
