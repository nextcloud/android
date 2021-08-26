package com.nmc.android.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.owncloud.android.R
import com.owncloud.android.databinding.OnboardingElementBinding
import com.owncloud.android.utils.theme.ThemeDrawableUtils

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
        val fontColor = ResourcesCompat.getColor(requireContext().resources, R.color.login_text_color, null)
        binding.ivOnboarding.setImageDrawable(ThemeDrawableUtils.tintDrawable(onBoardingItem?.image ?: 0, fontColor))
        binding.tvOnboarding.setText(onBoardingItem?.content ?: 0)
    }
}