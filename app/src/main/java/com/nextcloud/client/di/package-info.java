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

/**
 * This package contains application Dependency Injection code, based on Dagger 2.
 * <p>
 * To enable dependency injection for a component, such as {@link android.app.Activity},
 * {@link androidx.fragment.app.Fragment} or {@link android.app.Service}, the component must be
 * first registered in {@link com.nextcloud.client.di.ComponentsModule} class.
 * <p>
 * {@link com.nextcloud.client.di.ComponentsModule} will be used by Dagger compiler to
 * create an injector for a given class.
 *
 * @see com.nextcloud.client.di.InjectorNotFoundException
 * @see dagger.android.AndroidInjection
 * @see dagger.android.support.AndroidSupportInjection
 */
package com.nextcloud.client.di;
