package com.nmc.android.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.nextcloud.client.preferences.AppPreferences
import com.nmc.android.utils.makeLinks
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityLoginPrivacySettingsBinding
import com.owncloud.android.ui.activity.ExternalSiteWebView
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.ToolbarActivity
import javax.inject.Inject

class LoginPrivacySettingsActivity : ToolbarActivity() {

    companion object {
        //privacy user action to maintain the state of privacy policy
        const val NO_ACTION = 0 //user has taken no action
        const val REJECT_ACTION = 1 //user rejected the privacy policy
        const val ACCEPT_ACTION = 2 //user has accepted the privacy policy

        @JvmStatic
        fun openPrivacySettingsActivity(context: Context) {
            val intent = Intent(context, LoginPrivacySettingsActivity::class.java)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityLoginPrivacySettingsBinding

    @Inject
    lateinit var preferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginPrivacySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        //don't show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        updateActionBarTitleAndHomeButtonByString(resources.getString(R.string.privacy_settings))
        setUpPrivacyText()
        binding.privacyAcceptBtn.setOnClickListener {
            //on accept finish the activity
            //update the accept privacy action to preferences
            preferences.privacyPolicyAction = ACCEPT_ACTION
            openFileDisplayActivity()
        }
    }

    private fun setUpPrivacyText() {
        val privacyText = String.format(
            resources.getString(R.string.login_privacy_settings_intro_text), resources
                .getString(R.string.login_privacy_policy), resources
                .getString(R.string.login_privacy_reject), resources
                .getString(R.string.login_privacy_settings)
        )
        binding.tvLoginPrivacyIntroText.text = privacyText

        //make links clickable
        binding.tvLoginPrivacyIntroText.makeLinks(
            Pair(resources.getString(R.string.login_privacy_policy), View.OnClickListener {
                //open privacy policy url
                val intent = Intent(this, ExternalSiteWebView::class.java)
                intent.putExtra(
                    ExternalSiteWebView.EXTRA_TITLE,
                    resources.getString(R.string.privacy_policy)
                )
                intent.putExtra(ExternalSiteWebView.EXTRA_URL, resources.getString(R.string.privacy_policy_url))
                intent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false)
                intent.putExtra(ExternalSiteWebView.EXTRA_MENU_ITEM_ID, -1)
                startActivity(intent)
            }), Pair(resources
                .getString(R.string.login_privacy_reject), View.OnClickListener {
                //disable data analysis option and close the activity
                preferences.setDataAnalysis(false)
                //update the reject privacy action to preferences
                preferences.privacyPolicyAction = REJECT_ACTION
                openFileDisplayActivity()
            }), Pair(resources
                .getString(R.string.login_privacy_settings), View.OnClickListener {
                //open privacy settings screen
                PrivacySettingsActivity.openPrivacySettingsActivity(this, true)
            })
        )
    }

    private fun openFileDisplayActivity() {
        val i = Intent(this, FileDisplayActivity::class.java)
        i.action = FileDisplayActivity.RESTART
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(i)
        finish()
    }

    override fun onBackPressed() {
        //user cannot close this screen without accepting or rejecting the privacy policy
    }
}
