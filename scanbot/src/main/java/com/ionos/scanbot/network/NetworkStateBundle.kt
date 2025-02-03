/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.network

/**
 * Created with love by Yehor Levchenko.
 * Date: 26.01.2022.
 */
data class NetworkStateBundle(
    val online: Boolean,
    val wifiAvailable: Boolean,
    val mobileAvailable: Boolean,
    val wasOfflineBefore: Boolean
)