/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui.pager

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.widget.ViewPager2
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.util.rotate
import com.owncloud.android.R

private const val NO_SHIFT = -1
private const val NO_POSITION = -1
private const val FIRST_ENTITY_POSITION = 1

private const val OFFSCREEN_PAGE_LIMIT = 1

class PlayerPager @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    @SuppressLint("WrongConstant")
    private val viewPager = ViewPager2(context).apply {
        id = R.id.player_view_pager
        offscreenPageLimit = OFFSCREEN_PAGE_LIMIT
    }
    private lateinit var adapter: PlayerPagerAdapter
    private var shift = NO_SHIFT
    private var restoredShift = NO_SHIFT

    var onItemSelected: ((PlaybackFile) -> Unit)? = null

    private val lastStubPosition get() = adapter.itemCount - 1

    private val lastEntityPosition get() = adapter.itemCount - 2

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            if (isStubPosition(position)) return
            onItemSelected?.invoke(adapter.getEntityForPosition(position))
        }

        override fun onPageScrollStateChanged(state: Int) {
            if (state != ViewPager2.SCROLL_STATE_IDLE) return
            wrapAroundStubPosition()
        }
    }

    init {
        addView(viewPager, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun initialize(fragmentManager: FragmentManager, lifecycle: Lifecycle, createFragment: (PlaybackFile) -> Fragment) {
        adapter = PlayerPagerAdapter(fragmentManager, lifecycle, createFragment)
        viewPager.adapter = adapter
    }

    override fun onSaveInstanceState(): Parcelable {
        val state = PlayerPagerState(super.onSaveInstanceState())
        state.shiftedPosition = shift
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val restoredState = state as PlayerPagerState
        super.onRestoreInstanceState(restoredState.superState)
        restoredShift = restoredState.shiftedPosition
    }

    fun getItems(): List<PlaybackFile> = adapter.getEntities()

    fun setItems(items: List<PlaybackFile>) {
        adapter.setEntities(if (restoredShift != NO_SHIFT) shiftRestoredPosition(items) else items)
        notifyDataSetChangedWithoutCallingListener()
    }

    fun setCurrentItem(item: PlaybackFile) {
        val position = adapter.getEntityIndex(item)
        if (position != NO_POSITION && viewPager.currentItem != position) {
            viewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
            viewPager.setCurrentItem(position, true)
            viewPager.registerOnPageChangeCallback(onPageChangeCallback)
        }
    }

    private fun isStubPosition(position: Int): Boolean = position == 0 || position >= lastStubPosition

    private fun wrapAroundStubPosition() {
        if (!adapter.isPadded()) return

        when {
            viewPager.currentItem == 0 -> viewPager.setCurrentItem(lastEntityPosition, false)
            viewPager.currentItem >= lastStubPosition -> viewPager.setCurrentItem(FIRST_ENTITY_POSITION, false)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun notifyDataSetChangedWithoutCallingListener() {
        viewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
        adapter.notifyDataSetChanged()
        viewPager.registerOnPageChangeCallback(onPageChangeCallback)
    }

    private fun shiftRestoredPosition(items: List<PlaybackFile>): List<PlaybackFile> {
        shift = restoredShift
        restoredShift = NO_SHIFT
        return items.rotate(shift)
    }

    class PlayerPagerState : BaseSavedState {
        var shiftedPosition: Int = 0

        constructor(superState: Parcelable?) : super(superState)

        constructor(parcel: Parcel) : super(parcel) {
            shiftedPosition = parcel.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(shiftedPosition)
        }

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<PlayerPagerState> {

                override fun createFromParcel(parcel: Parcel): PlayerPagerState = PlayerPagerState(parcel)

                override fun newArray(size: Int): Array<PlayerPagerState?> = arrayOfNulls(size)
            }
        }
    }
}
