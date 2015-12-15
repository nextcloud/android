package com.owncloud.android.db;

/**
 * Created by masensio on 14/12/2015.
 */
public enum UploadResult {
    UPLOADED(0),
    NETWORK_CONNECTION(1),
    CREDENTIAL_ERROR(2),
    FOLDER_ERROR(3),
    CONFLICT_ERROR(4),
    FILE_ERROR(5),
    PRIVILEDGES_ERROR(6),
    CANCELLED(7);

    private final int value;

    UploadResult(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
    public static UploadResult fromValue(int value)
    {
        switch (value)
        {
            case 0:
                return UPLOADED;
            case 1:
                return NETWORK_CONNECTION;
            case 2:
                return CREDENTIAL_ERROR;
            case 3:
                return FOLDER_ERROR;
            case 4:
                return CONFLICT_ERROR;
            case 5:
                return FILE_ERROR;
            case 6:
                return PRIVILEDGES_ERROR;
            case 7:
                return CANCELLED;
        }
        return null;
    }
}
