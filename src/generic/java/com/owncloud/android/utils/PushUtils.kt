/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author Chris Narkiewicz
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2019 Chris Narkiewicz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils

import android.content.Context
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.SignatureVerification
import java.security.Key

object PushUtils {
    const val KEY_PUSH = "push"
    @JvmStatic
    fun pushRegistrationToServer(accountManager: UserAccountManager?, pushToken: String?) {
        // do nothing
    }

    @JvmStatic
    fun reinitKeys(accountManager: UserAccountManager?) {
        val context = MainApp.getAppContext()
        AppPreferencesImpl.fromContext(context).setKeysReInitEnabled()
    }

    fun readKeyFromFile(readPublicKey: Boolean): Key? {
        return null
    }

    fun verifySignature(
        context: Context?,
        accountManager: UserAccountManager?,
        signatureBytes: ByteArray?,
        subjectBytes: ByteArray?
    ): SignatureVerification? {
        return null
    }
}
