/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.mixins

import android.content.Intent
import android.os.Bundle

/**
 * Interface allowing to implement part of [android.app.Activity] logic as
 * a mix-in.
 */
interface ActivityMixin {
    fun onNewIntent(intent: Intent) {
        /* no-op */
    }

    fun onSaveInstanceState(outState: Bundle) {
        /* no-op */
    }

    fun onCreate(savedInstanceState: Bundle?) {
        /* no-op */
    }

    fun onRestart() {
        /* no-op */
    }

    fun onStart() {
        /* no-op */
    }

    fun onResume() {
        /* no-op */
    }

    fun onPause() {
        /* no-op */
    }

    fun onStop() {
        /* no-op */
    }

    fun onDestroy() {
        /* no-op */
    }
}
