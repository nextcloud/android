/**
 *   ownCloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2015 Bartosz Przybylski
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

package com.owncloud.android.features;

import android.os.Parcel;
import android.os.Parcelable;

import com.owncloud.android.BuildConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Bartosz Przybylski
 */
public class FeatureList {

    static final private boolean SHOW_ON_FIRST_RUN = true;
    static final private boolean SHOW_ON_UPGRADE = false;


    static final private FeatureItem featuresList[] = {
            // Basic features showed on first install
            new FeatureItem(R.drawable.what_new_instant_upload, R.string.welcome_feature_1_title, R.string.welcome_feature_1_text, "1.0.0", "0", SHOW_ON_FIRST_RUN),
            new FeatureItem(R.drawable.whats_new_files, R.string.welcome_feature_2_title,  R.string.welcome_feature_2_text, "1.0.0", "0", SHOW_ON_FIRST_RUN),
            new FeatureItem(R.drawable.whats_new_share, R.string.welcome_feature_3_title,  R.string.welcome_feature_3_text, "1.0.0", "0" ,SHOW_ON_FIRST_RUN),
            new FeatureItem(R.drawable.whats_new_accounts, R.string.welcome_feature_4_title,  R.string.welcome_feature_4_text, "1.0.0", "0", SHOW_ON_FIRST_RUN),
            new FeatureItem(R.drawable.whats_new_oc_files, R.string.welcome_feature_5_title,  FeatureItem.DO_NOT_SHOW, "1.0.0", "0", SHOW_ON_FIRST_RUN),
            // Features introduced in certain point in time
    };


    static public FeatureItem[] get() {
        return featuresList;
    }

    static public FeatureItem[] getFiltered(final int lastSeenVersionCode, final boolean isFirstRun, boolean isBeta) {
        List<FeatureItem> features = new LinkedList<>();

        for (FeatureItem item : get()) {
            final int itemVersionCode = isBeta ? item.getBetaVersionNumber() : item.getVersionNumber();
            if (isFirstRun && item.shouldShowOnFirstRun()) {
                features.add(item);
            } else if (!isFirstRun && !item.shouldShowOnFirstRun() &&
                    MainApp.getVersionCode() >= itemVersionCode &&
                    lastSeenVersionCode < itemVersionCode) {
                features.add(item);
            }
        }
        return features.toArray(new FeatureItem[features.size()]);
    }

    static public class FeatureItem implements Parcelable {
        public static final int DO_NOT_SHOW = -1;
        private int image;
        private int titleText;
        private int contentText;
        private int versionNumber;
        private int betaVersion;
        private boolean showOnInitialRun;

        public FeatureItem(int image, int titleText, int contentText, String version, String betaVersion) {
            this(image, titleText, contentText, version, betaVersion, false);
        }

        public FeatureItem(int image, int titleText, int contentText, String version, String betaVersion, boolean showOnInitialRun) {
            this.image = image;
            this.titleText = titleText;
            this.contentText = contentText;
            this.versionNumber = versionCodeFromString(version);
            this.betaVersion = Integer.parseInt(betaVersion);
            this.showOnInitialRun = showOnInitialRun;
        }

        public boolean shouldShowImage() { return image != DO_NOT_SHOW; }
        public int getImage() { return image; }

        public boolean shouldShowTitleText() { return titleText != DO_NOT_SHOW; }
        public int getTitleText() { return titleText; }

        public boolean shouldShowContentText() { return contentText != DO_NOT_SHOW; }
        public int getContentText() { return contentText; }

        public int getVersionNumber() { return versionNumber; }
        public int getBetaVersionNumber() { return betaVersion; }
        public boolean shouldShowOnFirstRun() { return showOnInitialRun; }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(image);
            dest.writeInt(titleText);
            dest.writeInt(contentText);
            dest.writeInt(versionNumber);
            dest.writeInt(betaVersion);
            dest.writeByte((byte) (showOnInitialRun ? 1 : 0));
        }

        private FeatureItem(Parcel p) {
            image = p.readInt();
            titleText = p.readInt();
            contentText = p.readInt();
            versionNumber = p.readInt();
            betaVersion = p.readInt();
            showOnInitialRun = p.readByte() == 1;
        }
        public static final Parcelable.Creator CREATOR =
                new Parcelable.Creator() {

                    @Override
                    public Object createFromParcel(Parcel source) {
                        return new FeatureItem(source);
                    }

                    @Override
                    public Object[] newArray(int size) {
                        return new FeatureItem[size];
                    }
                };
    }

    static int versionCodeFromString(String version) {
        String v[] = version.split(Pattern.quote("."));
        if (v.length != 3) {
            Log_OC.wtf("FeatureList", "Version string is incorrect " + version);
            return 0;
        }
        int result = Integer.parseInt(v[0])*(int)(10e6) +
                Integer.parseInt(v[1])*(int)(10e4) +
                Integer.parseInt(v[2])*100;

        return result;
    }
}
