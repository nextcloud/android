/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 ZetaTom
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.owncloud.android.MainApp
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection.HTTP_OK
import java.net.URLEncoder

class NominatimClient constructor(geocoderBaseUrl: String, email: String) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val reverseUrl = "${geocoderBaseUrl}reverse?format=jsonv2&email=${URLEncoder.encode(email, ENCODING_UTF_8)}"

    private fun doRequest(requestUrl: String): String? {
        val request = Request.Builder().url(requestUrl).header(HEADER_USER_AGENT, MainApp.getUserAgent()).build()

        try {
            val response = client.newCall(request).execute()
            if (response.code == HTTP_OK) {
                return response.body.string()
            }
        } catch (_: Exception) {
        }

        return null
    }

    /**
     * Reverse geocode specified location - get human readable name suitable for displaying from given coordinates.
     *
     * @param latitude GPS latitude
     * @param longitude GPS longitude
     * @param zoom level of detail to request
     */
    fun reverseGeocode(
        latitude: Double,
        longitude: Double,
        zoom: ZoomLevel = ZoomLevel.TOWN_BOROUGH
    ): ReverseGeocodingResult? {
        val response = doRequest("$reverseUrl&addressdetails=0&zoom=${zoom.int}&lat=$latitude&lon=$longitude")
        return response?.let { gson.fromJson(it, ReverseGeocodingResult::class.java) }
    }

    companion object {
        private const val ENCODING_UTF_8 = "UTF-8"
        private const val HEADER_USER_AGENT = "User-Agent"

        @Suppress("MagicNumber")
        enum class ZoomLevel(val int: Int) {
            COUNTRY(3),
            STATE(5),
            COUNTY(8),
            CITY(10),
            TOWN_BOROUGH(12),
            VILLAGE_SUBURB(13),
            NEIGHBOURHOOD(14),
            LOCALITY(15),
            MAJOR_STREETS(16),
            MINOR_STREETS(17),
            BUILDING(18),
            MAX(19)
        }

        data class ReverseGeocodingResult(
            @SerializedName("lat")
            val latitude: Double,
            @SerializedName("lon")
            val longitude: Double,
            val name: String,
            @SerializedName("display_name")
            val displayName: String
        )
    }
}
