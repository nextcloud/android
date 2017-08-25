package com.owncloud.android.operations;

/**
 * RuntimeException for throwing errors of remote operation calls.
 */
public class RemoteOperationFailedException extends RuntimeException {
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
