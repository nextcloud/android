/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.ical

import net.fortuna.ical4j.model.TimeZoneRegistry
import net.fortuna.ical4j.model.TimeZoneRegistryFactory

class AndroidCompatTimeZoneRegistryFactory : TimeZoneRegistryFactory() {
    override fun createRegistry(): TimeZoneRegistry = AndroidCompatTimeZoneRegistry()
}
