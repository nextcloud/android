package com.nmc.android.ui.onboarding

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.android.parcel.Parcelize

@Parcelize
data class OnBoardingItem(
    @DrawableRes
    val image: Int,
    @StringRes
    val content: Int
) : Parcelable
