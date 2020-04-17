/*
 * Nextcloud application
 *
 * @author Stefan Niedermann
 * Copyright (C) 2020 Stefan Niedermann <info@niedermann.it>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.integration.deck;

import android.app.PendingIntent;

import com.nextcloud.client.account.User;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.lib.resources.notifications.models.Notification;

import androidx.annotation.NonNull;

/**
 * This API is for an integration with the <a href="https://github.com/stefan-niedermann/nextcloud-deck">Nextcloud
 * Deck</a> app for android.
 */
public interface DeckApi {

    /**
     * Creates a PendingIntent that can be used in a NotificationBuilder to start the Deck app
     *
     * @param notification Notification object that should be processed
     * @param user         The user that is affected by the notification
     * @return Optional with a PendingIntent or an empty Optional if the notification is not from the
     * <a href="https://apps.nextcloud.com/apps/deck">Deck server app</a>.
     */
    @NonNull
    Optional<PendingIntent> createForwardToDeckActionIntent(@NonNull final Notification notification,
                                                            @NonNull final User user);
}
