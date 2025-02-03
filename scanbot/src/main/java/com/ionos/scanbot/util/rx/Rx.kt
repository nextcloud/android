/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.util.rx

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable



operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
    add(disposable)
}