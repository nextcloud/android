/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
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

package com.owncloud.android.files.managers;

import java.util.HashMap;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.widget.RemoteViews;

import com.owncloud.android.R;

public class OCNotificationManager {

    enum NotificationType {
        NOTIFICATION_SIMPLE,
        NOTIFICATION_PROGRESS
    }
    
    static public class NotificationData {
        private String mText, mSubtitle;
        private int mPercent;
        private boolean mOngoing;

        public NotificationData(String text, String subtitle, boolean ongoing) {
            this(text, subtitle, -1, ongoing);
        }
        
        public NotificationData(int percent, boolean ongoing) {
            this(null, null, percent, ongoing);
        }
        
        public NotificationData(String text, int percent, boolean ongoing) {
            this(text, null, percent, ongoing);
        }
        
        public NotificationData(String text, String subtitle, int percent, boolean ongoing) {
            mText = text;
            mPercent = percent;
            mSubtitle = subtitle;
            mOngoing = ongoing;
        }
        
        public String getText() { return mText; }
        public int getPercent() { return mPercent; }
        public String getSubtitle() { return mSubtitle; }
        public boolean getOngoing() { return mOngoing; }
    }
    
    static private OCNotificationManager mInstance = null;

    private class NotificationTypePair {
        public Notification mNotificaiton;
        public NotificationType mType;
        public NotificationTypePair(Notification n, NotificationType type) {
            mNotificaiton = n;
            mType = type;
        }
    }
    
    private Context mContext;
    private Map<Integer, NotificationTypePair> mNotificationMap;
    private int mNotificationCounter;
    NotificationManager mNM;
    
    static OCNotificationManager getInstance(Context context) {
        if (mInstance == null)
            mInstance = new OCNotificationManager(context);
        return mInstance;
    }
    
    OCNotificationManager(Context context) {
        mContext = context;
        mNotificationMap = new HashMap<Integer, NotificationTypePair>();
        mNM = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationCounter = 0;
    }
    
    public int postNotification(NotificationType type, NotificationData data) {
        mNotificationCounter++;
        Notification notification = null;
        
        switch (type) {
            case NOTIFICATION_SIMPLE:
                notification = new Notification(R.drawable.icon, data.getText(), System.currentTimeMillis());
                break;
            case NOTIFICATION_PROGRESS:
                notification = new Notification();
                notification.contentView = new RemoteViews(mContext.getPackageName(), R.layout.progressbar_layout);
                notification.contentView.setTextViewText(R.id.status_text,
                                                         data.getText());
                notification.contentView.setImageViewResource(R.id.status_icon,
                                                              R.id.icon);
                notification.contentView.setProgressBar(R.id.status_progress,
                                                        100,
                                                        data.getPercent(),
                                                        false);
                break;
            default:
                return -1;
        }
        if (data.getOngoing()) {
            notification.flags |= notification.flags | Notification.FLAG_ONGOING_EVENT;
        }
        
        mNotificationMap.put(mNotificationCounter, new NotificationTypePair(notification, type));
        return mNotificationCounter;
    }
    
    public boolean updateNotification(int notification_id, NotificationData data) {
        if (!mNotificationMap.containsKey(notification_id)) {
            return false;
        }
        NotificationTypePair pair = mNotificationMap.get(notification_id);
        switch (pair.mType) {
            case NOTIFICATION_PROGRESS:
                pair.mNotificaiton.contentView.setProgressBar(R.id.status_text,
                                                              100,
                                                              data.getPercent(),
                                                              false);
                return true;
            case NOTIFICATION_SIMPLE:
                pair.mNotificaiton = new Notification(R.drawable.icon,
                                                      data.getText(), System.currentTimeMillis());
                mNM.notify(notification_id, pair.mNotificaiton);
                return true;
            default:
                return false;
        }
    }
    
    public void discardNotification(int notification_id) {
        mNM.cancel(notification_id);
        mNotificationMap.remove(notification_id);
    }
}
