/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.license.oath.security.algorithm;

import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.spec.GCMParameterSpec;


public class AlgorithmParameterSpecFactoryImpl implements AlgorithmParameterSpecFactory {

	@Override
	public AlgorithmParameterSpec create(byte[] initializationVector) {
		return createParams(initializationVector, initializationVector.length);
	}

	private AlgorithmParameterSpec createParams(final byte[] buf, int len) {
		int algorithmLength = 128;
		return new GCMParameterSpec(algorithmLength, buf, 0, len);
	}
}
