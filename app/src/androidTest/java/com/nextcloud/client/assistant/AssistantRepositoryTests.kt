/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.assistant

import com.nextcloud.client.assistant.repository.remote.AssistantRemoteRepositoryImpl
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData
import com.owncloud.android.lib.resources.status.NextcloudVersion
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("MagicNumber")
class AssistantRepositoryTests : AbstractOnServerIT() {

    private var sut: AssistantRemoteRepositoryImpl? = null

    @Before
    fun setup() {
        sut = AssistantRemoteRepositoryImpl(nextcloudClient, capability)
    }

    @Test
    fun testGetTaskTypes() {
        testOnlyOnServer(NextcloudVersion.nextcloud_28)

        if (capability.assistant.isFalse) {
            return
        }

        val result = sut?.getTaskTypes()
        assertTrue(result?.isNotEmpty() == true)
    }

    @Test
    fun testGetTaskList() {
        testOnlyOnServer(NextcloudVersion.nextcloud_28)

        if (capability.assistant.isFalse) {
            return
        }

        val result = sut?.getTaskList("assistant")
        assertTrue(result?.isEmpty() == true || (result?.size ?: 0) > 0)
    }

    @Test
    fun testCreateTask() {
        testOnlyOnServer(NextcloudVersion.nextcloud_28)

        if (capability.assistant.isFalse) {
            return
        }

        val input = "Give me some random output for test purpose"
        val taskType = TaskTypeData(
            "core:text2text",
            "Free text to text prompt",
            "Runs an arbitrary prompt through a language model that returns a reply",
            emptyMap(),
            emptyMap()
        )
        val result = sut?.createTask(input, taskType)
        assertTrue(result?.isSuccess == true)
    }

    @Test
    fun testDeleteTask() {
        testOnlyOnServer(NextcloudVersion.nextcloud_28)

        if (capability.assistant.isFalse) {
            return
        }

        testCreateTask()

        sleep(120)

        val taskList = sut?.getTaskList("assistant")
        assertTrue(taskList != null)

        sleep(120)

        assert((taskList?.size ?: 0) > 0)

        val result = sut?.deleteTask(taskList!!.first().id)
        assertTrue(result?.isSuccess == true)
    }
}
