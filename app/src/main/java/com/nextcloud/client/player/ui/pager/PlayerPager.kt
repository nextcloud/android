/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.ui.pager

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.nextcloud.client.player.ui.pager.adapter.AbstractFragmentPagerAdapter
import com.nextcloud.client.player.ui.pager.adapter.DefaultFragmentPagerAdapter
import com.nextcloud.client.player.ui.pager.adapter.InfiniteFragmentPagerAdapter
import com.nextcloud.client.player.util.calculateShift
import com.nextcloud.client.player.util.rotate
import com.owncloud.android.R

class PlayerPager<T> @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {
    private val viewPager: ViewPager
    private lateinit var modeStrategy: ModeStrategy<T>
    private lateinit var adapter: AbstractFragmentPagerAdapter<T>
    private lateinit var onPageChangeListener: OnPageChangeListener
    private var playerPagerListener: PlayerPagerListener<T>? = null
    private var currentPosition = -1
    private var shift = -1
    private var restoredShift = -1

    init {
        inflate(context, R.layout.player_pager, this)
        viewPager = findViewById<ViewPager>(R.id.viewPager)
    }

    fun initialize(
        fragmentManager: FragmentManager,
        mode: PlayerPagerMode,
        fragmentFactory: PlayerPagerFragmentFactory<T>
    ) {
        modeStrategy = createModeStrategy(mode)
        adapter = modeStrategy.createAdapter(fragmentManager, fragmentFactory)
        viewPager.setAdapter(adapter)
        onPageChangeListener = modeStrategy.createListener()
    }

    private fun createModeStrategy(mode: PlayerPagerMode): ModeStrategy<T> = when (mode) {
        PlayerPagerMode.DEFAULT -> FiniteModeStrategy()
        PlayerPagerMode.INFINITE -> InfiniteModeStrategy()
    }

    fun setPlayerPagerListener(playerPagerListener: PlayerPagerListener<T>?) {
        this.playerPagerListener = playerPagerListener
    }

    override fun onSaveInstanceState(): Parcelable {
        val state = super.onSaveInstanceState()
        val infiniteViewPagerState = InfiniteViewPagerState(state)
        infiniteViewPagerState.shiftedPosition = shift
        return infiniteViewPagerState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val restoredState: InfiniteViewPagerState = state as InfiniteViewPagerState
        super.onRestoreInstanceState(restoredState.superState)
        restoredShift = restoredState.shiftedPosition
    }

    fun getItems(): List<T> = adapter.getEntities()

    fun setItems(items: List<T>) {
        var items = if (restoredShift != -1) shiftRestoredPosition(items) else items

        val calculatedCurrentPositionWithOffsetIfNeeded =
            modeStrategy.getCurrentPosition(adapter.count, currentPosition)

        var currentItem: T? = null
        if (calculatedCurrentPositionWithOffsetIfNeeded >= 0 &&
            currentItemPositionsNotTheSameAfterShuffleMatch(calculatedCurrentPositionWithOffsetIfNeeded)
        ) {
            currentItem = adapter.getEntities()[calculatedCurrentPositionWithOffsetIfNeeded]
            items = calculateShiftAndRotateList(items, calculatedCurrentPositionWithOffsetIfNeeded, currentItem)
        }

        adapter.setEntities(items)
        if (currentItem != null) {
            adapter.setCurrentEntity(if (!items.isEmpty()) currentItem else null)
        }

        notifyDataSetChangedWithoutCallingListener()
        setCurrentItem(currentItem, false)
    }

    private fun currentItemPositionsNotTheSameAfterShuffleMatch(calculatedCurrentPosition: Int): Boolean =
        adapter.getEntities().isEmpty() &&
            this.currentPosition >= 0 &&
            calculatedCurrentPosition < adapter.getEntities().size

    private fun calculateShiftAndRotateList(
        items: List<T>,
        calculatedCurrentPositionWithOffsetForInfinityStrategy: Int,
        currentItem: T?
    ): List<T> {
        shift = items.calculateShift(calculatedCurrentPositionWithOffsetForInfinityStrategy, currentItem)
        return items.rotate(shift)
    }

    private fun notifyDataSetChangedWithoutCallingListener() {
        viewPager.removeOnPageChangeListener(onPageChangeListener)
        adapter.notifyDataSetChanged()
        viewPager.addOnPageChangeListener(onPageChangeListener)
    }

    private fun shiftRestoredPosition(items: List<T>): List<T> {
        shift = restoredShift
        restoredShift = -1
        return items.rotate(shift)
    }

    fun setCurrentItem(item: T?) {
        setCurrentItem(item, true)
    }

    private fun setCurrentItem(item: T?, smoothScroll: Boolean) {
        currentPosition = item?.let(adapter::getEntityIndex) ?: -1
        if (currentPosition != -1 && viewPager.currentItem != currentPosition) {
            viewPager.removeOnPageChangeListener(onPageChangeListener)
            viewPager.setCurrentItem(currentPosition, smoothScroll)
            viewPager.addOnPageChangeListener(onPageChangeListener)
        }
    }

    private inner class DefaultOnPageChangeListener : OnPageChangeListener {
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) = Unit

        override fun onPageSelected(position: Int) {
            playerPagerListener?.onSwitchToItem(adapter.getEntityForPosition(position))
        }

        override fun onPageScrollStateChanged(state: Int) = Unit
    }

    private inner class InfinityOnPageChangeListener : OnPageChangeListener {
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
            playerPagerListener?.onSwitchToItem(adapter.getEntityForPosition(position))
        }

        override fun onPageScrollStateChanged(state: Int) = Unit
    }

    private interface ModeStrategy<T> {
        fun createAdapter(
            fragmentManager: FragmentManager,
            fragmentFactory: PlayerPagerFragmentFactory<T>
        ): AbstractFragmentPagerAdapter<T>

        fun createListener(): OnPageChangeListener

        fun getCurrentPosition(itemCount: Int, position: Int): Int
    }

    private inner class FiniteModeStrategy : ModeStrategy<T> {
        override fun createAdapter(
            fragmentManager: FragmentManager,
            fragmentFactory: PlayerPagerFragmentFactory<T>
        ): AbstractFragmentPagerAdapter<T> = DefaultFragmentPagerAdapter(fragmentManager, fragmentFactory)

        override fun createListener(): OnPageChangeListener = DefaultOnPageChangeListener()

        override fun getCurrentPosition(itemCount: Int, position: Int): Int = position
    }

    private inner class InfiniteModeStrategy : ModeStrategy<T> {
        override fun createAdapter(
            fragmentManager: FragmentManager,
            fragmentFactory: PlayerPagerFragmentFactory<T>
        ): AbstractFragmentPagerAdapter<T> = InfiniteFragmentPagerAdapter(fragmentManager, fragmentFactory)

        override fun createListener(): OnPageChangeListener = InfinityOnPageChangeListener()

        override fun getCurrentPosition(itemCount: Int, position: Int): Int =
            if (itemCount > 1) position - 1 else position
    }

    class InfiniteViewPagerState : BaseSavedState {
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
            val CREATOR = object : Parcelable.Creator<InfiniteViewPagerState> {

                override fun createFromParcel(parcel: Parcel): InfiniteViewPagerState = InfiniteViewPagerState(parcel)

                override fun newArray(size: Int): Array<InfiniteViewPagerState?> = arrayOfNulls(size)
            }
        }
    }
}
