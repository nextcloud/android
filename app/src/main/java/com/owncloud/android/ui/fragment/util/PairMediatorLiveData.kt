/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 *
 * Inspired by https://medium.com/codex/how-to-use-mediatorlivedata-with-multiple-livedata-types-a40e1a59e8cf
 */
package com.owncloud.android.ui.fragment.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

class PairMediatorLiveData<F, S>(firstLiveData: LiveData<F>, secondLiveData: LiveData<S>) :
    MediatorLiveData<Pair<F?, S?>>() {
    init {
        addSource(firstLiveData) { firstLiveDataValue: F -> value = firstLiveDataValue to secondLiveData.value }
        addSource(secondLiveData) { secondLiveDataValue: S -> value = firstLiveData.value to secondLiveDataValue }
    }
}
