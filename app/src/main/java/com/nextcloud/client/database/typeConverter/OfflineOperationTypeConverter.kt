/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.typeConverter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nextcloud.model.OfflineOperationType

class OfflineOperationTypeConverter {

    @TypeConverter
    fun fromOfflineOperationType(type: OfflineOperationType?): String? {
        return Gson().toJson(type)
    }

    @TypeConverter
    fun toOfflineOperationType(type: String?): OfflineOperationType? {
        return Gson().fromJson(type, object : TypeToken<OfflineOperationType>() {}.type)
    }
}
