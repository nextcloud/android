/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.fileactions

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.PictureDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.nextcloud.android.lib.resources.clientintegration.ClientIntegrationUI
import com.nextcloud.android.lib.resources.clientintegration.Element
import com.nextcloud.android.lib.resources.clientintegration.ElementTypeAdapter
import com.nextcloud.android.lib.resources.clientintegration.Endpoint
import com.nextcloud.client.account.User
import com.nextcloud.common.JSONRequestBody
import com.nextcloud.operations.GetMethod
import com.nextcloud.operations.PostMethod
import com.nextcloud.ui.composeActivity.ComposeActivity
import com.nextcloud.ui.composeActivity.ComposeDestination
import com.nextcloud.utils.GlideHelper
import com.nextcloud.utils.extensions.showToast
import com.owncloud.android.R
import com.owncloud.android.databinding.FileActionsBottomSheetBinding
import com.owncloud.android.databinding.FileActionsBottomSheetItemBinding
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.ocs.ServerResponse
import com.owncloud.android.lib.resources.status.Method
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.RequestBody
import org.apache.commons.httpclient.HttpStatus
import java.io.IOException

class ClientIntegration(
    private var sheet: FileActionsBottomSheet,
    private var user: User,
    private var context: Context
) {

    fun inflateClientIntegrationActionView(
        endpoint: Endpoint,
        layoutInflater: LayoutInflater,
        binding: FileActionsBottomSheetBinding,
        viewModel: FileActionsViewModel,
        viewThemeUtils: ViewThemeUtils
    ): View {
        val itemBinding = FileActionsBottomSheetItemBinding.inflate(layoutInflater, binding.fileActionsList, false)
            .apply {
                root.setOnClickListener {
                    if (viewModel.uiState.value is FileActionsViewModel.UiState.LoadedForSingleFile) {
                        val singleFile = (viewModel.uiState.value as FileActionsViewModel.UiState.LoadedForSingleFile)

                        val fileId = singleFile.titleFile?.localId.toString()
                        val filePath = singleFile.titleFile?.remotePath.toString()

                        requestClientIntegration(endpoint, fileId, filePath)
                    } else {
                        requestClientIntegration(endpoint, "", "")
                    }
                }
                text.text = endpoint.name

                if (endpoint.icon != null) {
                    sheet.lifecycleScope.launch {
                        val client = OwnCloudClientManagerFactory.getDefaultSingleton()
                            .getNextcloudClientFor(user.toOwnCloudAccount(), context)

                        val drawable =
                            GlideHelper.getDrawable(context, client, client.baseUri.toString() + endpoint.icon)
                                ?.mutate()

                        val px = DisplayUtils.convertDpToPixel(
                            context.resources.getDimension(R.dimen.iconized_single_line_item_icon_size),
                            context
                        )
                        val returnedBitmap =
                            createBitmap(drawable?.intrinsicWidth ?: px, drawable?.intrinsicHeight ?: px)

                        val canvas = Canvas(returnedBitmap)
                        canvas.drawPicture((drawable as PictureDrawable).picture)

                        val d = returnedBitmap.toDrawable(context.resources)

                        val tintedDrawable = viewThemeUtils.platform.tintDrawable(
                            context,
                            d
                        )

                        withContext(Dispatchers.Main) {
                            icon.setImageDrawable(tintedDrawable)
                        }
                    }
                } else {
                    val tintedDrawable = viewThemeUtils.platform.tintDrawable(
                        context,
                        AppCompatResources.getDrawable(context, R.drawable.ic_activity)!!
                    )

                    icon.setImageDrawable(tintedDrawable)
                }
            }
        return itemBinding.root
    }

    private fun requestClientIntegration(endpoint: Endpoint, fileId: String, filePath: String) {
        sheet.lifecycleScope.launch {
            val client = OwnCloudClientManagerFactory.getDefaultSingleton()
                .getNextcloudClientFor(user.toOwnCloudAccount(), context)

            // construct url
            var url = (client.baseUri.toString() + endpoint.url).toUri()
                .buildUpon()
                .appendQueryParameter("format", "json")
                .build()
                .toString()

            // Always replace known placeholder in url
            url = url.replace("{filePath}", filePath, false)
            url = url.replace("{fileId}", fileId, false)

            val method = when (endpoint.method) {
                Method.POST -> {
                    val requestBody = if (endpoint.params?.isNotEmpty() == true) {
                        val jsonRequestBody = JSONRequestBody()
                        endpoint.params!!.forEach {
                            when (it.value) {
                                "{filePath}" -> jsonRequestBody.put(it.key, filePath)
                                "{fileId}" -> jsonRequestBody.put(it.key, fileId)
                            }
                        }

                        jsonRequestBody.get()
                    } else {
                        RequestBody.EMPTY
                    }

                    PostMethod(url, true, requestBody)
                }

                else -> GetMethod(url, true)
            }

            val result = try {
                client.execute(method)
            } catch (_: IOException) {
                context.showToast(context.resources.getString(R.string.failed_to_start_action))
            }
            val response = method.getResponseBodyAsString()

            var output: ClientIntegrationUI?
            try {
                output = parseClientIntegrationResult(response)
                startClientIntegration(endpoint, output)
            } catch (_: JsonSyntaxException) {
                if (result == HttpStatus.SC_OK) {
                    context.showToast(context.resources.getString(R.string.action_triggered))
                } else {
                    context.showToast(context.resources.getString(R.string.failed_to_start_action))
                }
            }
            sheet.dismiss()
        }
    }

    private fun startClientIntegration(endpoint: Endpoint, clientIntegrationUI: ClientIntegrationUI) {
        sheet.lifecycleScope.launch {
            val integrationScreen = ComposeDestination.ClientIntegrationScreen(endpoint.name, clientIntegrationUI)

            val bundle = Bundle().apply {
                putParcelable(ComposeActivity.DESTINATION, integrationScreen)
            }

            val composeActivity = Intent(context, ComposeActivity::class.java).apply {
                putExtras(bundle)
            }

            context.startActivity(composeActivity)
            sheet.dismiss()
        }
    }

    private fun parseClientIntegrationResult(response: String?): ClientIntegrationUI {
        val gson =
            GsonBuilder()
                .registerTypeHierarchyAdapter(Element::class.java, ElementTypeAdapter())
                .create()

        val element: JsonElement = JsonParser.parseString(response)
        return gson
            .fromJson(element, object : TypeToken<ServerResponse<ClientIntegrationUI>>() {})
            .ocs
            .data
    }
}
