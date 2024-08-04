/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.os.Bundle
import android.view.View

/**
 * Activity providing information about ways to participate in the app's development.
 */
class HuaweiCommunityActivity : CommunityActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.communityReleaseCandidatePlaystore.visibility = View.GONE
    }
}
