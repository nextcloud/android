/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Stefan Niedermann <info@niedermann.it>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.client.integrations.deck

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.nextcloud.client.account.User
import com.owncloud.android.lib.resources.notifications.models.Notification
import java.util.Optional

class DeckApiImpl(private val context: Context, private val packageManager: PackageManager) : DeckApi {
    override fun createForwardToDeckActionIntent(notification: Notification, user: User): Optional<PendingIntent> {
        if (APP_NAME.equals(notification.app, ignoreCase = true)) {
            val intent = Intent()
            for (appPackage in DECK_APP_PACKAGES) {
                intent.setClassName(appPackage, DECK_ACTIVITY_TO_START)
                if (packageManager.resolveActivity(intent, 0) != null) {
                    return Optional.of(createPendingIntent(intent, notification, user))
                }
            }
        }
        return Optional.empty()
    }

    private fun createPendingIntent(intent: Intent, notification: Notification, user: User): PendingIntent {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context,
            notification.getNotificationId(),
            putExtrasToIntent(intent, notification, user),
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun putExtrasToIntent(intent: Intent, notification: Notification, user: User): Intent = intent
        .putExtra(EXTRA_ACCOUNT, user.accountName)
        .putExtra(EXTRA_LINK, notification.getLink())
        .putExtra(EXTRA_OBJECT_ID, notification.getObjectId())
        .putExtra(EXTRA_SUBJECT, notification.getSubject())
        .putExtra(EXTRA_SUBJECT_RICH, notification.getSubjectRich())
        .putExtra(EXTRA_MESSAGE, notification.getMessage())
        .putExtra(EXTRA_MESSAGE_RICH, notification.getMessageRich())
        .putExtra(EXTRA_USER, notification.getUser())
        .putExtra(EXTRA_NID, notification.getNotificationId())

    companion object {
        const val APP_NAME = "deck"
        val DECK_APP_PACKAGES = arrayOf(
            "it.niedermann.nextcloud.deck",
            "it.niedermann.nextcloud.deck.play",
            "it.niedermann.nextcloud.deck.dev"
        )
        const val DECK_ACTIVITY_TO_START = "it.niedermann.nextcloud.deck.ui.PushNotificationActivity"
        private const val EXTRA_ACCOUNT = "account"
        private const val EXTRA_LINK = "link"
        private const val EXTRA_OBJECT_ID = "objectId"
        private const val EXTRA_SUBJECT = "subject"
        private const val EXTRA_SUBJECT_RICH = "subjectRich"
        private const val EXTRA_MESSAGE = "message"
        private const val EXTRA_MESSAGE_RICH = "messageRich"
        private const val EXTRA_USER = "user"
        private const val EXTRA_NID = "nid"
    }
}
