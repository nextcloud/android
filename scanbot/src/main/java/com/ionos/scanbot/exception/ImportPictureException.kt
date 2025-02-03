/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.exception

import android.net.Uri

internal class ImportPictureException(val pictureUri: Uri, cause: Throwable) : Exception(cause)
