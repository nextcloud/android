/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.license.oath.security.algorithm;

import java.security.spec.AlgorithmParameterSpec;


public interface AlgorithmParameterSpecFactory {
	AlgorithmParameterSpec create(byte[] initializationVector);
}
