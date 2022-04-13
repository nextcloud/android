//**
// *   ownCloud Android client application
// *
// *   Copyright (C) 2015 ownCloud Inc.
// *
// *   This program is free software: you can redistribute it and/or modify
// *   it under the terms of the GNU General Public License version 2,
// *   as published by the Free Software Foundation.
// *
// *   This program is distributed in the hope that it will be useful,
// *   but WITHOUT ANY WARRANTY; without even the implied warranty of
// *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// *   GNU General Public License for more details.
// *
// *   You should have received a copy of the GNU General Public License
// *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
// *
// */
//
//package com.owncloud.android.authentication;
//
//import android.app.Activity;
//import android.content.Context;
//import android.content.Intent;
//import android.os.Bundle;
//import android.os.RemoteException;
//import android.support.test.InstrumentationRegistry;
//import android.support.test.rule.ActivityTestRule;
//import android.support.test.runner.AndroidJUnit4;
//import android.support.test.uiautomator.UiDevice;
//import android.test.suitebuilder.annotation.LargeTest;
//
//import com.owncloud.android.R;
//
//import org.junit.Before;
//import org.junit.Rule;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//import java.lang.reflect.Field;
//
//import static android.support.test.espresso.Espresso.onView;
//import static android.support.test.espresso.action.ViewActions.click;
//import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
//import static android.support.test.espresso.action.ViewActions.typeText;
//import static android.support.test.espresso.assertion.ViewAssertions.matches;
//import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
//import static android.support.test.espresso.matcher.ViewMatchers.withId;
//import static org.hamcrest.Matchers.not;
//import static org.junit.Assert.assertTrue;
//
//@RunWith(AndroidJUnit4.class)
//@LargeTest
//public class AuthenticatorActivityTest {
//
//    public static final String EXTRA_ACTION = "ACTION";
//    public static final String EXTRA_ACCOUNT = "ACCOUNT";
//
//    private static final int WAIT_LOGIN = 5000;
//
//    private static final String ERROR_MESSAGE = "Activity not finished";
//    private static final String RESULT_CODE = "mResultCode";
//
//
//    @Rule
//    public ActivityTestRule<AuthenticatorActivity> mActivityRule = new ActivityTestRule<AuthenticatorActivity>(
//            AuthenticatorActivity.class){
//        @Override
//        protected Intent getActivityIntent() {
//
//            Context targetContext = InstrumentationRegistry.getInstrumentation()
//                    .getTargetContext();
//            Intent result = new Intent(targetContext, AuthenticatorActivity.class);
//            result.putExtra(EXTRA_ACTION, AuthenticatorActivity.ACTION_CREATE);
//            result.putExtra(EXTRA_ACCOUNT, "");
//            return result;
//        }
//    };
//
//    @Before
//    public void init(){
//        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
//        /*Point[] coordinates = new Point[4];
//        coordinates[0] = new Point(248, 1020);
//        coordinates[1] = new Point(248, 429);
//        coordinates[2] = new Point(796, 1020);
//        coordinates[3] = new Point(796, 429);*/
//        try {
//            if (!uiDevice.isScreenOn()) {
//                uiDevice.wakeUp();
//                //uiDevice.swipe(coordinates, 10);
//            }
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Test
//    public void checkLogin()
//        throws InterruptedException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
//        Bundle arguments = InstrumentationRegistry.getArguments();
//
//        // Get values passed
//        String testUser = arguments.getString("TEST_USER");
//        String testPassword = arguments.getString("TEST_PASSWORD");
//        String testServerURL = arguments.getString("TEST_SERVER_URL");
//
//        // Check that login button is disabled
//        onView(withId(R.id.buttonOK))
//                .check(matches(not(isEnabled())));
//
//        // Type server url
//        onView(withId(R.id.hostUrlInput))
//                .perform(typeText(testServerURL), closeSoftKeyboard());
//        onView(withId(R.id.account_username)).perform(click());
//
//        // Type user
//        onView(withId(R.id.account_username))
//                .perform(typeText(testUser), closeSoftKeyboard());
//
//        // Type user pass
//        onView(withId(R.id.account_password))
//                .perform(typeText(testPassword), closeSoftKeyboard());
//        onView(withId(R.id.buttonOK)).perform(click());
//
//        // Check that the Activity ends after clicking
//
//        Thread.sleep(WAIT_LOGIN);
//        Field f = Activity.class.getDeclaredField(RESULT_CODE);
//        f.setAccessible(true);
//        int mResultCode = f.getInt(mActivityRule.getActivity());
//
//        assertTrue(ERROR_MESSAGE, mResultCode == Activity.RESULT_OK);
//
//    }
//}