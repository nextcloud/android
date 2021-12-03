package com.nmc.android.ui.onboarding

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.viewpager.widget.ViewPager
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.databinding.ActivityOnboardingBinding
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.utils.DisplayUtils
import javax.inject.Inject

class OnBoardingActivity : BaseActivity() {

    companion object {
        private val DEFAULT_IMAGES =
            arrayOf(
                R.drawable.intro_screen_first, R.drawable.intro_screen_second, R.drawable
                    .intro_screen_third
            )
        private val TAB_PORT_IMAGES =
            arrayOf(
                R.drawable.intro_screen_first_port_tab, R.drawable.intro_screen_second_port_tab, R.drawable
                    .intro_screen_third_port_tab
            )
        private val TAB_LAND_IMAGES =
            arrayOf(
                R.drawable.intro_screen_first_land_tab, R.drawable.intro_screen_second_land_tab, R.drawable
                    .intro_screen_third_land_tab
            )
        private val CONTENT = arrayOf(R.string.first_run_2_text, R.string.first_run_3_text, R.string.first_run_4_text)
        private fun getOnBoardingItems(): List<OnBoardingItem> {
            val onBoardingItems = mutableListOf<OnBoardingItem>()
            val onBoardingImages = getOnBoardingImages()
            for (i in onBoardingImages.indices) {
                val onBoardingItem = OnBoardingItem(onBoardingImages[i], CONTENT[i])
                onBoardingItems.add(onBoardingItem)
            }
            return onBoardingItems
        }

        private fun getOnBoardingImages(): Array<Int> {
            return if (DisplayUtils.isTablet()) {
                if (DisplayUtils.isLandscapeOrientation()) {
                    TAB_LAND_IMAGES
                } else {
                    TAB_PORT_IMAGES
                }
            } else {
                DEFAULT_IMAGES
            }
        }

        fun launchOnBoardingActivity(context: Context) {
            val intent = Intent(context, OnBoardingActivity::class.java)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityOnboardingBinding
    private var selectedPosition = 0

    @Inject
    lateinit var appPreferences: AppPreferences

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //if device is not tablet then we have to lock it to Portrait mode
        //as we don't have images for that
        if (!DisplayUtils.isTablet()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateOnBoardingPager(selectedPosition)

        binding.btnOnboardingLogin.setOnClickListener {
            appPreferences.onBoardingComplete = true
            startActivity(Intent(this, AuthenticatorActivity::class.java))
            finish()
        }

        binding.viewPagerOnboarding.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageSelected(position: Int) {

                //-1 to position because this position doesn't start from 0
                selectedPosition = position - 1
                
                //pass directly the position here because this position will doesn't start from 0
                binding.progressIndicator.animateToStep(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }
        })
    }

    private fun updateOnBoardingPager(selectedPosition: Int) {
        val onBoardingItemList = getOnBoardingItems()
        val adapter = OnBoardingPagerAdapter(this, onBoardingItemList)
        binding.progressIndicator.setNumberOfSteps(onBoardingItemList.size)
        binding.viewPagerOnboarding.adapter = adapter
        binding.viewPagerOnboarding.currentItem = selectedPosition
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOnBoardingPager(selectedPosition)
    }
}