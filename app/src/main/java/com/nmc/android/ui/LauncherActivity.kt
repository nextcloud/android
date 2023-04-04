package com.nmc.android.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.databinding.ActivitySplashBinding
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.utils.StringUtils
import javax.inject.Inject

class LauncherActivity : BaseActivity() {

    companion object {
        const val SPLASH_DURATION = 1500L
    }

    private lateinit var binding : ActivitySplashBinding

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        //Mandatory to call this before super method to show system launch screen for api level 31+
        installSplashScreen()

        //flag to avoid redirection to AuthenticatorActivity
        //as we need this activity to be shown
        //Note: Should be kept before super() method
        enableAccountHandling = false
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSplashTitle()
        scheduleSplashScreen()
    }

    private fun setSplashTitle() {
        val appName = resources.getString(R.string.app_name)
        val textToBold = resources.getString(R.string.project_name)
        binding.tvSplash.text = StringUtils.makeTextBold(appName, textToBold)
    }

    private fun scheduleSplashScreen() {
        Handler().postDelayed(
            {
                //if user is null then go to authenticator activity
                if (!user.isPresent) {
                    startActivity(Intent(this, AuthenticatorActivity::class.java))
                } else {
                    startActivity(Intent(this, FileDisplayActivity::class.java))
                }
                finish()
            },
            SPLASH_DURATION
        )
    }
}