/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.di;

import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.fragment.FileDetailFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

/**
 * Register classes that require dependency injection. This class is used by Dagger compiler
 * only.
 */
@Module
abstract class ComponentsModule {
    @ContributesAndroidInjector
    abstract FileDisplayActivity fileDisplayActivity();

    @ContributesAndroidInjector
    abstract FileDetailFragment fileDetailFragment();
}
