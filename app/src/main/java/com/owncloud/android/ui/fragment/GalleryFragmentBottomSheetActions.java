package com.owncloud.android.ui.fragment;

public interface GalleryFragmentBottomSheetActions {

    /**
     * hide all the images in particular Folder.
     */
    void hideImages(boolean isHideImagesClicked);

    /**
     * hide all the videos in particular folder.
     */
    void hideVideos(boolean isHideVideosClicked);

    /**
     * load all media of a particular folder.
     */
    void selectMediaFolder();

}
