package com.nmc.android.ui.onboarding

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.owncloud.android.databinding.OnboardingElementBinding

class OnBoardingPagerAdapter(val context: Context, val items: List<OnBoardingItem>) :
    PagerAdapter() {

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val binding = OnboardingElementBinding.inflate(LayoutInflater.from(context), container, false)

        //val fontColor = ResourcesCompat.getColor(requireContext().resources, R.color.login_text_color, null)
        //binding.ivOnboarding.setImageDrawable(ThemeDrawableUtils.tintDrawable(onBoardingItem?.image ?: 0, fontColor))
        //binding.tvOnboarding.setText(onBoardingItem?.content ?: 0)
        Glide.with(context)
            .load(items[position].image)
            //.override(screenWidthInDp.toInt(), screenHeightInDp.toInt())
            .into(binding.ivOnboarding)
        container.addView(binding.root, 0)
        return binding.root
    }
}