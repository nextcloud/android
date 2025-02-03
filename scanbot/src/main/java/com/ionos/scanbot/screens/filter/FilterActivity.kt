/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.filter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import com.ionos.scanbot.R
import com.ionos.scanbot.databinding.ScanbotActivityFilterBinding
import com.ionos.scanbot.di.inject
import com.ionos.scanbot.exception.CreateIntentException
import com.ionos.scanbot.filter.color.ColorFilterType
import com.ionos.scanbot.screens.base.BaseActivity
import com.ionos.scanbot.screens.common.LockProgressDialog
import com.ionos.scanbot.screens.filter.FilterScreen.Event
import com.ionos.scanbot.screens.filter.FilterScreen.Event.CloseScreenEvent
import com.ionos.scanbot.screens.filter.FilterScreen.Event.ShowErrorEvent
import com.ionos.scanbot.screens.filter.FilterScreen.State
import com.ionos.scanbot.screens.filter.FilterScreen.ViewModel
import com.ionos.scanbot.screens.filter.use_case.GetColorFilterName

internal class FilterActivity : BaseActivity<Event, State, ViewModel>() {

	companion object {
		private const val PICTURE_ID: String = "PICTURE_ID"
		private const val FILTER_TYPE: String = "FILTER_TYPE"

		fun createIntent(context: Context, pictureId: String, filterType: ColorFilterType): Intent {
			return Intent(context, FilterActivity::class.java).apply {
				putExtra(PICTURE_ID, pictureId)
				putExtra(FILTER_TYPE, filterType)
				addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
			}
		}
	}

	override val viewModelFactory by lazy { viewModelFactoryAssistant.create(getInitialState()) }
	override val viewBinding by lazy { ScanbotActivityFilterBinding.inflate(layoutInflater) }

	private val viewModelFactoryAssistant by inject { filterViewModelFactoryAssistant() }
	private val progressDialog by lazy { LockProgressDialog() }

	private val getColorFilterName by lazy { GetColorFilterName(this) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		initOnBackPressedCallback()
		initListeners()
        viewBinding.tvApplyToAll.apply {
            visibility = View.VISIBLE
            setOnClickListener { viewModel.onApplyForAllClicked() }
        }
    }

	private fun getInitialState(): State {
		val pictureId = intent?.getStringExtra(PICTURE_ID)
		val filterType = intent?.getSerializableExtra(FILTER_TYPE) as? ColorFilterType
		return if (pictureId != null && filterType != null) {
			State(pictureId, filterType)
		} else {
			throw CreateIntentException(this::class)
		}
	}

	private fun initOnBackPressedCallback() {
		onBackPressedDispatcher.addCallback(this, true) { viewModel.onBackPressed() }
	}

	private fun initListeners() = with(viewBinding) {
		filterControls.setListener(viewModel)
		toolbar.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
		toolbar.tvSave.setOnClickListener { viewModel.onSaveClicked() }
	}

	override fun State.render() = with(viewBinding) {
		toolbar.tvTitle.text = getColorFilterName(filterType)
		viewBinding.tvApplyToAll.visibility = if (showApplyToAll) View.VISIBLE else View.GONE
		filterControls.setFilterType(filterType)
		imageView.setImageBitmap(image)
		renderProcessing(processing)
	}

	private fun renderProcessing(processing: Boolean) = if (processing) {
		progressDialog.start(this, getString(R.string.scanbot_filter_applying_for_all))
	} else {
		progressDialog.stop()
	}

	override fun Event.handle() = when (this) {
		is CloseScreenEvent -> finish()
		is ShowErrorEvent -> showMessage(R.string.scanbot_fail)
	}
}