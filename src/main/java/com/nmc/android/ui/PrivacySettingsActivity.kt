package com.nmc.android.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityPrivacySettingsBinding
import com.owncloud.android.ui.activity.ToolbarActivity
import com.owncloud.android.utils.theme.ThemeCheckableUtils
import javax.inject.Inject

class PrivacySettingsActivity : ToolbarActivity() {

    companion object {
        private const val EXTRA_SHOW_SETTINGS = "show_settings_button"

        @JvmStatic
        fun openPrivacySettingsActivity(context: Context, isShowSettings: Boolean) {
            val intent = Intent(context, PrivacySettingsActivity::class.java)
            intent.putExtra(EXTRA_SHOW_SETTINGS, isShowSettings)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityPrivacySettingsBinding

    /**
     * variable to check if save settings button needs to be shown or not
     * currently we are showing only when user opens this activity from LoginPrivacySettingsActivity
     */
    private var isShowSettingsButton = false

    @Inject
    lateinit var preferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        updateActionBarTitleAndHomeButtonByString(resources.getString(R.string.privacy_settings))
        setUpViews()
        showHideSettingsButton()
    }

    private fun showHideSettingsButton() {
        isShowSettingsButton = intent.getBooleanExtra(EXTRA_SHOW_SETTINGS, false)
        binding.privacySaveSettingsBtn.visibility = if (isShowSettingsButton) View.VISIBLE else View.GONE
    }

    fun setUpViews() {
        ThemeCheckableUtils.tintSwitch(binding.switchDataCollection, 0)
        ThemeCheckableUtils.tintSwitch(binding.switchDataAnalysis, 0)
        binding.switchDataAnalysis.isChecked = preferences.isDataAnalysisEnabled
        binding.switchDataAnalysis.setOnCheckedChangeListener { _, isChecked ->
            preferences.setDataAnalysis(isChecked)
        }
        binding.privacySaveSettingsBtn.setOnClickListener {
            //finish the activity as we are changing the setting on switch check change
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
