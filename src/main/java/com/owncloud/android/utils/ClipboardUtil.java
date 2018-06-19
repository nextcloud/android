package com.owncloud.android.utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;

public class ClipboardUtil {
    private static final String TAG = ClipboardUtil.class.getName();

    private ClipboardUtil() {
    }

    public static void copyToClipboard(Activity activity, String text) {
        if (text != null && text.length() > 0) {
            try {
                ClipData clip = ClipData.newPlainText(
                        activity.getString(
                                R.string.clipboard_label, activity.getString(R.string.app_name)),
                        text
                );
                ((ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(clip);

                Toast.makeText(activity, R.string.clipboard_text_copied, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(activity, R.string.clipboard_uxexpected_error, Toast.LENGTH_SHORT).show();
                Log_OC.e(TAG, "Exception caught while copying to clipboard", e);
            }
        } else {
            Toast.makeText(activity, R.string.clipboard_no_text_to_copy, Toast.LENGTH_SHORT).show();
        }
    }
}
