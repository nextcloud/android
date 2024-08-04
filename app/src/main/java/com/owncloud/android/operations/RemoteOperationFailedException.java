/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017-2019 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.operations;

/**
 * RuntimeException for throwing errors of remote operation calls.
 */
public class RemoteOperationFailedException extends RuntimeException {
    private static final long serialVersionUID = 5429778514835938713L;

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A <code>null</code> value
     *                is permitted, and indicates that the cause is nonexistent
     *                or unknown.)
     */
    public RemoteOperationFailedException(String message, Throwable cause) {
        super(message + " / " + cause.getMessage(), cause);
    }

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     */
    public RemoteOperationFailedException(String message) {
        super(message);
    }
}
