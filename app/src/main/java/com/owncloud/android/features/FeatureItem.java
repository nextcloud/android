/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018-2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.features;

import android.os.Parcel;
import android.os.Parcelable;

import com.owncloud.android.R;

public class FeatureItem implements Parcelable {
    private static final int DO_NOT_SHOW = -1;
    private int image;
    private int titleText;
    private int contentText;
    private boolean contentCentered;
    private boolean bulletList;

    public FeatureItem(int image, int titleText, int contentText, boolean contentCentered, boolean bulletList) {
        this.image = image;
        this.titleText = titleText;
        this.contentText = contentText;
        this.contentCentered = contentCentered;
        this.bulletList = bulletList;
    }

    public boolean shouldShowImage() {
        return image != DO_NOT_SHOW;
    }

    public boolean shouldShowTitleText() {
        return titleText != DO_NOT_SHOW && titleText != R.string.empty;
    }

    public boolean shouldShowContentText() {
        return contentText != DO_NOT_SHOW && contentText != R.string.empty;
    }

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
        dest.writeByte((byte) (contentCentered ? 1 : 0));
        dest.writeByte((byte) (bulletList ? 1 : 0));
    }

    private FeatureItem(Parcel p) {
        image = p.readInt();
        titleText = p.readInt();
        contentText = p.readInt();
        contentCentered = p.readByte() == 1;
        bulletList = p.readByte() == 1;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new FeatureItem(source);
        }

        @Override
        public Object[] newArray(int size) {
            return new FeatureItem[size];
        }
    };

    public int getImage() {
        return this.image;
    }

    public int getTitleText() {
        return this.titleText;
    }

    public int getContentText() {
        return this.contentText;
    }
}
