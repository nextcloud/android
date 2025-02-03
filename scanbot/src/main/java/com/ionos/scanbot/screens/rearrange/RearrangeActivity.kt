/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.rearrange

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.recyclerview.widget.ItemTouchHelper
import com.ionos.scanbot.R
import com.ionos.scanbot.databinding.ScanbotActivityRearrangeBinding
import com.ionos.scanbot.di.inject
import com.ionos.scanbot.screens.base.BaseActivity
import com.ionos.scanbot.screens.rearrange.RearrangeScreen.*
import com.ionos.scanbot.screens.rearrange.RearrangeScreen.Event.*
import com.ionos.scanbot.screens.rearrange.recycler.RearrangeCallback

internal class RearrangeActivity : BaseActivity<Event, State, ViewModel>() {
	override val viewModelFactory by inject { rearrangeViewModelFactory() }
	override val viewBinding by lazy { ScanbotActivityRearrangeBinding.inflate(layoutInflater) }
	private val adapter by inject { rearrangeAdapter() }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		initOnBackPressedCallback()
		initToolbar()
		initRecyclerView()
	}

	override fun onStart() {
		super.onStart()
		viewModel.onStart()
	}

	private fun initOnBackPressedCallback() {
		onBackPressedDispatcher.addCallback(this, true) { viewModel.onBackPressed() }
	}

	private fun initToolbar() = with(viewBinding) {
		setSupportActionBar(scanbotRearrangeToolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.setTitle(R.string.scanbot_rearrange_title)
	}

	private fun initRecyclerView() = with(viewBinding) {
		val rearrangeCallback = RearrangeCallback(viewModel::onPictureDragged)
		val itemTouchHelper = ItemTouchHelper(rearrangeCallback)
		itemTouchHelper.attachToRecyclerView(rvScanbotRearrange)
		rvScanbotRearrange.adapter = adapter
	}

	override fun State.render() = with(viewBinding) {
		pbScanbotRearrange.visibility = if (processing) View.VISIBLE else View.GONE
	}

	override fun Event.handle() = when (this) {
		is ShowItemsEvent -> adapter.setItems(items)
		is ShowErrorMessageEvent -> showMessage(R.string.scanbot_fail)
		is CloseScreenEvent -> finish()
	}
}
