/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.lib.common.network.OnDatatransferProgressListener

@Suppress("MagicNumber")
fun OnDatatransferProgressListener.getPercent(totalTransferredSoFar: Long, totalToTransfer: Long): Int =
    ((100.0 * totalTransferredSoFar.toDouble() / totalToTransfer.toDouble()).toInt()).coerceAtMost(100)
