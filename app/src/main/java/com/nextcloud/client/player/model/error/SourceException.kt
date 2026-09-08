/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.model.error

class SourceException(errorCode: Int = 0) :
    Exception(
        "Source not found. Error code: $errorCode"
    )
