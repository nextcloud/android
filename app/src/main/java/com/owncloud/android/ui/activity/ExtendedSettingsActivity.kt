package com.owncloud.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.ui.ChooseStorageLocationDialogFragment
import com.owncloud.android.ui.dialog.ThemeSelectionDialog
import com.owncloud.android.ui.model.ExtendedSettingsActivityDialog

class ExtendedSettingsActivity : AppCompatActivity() {

    private var dialogShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            dialogShown = savedInstanceState.getBoolean(KEY_DIALOG_SHOWN, false)
        }

        if (dialogShown) {
            return
        }

        val dialogType = intent.getStringExtra(EXTRA_DIALOG_TYPE) ?: run {
            finish()
            return
        }

        when (dialogType) {
            ExtendedSettingsActivityDialog.StorageLocation.key -> {
                setupStorageLocationDialog()
            }
            ExtendedSettingsActivityDialog.Theme.key -> {
                setupThemeDialog()
            }
            else -> {
                finish()
            }
        }

        dialogShown = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_DIALOG_SHOWN, dialogShown)
    }

    private fun setupStorageLocationDialog() {
        if (supportFragmentManager.findFragmentByTag(TAG_STORAGE_DIALOG) != null) {
            return
        }

        val dialog = ChooseStorageLocationDialogFragment.newInstance()

        supportFragmentManager.setFragmentResultListener(
            ExtendedSettingsActivityDialog.StorageLocation.key,
            this
        ) { _, result ->
            setResult(
                ChooseStorageLocationDialogFragment.STORAGE_LOCATION_RESULT_CODE,
                Intent().putExtra(
                    ExtendedSettingsActivityDialog.StorageLocation.key,
                    result.getString(ExtendedSettingsActivityDialog.StorageLocation.key)
                )
            )
            finish()
        }

        dialog.show(supportFragmentManager, TAG_STORAGE_DIALOG)
    }

    private fun setupThemeDialog() {
        if (supportFragmentManager.findFragmentByTag(TAG_THEME_DIALOG) != null) {
            return
        }

        val dialog = ThemeSelectionDialog.newInstance()

        supportFragmentManager.setFragmentResultListener(
            ThemeSelectionDialog.RESULT_KEY,
            this
        ) { _, result ->
            setResult(
                RESULT_OK,
                Intent().putExtra(
                    ThemeSelectionDialog.RESULT_KEY,
                    result.getString(ThemeSelectionDialog.RESULT_KEY)
                )
            )
            finish()
        }

        dialog.show(supportFragmentManager, TAG_THEME_DIALOG)
    }

    companion object {
        private const val EXTRA_DIALOG_TYPE = "dialog_type"
        private const val KEY_DIALOG_SHOWN = "dialog_shown"
        private const val TAG_STORAGE_DIALOG = "choose_storage_location"
        private const val TAG_THEME_DIALOG = "theme_selection"

        fun createIntent(context: Context, dialogType: ExtendedSettingsActivityDialog): Intent {
            return Intent(context, ExtendedSettingsActivity::class.java).apply {
                putExtra(EXTRA_DIALOG_TYPE, dialogType.key)
            }
        }
    }
}
