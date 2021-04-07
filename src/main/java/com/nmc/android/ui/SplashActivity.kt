package com.nmc.android.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import com.owncloud.android.R
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.utils.StringUtils

class SplashActivity : AppCompatActivity() {

    companion object {
        const val SPLASH_DURATION = 1500L
    }

    private lateinit var splashLabel: AppCompatTextView

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
                startActivity(Intent(this, FileDisplayActivity::class.java))
                finish()
            },
            SPLASH_DURATION
        )
    }
}