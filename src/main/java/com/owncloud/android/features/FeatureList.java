/*
 *   Nextcloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2015 Bartosz Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
 *   Copyright (C) 2016 Nextcloud.
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.features;

import android.os.Parcel;
import android.os.Parcelable;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Bartosz Przybylski
 */
public class FeatureList {
    private static final boolean SHOW_ON_FIRST_RUN = true;
    private static final boolean SHOW_ON_UPGRADE = false;

    private static final int VERSION_1_0_0 = 10000099;
    private static final int VERSION_3_0_0 = 30000099;
    private static final int BETA_VERSION_0 = 0;

    static public ArrayList<FeatureItem> get(boolean isMultiAccount) {
        ArrayList<FeatureItem> featuresList = new ArrayList<>();
        // Basic features showed on first install
        featuresList.add(new FeatureItem(R.drawable.whats_new_files,
                R.string.welcome_feature_1_title, R.string.welcome_feature_1_text,
                VERSION_1_0_0, BETA_VERSION_0, SHOW_ON_FIRST_RUN, true, false));
        if (isMultiAccount) {
            featuresList.add(new FeatureItem(R.drawable.whats_new_accounts,
                    R.string.welcome_feature_2_title, R.string.welcome_feature_2_text,
                    VERSION_1_0_0, BETA_VERSION_0, SHOW_ON_FIRST_RUN, true, false));
        }
        featuresList.add(new FeatureItem(R.drawable.whats_new_auto_upload,
                R.string.welcome_feature_3_title, R.string.welcome_feature_3_text,
                VERSION_1_0_0, BETA_VERSION_0, SHOW_ON_FIRST_RUN, true, false));

        // 3.0.0
        featuresList.add(new FeatureItem(R.drawable.whats_new_end_to_end_encryption,
                R.string.whats_new_end_to_end_encryption_title, R.string.whats_new_end_to_end_encryption_content,
                VERSION_3_0_0, BETA_VERSION_0, SHOW_ON_UPGRADE, false, false));
        featuresList.add(new FeatureItem(R.drawable.whats_new_resized_images, R.string.whats_new_resized_images_title,
                R.string.whats_new_resized_images_content, VERSION_3_0_0, BETA_VERSION_0, SHOW_ON_UPGRADE,
                false, false));
        featuresList.add(new FeatureItem(R.drawable.whats_new_ipv6, R.string.whats_new_ipv6_title,
                R.string.whats_new_ipv6_content, VERSION_3_0_0,
                BETA_VERSION_0, SHOW_ON_UPGRADE, false, false));

        return featuresList;
    }

    static public FeatureItem[] getFiltered(int lastSeenVersionCode, boolean isFirstRun, boolean isBeta,
                                            boolean isMultiAccount) {
        List<FeatureItem> features = new LinkedList<>();

        for (FeatureItem item : get(isMultiAccount)) {
            final int itemVersionCode = isBeta ? item.getBetaVersionNumber() : item.getVersionCode();
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
        private int versionCode;
        private int betaVersion;
        private boolean showOnInitialRun;
        private boolean contentCentered;
        private boolean bulletList;

        public FeatureItem(int image, int titleText, int contentText, int version, int betaVersion) {
            this(image, titleText, contentText, version, betaVersion, false, true, true);
        }

        public FeatureItem(int image, int titleText, int contentText, int version, int betaVersion,
                           boolean showOnInitialRun) {
            this(image, titleText, contentText, version, betaVersion, showOnInitialRun, true, true);
        }

        public FeatureItem(int image, int titleText, int contentText, int versionCode, int betaVersion,
                           boolean showOnInitialRun, boolean contentCentered, boolean bulletList) {
            this.image = image;
            this.titleText = titleText;
            this.contentText = contentText;
            this.versionCode = versionCode;
            this.betaVersion = betaVersion;
            this.showOnInitialRun = showOnInitialRun;
            this.contentCentered = contentCentered;
            this.bulletList = bulletList;
        }

        public boolean shouldShowImage() { return image != DO_NOT_SHOW; }
        public int getImage() { return image; }

        public boolean shouldShowTitleText() { return titleText != DO_NOT_SHOW; }
        public int getTitleText() { return titleText; }

        public boolean shouldShowContentText() { return contentText != DO_NOT_SHOW; }
        public int getContentText() { return contentText; }

        public int getVersionCode() {
            return versionCode;
        }
        public int getBetaVersionNumber() { return betaVersion; }
        public boolean shouldShowOnFirstRun() { return showOnInitialRun; }

        public boolean shouldContentCentered() {
            return contentCentered;
        }

        public boolean shouldShowBulletPointList() {
            return bulletList;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(image);
            dest.writeInt(titleText);
            dest.writeInt(contentText);
            dest.writeInt(versionCode);
            dest.writeInt(betaVersion);
            dest.writeByte((byte) (showOnInitialRun ? 1 : 0));
            dest.writeByte((byte) (contentCentered ? 1 : 0));
            dest.writeByte((byte) (bulletList ? 1 : 0));
        }

        private FeatureItem(Parcel p) {
            image = p.readInt();
            titleText = p.readInt();
            contentText = p.readInt();
            versionCode = p.readInt();
            betaVersion = p.readInt();
            showOnInitialRun = p.readByte() == 1;
            contentCentered = p.readByte() == 1;
            bulletList = p.readByte() == 1;
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
}
