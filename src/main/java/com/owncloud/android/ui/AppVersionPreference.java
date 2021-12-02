package com.owncloud.android.ui;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeTextUtils;

public class AppVersionPreference extends Preference {

    private static final String TAG = AppVersionPreference.class.getSimpleName();

    public AppVersionPreference(Context context) {
        super(context);
    }

    public AppVersionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AppVersionPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View v = super.getView(convertView, parent);
        updatePreferenceView(v.findViewById(R.id.title), v.findViewById(R.id.summary));
        return v;
    }

    private void updatePreferenceView(TextView title, TextView summary) {
        String appVersion = getAppVersion();
        int accentColor = ThemeColorUtils.appBarPrimaryFontColor(getContext());
        title.setText(ThemeTextUtils.getColoredTitle(getContext().getString(R.string.app_name),
                                                     accentColor));
        // String buildNumber = getContext().getResources().getString(R.string.buildNumber);

        // if (TextUtils.isEmpty(buildNumber)) {
        summary.setText(String.format(getContext().getString(R.string.about_version), appVersion));
       /* }
        else {
            summary.setText(String.format(getContext().getString(R.string.about_version_with_build),
                                          appVersion,
                                          buildNumber));
        }*/
    }

    private String getAppVersion() {
        String temp;
        try {
            PackageInfo pkg = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            temp = pkg.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            temp = "";
            Log_OC.e(TAG, "Error while showing about dialog", e);
        }
        return temp;
    }

}
