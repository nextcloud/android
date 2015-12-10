package com.owncloud.android.ui.dialog;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDialog;
import android.util.AttributeSet;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.lang.reflect.Method;

public class OwnCloudListPreference extends ListPreference {
    private static final String TAG = OwnCloudListPreference.class.getSimpleName();

    private Context mContext;
    private AppCompatDialog mDialog;

    public OwnCloudListPreference(Context context) {
        super(context);
        this.mContext = context;
    }

    public OwnCloudListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public OwnCloudListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public OwnCloudListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void showDialog(Bundle state) {
        if (getEntries() == null || getEntryValues() == null) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array.");
        }

        int preselect = findIndexOfValue(getValue());

        // same thing happens for the Standard ListPreference though
        android.support.v7.app.AlertDialog.Builder builder =
                new android.support.v7.app.AlertDialog.Builder(mContext, R.style.ownCloud_AlertDialog)
                .setTitle(getDialogTitle())
                .setIcon(getDialogIcon())
                .setSingleChoiceItems(getEntries(), preselect, this);

        PreferenceManager pm = getPreferenceManager();
        try {
            Method method = pm.getClass().getDeclaredMethod(
                    "registerOnActivityDestroyListener",
                    PreferenceManager.OnActivityDestroyListener.class);
            method.setAccessible(true);
            method.invoke(pm, this);
        } catch (Exception e) {
            // no way to handle this but logging it
            Log_OC.e(TAG, "error invoking registerOnActivityDestroyListener", e);
        }

        mDialog = builder.create();
        if (state != null) {
            mDialog.onRestoreInstanceState(state);
        }
        mDialog.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which >= 0 && getEntryValues() != null) {
            String value = getEntryValues()[which].toString();
            if (callChangeListener(value)) {
                setValue(value);

                // Workaround for pre kitkat since they don't support change listener within setValue
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    setSummary(getEntries()[which]);
                }
            }
            dialog.dismiss();
        }
    }

    @Override
    public AppCompatDialog getDialog() {
        return mDialog;
    }

    @Override
    public void onActivityDestroy() {
        super.onActivityDestroy();
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }
}
