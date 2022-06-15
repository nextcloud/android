/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author Andy Scherzinger
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
