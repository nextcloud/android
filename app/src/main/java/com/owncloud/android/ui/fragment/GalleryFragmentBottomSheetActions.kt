package com.owncloud.android.ui.fragment

interface GalleryFragmentBottomSheetActions {
    /**
     * hide all the images in particular Folder.
     */
    fun hideImages(isHideImagesClicked: Boolean)

    /**
     * hide all the videos in particular folder.
     */
    fun hideVideos(isHideVideosClicked: Boolean)

    /**
     * load all media of a particular folder.
     */
    fun selectMediaFolder()

    /**
     * sort by modified date
     */
    fun sortByModifiedDate()

    /**
     * sort by Created Date
     */
    fun sortByCreatedDate()

    /**
     * sort By Upload Date
     */
    fun sortByUploadDate()
}