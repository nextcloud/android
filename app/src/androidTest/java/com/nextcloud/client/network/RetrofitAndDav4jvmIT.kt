/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.network

import at.bitfire.dav4jvm.DavResource
import at.bitfire.dav4jvm.Response
import at.bitfire.dav4jvm.property.DisplayName
import at.bitfire.dav4jvm.property.ResourceType
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nextcloud.android.lib.resources.users.UserApi
import com.nextcloud.common.OkHttpMethodBase
import com.owncloud.android.AbstractIT.nextcloudClient
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import java.net.HttpURLConnection

/**
 * Showcases Retrofit (OCS/JSON) and dav4jvm (WebDAV) side by side. Both reuse the OkHttpClient of
 * [nextcloudClient], so TLS, proxy and client-certificate handling stay identical to the existing
 * `RemoteOperation`s.
 */
class RetrofitAndDav4jvmIT : AbstractOnServerIT() {
    @Test
    fun retrofitFetchesSameUserAsRemoteOperation() {
        val userApi = buildRetrofit().create(UserApi::class.java)

        val response = userApi.getUser(FORMAT_JSON).execute()

        assertTrue("HTTP ${response.code()}", response.isSuccessful)
        val user = response.body()?.ocs?.data
        assertNotNull(user)

        val legacyResult = GetUserInfoRemoteOperation().execute(nextcloudClient)
        assertTrue(legacyResult.isSuccess)
        assertEquals(legacyResult.resultData.id, user?.id)
        assertEquals(legacyResult.resultData.displayName, user?.displayName)
    }

    @Test
    fun retrofitSuspendFunctionReturnsBodyOrThrowsHttpException() {
        val userApi = buildRetrofit().create(UserApi::class.java)
        val unauthorizedApi = buildRetrofit(Credentials.basic(INVALID_LOGIN, INVALID_LOGIN)).create(UserApi::class.java)

        // suspend functions run the call on OkHttp's dispatcher, so no Dispatchers.IO switch is needed
        val user = runBlocking { userApi.fetchUser(FORMAT_JSON).ocs.data }
        val error = assertThrows(HttpException::class.java) {
            runBlocking { unauthorizedApi.fetchUser(FORMAT_JSON) }
        }

        assertEquals(nextcloudClient.userId, user.id)
        assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, error.code())
    }

    @Test
    fun dav4jvmMkColThenPropfindRootListsFolder() {
        val davHttpClient = buildDavOkHttpClient()
        val rootUrl = "${nextcloudClient.filesDavUri}/".toHttpUrl()
        val root = DavResource(davHttpClient, rootUrl)
        val folder = DavResource(davHttpClient, "$rootUrl$FOLDER_NAME/".toHttpUrl())

        folder.mkCol(xmlBody = null) { }

        val responses = mutableListOf<Pair<Response, Response.HrefRelation>>()
        root.propfind(DEPTH_ONE, DisplayName.NAME, ResourceType.NAME) { response, relation ->
            responses += response to relation
        }

        val self = responses.firstOrNull { (_, relation) -> relation == Response.HrefRelation.SELF }?.first
        val members = responses.filter { (_, relation) -> relation == Response.HrefRelation.MEMBER }.map { it.first }

        assertTrue(self?.get(ResourceType::class.java)?.types?.contains(ResourceType.COLLECTION) == true)
        val created = members.firstOrNull { it.hrefName() == FOLDER_NAME }
        assertNotNull("$FOLDER_NAME missing in PROPFIND result", created)
        assertTrue(created?.get(ResourceType::class.java)?.types?.contains(ResourceType.COLLECTION) == true)
    }

    private fun buildRetrofit(credentials: String = nextcloudClient.credentials): Retrofit = Retrofit
        .Builder()
        .baseUrl("${nextcloudClient.baseUri}/")
        .client(authenticatedOkHttpClient(credentials))
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()

    private fun authenticatedOkHttpClient(credentials: String): OkHttpClient = nextcloudClient.client
        .newBuilder()
        .addInterceptor { chain ->
            val request =
                chain
                    .request()
                    .newBuilder()
                    .header(OkHttpMethodBase.AUTHORIZATION, credentials)
                    .header(OkHttpMethodBase.USER_AGENT, OwnCloudClientManagerFactory.getUserAgent())
                    .header(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE)
                    .build()
            chain.proceed(request)
        }.build()

    // dav4jvm handles redirects itself and requires the underlying client not to follow them
    private fun buildDavOkHttpClient(): OkHttpClient = nextcloudClient.client
        .newBuilder()
        .followRedirects(false)
        .addInterceptor { chain ->
            val request =
                chain
                    .request()
                    .newBuilder()
                    .header(OkHttpMethodBase.AUTHORIZATION, nextcloudClient.credentials)
                    .header(OkHttpMethodBase.USER_AGENT, OwnCloudClientManagerFactory.getUserAgent())
                    .build()
            chain.proceed(request)
        }.build()

    companion object {
        private const val FORMAT_JSON = "json"
        private const val JSON_MEDIA_TYPE = "application/json"
        private const val FOLDER_NAME = "files-dav4jvmShowcase"
        private const val DEPTH_ONE = 1
        private const val INVALID_LOGIN = "invalid"

        private val json = Json { ignoreUnknownKeys = true }
    }
}
