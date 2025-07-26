/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.typeConverter

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.nextcloud.model.OfflineOperationType
import com.google.gson.GsonBuilder
import com.nextcloud.client.database.typeAdapter.OfflineOperationTypeAdapter

@ProvidedTypeConverter
class OfflineOperationTypeConverter {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(OfflineOperationType::class.java, OfflineOperationTypeAdapter())
        .create()

    @TypeConverter
    fun fromOfflineOperationType(type: OfflineOperationType?): String? = gson.toJson(type)

    @TypeConverter
    fun toOfflineOperationType(type: String?): OfflineOperationType? =
        gson.fromJson(type, OfflineOperationType::class.java)
}
