/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.base

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.ionos.scanbot.R
import com.ionos.scanbot.controller.ScanbotController
import com.ionos.scanbot.di.inject
import com.ionos.scanbot.screens.base.BaseScreen.Event
import com.ionos.scanbot.screens.base.BaseScreen.State
import com.ionos.scanbot.screens.base.BaseScreen.ViewModel
import com.ionos.scanbot.util.config.applyDefaultFontScale

internal abstract class BaseActivity<E : Event, S : State<E>, VM : ViewModel<E, S>> : AppCompatActivity() {
    protected abstract val viewModelFactory: ViewModelProvider.Factory
    protected abstract val viewBinding: ViewBinding

    protected val context: Context get() = this
    protected val viewModel: VM by viewModels { viewModelFactory }
    protected val scanbotController: ScanbotController by inject { scanbotController() }

    override fun attachBaseContext(newBase: Context) {
        newBase.resources.configuration.applyDefaultFontScale()
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        viewModel.state.observe(this) { it.renderInternal() }
    }

    private fun applyTheme() {
        val outValue = TypedValue()
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (context.theme.resolveAttribute(R.attr.scanbotTheme, outValue, true)) {
            setTheme(outValue.resourceId)
        } else {
            setTheme(R.style.ScanbotDefaultTheme)
        }
        if (theme.resolveAttribute(R.attr.scanbot_status_bar_color, outValue, true)) {
            window.statusBarColor = outValue.data
        }
        if (theme.resolveAttribute(R.attr.scanbot_light_status_bar, outValue, true)) {
            insetsController.isAppearanceLightStatusBars = outValue.data != 0
        }
        if (theme.resolveAttribute(R.attr.scanbot_navigation_bar_color, outValue, true)) {
            window.navigationBarColor = outValue.data
        }
        if (theme.resolveAttribute(R.attr.scanbot_light_navigation_bar, outValue, true)) {
            insetsController.isAppearanceLightNavigationBars = outValue.data != 0
        }
        if (theme.resolveAttribute(R.attr.scanbot_window_background, outValue, true)) {
            window.decorView.setBackgroundColor(outValue.data)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        scanbotController.restoreState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        scanbotController.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        newConfig.applyDefaultFontScale()
        super.onConfigurationChanged(newConfig)
    }

    final override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    protected fun showMessage(@StringRes messageRes: Int) {
        val message = getString(messageRes)
        showMessage(message)
    }

    protected open fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun S.renderInternal() {
        render()
        event?.handleInternal()
    }

    private fun E.handleInternal() {
        handle()
        viewModel.onEventHandled()
    }

    protected abstract fun S.render()

    protected abstract fun E.handle()
}
