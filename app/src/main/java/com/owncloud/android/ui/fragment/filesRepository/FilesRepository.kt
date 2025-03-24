/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.filesRepository

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.nextcloud.android.lib.resources.recommendations.GetRecommendationsRemoteOperation
import com.nextcloud.android.lib.resources.recommendations.Recommendation
import com.nextcloud.repository.ClientRepositoryType
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilesRepository(private val clientRepository: ClientRepositoryType, private val lifecycleOwner: LifecycleOwner) :
    FilesRepositoryType {
    private val tag = "FilesRepository"

    override fun fetchRecommendedFiles(onCompleted: (ArrayList<Recommendation>) -> Unit) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = clientRepository.getNextcloudClient() ?: return@launch
                val result = GetRecommendationsRemoteOperation().execute(client)
                if (result.isSuccess) {
                    val recommendations = result.getResultData().recommendations
                    Log_OC.d(tag, "Recommended files fetched size: " + recommendations.size)

                    withContext(Dispatchers.Main) {
                        onCompleted(recommendations)
                    }
                } else {
                    Log_OC.d(tag, "Recommended files cannot be fetched: " + result.code)
                }
            } catch (e: Exception) {
                Log_OC.d(tag, "Exception caught while fetching recommended files: $e")
            }
        }
    }
}
