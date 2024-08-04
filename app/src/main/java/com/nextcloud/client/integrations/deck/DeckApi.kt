/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Stefan Niedermann <info@niedermann.it>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.client.integrations.deck

import android.app.PendingIntent
import com.nextcloud.client.account.User
import com.owncloud.android.lib.resources.notifications.models.Notification
import java.util.Optional

/**
 * This API is for an integration with the [Nextcloud
 * Deck](https://github.com/stefan-niedermann/nextcloud-deck) app for android.
 */
interface DeckApi {
    /**
     * Creates a PendingIntent that can be used in a NotificationBuilder to open the notification link in Deck app
     *
     * @param notification Notification Notification that could be forwarded to Deck
     * @param user         The user that is affected by the notification
     * @return If notification can be consumed by Deck, a PendingIntent opening notification link in Deck app; empty
     * value otherwise
     * @see [Deck Server App](https://apps.nextcloud.com/apps/deck)
     */
    fun createForwardToDeckActionIntent(notification: Notification, user: User): Optional<PendingIntent>
}
