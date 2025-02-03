/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.initializer

interface ScanbotInitializer {

	fun initialize()

	fun isInitialized(): Boolean

	fun isLicenseValid(): Boolean
}
