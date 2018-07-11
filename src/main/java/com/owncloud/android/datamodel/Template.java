/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.datamodel;

import org.parceler.Parcel;
import org.parceler.ParcelConstructor;

/**
 * Template for creating a file from it via RichDocuments app
 */

@Parcel
public class Template {

    private int id;
    private String name;
    private String thumbnailLink;
    private String type;
    private String extension;

    @ParcelConstructor
    public Template(int id, String name, String thumbnailLink, String type, String extension) {
        this.id = id;
        this.name = name;
        this.thumbnailLink = thumbnailLink;
        this.type = type;
        this.extension = extension;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getThumbnailLink() {
        return thumbnailLink;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }
}
