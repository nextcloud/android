/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.etm

import androidx.fragment.app.Fragment

abstract class EtmBaseFragment : Fragment() {
    protected val vm: EtmViewModel get() {
        return (activity as EtmActivity).vm
    }
}
