/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui

import android.content.Context
import android.util.AttributeSet
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.materialswitch.MaterialSwitch
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class ThemeableSwitchPreference : SwitchPreferenceCompat {
    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        MainApp.getAppComponent().inject(this)
        widgetLayoutResource = R.layout.themeable_switch
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val switchWidget = holder.findViewById(R.id.switch_widget) as? MaterialSwitch ?: return
        switchWidget.isChecked = isChecked
        viewThemeUtils.material.colorMaterialSwitch(switchWidget)
    }
}
