/*
 *   Nextcloud Android client application
 *
 *   @author Andy Scherzinger
 *   Copyright (C) 2016 Andy Scherzinger
 *   Copyright (C) 2016 Nextcloud
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.authentication

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner

/**
 * Tests to check the parser logic of the login data URL parser.
 */
@RunWith(BlockJUnit4ClassRunner::class)
class AuthenticatorDataUrlTest {
    private val schemeUrl = "nc://login/"
    private val plus = "&"
    private val userValue = "testuser123"
    private val userUrlPart = "user:$userValue"
    private val passwordValue = "testpassword123"
    private val passwordUrlPart = "password:$passwordValue"
    private val addressValue = "testserver123"
    private val addressUrlPart = "server:$addressValue"
    private val urlStarts = arrayOf(schemeUrl)
    @Test
    fun allDataUrlElements() {
        val dataUrl = userUrlPart + plus + passwordUrlPart + plus + addressUrlPart
        for (urlStart in urlStarts) {
            val info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl)
            println(urlStart + dataUrl)
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + URL_PARSING, userValue, info.username)
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + URL_PARSING, passwordValue, info.password)
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + URL_PARSING, addressValue, info.serverAddress)
        }
    }

    @Test
    fun oneElement() {
        for (urlStart in urlStarts) {
            println(urlStart + userUrlPart)
            Assert.assertEquals(
                INCORRECT_USER_VALUE_IN + urlStart + URL_PARSING, userValue,
                AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + userUrlPart).username
            )
            println(urlStart + passwordUrlPart)
            Assert.assertEquals(
                INCORRECT_USER_VALUE_IN + urlStart + URL_PARSING, passwordValue,
                AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + passwordUrlPart).password
            )
            println(urlStart + addressUrlPart)
            Assert.assertEquals(
                INCORRECT_USER_VALUE_IN + urlStart + URL_PARSING, addressValue,
                AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + addressUrlPart).serverAddress
            )
        }
    }

    @Test
    fun twoDataUrlElements() {
        var dataUrl = userUrlPart + plus + passwordUrlPart
        for (urlStart in urlStarts) {
            println(urlStart + dataUrl)
            val info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl)
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING, userValue, info.username)
            Assert.assertEquals(
                INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING,
                passwordValue,
                info.password
            )
        }
        dataUrl = userUrlPart + plus + addressUrlPart
        for (urlStart in urlStarts) {
            println(urlStart + dataUrl)
            val info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl)
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING, userValue, info.username)
            Assert.assertEquals(
                INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING, addressValue,
                info.serverAddress
            )
        }
        dataUrl = addressUrlPart + plus + userUrlPart
        for (urlStart in urlStarts) {
            println(urlStart + dataUrl)
            val info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl)
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING, userValue, info.username)
            Assert.assertEquals(
                INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING, addressValue,
                info.serverAddress
            )
        }
        dataUrl = addressUrlPart + plus + passwordUrlPart
        for (urlStart in urlStarts) {
            println(urlStart + dataUrl)
            val info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl)
            Assert.assertEquals(
                INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING,
                passwordValue,
                info.password
            )
            Assert.assertEquals(
                INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING,
                addressValue,
                info.serverAddress
            )
        }
        dataUrl = passwordUrlPart + plus + userUrlPart
        for (urlStart in urlStarts) {
            println(urlStart + dataUrl)
            val info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl)
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING, userValue, info.username)
            Assert.assertEquals(
                INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING,
                passwordValue,
                info.password
            )
        }
        dataUrl = passwordUrlPart + plus + addressUrlPart
        for (urlStart in urlStarts) {
            println(urlStart + dataUrl)
            val info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl)
            Assert.assertEquals(
                INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING,
                passwordValue,
                info.password
            )
            Assert.assertEquals(
                INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING,
                addressValue,
                info.serverAddress
            )
        }
    }

    @Test
    fun tooManyDataUrlElements() {
        val dataUrl = userUrlPart + plus + passwordUrlPart + plus + addressUrlPart + plus + "notexist:boom"
        for (urlStart in urlStarts) {
            try {
                println(urlStart + dataUrl)
                AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl)
                Assert.fail("Illegal Argument Exception expected!")
            } catch (e: IllegalArgumentException) {
                // all well
                println("Error with $urlStart$dataUrl")
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun noDataUrlElements() {
        AuthenticatorActivity.parseLoginDataUrl(schemeUrl, "")
    }

    @Test
    fun onlyOneDataUrlElements() {
        val loginUrlInfo = AuthenticatorActivity.parseLoginDataUrl(schemeUrl, schemeUrl + userUrlPart)
        Assert.assertEquals(userValue, loginUrlInfo.username)
    }

    @Test
    fun onlyTwoDataUrlElements() {
        val loginUrlInfo = AuthenticatorActivity.parseLoginDataUrl(
            schemeUrl, schemeUrl + userUrlPart +
                plus + addressUrlPart
        )
        Assert.assertEquals(userValue, loginUrlInfo.username)
        Assert.assertEquals(addressValue, loginUrlInfo.serverAddress)
    }

    companion object {
        private const val URL_PARSING = " url parsing"
        private const val INCORRECT_USER_VALUE_IN = "Incorrect user value in "
    }
}