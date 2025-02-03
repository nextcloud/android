/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.rearrange.recycler

import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.ionos.scanbot.databinding.ScanbotItemRearrangeBinding
import com.ionos.scanbot.entity.Picture
import com.ionos.scanbot.image_loader.ImageLoader
import com.ionos.scanbot.image_loader.ImageLoaderOptions
import com.ionos.scanbot.image_loader.ScaleType
import com.ionos.scanbot.repository.RepositoryFacade

internal class RearrangeViewHolder(
	private val viewBinding: ScanbotItemRearrangeBinding,
    private val imageLoader: ImageLoader,
	private val repositoryFacade: RepositoryFacade,
) : RecyclerView.ViewHolder(viewBinding.root) {

	fun bind(item: RearrangeItem) = with(viewBinding) {
		tvSequentialNumber.text = item.sequenceNumber.toString()
		ivRearrangePreview.loadPicture(item.picture)
	}

	private fun ImageView.loadPicture(picture: Picture) {
		repositoryFacade.readModifiedFile(picture.id)
			?.let(imageLoader::load)
			?.options(ImageLoaderOptions(ScaleType.CENTER_INSIDE))
			?.into(this)
	}
}
