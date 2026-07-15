/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.ical

import net.fortuna.ical4j.model.TimeZone
import net.fortuna.ical4j.model.TimeZoneRegistry
import net.fortuna.ical4j.model.TimeZoneRegistryImpl
import java.time.ZoneId
import java.time.zone.ZoneRules
import java.util.concurrent.ConcurrentHashMap

/**
 * ical4j 4's default [TimeZoneRegistryImpl.register] publishes every timezone through
 * [net.fortuna.ical4j.model.ZoneRulesProviderImpl], which extends the platform
 * [java.time.zone.ZoneRulesProvider]. Both its constructor and
 * [java.time.zone.ZoneRulesProvider.registerProvider] are blocked hidden APIs on Android and
 * crash with a NoSuchMethodError.
 *
 * This registry keeps timezones defined inside parsed calendars in a local map and delegates
 * standard timezone lookups to a plain [TimeZoneRegistryImpl], whose read path never touches the
 * blocked provider.
 */
class AndroidCompatTimeZoneRegistry : TimeZoneRegistry {

    private val delegate = TimeZoneRegistryImpl()
    private val registered = ConcurrentHashMap<String, TimeZone>()

    override fun register(timezone: TimeZone) {
        registered[timezone.id] = timezone
    }

    override fun register(timezone: TimeZone, update: Boolean) {
        register(timezone)
    }

    override fun clear() {
        registered.clear()
    }

    override fun getTimeZone(id: String): TimeZone? = registered[id] ?: delegate.getTimeZone(id)

    override fun getZoneRules(): MutableMap<String, ZoneRules> = delegate.zoneRules

    override fun getZoneId(tzId: String): ZoneId = delegate.getZoneId(tzId)

    override fun getTzId(zoneId: String): String = delegate.getTzId(zoneId)
}
