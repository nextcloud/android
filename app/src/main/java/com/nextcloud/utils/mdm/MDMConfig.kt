/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.mdm

import android.content.Context
import com.nextcloud.utils.extensions.getRestriction
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.utils.appConfig.AppConfigKeys

object MDMConfig {
   fun multiAccountSupport(context: Context): Boolean {
       val multiAccountSupport = context.resources.getBoolean(R.bool.multiaccount_support)
       val disableMultiAccountViaMDM = context.getRestriction(
           AppConfigKeys.DisableMultiAccount,
           MainApp.getAppContext().resources.getBoolean(R.bool.disable_multiaccount)
       )

       return multiAccountSupport && !disableMultiAccountViaMDM
   }
}
