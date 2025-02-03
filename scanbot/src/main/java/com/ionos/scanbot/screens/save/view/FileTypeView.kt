/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.save.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RadioGroup
import androidx.annotation.IdRes
import com.ionos.scanbot.R
import com.ionos.scanbot.screens.save.SaveScreen.FileType

internal class FileTypeView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
) : RadioGroup(context, attrs) {

	init {
		orientation = HORIZONTAL
		LayoutInflater.from(context).inflate(R.layout.scanbot_view_save_file_type, this)
		check(R.id.choice_pdf_ocr)
	}

	fun setOnFileTypeChangedListener(listener: (FileType) -> Unit) {
		setOnCheckedChangeListener { _, checkedId -> listener.invoke(getCheckedFileType(checkedId)) }
	}

	fun setCheckedFileType(fileType: FileType) {
		if (getCheckedFileType() != fileType) {
			check(fileType)
		}
	}

	private fun getCheckedFileType(): FileType {
		return getCheckedFileType(checkedRadioButtonId)
	}

	private fun getCheckedFileType(@IdRes checkedId: Int): FileType = when (checkedId) {
		R.id.choice_pdf_ocr -> FileType.PDF_OCR
		R.id.choice_pdf -> FileType.PDF
		R.id.choice_jpg -> FileType.JPG
		R.id.choice_png -> FileType.PNG
		else -> throw IllegalStateException()
	}

	private fun check(fileType: FileType) = when (fileType) {
		FileType.PDF_OCR -> check(R.id.choice_pdf_ocr)
		FileType.PDF -> check(R.id.choice_pdf)
		FileType.JPG -> check(R.id.choice_jpg)
		FileType.PNG -> check(R.id.choice_png)
	}
}
