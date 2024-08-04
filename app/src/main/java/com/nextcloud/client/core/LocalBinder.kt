/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.core

import android.app.Service
import android.os.Binder

/**
 * This is a generic binder that provides access to a locally bound service instance.
 */
abstract class LocalBinder<S : Service>(val service: S) : Binder()
