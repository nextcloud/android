/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.fragment;

/**
 * Actions interface to be implemented by any class that makes use of
 * {@link com.owncloud.android.ui.fragment.OCFileListBottomSheetDialog}.
 */
public interface OCFileListBottomSheetActions {
    /**
     * creates a folde rwithin the actual folder.
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
}
