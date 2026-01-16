/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
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
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.nextcloud.android.lib.resources.clientintegration.ClientIntegrationUI
import com.nextcloud.android.lib.resources.clientintegration.Element
import com.nextcloud.android.lib.resources.clientintegration.ElementTypeAdapter
import com.nextcloud.android.lib.resources.clientintegration.Endpoint
import com.nextcloud.android.lib.resources.clientintegration.TooltipResponse
import com.nextcloud.client.account.User
import com.nextcloud.common.JSONRequestBody
import com.nextcloud.operations.GetMethod
import com.nextcloud.operations.PostMethod
import com.nextcloud.ui.composeActivity.ComposeActivity
import com.nextcloud.ui.composeActivity.ComposeDestination
import com.nextcloud.utils.GlideHelper
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
                    sheet.lifecycleScope.launch(Dispatchers.IO) {
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
        sheet.lifecycleScope.launch(Dispatchers.IO) {
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
                showMessage(context.resources.getString(R.string.failed_to_start_action))
            }
            val response = method.getResponseBodyAsString()

            try {
                val output = parseClientIntegrationResult(response)
                if (output.root != null && output.root?.rows != null) {
                    startClientIntegration(endpoint, output)
                } else {
                    val tooltipResponse = parseTooltipResult(response)
                    showMessage(tooltipResponse.tooltip)
                }
            } catch (_: JsonSyntaxException) {
                if (result == HttpStatus.SC_OK) {
                    showMessage(context.resources.getString(R.string.action_triggered))
                } else {
                    showMessage(context.resources.getString(R.string.failed_to_start_action))
                }
            }
            sheet.dismiss()
        }
    }

    private suspend fun showMessage(message: String) = withContext(Dispatchers.Main) {
        DisplayUtils.showSnackMessage(sheet.requireActivity(), message)
    }

    private fun parseTooltipResult(response: String?): TooltipResponse {
        val element: JsonElement = JsonParser.parseString(response)
        return Gson()
            .fromJson(element, object : TypeToken<ServerResponse<TooltipResponse>>() {})
            .ocs
            .data
    }

    private fun startClientIntegration(endpoint: Endpoint, data: ClientIntegrationUI) {
        sheet.lifecycleScope.launch(Dispatchers.IO) {
            val integrationScreen = ComposeDestination.ClientIntegrationScreen(endpoint.name, data)

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
