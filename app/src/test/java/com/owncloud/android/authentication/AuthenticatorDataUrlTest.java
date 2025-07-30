/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.authentication;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

/**
 * Tests to check the parser logic of the login data URL parser.
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class AuthenticatorDataUrlTest {
    private static final String URL_PARSING = " url parsing";
    private static final String INCORRECT_USER_VALUE_IN = "Incorrect user value in ";
    private String schemeUrl = "nc://login/";
    private String plus = "&";

    private String userValue = "testuser123";
    private String userUrlPart = "user:" + userValue;

    private String passwordValue = "testpassword123";
    private String passwordUrlPart = "password:" + passwordValue;

    private String addressValue = "testserver123";
    private String addressUrlPart = "server:" + addressValue;

    private String[] urlStarts = new String[]{schemeUrl};

    @Test
    public void allDataUrlElements() {
        String dataUrl = userUrlPart + plus + passwordUrlPart + plus + addressUrlPart;

        for (String urlStart : urlStarts) {
            LoginUrlInfo info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl);
            System.out.println(urlStart + dataUrl);
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + URL_PARSING, userValue, info.getLoginName());
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + URL_PARSING, passwordValue, info.getAppPassword());
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + URL_PARSING, addressValue, info.getServer());
        }
    }

    @Test
    public void oneElement() {

        for (String urlStart : urlStarts) {
            System.out.println(urlStart + userUrlPart);
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + URL_PARSING, userValue,
                    AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + userUrlPart).getLoginName());
            System.out.println(urlStart + passwordUrlPart);
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + URL_PARSING, passwordValue,
                    AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + passwordUrlPart).getAppPassword());
            System.out.println(urlStart + addressUrlPart);
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + URL_PARSING, addressValue,
                    AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + addressUrlPart).getServer());
        }
    }

    @Test
    public void twoDataUrlElements() {

        String dataUrl = userUrlPart + plus + passwordUrlPart;
        for (String urlStart : urlStarts) {
            System.out.println(urlStart + dataUrl);
            LoginUrlInfo info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl);
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING, userValue, info
                    .getLoginName());
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING, passwordValue, info
                    .getAppPassword());
        }

        dataUrl = userUrlPart + plus + addressUrlPart;
        for (String urlStart : urlStarts) {
            System.out.println(urlStart + dataUrl);
            LoginUrlInfo info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl);
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING, userValue, info
                    .getLoginName());
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING, addressValue,
                    info.getServer());
        }


        dataUrl = addressUrlPart + plus + userUrlPart;
        for (String urlStart : urlStarts) {
            System.out.println(urlStart + dataUrl);
            LoginUrlInfo info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl);
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING, userValue, info
                    .getLoginName());
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING, addressValue,
                    info.getServer());
        }

        dataUrl = addressUrlPart + plus + passwordUrlPart;
        for (String urlStart : urlStarts) {
            System.out.println(urlStart + dataUrl);
            LoginUrlInfo info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl);
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING, passwordValue, info
                    .getAppPassword());
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING, addressValue, info
                    .getServer());
        }

        dataUrl = passwordUrlPart + plus + userUrlPart;
        for (String urlStart : urlStarts) {
            System.out.println(urlStart + dataUrl);
            LoginUrlInfo info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl);
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING, userValue, info
                    .getLoginName());
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING, passwordValue, info
                    .getAppPassword());
        }

        dataUrl = passwordUrlPart + plus + addressUrlPart;
        for (String urlStart : urlStarts) {
            System.out.println(urlStart + dataUrl);
            LoginUrlInfo info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl);
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING, passwordValue, info
                    .getAppPassword());
            Assert.assertEquals(INCORRECT_USER_VALUE_IN + urlStart + dataUrl + URL_PARSING, addressValue, info
                    .getServer());
        }
    }

    @Test
    public void tooManyDataUrlElements() {
        String dataUrl = userUrlPart + plus + passwordUrlPart + plus + addressUrlPart + plus + "notexist:boom";

        for (String urlStart : urlStarts) {
            try {
                System.out.println(urlStart + dataUrl);
                AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl);
                Assert.fail("Illegal Argument Exception expected!");
            } catch (IllegalArgumentException e) {
                // all well
                System.out.println("Error with " + urlStart + dataUrl);
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void noDataUrlElements() {
        AuthenticatorActivity.parseLoginDataUrl(schemeUrl, "");
    }

    @Test
    public void onlyOneDataUrlElements() {
        LoginUrlInfo loginUrlInfo = AuthenticatorActivity.parseLoginDataUrl(schemeUrl, schemeUrl + userUrlPart);

        Assert.assertEquals(userValue, loginUrlInfo.getLoginName());
    }

    @Test
    public void onlyTwoDataUrlElements() {
        LoginUrlInfo loginUrlInfo = AuthenticatorActivity.parseLoginDataUrl(schemeUrl, schemeUrl + userUrlPart +
            plus + addressUrlPart);

        Assert.assertEquals(userValue, loginUrlInfo.getLoginName());
        Assert.assertEquals(addressValue, loginUrlInfo.getServer());
    }
}
