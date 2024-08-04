/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.extensions

import android.os.Bundle
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
class BundleExtensionTests {

    private val key = "testDataKey"

    @Test
    fun test_get_serializable_argument_when_given_valid_bundle_should_return_expected_data() {
        val bundle = Bundle()
        val testObject = TestData("Hello")
        bundle.putSerializable(key, testObject)
        val retrievedObject = bundle.getSerializableArgument(key, TestData::class.java)
        assertEquals(testObject, retrievedObject)
    }

    @Test
    fun test_get_serializable_argument_when_given_valid_bundle_and_wrong_class_type_should_return_null() {
        val bundle = Bundle()
        val testObject = TestData("Hello")
        bundle.putSerializable(key, testObject)
        val retrievedObject = bundle.getSerializableArgument(key, Array<String>::class.java)
        assertNull(retrievedObject)
    }

    @Test
    fun test_get_parcelable_argument_when_given_valid_bundle_and_wrong_class_type_should_return_null() {
        val bundle = Bundle()
        val testObject = TestData("Hello")
        bundle.putSerializable(key, testObject)
        val retrievedObject = bundle.getParcelableArgument(key, OtherTestData::class.java)
        assertNull(retrievedObject)
    }

    @Test
    fun test_get_parcelable_argument_when_given_valid_bundle_should_return_expected_data() {
        val bundle = Bundle()
        val testObject = TestDataParcelable("Hello")
        bundle.putParcelable(key, testObject)
        val retrievedObject = bundle.getParcelableArgument(key, TestDataParcelable::class.java)
        assertEquals(testObject, retrievedObject)
    }

    @Test
    fun test_get_serializable_argument_when_given_null_bundle_should_return_null() {
        val retrievedObject = (null as Bundle?).getSerializableArgument(key, TestData::class.java)
        assertNull(retrievedObject)
    }

    @Test
    fun test_get_parcelable_argument_when_given_null_bundle_should_return_null() {
        val retrievedObject = (null as Bundle?).getParcelableArgument(key, TestDataParcelable::class.java)
        assertNull(retrievedObject)
    }
}
