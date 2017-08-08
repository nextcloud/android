package com.owncloud.android.datamodel;

import android.util.SparseArray;

/**
 * Types of media folder.
 */
public enum MediaFolderType {
    CUSTOM(0),
    IMAGE(1),
    VIDEO(2);

    private Integer id;

    private static SparseArray<MediaFolderType> reverseMap = new SparseArray<>(3);

    static {
        reverseMap.put(CUSTOM.getId(), CUSTOM);
        reverseMap.put(IMAGE.getId(), IMAGE);
        reverseMap.put(VIDEO.getId(), VIDEO);
    }

    MediaFolderType(Integer id) {
        this.id = id;
    }

    public static MediaFolderType getById(Integer id) {
        return reverseMap.get(id);
    }

    public Integer getId() {
        return id;
    }
}
