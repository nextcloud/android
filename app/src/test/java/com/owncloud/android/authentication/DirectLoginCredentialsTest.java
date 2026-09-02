/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.authentication;

import org.junit.Assert;
import org.junit.Test;

public class DirectLoginCredentialsTest {
    @Test
    public void trimsCredentials() {
        DirectLoginCredentials credentials = new DirectLoginCredentials(" user ", " app password ");

        Assert.assertEquals("user", credentials.getUsername());
        Assert.assertEquals("app password", credentials.getPassword());
    }

    @Test
    public void usernameIsEmptyWhenMissing() {
        DirectLoginCredentials credentials = new DirectLoginCredentials("  ", "app password");

        Assert.assertTrue(credentials.isUsernameEmpty());
        Assert.assertFalse(credentials.isPasswordEmpty());
    }

    @Test
    public void passwordIsEmptyWhenMissing() {
        DirectLoginCredentials credentials = new DirectLoginCredentials("user", null);

        Assert.assertFalse(credentials.isUsernameEmpty());
        Assert.assertTrue(credentials.isPasswordEmpty());
    }
}
