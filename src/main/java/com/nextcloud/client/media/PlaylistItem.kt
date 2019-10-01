package com.nextcloud.client.media

import android.accounts.Account
import com.owncloud.android.datamodel.OCFile

data class PlaylistItem(val file: OCFile, val startPositionMs: Int, val autoPlay: Boolean, val account: Account)
