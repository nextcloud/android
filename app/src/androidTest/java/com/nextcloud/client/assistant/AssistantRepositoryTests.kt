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

import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.assistant.repository.AssistantRepository
import com.owncloud.android.AbstractOnServerIT
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AssistantRepositoryTests: AbstractOnServerIT() {

    private var sut: AssistantRepository? = null

    @Before
    fun setup() {
        val userAccountManager = UserAccountManagerImpl.fromContext(targetContext)
        sut = AssistantRepository(userAccountManager.user, targetContext)
    }

    @Test
    fun testGetTaskTypes() {
        assertTrue(sut?.getTaskTypes()?.resultData?.isNotEmpty() == true)
    }

    /*

       @Test
    fun testGetTaskList() {
        assertTrue(sut?.getTaskList("assistant")?.ocs?.data?.types?.isNotEmpty() == true)
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
        val taskList = sut?.getTaskList("assistant")?.ocs?.data
        assertTrue(sut?.getTaskList("assistant")?.ocs?.data?.types?.isNotEmpty() == true)
    }

     */


}
