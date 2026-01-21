package com.owncloud.android.ui

import android.content.Context
import android.preference.SwitchPreference
import android.util.AttributeSet
import android.view.View
import com.google.android.material.materialswitch.MaterialSwitch
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

@Suppress("DEPRECATION")
class ThemeableSwitchPreference : SwitchPreference {
    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    /**
     * Do not delete constructor. These are used.
     */
    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        MainApp.getAppComponent().inject(this)
        setWidgetLayoutResource(R.layout.themeable_switch)
    }

    @Deprecated("Deprecated in Java")
    override fun onBindView(view: View) {
        super.onBindView(view)
        val checkable = view.findViewById<View>(R.id.switch_widget)
        if (checkable is MaterialSwitch) {
            checkable.setChecked(isChecked)
            viewThemeUtils.material.colorMaterialSwitch(checkable)
        }
    }
}
