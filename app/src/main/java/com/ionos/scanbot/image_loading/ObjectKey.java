/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.image_loading;

import com.bumptech.glide.load.Key;

import java.nio.charset.Charset;
import java.security.MessageDigest;

import androidx.annotation.NonNull;

public final class ObjectKey implements Key {
    private static final Charset CHARSET = Charset.forName(STRING_CHARSET_NAME);

    private final Object object;

    public ObjectKey(@NonNull Object object) {
        this.object = object;
    }

    @Override
    public String toString() {
        return "ObjectKey{" + "object=" + object + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ObjectKey) {
            ObjectKey other = (ObjectKey) o;
            return object.equals(other.object);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return object.hashCode();
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(object.toString().getBytes(CHARSET));
    }
}
