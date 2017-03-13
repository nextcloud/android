/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
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
package com.owncloud.android.ui.events;

/**
 * Hide menu items event
 */

public class ToggleMenuItemsVisibilityEvent {
    public enum MenuHideType {
        HIDE_LIST_GRID_SWITCH_ITEM,
        HIDE_SORT_ITEM,
        HIDE_SORT_AND_LG_SWITCH_ITEM
    }


    public final MenuHideType menuHideType;
    public final boolean hideMenuItems;

    public ToggleMenuItemsVisibilityEvent(MenuHideType menuHideType, boolean hideMenuItems) {
        this.menuHideType = menuHideType;
        this.hideMenuItems = hideMenuItems;
    }

    public boolean isHideMenuItems() {
        return hideMenuItems;
    }

    public MenuHideType getMenuHideType() {
        return menuHideType;
    }

}
