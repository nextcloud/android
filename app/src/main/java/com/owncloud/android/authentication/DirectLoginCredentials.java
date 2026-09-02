/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.authentication;

public final class DirectLoginCredentials {
    private final String username;
    private final String password;

    public DirectLoginCredentials(String username, String password) {
        this.username = normalize(username);
        this.password = normalize(password);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isUsernameEmpty() {
        return username.isEmpty();
    }

    public boolean isPasswordEmpty() {
        return password.isEmpty();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
