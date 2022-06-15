/**
 * Nextcloud Android client application
 *
 * @author Alejandro Bautista
 * Copyright (C) 2017 Andy Scherzinger
 * Copyright (C) 2017 Mario Danic
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

package com.owncloud.android.utils.svg;


import com.bumptech.glide.request.target.SimpleTarget;

/**
 * SimpleTarget for MenuItems
 */

public abstract class MenuSimpleTarget<Z> extends SimpleTarget<Z>{

    private final int mIdMenuItem;

    public MenuSimpleTarget(int idMenuItem){
        super();
        this.mIdMenuItem=idMenuItem;
    }


    public int getIdMenuItem() {
        return mIdMenuItem;
    }
}
