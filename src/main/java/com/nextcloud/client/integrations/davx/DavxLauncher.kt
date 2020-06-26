package com.nextcloud.client.integrations.davx

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.utils.DisplayUtils

class DavxLauncher(
    private val packageManager: PackageManager
) {

    private companion object {
        private const val DAV_PATH = "/remote.php/dav"
        private const val TRUE = 1

        private const val DAVX5_PACKAGE = "at.bitfire.davdroid"
        private const val DAVX5_LOGIN_ACTIVITY_CLASS = "at.bitfire.davdroid.ui.setup.LoginActivity"

        private val LAUNCH_DAVX5_LOGIN_INTENT = Intent().apply {
            setClassName(DAVX5_PACKAGE, DAVX5_LOGIN_ACTIVITY_CLASS)
        }

        private val APP_MARKET_URI = Uri.parse("market://details?id=$DAVX5_PACKAGE")
        private val LAUNCH_APP_MARKET_INTENT = Intent(Intent.ACTION_VIEW, APP_MARKET_URI)

        private val FDROID_WEBPAGE_URI = Uri.parse("https://f-droid.org/repository/browse/?fdid=$DAVX5_PACKAGE")
        private val LAUNCH_FDROID_WEBPAGE_INTENT = Intent(Intent.ACTION_VIEW, FDROID_WEBPAGE_URI)
    }

    private fun canLaunch(intent: Intent): Boolean {
        return packageManager.resolveActivity(intent, 0) != null
    }

    /**
     * Launch DAVx5 login flow or installation
     *
     * @param activity Activity that will receive DAVx5 flow result
     * @param requestCode Request code to identify DAVx5 request
     * @param username User username used by DAVx5 to perform login operation
     * @param serverBaseUri optional server URI passed to DAVx5 login flow
     *
     * @return true if launch action succeeded, false if no activity can handle any possible DAVx5 flow
     */
    fun launchLogin(activity: Activity, requestCode: Int, username: String, serverBaseUri: Uri?): Boolean {
        if (canLaunch(LAUNCH_DAVX5_LOGIN_INTENT)) {
            launchDavx(activity, requestCode, username, serverBaseUri)
            return true
        } else if (canLaunch(LAUNCH_APP_MARKET_INTENT)) {
            activity.startActivity(LAUNCH_APP_MARKET_INTENT)
            return true
        } else if (canLaunch(LAUNCH_FDROID_WEBPAGE_INTENT)) {
            // no supported apps market is available - launch f-droid website
            activity.startActivity(LAUNCH_FDROID_WEBPAGE_INTENT)
            DisplayUtils.showSnackMessage(activity, R.string.prefs_calendar_contacts_no_store_error)
            return true
        } else {
            // not even web-browser?
            return false
        }
    }

    private fun launchDavx(activity: Activity, requestCode: Int, username: String, serverBaseUri: Uri?) {
        val intent = LAUNCH_DAVX5_LOGIN_INTENT.clone() as Intent
        if (serverBaseUri != null) {
            intent.putExtra("url", serverBaseUri.toString() + DAV_PATH)
            intent.putExtra("loginFlow", TRUE)
            intent.data = Uri.parse(serverBaseUri.toString() + AuthenticatorActivity.WEB_LOGIN)
            intent.putExtra("davPath", DAV_PATH)
        }
        intent.putExtra("username", username)
        activity.startActivityForResult(intent, requestCode)
    }
}
