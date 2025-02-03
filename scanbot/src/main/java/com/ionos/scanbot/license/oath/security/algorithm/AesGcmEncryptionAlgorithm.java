/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.license.oath.security.algorithm;


import com.ionos.scanbot.license.oath.security.key.AesGcmKey;
import com.ionos.scanbot.license.oath.security.key.AesGcmKey;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class AesGcmEncryptionAlgorithm implements EncryptionAlgorithm {

	private final SecretKey secretKey;
	private final AlgorithmParameterSpec paramSpec;

	public AesGcmEncryptionAlgorithm(AesGcmKey aesGsmKey, AlgorithmParameterSpecFactory algorithmParameterSpecFactory) {
		String ALGORITHM = "AES";
		this.secretKey = new SecretKeySpec(aesGsmKey.key, ALGORITHM);
		this.paramSpec = algorithmParameterSpecFactory.create(aesGsmKey.iv);
	}

	@Override
	public byte[] encrypt(String str) throws IllegalBlockSizeException, BadPaddingException,
			NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
		return createCipher(Cipher.ENCRYPT_MODE).doFinal(str.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public String decrypt(byte[] dec) throws IllegalBlockSizeException, BadPaddingException,
			NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
		return new String(createCipher(Cipher.DECRYPT_MODE).doFinal(dec), StandardCharsets.UTF_8);
	}

	private Cipher createCipher(int encryptMode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
		Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
		cipher.init(encryptMode, secretKey, paramSpec);
		return cipher;
	}
}