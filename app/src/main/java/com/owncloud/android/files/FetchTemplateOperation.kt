/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.files

import com.owncloud.android.datamodel.Template
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.dialog.ChooseRichDocumentsTemplateDialogFragment
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.GetMethod
import org.json.JSONObject

class FetchTemplateOperation(private val type: ChooseRichDocumentsTemplateDialogFragment.Type) :
    RemoteOperation<Any>() {

    @Suppress("TooGenericExceptionCaught")
    override fun run(client: OwnCloudClient): RemoteOperationResult<Any> {
        var getMethod: GetMethod? = null

        return try {
            getMethod = GetMethod(templateUrl(client.baseUri.toString())).apply {
                addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)
            }

            val status = client.executeMethod(getMethod, SYNC_READ_TIMEOUT, SYNC_CONNECTION_TIMEOUT)
            if (status != HttpStatus.SC_OK) {
                client.exhaustResponse(getMethod.responseBodyAsStream)
                return RemoteOperationResult(false, getMethod)
            }

            val templates = parseTemplates(getMethod.responseBodyAsString)
            RemoteOperationResult<Any>(true, getMethod).apply { setData(ArrayList<Any>(templates)) }
        } catch (e: Exception) {
            RemoteOperationResult<Any>(e).also {
                Log_OC.e(TAG, "Get templates for type $type failed: ${it.logMessage}", it.exception)
            }
        } finally {
            getMethod?.releaseConnection()
        }
    }

    private fun templateUrl(baseUri: String): String = baseUri + TEMPLATE_URL + type.name.lowercase() + JSON_FORMAT

    private fun parseTemplates(response: String): List<Template> {
        val data = JSONObject(response).getJSONObject(NODE_OCS).getJSONArray(NODE_DATA)

        return (0 until data.length()).map { index ->
            data.getJSONObject(index).toTemplate()
        }
    }

    private fun JSONObject.toTemplate(): Template = Template(
        getLong(NODE_ID),
        getString(NODE_NAME),
        optString(NODE_PREVIEW),
        Template.Type.parse(getString(NODE_TYPE)),
        getString(NODE_EXTENSION)
    )

    companion object {
        private val TAG = FetchTemplateOperation::class.java.simpleName
        private const val SYNC_READ_TIMEOUT = 40000
        private const val SYNC_CONNECTION_TIMEOUT = 5000
        private const val TEMPLATE_URL = "/ocs/v2.php/apps/richdocuments/api/v1/templates/"
        private const val JSON_FORMAT = "?format=json"

        private const val NODE_OCS = "ocs"
        private const val NODE_DATA = "data"
        private const val NODE_ID = "id"
        private const val NODE_NAME = "name"
        private const val NODE_PREVIEW = "preview"
        private const val NODE_TYPE = "type"
        private const val NODE_EXTENSION = "extension"
    }
}
