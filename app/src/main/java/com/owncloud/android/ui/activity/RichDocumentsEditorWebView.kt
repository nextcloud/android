/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.webkit.JavascriptInterface
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RichDocumentsCreateAssetOperation
import com.owncloud.android.ui.asynctasks.PrintAsyncTask
import com.owncloud.android.ui.asynctasks.RichDocumentsLoadUrlTask
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileStorageUtils
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Opens document for editing via Richdocuments app in a web view
 */
class RichDocumentsEditorWebView : EditorWebView() {
    @JvmField
    @Inject
    var currentAccountProvider: CurrentAccountProvider? = null

    @JvmField
    @Inject
    var clientFactory: ClientFactory? = null

    private var activityResult: ActivityResultLauncher<Intent>? = null

    @SuppressFBWarnings("ANDROID_WEB_VIEW_JAVASCRIPT_INTERFACE")
    override fun postOnCreate() {
        super.postOnCreate()

        webView.addJavascriptInterface(RichDocumentsMobileInterface(), "RichDocumentsMobileInterface")

        loadUrl(intent.getStringExtra(EXTRA_URL))

        registerActivityResult()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    private fun openFileChooser() {
        val action = Intent(this, FilePickerActivity::class.java)
        action.putExtra(OCFileListFragment.ARG_MIMETYPE, "image/")
        activityResult?.launch(action)
    }

    private fun registerActivityResult() {
        activityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (RESULT_OK == result.resultCode) {
                    result.data?.let {
                        handleRemoteFile(it)
                    }
                }
            }
    }

    private fun handleRemoteFile(data: Intent) {
        val file = FolderPickerActivity.EXTRA_FILES?.let { data.getParcelableArgument(it, OCFile::class.java) }

        Thread {
            val user = currentAccountProvider?.user
            val operation = RichDocumentsCreateAssetOperation(file?.remotePath)
            val result = operation.execute(user, this)
            if (result.isSuccess) {
                val asset = result.singleData as String
                runOnUiThread {
                    webView.evaluateJavascript(
                        "OCA.RichDocuments.documentsMain.postAsset('" +
                            file?.fileName + "', '" + asset + "');",
                        null
                    )
                }
            } else {
                runOnUiThread { DisplayUtils.showSnackMessage(this, "Inserting image failed!") }
            }
        }.start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(EXTRA_URL, url)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        url = savedInstanceState.getString(EXTRA_URL)
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        webView.evaluateJavascript(
            "if (typeof OCA.RichDocuments.documentsMain.postGrabFocus !== 'undefined') " +
                "{ OCA.RichDocuments.documentsMain.postGrabFocus(); }",
            null
        )
    }

    private fun printFile(url: Uri) {
        val account = accountManager.currentOwnCloudAccount
        if (account == null) {
            DisplayUtils.showSnackMessage(webView, getString(R.string.failed_to_print))
            return
        }
        val targetFile = File(FileStorageUtils.getTemporalPath(account.name) + "/print.pdf")
        PrintAsyncTask(targetFile, url.toString(), WeakReference(this)).execute()
    }

    public override fun loadUrl(url: String?) {
        if (TextUtils.isEmpty(url)) {
            RichDocumentsLoadUrlTask(this, user.get(), file).execute()
        } else {
            super.loadUrl(url)
        }
    }

    private fun showSlideShow(url: Uri) {
        val intent = Intent(this, ExternalSiteWebView::class.java)
        intent.putExtra(EXTRA_URL, url.toString())
        intent.putExtra(EXTRA_SHOW_SIDEBAR, false)
        intent.putExtra(EXTRA_SHOW_TOOLBAR, false)
        startActivity(intent)
    }

    private inner class RichDocumentsMobileInterface : MobileInterface() {
        @JavascriptInterface
        fun insertGraphic() {
            openFileChooser()
        }

        @JavascriptInterface
        fun documentLoaded() {
            runOnUiThread { hideLoading() }
        }

        @JavascriptInterface
        fun downloadAs(json: String?) {
            try {
                json ?: return
                val downloadJson = JSONObject(json)
                val url = downloadJson.getString(URL).toUri()
                when (downloadJson.getString(TYPE)) {
                    PRINT -> printFile(url)
                    SLIDESHOW -> showSlideShow(url)
                    else -> downloadFile(url)
                }
            } catch (e: JSONException) {
                Log_OC.e(this, "Failed to parse download json message: $e")
            }
        }

        @JavascriptInterface
        fun fileRename(renameString: String?) {
            // when shared file is renamed in another instance, we will get notified about it
            // need to change filename for sharing
            try {
                renameString ?: return
                val renameJson = JSONObject(renameString)
                val newName = renameJson.getString(NEW_NAME)
                file.fileName = newName
            } catch (e: JSONException) {
                Log_OC.e(this, "Failed to parse rename json message: $e")
            }
        }

        @JavascriptInterface
        fun paste() {
            // Javascript cannot do this by itself, so help out.
            webView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PASTE))
            webView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PASTE))
        }

        @JavascriptInterface
        fun hyperlink(hyperlink: String?) {
            try {
                hyperlink ?: return
                val url = JSONObject(hyperlink).getString(HYPERLINK)
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = url.toUri()
                startActivity(intent)
            } catch (e: JSONException) {
                Log_OC.e(this, "Failed to parse download json message: $e")
            }
        }
    }

    companion object {
        private const val URL = "URL"
        private const val HYPERLINK = "Url"
        private const val TYPE = "Type"
        private const val PRINT = "print"
        private const val SLIDESHOW = "slideshow"
        private const val NEW_NAME = "NewName"
    }
}
