/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.crop.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.ionos.scanbot.databinding.ScanbotViewCropBinding

internal class CropView @JvmOverloads constructor(
	context: Context,
	attributeSet: AttributeSet? = null,
) : FrameLayout(context, attributeSet) {

	private val viewBinding: ScanbotViewCropBinding

	init {
		viewBinding = ScanbotViewCropBinding.inflate(LayoutInflater.from(context), this)
	}

	fun render(resource: Bitmap) = with(viewBinding) {
		polygonView.setImageBitmap(resource)
		magnifier.setupMagnifier(polygonView)
	}

	fun getPolygon(): List<PointF> {
		return viewBinding.polygonView.polygon.toList()
	}

	fun setPolygon(polygon: List<PointF>) {
		viewBinding.polygonView.polygon = polygon.toList()
	}

	override fun onSaveInstanceState(): Parcelable? {
		val superState = super.onSaveInstanceState() ?: return null
		return SavedState(superState, viewBinding.polygonView.polygon)
	}

	override fun onRestoreInstanceState(state: Parcelable?) {
		val savedState = state as? SavedState ?: return
		super.onRestoreInstanceState(savedState.superState)
		setPolygon(savedState.polygon)
	}

	class SavedState : BaseSavedState {
		val polygon: List<PointF>

		constructor(superState: Parcelable, polygon: List<PointF>) : super(superState) {
			this.polygon = polygon
		}

		constructor(parcel: Parcel) : super(parcel) {
			val polygon = listOf<PointF>()
			parcel.readList(polygon, PointF::class.java.classLoader)
			this.polygon = polygon
		}

		override fun writeToParcel(out: Parcel, flags: Int) {
			super.writeToParcel(out, flags)
			out.writeList(polygon)
		}

		companion object CREATOR : Parcelable.Creator<SavedState> {
			override fun createFromParcel(parcel: Parcel): SavedState {
				return SavedState(parcel)
			}

			override fun newArray(size: Int): Array<SavedState?> {
				return arrayOfNulls(size)
			}
		}
	}
}