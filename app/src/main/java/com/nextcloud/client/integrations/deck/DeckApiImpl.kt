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
package com.nextcloud.client.integrations.deck

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.nextcloud.client.account.User
import com.nextcloud.java.util.Optional
import com.owncloud.android.lib.resources.notifications.models.Notification

class DeckApiImpl(private val context: Context, private val packageManager: PackageManager) : DeckApi {
    override fun createForwardToDeckActionIntent(notification: Notification, user: User): Optional<PendingIntent?> {
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

    private fun putExtrasToIntent(intent: Intent, notification: Notification, user: User): Intent {
        return intent
            .putExtra(EXTRA_ACCOUNT, user.accountName)
            .putExtra(EXTRA_LINK, notification.getLink())
            .putExtra(EXTRA_OBJECT_ID, notification.getObjectId())
            .putExtra(EXTRA_SUBJECT, notification.getSubject())
            .putExtra(EXTRA_SUBJECT_RICH, notification.getSubjectRich())
            .putExtra(EXTRA_MESSAGE, notification.getMessage())
            .putExtra(EXTRA_MESSAGE_RICH, notification.getMessageRich())
            .putExtra(EXTRA_USER, notification.getUser())
            .putExtra(EXTRA_NID, notification.getNotificationId())
    }

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
