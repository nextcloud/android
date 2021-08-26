package com.nmc.android.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.widget.AppCompatTextView
import com.nextcloud.client.preferences.AppPreferences
import com.nmc.android.ui.onboarding.OnBoardingActivity
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
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
                //check if on-boarding is not completed then show on-boarding screen
                if (!appPreferences.onBoardingComplete) {
                    OnBoardingActivity.launchOnBoardingActivity(this)
                }
                //if user is null then go to authenticator activity
                else if (!user.isPresent) {
                    startActivity(Intent(this, AuthenticatorActivity::class.java))
                }
                //if user is logged in but did not accepted the privacy policy then take him there
                else if (user.isPresent && appPreferences.privacyPolicyAction == LoginPrivacySettingsActivity
                        .NO_ACTION
                ) {
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
