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

package com.nextcloud.extensions

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextcloud.test.model.OtherTestData
import com.nextcloud.test.model.TestData
import com.nextcloud.test.model.TestDataParcelable
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getSerializableArgument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("FunctionNaming")
@RunWith(AndroidJUnit4::class)
class IntentExtensionTests {

    private val key = "testDataKey"

    @Test
    fun test_get_serializable_argument_when_given_valid_intent_should_return_expected_data() {
        val intent = Intent()
        val testObject = TestData("Hello")
        intent.putExtra(key, testObject)
        val retrievedObject = intent.getSerializableArgument(key, TestData::class.java)
        assertEquals(testObject, retrievedObject)
    }

    @Test
    fun test_get_serializable_argument_when_given_valid_intent_and_wrong_class_type_should_return_null() {
        val intent = Intent()
        val testObject = TestData("Hello")
        intent.putExtra(key, testObject)
        val retrievedObject = intent.getSerializableArgument(key, Array<String>::class.java)
        assertNull(retrievedObject)
    }

    @Test
    fun test_get_parcelable_argument_when_given_valid_intent_and_wrong_class_type_should_return_null() {
        val intent = Intent()
        val testObject = TestData("Hello")
        intent.putExtra(key, testObject)
        val retrievedObject = intent.getParcelableArgument(key, OtherTestData::class.java)
        assertNull(retrievedObject)
    }

    @Test
    fun test_get_parcelable_argument_when_given_valid_intent_should_return_expected_data() {
        val intent = Intent()
        val testObject = TestDataParcelable("Hello")
        intent.putExtra(key, testObject)
        val retrievedObject = intent.getParcelableArgument(key, TestDataParcelable::class.java)
        assertEquals(testObject, retrievedObject)
    }

    @Test
    fun test_get_serializable_argument_when_given_null_intent_should_return_null() {
        val retrievedObject = (null as Intent?).getSerializableArgument(key, TestData::class.java)
        assertNull(retrievedObject)
    }

    @Test
    fun test_get_parcelable_argument_when_given_null_intent_should_return_null() {
        val retrievedObject = (null as Intent?).getParcelableArgument(key, TestDataParcelable::class.java)
        assertNull(retrievedObject)
    }
}
