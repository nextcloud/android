/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.interfaces

import com.owncloud.android.ui.fragment.OCFileListFragment

interface TransactionInterface {
    fun onOCFileListFragmentComplete(fragment: OCFileListFragment)
}
