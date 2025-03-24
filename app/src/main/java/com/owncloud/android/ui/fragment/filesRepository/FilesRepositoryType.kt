/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.filesRepository

import com.nextcloud.android.lib.resources.recommendations.Recommendation

interface FilesRepositoryType {
    fun fetchRecommendedFiles(onCompleted: (ArrayList<Recommendation>) -> Unit)
}
