/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import dagger.android.AndroidInjection
import javax.inject.Inject

/**
 * Manages widgets
 */
class DashboardWidgetProvider : AppWidgetProvider() {
    @Inject
    lateinit var widgetRepository: WidgetRepository

    @Inject
    lateinit var widgetUpdater: DashboardWidgetUpdater

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        AndroidInjection.inject(this, context)

        for (appWidgetId in appWidgetIds) {
            val widgetConfiguration = widgetRepository.getWidget(appWidgetId)

            widgetUpdater.updateAppWidget(
                appWidgetManager,
                appWidgetId,
                widgetConfiguration.title,
                widgetConfiguration.iconUrl,
                widgetConfiguration.addButton
            )
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        AndroidInjection.inject(this, context)

        if (intent?.action == OPEN_INTENT) {
            val clickIntent = Intent(Intent.ACTION_VIEW, intent.data)
            clickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context?.startActivity(clickIntent)
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray) {
        AndroidInjection.inject(this, context)

        for (appWidgetId in appWidgetIds) {
            widgetRepository.deleteWidget(appWidgetId)
        }
    }

    companion object {
        const val OPEN_INTENT = "open"
    }
}
