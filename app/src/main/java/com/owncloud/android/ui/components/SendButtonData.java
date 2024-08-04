/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.components;

import android.graphics.drawable.Drawable;

public class SendButtonData {
    private Drawable drawable;
    private CharSequence title;
    private String packageName;
    private String activityName;

    public SendButtonData(Drawable drawable, CharSequence title, String packageName, String activityName) {
        this.drawable = drawable;
        this.title = title;
        this.packageName = packageName;
        this.activityName = activityName;
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public CharSequence getTitle() {
        return title;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getActivityName() {
        return activityName;
    }
}