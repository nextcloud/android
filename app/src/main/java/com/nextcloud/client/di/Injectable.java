/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
