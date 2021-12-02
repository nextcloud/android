package com.nmc.android.ui.onboarding

import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.owncloud.android.databinding.OnboardingElementBinding
import com.owncloud.android.utils.DisplayUtils

class OnBoardingElementFragment : Fragment() {

    companion object {
        private const val ARG_ONBOARDING_ITEM = "on_boarding_item"
        fun newInstance(onBoardingItem: OnBoardingItem): OnBoardingElementFragment {
            val args = Bundle()
            args.putParcelable(ARG_ONBOARDING_ITEM, onBoardingItem)
            val fragment = OnBoardingElementFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var binding: OnboardingElementBinding
    private var onBoardingItem: OnBoardingItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            onBoardingItem = it.getParcelable(ARG_ONBOARDING_ITEM)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = OnboardingElementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //val fontColor = ResourcesCompat.getColor(requireContext().resources, R.color.login_text_color, null)
        //binding.ivOnboarding.setImageDrawable(ThemeDrawableUtils.tintDrawable(onBoardingItem?.image ?: 0, fontColor))
        //binding.tvOnboarding.setText(onBoardingItem?.content ?: 0)
        val metrics = requireContext().resources.displayMetrics
        val screenWidthInDp = DisplayUtils.convertPixelsToDp(metrics.widthPixels.toFloat(), requireContext())
        val screenHeightInDp = DisplayUtils.convertPixelsToDp(metrics.heightPixels.toFloat(), requireContext())
        Glide.with(requireContext())
            .load(onBoardingItem?.image)
            .skipMemoryCache(true)
            //.override(screenWidthInDp.toInt(), screenHeightInDp.toInt())
            .into(binding.ivOnboarding)
    }
}