/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2018 Mario Danic
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.datamodel;

import java.util.List;

public class MediaFoldersModel {
    private List<String> imageMediaFolders;
    private List<String> videoMediaFolders;

    /**
     * default constructor.
     */
    public MediaFoldersModel() {
        // keep default constructor for GSON
    }

    public MediaFoldersModel(List<String> imageMediaFolders, List<String> videoMediaFolders) {
        this.imageMediaFolders = imageMediaFolders;
        this.videoMediaFolders = videoMediaFolders;
    }

    public List<String> getImageMediaFolders() {
        return imageMediaFolders;
    }

    public void setImageMediaFolders(List<String> imageMediaFolders) {
        this.imageMediaFolders = imageMediaFolders;
    }

    public List<String> getVideoMediaFolders() {
        return videoMediaFolders;
    }

    public void setVideoMediaFolders(List<String> videoMediaFolders) {
        this.videoMediaFolders = videoMediaFolders;
    }
}
