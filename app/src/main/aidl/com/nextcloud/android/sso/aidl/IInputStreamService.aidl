/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2008-2011 CommonsWare, LLC
 * SPDX-License-Identifier: Apache-2.0
 *
 * From _The Busy Coder's Guide to Advanced Android Development_
 * http://commonsware.com/AdvAndroid
 *
 * More information here: https://github.com/abeluck/android-streams-ipc
 */
package com.nextcloud.android.sso.aidl;

// Declare the interface.
interface IInputStreamService {

    ParcelFileDescriptor performNextcloudRequestAndBodyStream(in ParcelFileDescriptor input,
                                                              in ParcelFileDescriptor requestBodyParcelFileDescriptor);

    ParcelFileDescriptor performNextcloudRequest(in ParcelFileDescriptor input);

    ParcelFileDescriptor performNextcloudRequestAndBodyStreamV2(in ParcelFileDescriptor input,
                                                                in ParcelFileDescriptor requestBodyParcelFileDescriptor);

    ParcelFileDescriptor performNextcloudRequestV2(in ParcelFileDescriptor input);
}
