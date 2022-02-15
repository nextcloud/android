package com.owncloud.android.ui.components;

/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

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