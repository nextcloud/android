/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.license.oath.settings;

import android.util.Base64;

import com.ionos.scanbot.license.oath.security.algorithm.EncryptionAlgorithm;
import com.ionos.scanbot.util.logger.LoggerUtil;

public class SecureEncryptor {
	private final EncryptionAlgorithm encryptionAlgorithm;

	public SecureEncryptor(EncryptionAlgorithm encryptionAlgorithm) {
		this.encryptionAlgorithm = encryptionAlgorithm;
	}

	public String decrypt(String value) throws Exception {
		return this.encryptionAlgorithm.decrypt(Base64.decode(value, Base64.DEFAULT));
	}

	public String encrypt(String value) {
		String encryptedValue = "";
		try {
			encryptedValue = Base64.encodeToString(this.encryptionAlgorithm.encrypt(value), Base64.DEFAULT);
		} catch (Exception e) {
			LoggerUtil.logE(getClass().getSimpleName(), e);
		}
		return encryptedValue;
	}
}
