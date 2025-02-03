/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.license.oath.security.key;

import java.security.SecureRandom;


public class AesGcmKeyGenerator {

	public final AesGcmKey generate() {
		int keyLength = 16;
		int ivLength = 12;
		return new AesGcmKey(generateRandomSecure(keyLength), generateRandomSecure(ivLength));
	}

	private byte[] generateRandomSecure(int length) {
		SecureRandom secureRandom = new SecureRandom();
		byte[] key = new byte[length];
		secureRandom.nextBytes(key);
		return key;
	}
}
