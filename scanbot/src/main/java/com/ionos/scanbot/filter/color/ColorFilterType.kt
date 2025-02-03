/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.filter.color

import io.scanbot.sdk.process.ImageFilterType
import java.io.Serializable

internal sealed class ColorFilterType private constructor(
	open val brightness: Int,
	open val sharpness: Int,
	open val contrast: Int,
	open val scanbotFilter: ImageFilterType,
) : Serializable {

	protected abstract val defaultBrightness: Int
	protected abstract val defaultSharpness: Int
	protected abstract val defaultContrast: Int

	fun isChanged(
		brightness: Boolean = false,
		sharpness: Boolean = false,
		contrast: Boolean = false
	): Boolean =
		if (brightness) this.brightness != this.defaultBrightness else false
				|| if (sharpness) this.sharpness != this.defaultSharpness else false
				|| if (contrast) this.contrast != this.defaultContrast else false

	fun modify(
		brightness: Int = this.brightness,
		sharpness: Int = this.sharpness,
		contrast: Int = this.contrast
	): ColorFilterType = create(brightness, sharpness, contrast)

	fun reset(
		brightness: Boolean = false,
		sharpness: Boolean = false,
		contrast: Boolean = false
	): ColorFilterType =
		create(
			if (brightness) this.defaultBrightness else this.brightness,
			if (sharpness) this.defaultSharpness else this.sharpness,
			if (contrast) this.defaultContrast else this.contrast
		)

	protected abstract fun create(
		brightness: Int = this.brightness,
		sharpness: Int = this.sharpness,
		contrast: Int = this.contrast
	): ColorFilterType

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ColorFilterType) return false
		if (javaClass != other.javaClass) return false

		return brightness == other.brightness
				&& sharpness == other.sharpness
				&& contrast == other.contrast
				&& scanbotFilter == other.scanbotFilter
	}

	override fun hashCode(): Int {
		var result: Int = scanbotFilter.hashCode()
		result = 31 * result + brightness
		result = 31 * result + sharpness
		result = 31 * result + contrast
		return result
	}

	data class MagicColor(
		override val brightness: Int = DEFAULT_BRIGHTNESS,
		override val sharpness: Int = DEFAULT_SHARPNESS,
		override val contrast: Int = DEFAULT_CONTRAST
	) : ColorFilterType(brightness, sharpness, contrast, ImageFilterType.COLOR_ENHANCED) {

		companion object {
			private const val DEFAULT_BRIGHTNESS: Int = 60
			private const val DEFAULT_SHARPNESS: Int = 60
			private const val DEFAULT_CONTRAST: Int = 60
		}

		override val defaultBrightness get() = DEFAULT_BRIGHTNESS
		override val defaultSharpness get() = DEFAULT_SHARPNESS
		override val defaultContrast get() = DEFAULT_CONTRAST

		override fun create(brightness: Int, sharpness: Int, contrast: Int): ColorFilterType =
			MagicColor(brightness, sharpness, contrast)
	}

	data class MagicText(
		override val brightness: Int = DEFAULT_BRIGHTNESS,
		override val sharpness: Int = DEFAULT_SHARPNESS,
		override val contrast: Int = DEFAULT_CONTRAST
	) : ColorFilterType(brightness, sharpness, contrast, ImageFilterType.COLOR_DOCUMENT) {

		companion object {
			private const val DEFAULT_BRIGHTNESS: Int = 40
			private const val DEFAULT_SHARPNESS: Int = 50
			private const val DEFAULT_CONTRAST: Int = 60
		}

		override val defaultBrightness get() = DEFAULT_BRIGHTNESS
		override val defaultSharpness get() = DEFAULT_SHARPNESS
		override val defaultContrast get() = DEFAULT_CONTRAST

		override fun create(brightness: Int, sharpness: Int, contrast: Int): ColorFilterType =
			MagicText(brightness, sharpness, contrast)
	}

	data class Color(
		override val brightness: Int = DEFAULT_BRIGHTNESS,
		override val sharpness: Int = DEFAULT_SHARPNESS,
		override val contrast: Int = DEFAULT_CONTRAST
	) : ColorFilterType(brightness, sharpness, contrast, ImageFilterType.DEEP_BINARIZATION) {

		companion object {
			private const val DEFAULT_BRIGHTNESS: Int = 60
			private const val DEFAULT_SHARPNESS: Int = 50
			private const val DEFAULT_CONTRAST: Int = 40
		}

		override val defaultBrightness get() = DEFAULT_BRIGHTNESS
		override val defaultSharpness get() = DEFAULT_SHARPNESS
		override val defaultContrast get() = DEFAULT_CONTRAST

		override fun create(brightness: Int, sharpness: Int, contrast: Int): ColorFilterType =
			Color(brightness, sharpness, contrast)
	}

	data class Grayscale(
		override val brightness: Int = DEFAULT_BRIGHTNESS,
		override val sharpness: Int = DEFAULT_SHARPNESS,
		override val contrast: Int = DEFAULT_CONTRAST
	) : ColorFilterType(brightness, sharpness, contrast, ImageFilterType.GRAYSCALE) {

		companion object {
			private const val DEFAULT_BRIGHTNESS: Int = 20
			private const val DEFAULT_SHARPNESS: Int = 40
			private const val DEFAULT_CONTRAST: Int = 60
		}

		override val defaultBrightness get() = DEFAULT_BRIGHTNESS
		override val defaultSharpness get() = DEFAULT_SHARPNESS
		override val defaultContrast get() = DEFAULT_CONTRAST

		override fun create(brightness: Int, sharpness: Int, contrast: Int): ColorFilterType =
			Grayscale(brightness, sharpness, contrast)
	}

	data class BlackWhite(
		override val brightness: Int = DEFAULT_BRIGHTNESS,
		override val sharpness: Int = DEFAULT_SHARPNESS,
		override val contrast: Int = DEFAULT_CONTRAST
	) : ColorFilterType(brightness, sharpness, contrast, ImageFilterType.BLACK_AND_WHITE) {

		companion object {
			private const val DEFAULT_BRIGHTNESS: Int = 60
			private const val DEFAULT_SHARPNESS: Int = 40
			private const val DEFAULT_CONTRAST: Int = 20
		}

		override val defaultBrightness get() = DEFAULT_BRIGHTNESS
		override val defaultSharpness get() = DEFAULT_SHARPNESS
		override val defaultContrast get() = DEFAULT_CONTRAST

		override fun create(brightness: Int, sharpness: Int, contrast: Int): ColorFilterType =
			BlackWhite(brightness, sharpness, contrast)
	}

	data class None(
		override val brightness: Int = DEFAULT_BRIGHTNESS,
		override val sharpness: Int = DEFAULT_SHARPNESS,
		override val contrast: Int = DEFAULT_CONTRAST
	) : ColorFilterType(brightness, sharpness, contrast, ImageFilterType.NONE) {

		companion object {
			private const val DEFAULT_BRIGHTNESS: Int = 50
			private const val DEFAULT_SHARPNESS: Int = 50
			private const val DEFAULT_CONTRAST: Int = 50
		}

		override val defaultBrightness get() = DEFAULT_BRIGHTNESS
		override val defaultSharpness get() = DEFAULT_SHARPNESS
		override val defaultContrast get() = DEFAULT_CONTRAST

		override fun create(brightness: Int, sharpness: Int, contrast: Int): ColorFilterType =
			None(brightness, sharpness, contrast)
	}
}