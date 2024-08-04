/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment;

import com.owncloud.android.lib.common.Creator;

/**
 * Actions interface to be implemented by any class that makes use of
 * {@link com.owncloud.android.ui.fragment.OCFileListBottomSheetDialog}.
 */
public interface OCFileListBottomSheetActions {
    /**
     * creates a folder within the actual folder.
     */
    void createFolder();

    /**
     * offers a file upload with the Android OS file picker to the current folder.
     */
    void uploadFromApp();

    /**
     * offers a file upload with the app file picker to the current folder.
     */
    void uploadFiles();

    /**
     * opens template selection for documents
     */
    void newDocument();

    /**
     * opens template selection for spreadsheets
     */
    void newSpreadsheet();

    /**
     * opens template selection for presentations
     */
    void newPresentation();

    /**
     * offers direct camera upload to the current folder.
     */
    void directCameraUpload();

    /**
     * offers scanning document upload to the current folder.
     */
    void scanDocUpload();

    /**
     * open template selection for creator @link Creator
     */
    void showTemplate(Creator creator, String headline);

    /**
     * open editor for rich workspace
     */
    void createRichWorkspace();
}
