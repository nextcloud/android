/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2024 Alper Ozturk
 * Copyright (C) 2024 Nextcloud GmbH
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.assistant

import com.nextcloud.client.assistant.repository.AssistantRepository
import com.owncloud.android.AbstractOnServerIT
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AssistantRepositoryTests : AbstractOnServerIT() {

    private var sut: AssistantRepository? = null

    @Before
    fun setup() {
        sut = AssistantRepository(nextcloudClient)
    }

    @Test
    fun testGetTaskTypes() {
        assertTrue(sut?.getTaskTypes()?.resultData?.types?.isNotEmpty() == true)
    }

    @Test
    fun testGetTaskList() {
        val result = sut?.getTaskList("assistant")?.resultData?.tasks

        if (result == null) {
            fail("Expected to get task list but found null")
        }

        assertTrue(result?.isEmpty() == true || (result?.size ?: 0) > 0)
    }

    @Test
    fun testCreateTask() {
        val input = "How many files I have?"
        val type = "OCP\\TextProcessing\\HeadlineTaskType"
        val result = sut?.createTask(input, type)
        assertTrue(result != null)
    }

    @Test
    fun testDeleteTask() {
        testCreateTask()

        shortSleep()

        val taskList = sut?.getTaskList("assistant")?.resultData?.tasks
        val taskListCountBeforeDelete = taskList?.size

        if (taskList.isNullOrEmpty()) {
            fail("Expected to get task list but found null or empty list")
        }

        sut?.deleteTask(taskList!!.first().id)
        assertTrue(taskListCountBeforeDelete == taskListCountBeforeDelete?.minus(1))
    }
}
