/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
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

package com.owncloud.android.authenticator;

public interface OnConnectCheckListener {

    enum ResultType {
        OK_SSL, OK_NO_SSL, SSL_INIT_ERROR, HOST_NOT_AVAILABLE, TIMEOUT, NO_NETWORK_CONNECTION, INCORRECT_ADDRESS, INSTANCE_NOT_CONFIGURED, FILE_NOT_FOUND, UNKNOWN_ERROR, WRONG_CONNECTION,  SSL_UNVERIFIED_SERVER, BAD_OC_VERSION
    }

    public void onConnectionCheckResult(ResultType type);

}
