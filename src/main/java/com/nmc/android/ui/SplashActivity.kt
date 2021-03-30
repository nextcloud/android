package com.nmc.android.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.owncloud.android.R
import com.owncloud.android.ui.activity.FileDisplayActivity

class SplashActivity : AppCompatActivity() {

    companion object {
        const val SPLASH_DURATION = 1500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        scheduleSplashScreen()
    }

    private fun scheduleSplashScreen() {
        Handler().postDelayed(
            {
                startActivity(Intent(this, FileDisplayActivity::class.java))
                finish()
            },
            SPLASH_DURATION
        )
    }
}