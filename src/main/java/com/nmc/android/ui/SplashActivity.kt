package com.nmc.android.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.widget.AppCompatTextView
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.utils.StringUtils
import javax.inject.Inject

class SplashActivity : BaseActivity() {

    companion object {
        const val SPLASH_DURATION = 1500L
    }

    private lateinit var splashLabel: AppCompatTextView

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        splashLabel = findViewById(R.id.tvSplash)
        setSplashTitle()
        scheduleSplashScreen()
    }

    private fun setSplashTitle() {
        val appName = resources.getString(R.string.app_name)
        val textToBold = resources.getString(R.string.project_name)
        splashLabel.text = StringUtils.makeTextBold(appName, textToBold)
    }

    private fun scheduleSplashScreen() {
        Handler().postDelayed(
            {
                //check if user is logged in but has not selected privacy policy
                //show him the privacy policy screen again
                if (user != null && appPreferences.privacyPolicyAction == LoginPrivacySettingsActivity.NO_ACTION) {
                    LoginPrivacySettingsActivity.openPrivacySettingsActivity(this)
                } else {
                    startActivity(Intent(this, FileDisplayActivity::class.java))
                }
                finish()
            },
            SPLASH_DURATION
        )
    }
}
