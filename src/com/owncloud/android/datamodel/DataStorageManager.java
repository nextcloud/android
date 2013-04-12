/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.datamodel;

import java.util.List;
import java.util.Vector;

public interface DataStorageManager {

    public static final int ROOT_PARENT_ID = 0;
    
    public OCFile getFileByPath(String path);

    public OCFile getFileById(long id);

    public boolean fileExists(String path);

    public boolean fileExists(long id);

    public boolean saveFile(OCFile file);

    public void saveFiles(List<OCFile> files);

    public Vector<OCFile> getDirectoryContent(OCFile f);
    
    public void removeFile(OCFile file, boolean removeLocalCopy);
    
    public void removeDirectory(OCFile dir, boolean removeDBData, boolean removeLocalContent);

    public void moveDirectory(OCFile dir, String newPath);

    public Vector<OCFile> getDirectoryImages(OCFile mParentFolder);
}
