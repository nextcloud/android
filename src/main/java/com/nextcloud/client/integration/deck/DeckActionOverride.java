package com.nextcloud.client.integration.deck;

import android.app.PendingIntent;
import android.content.Intent;

import com.nextcloud.client.account.User;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.lib.resources.notifications.models.Notification;

import androidx.annotation.NonNull;

public interface DeckActionOverride {

    @NonNull
    Optional<PendingIntent> handleNotification(@NonNull final Notification notification, @NonNull final User user);
}
