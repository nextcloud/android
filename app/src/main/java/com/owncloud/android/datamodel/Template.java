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

import java.util.Locale;

/**
 * Template for creating a file from it via RichDocuments app
 */
@Parcel
public class Template {
    public int id;
    public String name;
    public String thumbnailLink;
    public Type type;
    public String extension;

    public Template() {
        // empty constructor
    }

    public Template(int id, String name, String thumbnailLink, Type type, String extension) {
        this.id = id;
        this.name = name;
        this.thumbnailLink = thumbnailLink;
        this.type = type;
        this.extension = extension;
    }

    public enum Type {
        DOCUMENT, SPREADSHEET, PRESENTATION, UNKNOWN
    }

    public static Type parse(String type) {
        switch (type.toLowerCase(Locale.US)) {
            case "document":
                return Type.DOCUMENT;
            case "spreadsheet":
                return Type.SPREADSHEET;
            case "presentation":
                return Type.PRESENTATION;
            default:
                return Type.UNKNOWN;
        }
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getThumbnailLink() {
        return this.thumbnailLink;
    }

    public Type getType() {
        return this.type;
    }

    public String getExtension() {
        return this.extension;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setThumbnailLink(String thumbnailLink) {
        this.thumbnailLink = thumbnailLink;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }
}
