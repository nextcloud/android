/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.etm

import androidx.fragment.app.Fragment
import kotlin.reflect.KClass

data class EtmMenuEntry(val iconRes: Int, val titleRes: Int, val pageClass: KClass<out Fragment>)
