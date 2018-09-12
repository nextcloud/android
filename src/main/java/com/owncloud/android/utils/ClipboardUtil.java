/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Helper implementation to copy a string into the system clipboard.
 */
public final class ClipboardUtil {
    private static final String TAG = ClipboardUtil.class.getName();

    private ClipboardUtil() {
    }

    public static void copyToClipboard(Activity activity, String text) {
        copyToClipboard(activity, text, true);
    }

    public static void copyToClipboard(Activity activity, String text, boolean showToast) {
        if (text != null && text.length() > 0) {
            try {
                ClipData clip = ClipData.newPlainText(
                        activity.getString(
                                R.string.clipboard_label, activity.getString(R.string.app_name)),
                        text
                );
                ((ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(clip);

                if (showToast) {
                    Toast.makeText(activity, R.string.clipboard_text_copied, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(activity, R.string.clipboard_unexpected_error, Toast.LENGTH_SHORT).show();
                Log_OC.e(TAG, "Exception caught while copying to clipboard", e);
            }
        } else {
            Toast.makeText(activity, R.string.clipboard_no_text_to_copy, Toast.LENGTH_SHORT).show();
        }
    }
}
