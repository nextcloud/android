/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.nextcloud.client.account.User
import com.nextcloud.ui.teams.detail.model.TeamResourceTarget
import com.owncloud.android.ui.activity.FileDisplayActivity

class TeamResourceOpener(private val activity: Activity, private val user: User) {

    fun open(target: TeamResourceTarget) {
        when (target) {
            is TeamResourceTarget.Folder -> openInFiles(FileDisplayActivity.KEY_FILE_PATH, target.remotePath)
            is TeamResourceTarget.File -> openInFiles(FileDisplayActivity.KEY_FILE_ID, target.fileId)
            is TeamResourceTarget.TalkConversation -> openInApp(TALK_SCHEME, TALK_PATH + target.token, target.webPage)
            is TeamResourceTarget.DeckBoard -> openInApp(DECK_SCHEME, DECK_PATH + target.boardId, target.webPage)
            is TeamResourceTarget.WebPage -> openWebPage(target)
        }
    }

    private fun openInFiles(key: String, value: String) {
        val intent = Intent(activity, FileDisplayActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(FileDisplayActivity.KEY_ACCOUNT, user.accountName)
            putExtra(key, value)
        }
        activity.startActivity(intent)
    }

    private fun openWebPage(target: TeamResourceTarget.WebPage) {
        activity.startActivity(TeamWebView.intent(activity, target.title, target.url))
    }

    private fun openInApp(scheme: String, path: String, fallback: TeamResourceTarget.WebPage) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, appLink(scheme, path)))
        } catch (_: ActivityNotFoundException) {
            openWebPage(fallback)
        }
    }

    private fun appLink(scheme: String, path: String): Uri {
        val server = user.server.uri
        val userId = Uri.encode(user.accountName.substringBeforeLast(ACCOUNT_SEPARATOR))
        val basePath = server.rawPath.orEmpty().trimEnd(PATH_SEPARATOR)
        return Uri.Builder().apply {
            scheme(scheme)
            encodedAuthority("$userId$ACCOUNT_SEPARATOR${server.rawAuthority}")
            encodedPath("$basePath$path")
        }.build()
    }

    companion object {
        private const val TALK_SCHEME = "nextcloudtalk"
        private const val TALK_PATH = "/call/"
        private const val DECK_SCHEME = "nextclouddeck"
        private const val DECK_PATH = "/board/"
        private const val ACCOUNT_SEPARATOR = '@'
        private const val PATH_SEPARATOR = '/'
    }
}
