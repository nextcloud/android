/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.model

import android.content.Context
import com.owncloud.android.R

data class DurationOption(
    val value: Long,
    val displayText: String
) {
    companion object {
        fun twoWaySyncIntervals(context: Context): List<DurationOption> = listOf(
            DurationOption(15L, context.getString(R.string.two_way_sync_interval_15_min)),
            DurationOption(30L, context.getString(R.string.two_way_sync_interval_30_min)),
            DurationOption(45L, context.getString(R.string.two_way_sync_interval_45_min)),
            DurationOption(60L, context.getString(R.string.two_way_sync_interval_1_hour)),
            DurationOption(120L, context.getString(R.string.two_way_sync_interval_2_hours)),
            DurationOption(240L, context.getString(R.string.two_way_sync_interval_4_hours)),
            DurationOption(360L, context.getString(R.string.two_way_sync_interval_6_hours)),
            DurationOption(480L, context.getString(R.string.two_way_sync_interval_8_hours)),
            DurationOption(720L, context.getString(R.string.two_way_sync_interval_12_hours)),
            DurationOption(1440L, context.getString(R.string.two_way_sync_interval_24_hours))
        )
    }
}
