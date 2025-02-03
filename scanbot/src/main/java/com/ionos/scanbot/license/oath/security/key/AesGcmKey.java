/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.license.oath.security.key;


public class AesGcmKey {
	public final byte[] key;
	public final byte[] iv;

	public AesGcmKey(byte[] key, byte[] iv) {
		this.key = key;
		this.iv = iv;
	}
}
