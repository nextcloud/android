/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.save

import android.app.Activity
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.core.widget.addTextChangedListener
import com.ionos.scanbot.R
import com.ionos.scanbot.databinding.ScanbotActivitySaveBinding
import com.ionos.scanbot.di.inject
import com.ionos.scanbot.exception.InvalidFileNameException
import com.ionos.scanbot.exception.NoFreeLocalSpaceException
import com.ionos.scanbot.exception.SaveDocumentException
import com.ionos.scanbot.screens.base.BaseActivity
import com.ionos.scanbot.screens.common.LockProgressDialog
import com.ionos.scanbot.screens.save.SaveScreen.Event
import com.ionos.scanbot.screens.save.SaveScreen.Event.CloseScreenEvent
import com.ionos.scanbot.screens.save.SaveScreen.Event.HandleErrorEvent
import com.ionos.scanbot.screens.save.SaveScreen.Event.LaunchUploadTargetPickerEvent
import com.ionos.scanbot.screens.save.SaveScreen.State
import com.ionos.scanbot.screens.save.SaveScreen.ViewModel
import io.reactivex.disposables.CompositeDisposable

internal class SaveActivity : BaseActivity<Event, State, ViewModel>() {
	override val viewModelFactory: SaveViewModelFactory by inject { saveViewModelFactory() }
	override val viewBinding by lazy { ScanbotActivitySaveBinding.inflate(layoutInflater) }

    private val selectDirectoryContract by inject { selectDirectoryContract() }
	private val progressDialog by lazy { LockProgressDialog() }

    private val overwriteDialogsDisposable = CompositeDisposable()
    private lateinit var selectDirectoryLauncher: ActivityResultLauncher<Unit>

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		initOnBackPressedCallback()
		initViews()
        registerSelectDirectoryLauncher()
	}

	override fun onDestroy() {
		overwriteDialogsDisposable.clear()
		super.onDestroy()
	}

	private fun initOnBackPressedCallback() {
		onBackPressedDispatcher.addCallback(this, true) { viewModel.onBackPressed() }
	}

	private fun initViews() = with(viewBinding) {
		toolbar.tvTitle.text = getString(R.string.scanbot_save_as)
		toolbar.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
		toolbar.tvSave.setOnClickListener { viewModel.onSaveClicked() }
		tvName.addTextChangedListener { viewModel.onFileNameChanged(it?.toString() ?: "") }
		fileTypeView.setOnFileTypeChangedListener { viewModel.onFileTypeChanged(it) }
		tvPath.setOnClickListener { viewModel.onSaveLocationPathClicked() }
	}

	override fun State.render() = with(viewBinding) {
		if (tvName.text.toString() != baseFileName) {
			tvName.setText(baseFileName)
		}
		fileTypeView.setCheckedFileType(fileType)
		tvPath.text = targetPath
		displayProgress(processing)
	}

	private fun displayProgress(display: Boolean) = if (display) {
		progressDialog.start(this, getString(R.string.scanbot_processing_document_title))
	} else {
		progressDialog.stop()
	}

	override fun Event.handle() = when (this) {
		is LaunchUploadTargetPickerEvent -> selectDirectoryLauncher.launch(Unit)
		is HandleErrorEvent -> error.handle()
        is CloseScreenEvent -> finishWithResult(successResult)
	}

	private fun Throwable.handle() = when (this) {
		is SaveDocumentException -> showSaveDocumentErrorMessage(cause)
		is InvalidFileNameException -> showMessage(R.string.scanbot_save_invalid_file_name)
		else -> showMessage(R.string.scanbot_unknown_exception)
	}

	private fun showSaveDocumentErrorMessage(cause: Throwable?) = when (cause) {
		is NoFreeLocalSpaceException -> showMessage(R.string.scanbot_no_free_space_message)
		else -> showMessage(R.string.scanbot_creating_document_error_message)
	}

    private fun registerSelectDirectoryLauncher() {
        selectDirectoryLauncher = registerForActivityResult(selectDirectoryContract) {
            when (it) {
                is SelectDirectoryContract.SelectDirectoryResult.Success -> viewModel.onUploadTargetPicked(it.selectedTarget)
                is SelectDirectoryContract.SelectDirectoryResult.Canceled -> viewModel.onUploadTargetPickerCanceled()
            }
        }
    }

    private fun finishWithResult(successResult: Boolean) {
        if (successResult) {
            setResult(Activity.RESULT_OK)
        } else {
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }
}
