/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Unpublished <unpublished@users.noreply.github.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.datamodel

import android.accounts.Account
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SignatureVerification(val signatureValid: Boolean, val account: Account?) : Parcelable
