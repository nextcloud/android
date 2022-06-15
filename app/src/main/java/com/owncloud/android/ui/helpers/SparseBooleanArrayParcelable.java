/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2016 ownCloud GmbH.
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

package com.owncloud.android.ui.helpers;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseBooleanArray;

/**
 * Wraps a SparseBooleanArrayParcelable to allow its serialization and desearialization
 * through {@link Parcelable} interface.
 */
public class SparseBooleanArrayParcelable implements Parcelable {

    @SuppressWarnings("PMD.SuspiciousConstantFieldName")
    public static Parcelable.Creator<SparseBooleanArrayParcelable> CREATOR =
        new Parcelable.Creator<SparseBooleanArrayParcelable>() {

        @Override
        public SparseBooleanArrayParcelable createFromParcel(Parcel source) {
            // read size of array from source
            int size = source.readInt();

            // then pairs of (key, value)s, in the object to wrap
            SparseBooleanArray sba = new SparseBooleanArray();
            for (int i = 0; i < size; i++) {
                sba.put(source.readInt(), source.readInt() != 0);
            }

            // wrap SparseBooleanArray
            return new SparseBooleanArrayParcelable(sba);
        }

        @Override
        public SparseBooleanArrayParcelable[] newArray(int size) {
            return new SparseBooleanArrayParcelable[size];
        }
    };

    private final SparseBooleanArray mSba;

    public SparseBooleanArrayParcelable(SparseBooleanArray sba) {
        if (sba == null) {
            throw new IllegalArgumentException("Cannot wrap a null SparseBooleanArray");
        }
        mSba = sba;
    }

    public SparseBooleanArray getSparseBooleanArray() {
        return mSba;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // first, size of the array
        dest.writeInt(mSba.size());

        // then, pairs of (key, value)
        for (int i = 0; i < mSba.size(); i++) {
            dest.writeInt(mSba.keyAt(i));
            dest.writeInt(mSba.valueAt(i) ? 1 : 0);
        }

    }
}