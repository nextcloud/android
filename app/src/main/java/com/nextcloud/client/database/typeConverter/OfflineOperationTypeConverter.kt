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

    private val gson = Gson()

    @TypeConverter
    fun fromOfflineOperationType(type: OfflineOperationType?): String? {
        return gson.toJson(type)
    }

    @TypeConverter
    fun toOfflineOperationType(typeString: String?): OfflineOperationType? {
        val type = object : TypeToken<OfflineOperationType>() {}.type
        return gson.fromJson(typeString, type)
    }
}
