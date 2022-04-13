package com.nmc.android.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.owncloud.android.ui.activity.FileDisplayActivity

/**
 * Launcher Activity is added for avoiding **white screen** when app launches
 * this activity will remove that white screen with magenta color
 * The white screen is due to the time taken by Application class to setup the app
 */
class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, SplashActivity::class.java))
        finish()
    }
}
