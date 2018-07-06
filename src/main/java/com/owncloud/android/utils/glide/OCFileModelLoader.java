/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud
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

package com.owncloud.android.utils.glide;

import android.support.annotation.NonNull;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.owncloud.android.datamodel.OCFile;

import java.io.InputStream;

public class OCFileModelLoader implements ModelLoader<GlideOcFile, InputStream> {
    @Override
    public boolean handles(@NonNull GlideOcFile model) {
        return true;
    }

    @Override
    public LoadData<InputStream> buildLoadData(@NonNull GlideOcFile model, int width, int height, @NonNull Options options) {
        OCFile file = model.getFile();

        if (GlideOCFileType.thumbnail.equals(model.getType())) {
            String path;
            if (model.getFile().getStoragePath().isEmpty()) {
                path = model.getPath();
            } else {
                path = model.getFile().getStoragePath();
            }

            return new LoadData<>(GlideKey.serverThumbnail(file), new FileFetcher(path));
        } else {
            return new LoadData<>(GlideKey.resizedImage(file), new FileFetcher(file.getStoragePath()));
        }
    }
}
