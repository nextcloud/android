/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
            context?.let {
                val clickIntent = Intent(Intent.ACTION_VIEW, intent.data).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(clickIntent)
            }
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
