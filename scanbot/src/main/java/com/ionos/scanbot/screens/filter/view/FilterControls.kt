/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.filter.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import com.ionos.scanbot.databinding.ScanbotViewFilterControlsBinding
import com.ionos.scanbot.filter.color.ColorFilterType

internal class FilterControls @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

	private val viewBinding: ScanbotViewFilterControlsBinding

	private var listener: FilterControlsListener? = null
	private var filterType: ColorFilterType = ColorFilterType.None()

	init {
		val inflater = LayoutInflater.from(context)
		viewBinding = ScanbotViewFilterControlsBinding.inflate(inflater, this, true)
		initFilterTypes()
		initSoftSettings()
		applyFilter(filterType)
	}

	fun setFilterType(filterType: ColorFilterType) {
		if (this.filterType != filterType) {
			applyFilter(filterType)
		}
	}

	fun setListener(listener: FilterControlsListener) {
		this.listener = listener
	}

	private fun initFilterTypes() = with(viewBinding) {
		controlFilter1.setOnClickListener { onFilterTypeChanged(ColorFilterType.MagicColor()) }
		controlFilter2.setOnClickListener { onFilterTypeChanged(ColorFilterType.MagicText()) }
		controlFilter3.setOnClickListener { onFilterTypeChanged(ColorFilterType.Color()) }
		controlFilter4.setOnClickListener { onFilterTypeChanged(ColorFilterType.Grayscale()) }
		controlFilter5.setOnClickListener { onFilterTypeChanged(ColorFilterType.BlackWhite()) }
		controlFilter6.setOnClickListener { onFilterTypeChanged(ColorFilterType.None()) }
	}

	private fun initSoftSettings() = with(viewBinding) {
		controlProgress1.onProgressChanged { onBrightnessChanged(filterType.modify(brightness = it)) }
		controlProgress2.onProgressChanged { onSharpnessChanged(filterType.modify(sharpness = it)) }
		controlProgress3.onProgressChanged { onContrastChanged(filterType.modify(contrast = it)) }

		controlReset1.setOnClickListener { onBrightnessChanged(filterType.reset(brightness = true)) }
		controlReset2.setOnClickListener { onSharpnessChanged(filterType.reset(sharpness = true)) }
		controlReset3.setOnClickListener { onContrastChanged(filterType.reset(contrast = true)) }
	}

	private fun onFilterTypeChanged(filterType: ColorFilterType) {
		applyFilter(filterType)
		listener?.onFilterTypeChanged(filterType)
	}

	private fun onBrightnessChanged(filterType: ColorFilterType) {
		applyFilter(filterType)
		listener?.onBrightnessChanged(filterType)
	}

	private fun onSharpnessChanged(filterType: ColorFilterType) {
		applyFilter(filterType)
		listener?.onSharpnessChanged(filterType)
	}

	private fun onContrastChanged(filterType: ColorFilterType) {
		applyFilter(filterType)
		listener?.onContrastChanged(filterType)
	}

	private fun applyFilter(filterType: ColorFilterType) = with(viewBinding) {
		this@FilterControls.filterType = filterType

		toggleControlFilterButton(filterType)

		controlProgress1.progress = filterType.brightness
		controlProgress2.progress = filterType.sharpness
		controlProgress3.progress = filterType.contrast

		controlProgress1.isSelected = filterType.isChanged(brightness = true)
		controlProgress2.isSelected = filterType.isChanged(sharpness = true)
		controlProgress3.isSelected = filterType.isChanged(contrast = true)

		controlImage1.isSelected = filterType.isChanged(brightness = true)
		controlImage2.isSelected = filterType.isChanged(sharpness = true)
		controlImage3.isSelected = filterType.isChanged(contrast = true)

		controlReset1.visibility = if (filterType.isChanged(brightness = true)) VISIBLE else INVISIBLE
		controlReset2.visibility = if (filterType.isChanged(sharpness = true)) VISIBLE else INVISIBLE
		controlReset3.visibility = if (filterType.isChanged(contrast = true)) VISIBLE else INVISIBLE
	}

	private fun toggleControlFilterButton(filterType: ColorFilterType) = with(viewBinding) {
		controlFilter1.isSelected = filterType is ColorFilterType.MagicColor
		controlFilter2.isSelected = filterType is ColorFilterType.MagicText
		controlFilter3.isSelected = filterType is ColorFilterType.Color
		controlFilter4.isSelected = filterType is ColorFilterType.Grayscale
		controlFilter5.isSelected = filterType is ColorFilterType.BlackWhite
		controlFilter6.isSelected = filterType is ColorFilterType.None
	}

	private fun SeekBar.onProgressChanged(listener: (progress: Int) -> Unit) {
		setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
			private var userProgress: Int? = null

			override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
				if (fromUser) {
					userProgress = progress
				}
			}

			override fun onStartTrackingTouch(seekBar: SeekBar?) {}

			override fun onStopTrackingTouch(seekBar: SeekBar?) {
				userProgress?.let(listener::invoke)
			}
		})
	}
}