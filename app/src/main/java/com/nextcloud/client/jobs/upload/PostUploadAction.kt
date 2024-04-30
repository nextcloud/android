/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2021 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.upload

enum class PostUploadAction(val value: Int) {
    NONE(FileUploadWorker.LOCAL_BEHAVIOUR_FORGET),
    COPY_TO_APP(FileUploadWorker.LOCAL_BEHAVIOUR_COPY),
    MOVE_TO_APP(FileUploadWorker.LOCAL_BEHAVIOUR_MOVE),
    DELETE_SOURCE(FileUploadWorker.LOCAL_BEHAVIOUR_DELETE)
}
