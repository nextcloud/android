/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui.pager

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.util.rotate
import com.owncloud.android.R

private const val NO_SHIFT = -1

class PlayerPager @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    private val viewPager: ViewPager
    private lateinit var adapter: PlayerPagerAdapter
    private var shift = NO_SHIFT
    private var restoredShift = NO_SHIFT

    var onItemSelected: ((PlaybackFile) -> Unit)? = null

    private val onPageChangeListener = object : OnPageChangeListener {
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) = Unit

        override fun onPageSelected(position: Int) {
            if (position == 0) {
                viewPager.setCurrentItem(adapter.count - 2, false)
                return
            }
            if (position >= adapter.count - 1) {
                viewPager.setCurrentItem(1, false)
                return
            }
            onItemSelected?.invoke(adapter.getEntityForPosition(position))
        }

        override fun onPageScrollStateChanged(state: Int) = Unit
    }

    init {
        inflate(context, R.layout.player_pager, this)
        viewPager = findViewById(R.id.viewPager)
    }

    fun initialize(fragmentManager: FragmentManager, createFragment: (PlaybackFile) -> Fragment) {
        adapter = PlayerPagerAdapter(fragmentManager, createFragment)
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
        if (position != -1 && viewPager.currentItem != position) {
            viewPager.removeOnPageChangeListener(onPageChangeListener)
            viewPager.setCurrentItem(position, true)
            viewPager.addOnPageChangeListener(onPageChangeListener)
        }
    }

    private fun notifyDataSetChangedWithoutCallingListener() {
        viewPager.removeOnPageChangeListener(onPageChangeListener)
        adapter.notifyDataSetChanged()
        viewPager.addOnPageChangeListener(onPageChangeListener)
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
