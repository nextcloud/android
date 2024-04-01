/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper_ozturk@proton.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.model

import androidx.lifecycle.LiveData

class WorkerStateLiveData private constructor() : LiveData<WorkerState>() {

    fun setWorkState(state: WorkerState) {
        postValue(state)
    }

    companion object {
        private var instance: WorkerStateLiveData? = null

        fun instance(): WorkerStateLiveData {
            return instance ?: synchronized(this) {
                instance ?: WorkerStateLiveData().also { instance = it }
            }
        }
    }
}
