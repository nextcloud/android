/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.operations;

public class UploadException extends Exception {
    private static final long serialVersionUID = 5931153844211429915L;

    public UploadException() {
        super();
    }

    public UploadException(String message) {
        super(message);
    }
}
