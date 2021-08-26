package com.nmc.android.ui.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class OnBoardingPagerAdapter(fragmentManager: FragmentManager, val items: List<OnBoardingItem>) :
    FragmentStatePagerAdapter
        (fragmentManager) {
    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Fragment {
        return OnBoardingElementFragment.newInstance(items[position])
    }
}