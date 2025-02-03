/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.gallery.pager

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import com.ionos.scanbot.entity.Picture
import com.ionos.scanbot.image_loader.ImageLoader
import com.ionos.scanbot.image_loader.ImageLoaderOptions
import com.ionos.scanbot.image_loader.ScaleType
import com.ionos.scanbot.repository.RepositoryFacade
import javax.inject.Inject

internal class GalleryPagerAdapter @Inject constructor(
	private val imageLoader: ImageLoader,
	private val repositoryFacade: RepositoryFacade,
) : PagerAdapter() {

	private val pictures: MutableList<Picture> = mutableListOf()

	internal fun setPictures(pictures: List<Picture>) {
		this.pictures.clear()
		this.pictures.addAll(pictures)
		notifyDataSetChanged()
	}

	override fun isViewFromObject(view: View, item: Any): Boolean {
		return item is GalleryPagerItem && item.imageView == view
	}

	override fun getCount(): Int = pictures.size

	override fun getItemPosition(item: Any): Int {
		return if (item is GalleryPagerItem) {
			val index = pictures.indexOf(item.picture)
			if (index > -1) index else POSITION_NONE
		} else {
			super.getItemPosition(item)
		}
	}

	override fun instantiateItem(container: ViewGroup, position: Int): GalleryPagerItem {
		val imageView = ImageView(container.context)
		val item = GalleryPagerItem(pictures[position], imageView)
		container.addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
		imageView.loadPicture(pictures[position])
		return item
	}

	override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
		if (item is GalleryPagerItem) {
			container.removeView(item.imageView)
		}
	}

	private fun ImageView.loadPicture(picture: Picture) {
        repositoryFacade.readModifiedFile(picture.id)
			?.let(imageLoader::load)
			?.options(ImageLoaderOptions(ScaleType.CENTER_INSIDE))
			?.into(this)
	}
}
