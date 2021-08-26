package com.nmc.android.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.viewpager.widget.ViewPager
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.databinding.ActivityOnboardingBinding
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import javax.inject.Inject

class OnBoardingActivity : BaseActivity() {

    companion object {
        private val IMAGES =
            arrayOf(R.drawable.first_run_files, R.drawable.first_run_groupware, R.drawable.first_run_talk)
        private val CONTENT = arrayOf(R.string.first_run_2_text, R.string.first_run_3_text, R.string.first_run_4_text)
        private fun getOnBoardingItems(): List<OnBoardingItem> {
            val onBoardingItems = mutableListOf<OnBoardingItem>()
            for (i in IMAGES.indices) {
                val onBoardingItem = OnBoardingItem(IMAGES[i], CONTENT[i])
                onBoardingItems.add(onBoardingItem)
            }
            return onBoardingItems
        }

        fun launchOnBoardingActivity(context: Context) {
            val intent = Intent(context, OnBoardingActivity::class.java)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityOnboardingBinding

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = OnBoardingPagerAdapter(supportFragmentManager, getOnBoardingItems())
        binding.progressIndicator.setNumberOfSteps(IMAGES.size)
        binding.viewPagerOnboarding.adapter = adapter

        binding.btnOnboardingLogin.setOnClickListener {
            appPreferences.onBoardingComplete = true
            startActivity(Intent(this, AuthenticatorActivity::class.java))
            finish()
        }

        binding.viewPagerOnboarding.addOnPageChangeListener(object : ViewPager.OnPageChangeListener{
            override fun onPageSelected(position: Int) {
                binding.progressIndicator.animateToStep(position + 1)
            }

            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }
        })
    }
}