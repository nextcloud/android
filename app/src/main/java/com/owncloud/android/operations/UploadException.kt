/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.operations

class UploadException : Exception {

    constructor(message: String?) : super(message)

    companion object {
        private const val serialVersionUID = 5931153844211429915L
    }
}
