/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.notifications;

import com.owncloud.android.R;

import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Extends the support class {@link NotificationCompat.Builder} to grant that
 * a progress bar is available in every Android version, because 
 * {@link NotificationCompat.Builder#setProgress(int, int, boolean)} has no
 * real effect for Android < 4.0
 */
public class NotificationBuilderWithProgressBar extends NotificationCompat.Builder {

    /**
     * Custom view to replace the original layout of the notifications
     */
    private RemoteViews mContentView = null;
    
    /**
     * Fatory method.
     * 
     * Instances of this class will be only returned in Android versions needing it.
     * 
     * @param context       Context that will use the builder to create notifications
     * @return              An instance of this class, or of the regular 
     *                      {@link NotificationCompat.Builder}, when it is good enough.
     */
    public static NotificationCompat.Builder newNotificationBuilderWithProgressBar(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return new NotificationBuilderWithProgressBar(context); 
        } else {
            return new NotificationCompat.Builder(context).
                    setColor(context.getResources().getColor(R.color.primary));
        }
    }
    
    /**
     * Constructor.
     * 
     * @param context       Context that will use the builder to create notifications.
     */
    private NotificationBuilderWithProgressBar(Context context) {
        super(context);
        mContentView = new RemoteViews(context.getPackageName(), R.layout.notification_with_progress_bar);
        setContent(mContentView);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationCompat.Builder setProgress(int max, int progress, boolean indeterminate) {
        mContentView.setProgressBar(R.id.progress, max, progress, indeterminate);
        if (max > 0) {
            mContentView.setViewVisibility(R.id.progressHolder, View.VISIBLE);
        } else {
            mContentView.setViewVisibility(R.id.progressHolder, View.GONE);
        }
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationCompat.Builder setSmallIcon(int icon) {
        super.setSmallIcon(icon);   // necessary
        mContentView.setImageViewResource(R.id.icon, icon);
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationCompat.Builder setContentTitle(CharSequence title) {
        super.setContentTitle(title);
        mContentView.setTextViewText(R.id.title, title);
        return this;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public NotificationCompat.Builder setContentText(CharSequence text) {
        super.setContentText(text);
        mContentView.setTextViewText(R.id.text, text);
        if (text != null && text.length() > 0) {
            mContentView.setViewVisibility(R.id.text, View.VISIBLE);
        } else {
            mContentView.setViewVisibility(R.id.text, View.GONE);
        }
        return this;
    }

    @Override
    public Notification build() {
        Notification result = super.build();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // super.build() in Android 2.x totally ruins whatever was made #setContent 
            result.contentView = mContentView;
        }
        return result;
    }
    
    
}
