/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee.model

import com.owncloud.android.R

enum class E2EEKeyCheck {
    NO_NETWORK,
    CHECK_FAILED,
    E2EE_UNAVAILABLE,
    ONLY_ON_SERVER,
    MISSING_EVERYWHERE,
    ONLY_ON_DEVICE,
    SAME_AS_SERVER,
    DIFFERS_FROM_SERVER,
    UNTRUSTED_CERTIFICATE;

    fun getMessageId(action: E2EEAction): Int? = when (this) {
        NO_NETWORK -> when (action) {
            E2EEAction.NEW_FOLDER -> {
                R.string.encryption_new_folder_no_network
            }

            E2EEAction.OPEN -> {
                R.string.encryption_open_no_network
            }

            else -> {
                R.string.encryption_encrypt_no_network
            }
        }

        CHECK_FAILED -> when (action) {
            E2EEAction.NEW_FOLDER -> {
                R.string.encryption_new_folder_check_failed
            }

            E2EEAction.OPEN -> {
                R.string.encryption_open_check_failed
            }

            else -> {
                R.string.encryption_encrypt_check_failed
            }
        }

        E2EE_UNAVAILABLE -> when (action) {
            E2EEAction.NEW_FOLDER -> {
                R.string.encryption_new_folder_unavailable
            }

            E2EEAction.OPEN -> {
                R.string.encryption_open_unavailable
            }

            else -> {
                R.string.encryption_encrypt_unavailable
            }
        }

        MISSING_EVERYWHERE -> {
            if (action == E2EEAction.OPEN) {
                R.string.encryption_key_handle_no_keys_description
            } else {
                null
            }
        }

        else -> null
    }
}
