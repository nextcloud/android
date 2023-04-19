package com.nmc.android.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.View
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.databinding.ActivitySplashBinding
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
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
        updateBoldTitleVisibility()
        scheduleSplashScreen()
    }

    private fun updateBoldTitleVisibility() {
        //For NC this will be empty so to handle that case we have added this check
        if (TextUtils.isEmpty(resources.getString(R.string.splashScreenBold))) {
            binding.splashScreenBold.visibility = View.GONE
        }
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
