package com.nextcloud.client.media

import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.OCFile

data class PlaylistItem(val file: OCFile, val startPositionMs: Int, val autoPlay: Boolean, val user: User)
