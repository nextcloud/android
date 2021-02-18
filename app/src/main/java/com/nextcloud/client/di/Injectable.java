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

/**
 * Marks object as injectable by {@link ActivityInjector} and {@link FragmentInjector}.
 * <p>
 * Any {@link android.app.Activity} or {@link androidx.fragment.app.Fragment} implementing
 * this interface will be automatically supplied with dependencies.
 * <p>
 * Activities are considered fully-initialized after call to {@link android.app.Activity#onCreate(Bundle)}
 * (this means after {@code super.onCreate(savedStateInstance)} returns).
 * <p>
 * Injectable Fragments are supplied with dependencies before {@link androidx.fragment.app.Fragment#onAttach(Context)}.
 */
public interface Injectable {}
