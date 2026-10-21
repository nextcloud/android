/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog.conflict.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ConflictFileData(val title: String, val timestamp: String, val fileSize: String) : Parcelable
