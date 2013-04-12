/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
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

package com.owncloud.android.network;

import java.util.Collection;

import eu.alefzero.webdav.OnDatatransferProgressListener;

public interface ProgressiveDataTransferer {

    public void addDatatransferProgressListener (OnDatatransferProgressListener listener);
    
    public void addDatatransferProgressListeners(Collection<OnDatatransferProgressListener> listeners);

    public void removeDatatransferProgressListener(OnDatatransferProgressListener listener);

}
