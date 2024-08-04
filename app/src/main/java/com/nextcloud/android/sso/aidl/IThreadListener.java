/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 David Luhmer <david-dev@live.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.android.sso.aidl;

public interface IThreadListener {

    void onThreadFinished(final Thread thread);
}
