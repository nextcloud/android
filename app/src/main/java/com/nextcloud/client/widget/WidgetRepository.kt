/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.widget

import android.content.SharedPreferences
import com.nextcloud.android.lib.resources.dashboard.DashBoardButtonType
import com.nextcloud.android.lib.resources.dashboard.DashboardButton
import com.nextcloud.android.lib.resources.dashboard.DashboardWidget
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import java.util.Optional
import javax.inject.Inject

class WidgetRepository @Inject constructor(
    private val userAccountManager: UserAccountManager,
    val preferences: SharedPreferences
) {
    fun saveWidget(widgetId: Int, widget: DashboardWidget, user: User) {
        val editor: SharedPreferences.Editor = preferences
            .edit()
            .putString(PREF__WIDGET_ID + widgetId, widget.id)
            .putString(PREF__WIDGET_TITLE + widgetId, widget.title)
            .putString(PREF__WIDGET_ICON + widgetId, widget.iconUrl)
            .putBoolean(PREF__WIDGET_ROUND_ICON + widgetId, widget.roundIcons)
            .putString(PREF__WIDGET_USER + widgetId, user.accountName)
        val buttonList = widget.buttons
        if (buttonList != null && buttonList.isNotEmpty()) {
            for (button in buttonList) {
                if (button.type == DashBoardButtonType.NEW) {
                    editor
                        .putString(PREF__WIDGET_ADD_BUTTON_TYPE + widgetId, button.type.toString())
                        .putString(PREF__WIDGET_ADD_BUTTON_URL + widgetId, button.link)
                        .putString(PREF__WIDGET_ADD_BUTTON_TEXT + widgetId, button.text)
                }
                if (button.type == DashBoardButtonType.MORE) {
                    editor
                        .putString(PREF__WIDGET_MORE_BUTTON_TYPE + widgetId, button.type.toString())
                        .putString(PREF__WIDGET_MORE_BUTTON_URL + widgetId, button.link)
                        .putString(PREF__WIDGET_MORE_BUTTON_TEXT + widgetId, button.text)
                }
            }
        }
        editor.apply()
    }

    fun deleteWidget(widgetId: Int) {
        preferences
            .edit()
            .remove(PREF__WIDGET_ID + widgetId)
            .remove(PREF__WIDGET_TITLE + widgetId)
            .remove(PREF__WIDGET_ICON + widgetId)
            .remove(PREF__WIDGET_ROUND_ICON + widgetId)
            .remove(PREF__WIDGET_USER + widgetId)
            .remove(PREF__WIDGET_ADD_BUTTON_TEXT + widgetId)
            .remove(PREF__WIDGET_ADD_BUTTON_URL + widgetId)
            .remove(PREF__WIDGET_ADD_BUTTON_TYPE + widgetId)
            .remove(PREF__WIDGET_MORE_BUTTON_TEXT + widgetId)
            .remove(PREF__WIDGET_MORE_BUTTON_URL + widgetId)
            .remove(PREF__WIDGET_MORE_BUTTON_TYPE + widgetId)
            .apply()
    }

    fun getWidget(widgetId: Int): WidgetConfiguration {
        val userOptional: User? =
            userAccountManager.getUser(preferences.getString(PREF__WIDGET_USER + widgetId, ""))

        val addButton = createAddButton(widgetId)
        val moreButton = createMoreButton(widgetId)

        return WidgetConfiguration(
            preferences.getString(PREF__WIDGET_ID + widgetId, "") ?: "",
            preferences.getString(PREF__WIDGET_TITLE + widgetId, "") ?: "",
            preferences.getString(PREF__WIDGET_ICON + widgetId, "") ?: "",
            preferences.getBoolean(PREF__WIDGET_ROUND_ICON + widgetId, false),
            userOptional,
            addButton,
            moreButton
        )
    }

    private fun createAddButton(widgetId: Int): DashboardButton? {
        var addButton: DashboardButton? = null
        if (preferences.contains(PREF__WIDGET_ADD_BUTTON_TYPE + widgetId)) {
            addButton = DashboardButton(
                DashBoardButtonType.valueOf(
                    preferences.getString(
                        PREF__WIDGET_ADD_BUTTON_TYPE + widgetId,
                        ""
                    ) ?: ""
                ),
                preferences.getString(PREF__WIDGET_ADD_BUTTON_TEXT + widgetId, "") ?: "",
                preferences.getString(PREF__WIDGET_ADD_BUTTON_URL + widgetId, "") ?: ""
            )
        }

        return addButton
    }

    private fun createMoreButton(widgetId: Int): DashboardButton? {
        var moreButton: DashboardButton? = null
        if (preferences.contains(PREF__WIDGET_MORE_BUTTON_TYPE + widgetId)) {
            moreButton = DashboardButton(
                DashBoardButtonType.valueOf(
                    preferences.getString(
                        PREF__WIDGET_MORE_BUTTON_TYPE + widgetId,
                        ""
                    ) ?: ""
                ),
                preferences.getString(PREF__WIDGET_MORE_BUTTON_TEXT + widgetId, "") ?: "",
                preferences.getString(PREF__WIDGET_MORE_BUTTON_URL + widgetId, "") ?: ""
            )
        }

        return moreButton
    }

    companion object {
        const val PREF__WIDGET_TITLE = "widget_title_"
        private const val PREF__WIDGET_ID = "widget_id_"
        private const val PREF__WIDGET_ICON = "widget_icon_"
        private const val PREF__WIDGET_ROUND_ICON = "widget_round_icon_"
        private const val PREF__WIDGET_USER = "widget_user_"
        private const val PREF__WIDGET_ADD_BUTTON_TEXT = "widget_add_button_text_"
        private const val PREF__WIDGET_ADD_BUTTON_URL = "widget_add_button_url_"
        private const val PREF__WIDGET_ADD_BUTTON_TYPE = "widget_add_button_type_"
        private const val PREF__WIDGET_MORE_BUTTON_TEXT = "widget_more_button_text_"
        private const val PREF__WIDGET_MORE_BUTTON_URL = "widget_more_button_url_"
        private const val PREF__WIDGET_MORE_BUTTON_TYPE = "widget_more_button_type_"
    }
}
