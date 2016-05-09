/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.authentication;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.owncloud.android.BuildConfig;
import static org.junit.Assert.assertTrue;
import com.owncloud.android.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import android.app.Activity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;

import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AuthenticatorActivityTest {

    public static final String EXTRA_ACTION = "ACTION";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";

    private int mResultCode = -2;
    private int WAIT_LOGIN = 5000;

    private static String errorMessage = "Activity not finished";


    @Rule
    public ActivityTestRule<AuthenticatorActivity> mActivityRule = new ActivityTestRule<AuthenticatorActivity>(
            AuthenticatorActivity.class){
        @Override
        protected Intent getActivityIntent() {

            Context targetContext = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext();
            Intent result = new Intent(targetContext, AuthenticatorActivity.class);
            result.putExtra(EXTRA_ACTION, "");
            result.putExtra(EXTRA_ACCOUNT, "");
            return result;
        }
    };

    @Test
    public void check_login() {

        // Check that login button is disabled
        onView(withId(R.id.buttonOK))
                .check(matches(not(isEnabled())));

        // Type server url
        onView(withId(R.id.hostUrlInput))
                .perform(typeText(BuildConfig.TEST_SERVER_URL), closeSoftKeyboard());
        onView(withId(R.id.account_username)).perform(click());

        // Type user
        onView(withId(R.id.account_username))
                .perform(typeText(BuildConfig.TEST_USER), closeSoftKeyboard());

        // Type user pass
        onView(withId(R.id.account_password))
                .perform(typeText(BuildConfig.TEST_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.buttonOK)).perform(click());

        // Check that the Activity ends after clicking
        try {

            Thread.sleep(WAIT_LOGIN);
            Field f = Activity.class.getDeclaredField("mResultCode");
            f.setAccessible(true);
            mResultCode = f.getInt(mActivityRule.getActivity());

        } catch (Exception e){
            e.printStackTrace();
        }

        assertTrue(errorMessage, mResultCode == Activity.RESULT_OK);

    }
}