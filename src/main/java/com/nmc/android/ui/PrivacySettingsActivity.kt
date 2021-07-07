package com.nmc.android.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityPrivacySettingsBinding
import com.owncloud.android.ui.activity.ToolbarActivity
import com.owncloud.android.utils.ThemeUtils
import javax.inject.Inject

class PrivacySettingsActivity : ToolbarActivity() {

    companion object {
        @JvmStatic
        fun openPrivacySettingsActivity(context: Context) {
            val intent = Intent(context, PrivacySettingsActivity::class.java)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityPrivacySettingsBinding

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
    }

    fun setUpViews() {
        ThemeUtils.tintSwitch(binding.switchDataCollection, 0)
        ThemeUtils.tintSwitch(binding.switchDataAnalysis, 0)
        binding.switchDataAnalysis.isChecked = preferences.isDataAnalysisEnabled
        binding.switchDataAnalysis.setOnCheckedChangeListener { _, isChecked ->
            preferences.setDataAnalysis(isChecked)
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
