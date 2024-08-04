/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.assistant

import com.nextcloud.client.assistant.repository.AssistantRepository
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.lib.resources.status.NextcloudVersion
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("MagicNumber")
class AssistantRepositoryTests : AbstractOnServerIT() {

    private var sut: AssistantRepository? = null

    @Before
    fun setup() {
        sut = AssistantRepository(nextcloudClient)
    }

    @Test
    fun testGetTaskTypes() {
        testOnlyOnServer(NextcloudVersion.nextcloud_28)

        if (capability.assistant.isFalse) {
            return
        }

        val result = sut?.getTaskTypes()
        assertTrue(result?.isSuccess == true)

        val taskTypes = result?.resultData?.types
        assertTrue(taskTypes?.isNotEmpty() == true)
    }

    @Test
    fun testGetTaskList() {
        testOnlyOnServer(NextcloudVersion.nextcloud_28)

        if (capability.assistant.isFalse) {
            return
        }

        val result = sut?.getTaskList("assistant")
        assertTrue(result?.isSuccess == true)

        val taskList = result?.resultData?.tasks
        assertTrue(taskList?.isEmpty() == true || (taskList?.size ?: 0) > 0)
    }

    @Test
    fun testCreateTask() {
        testOnlyOnServer(NextcloudVersion.nextcloud_28)

        if (capability.assistant.isFalse) {
            return
        }

        val input = "Give me some random output for test purpose"
        val type = "OCP\\TextProcessing\\FreePromptTaskType"
        val result = sut?.createTask(input, type)
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

        val resultOfTaskList = sut?.getTaskList("assistant")
        assertTrue(resultOfTaskList?.isSuccess == true)

        sleep(120)

        val taskList = resultOfTaskList?.resultData?.tasks

        assert((taskList?.size ?: 0) > 0)

        val result = sut?.deleteTask(taskList!!.first().id)
        assertTrue(result?.isSuccess == true)
    }
}
