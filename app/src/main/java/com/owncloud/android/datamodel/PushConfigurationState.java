/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author Andy Scherzinger
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2018 Andy Scherzinger
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android.datamodel;

public class PushConfigurationState {
    public String pushToken;
    public String deviceIdentifier;
    public String deviceIdentifierSignature;
    public String userPublicKey;
    public boolean shouldBeDeleted;

    public PushConfigurationState(String pushToken, String deviceIdentifier, String deviceIdentifierSignature, String userPublicKey, boolean shouldBeDeleted) {
        this.pushToken = pushToken;
        this.deviceIdentifier = deviceIdentifier;
        this.deviceIdentifierSignature = deviceIdentifierSignature;
        this.userPublicKey = userPublicKey;
        this.shouldBeDeleted = shouldBeDeleted;
    }

    public PushConfigurationState() {
        // empty constructor for JSON parser
    }

    public String getPushToken() {
        return this.pushToken;
    }

    public String getDeviceIdentifier() {
        return this.deviceIdentifier;
    }

    public String getDeviceIdentifierSignature() {
        return this.deviceIdentifierSignature;
    }

    public String getUserPublicKey() {
        return this.userPublicKey;
    }

    public boolean isShouldBeDeleted() {
        return this.shouldBeDeleted;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    public void setDeviceIdentifier(String deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
    }

    public void setDeviceIdentifierSignature(String deviceIdentifierSignature) {
        this.deviceIdentifierSignature = deviceIdentifierSignature;
    }

    public void setUserPublicKey(String userPublicKey) {
        this.userPublicKey = userPublicKey;
    }

    public void setShouldBeDeleted(boolean shouldBeDeleted) {
        this.shouldBeDeleted = shouldBeDeleted;
    }
}
