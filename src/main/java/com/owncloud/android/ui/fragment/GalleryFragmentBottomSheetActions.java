package com.owncloud.android.ui.fragment;

public interface GalleryFragmentBottomSheetActions {

    /**
     * hide all the images in particular Folder.
     */
    void hideImages();

    /**
     * hide all the videos in particular folder.
     */
    void hideVideos();

    /**
     * load all media of a particular folder.
     */
    void selectMediaFolder();

    /**
     * sort by modified date
     */
    void sortByModifiedDate();

    /**
     * sort by Created Date
     */
    void sortByCreatedDate();

    /**
     * sort By Upload Date
     */
    void sortByUploadDate();




}
