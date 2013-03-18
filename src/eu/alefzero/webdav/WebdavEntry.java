/* ownCloud Android client application
 *   Copyright (C) 2012  ownCloud
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
package eu.alefzero.webdav;

import java.util.Date;

import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;

import android.net.Uri;
import android.util.Log;

public class WebdavEntry {
    private String mName, mPath, mUri, mContentType;
    private long mContentLength, mCreateTimestamp, mModifiedTimestamp;

    public WebdavEntry(MultiStatusResponse ms, String splitElement) {
        resetData();
        if (ms.getStatus().length != 0) {
            mUri = ms.getHref();

            mPath = mUri.split(splitElement, 2)[1];

            int status = ms.getStatus()[0].getStatusCode();
            DavPropertySet propSet = ms.getProperties(status);
            @SuppressWarnings("rawtypes")
            DavProperty prop = propSet.get(DavPropertyName.DISPLAYNAME);
            if (prop != null)
                mName = (String) prop.getName().toString();
            else {
                String[] tmp = mPath.split("/");
                if (tmp.length > 0)
                    mName = tmp[tmp.length - 1];
            }

            // use unknown mimetype as default behavior
            mContentType = "application/octet-stream";
            prop = propSet.get(DavPropertyName.GETCONTENTTYPE);
            if (prop != null) {
                mContentType = (String) prop.getValue();
                // dvelasco: some builds of ownCloud server 4.0.x added a trailing ';' to the MIME type ; if looks fixed, but let's be cautious
                if (mContentType.indexOf(";") >= 0) {
                    mContentType = mContentType.substring(0, mContentType.indexOf(";"));
                }
            }
            
            // check if it's a folder in the standard way: see RFC2518 12.2 , or RFC4918 14.3 
            prop = propSet.get(DavPropertyName.RESOURCETYPE);
            if (prop!= null) {
                Object value = prop.getValue();
                if (value != null) {
                    mContentType = "DIR";   // a specific attribute would be better, but this is enough; unless while we have no reason to distinguish MIME types for folders
                }
            }

            prop = propSet.get(DavPropertyName.GETCONTENTLENGTH);
            if (prop != null)
                mContentLength = Long.parseLong((String) prop.getValue());

            prop = propSet.get(DavPropertyName.GETLASTMODIFIED);
            if (prop != null) {
                Date d = WebdavUtils
                        .parseResponseDate((String) prop.getValue());
                mModifiedTimestamp = (d != null) ? d.getTime() : 0;
            }

            prop = propSet.get(DavPropertyName.CREATIONDATE);
            if (prop != null) {
                Date d = WebdavUtils
                        .parseResponseDate((String) prop.getValue());
                mCreateTimestamp = (d != null) ? d.getTime() : 0;
            }

        } else {
            Log.e("WebdavEntry",
                    "General fuckup, no status for webdav response");
        }
    }

    public String path() {
        return mPath;
    }
    
    public String decodedPath() {
        return Uri.decode(mPath);
    }

    public String name() {
        return mName;
    }

    public boolean isDirectory() {
        return mContentType.equals("DIR");
    }

    public String contentType() {
        return mContentType;
    }

    public String uri() {
        return mUri;
    }

    public long contentLength() {
        return mContentLength;
    }

    public long createTimestamp() {
        return mCreateTimestamp;
    }

    public long modifiedTimestamp() {
        return mModifiedTimestamp;
    }

    private void resetData() {
        mName = mUri = mContentType = null;
        mContentLength = mCreateTimestamp = mModifiedTimestamp = 0;
    }
}
