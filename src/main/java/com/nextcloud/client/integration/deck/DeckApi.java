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
    Optional<PendingIntent> createForwardToDeckActionIntent(@NonNull final Notification notification, @NonNull final User user);
}
