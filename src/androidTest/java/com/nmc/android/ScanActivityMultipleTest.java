package com.nmc.android;

import android.Manifest;

import com.nmc.android.ui.ScanActivity;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.TestCase.assertEquals;

@RunWith(AndroidJUnit4.class)
@LargeTest
/**
 * Scan test to test the max number of possible scans till device throws exception or unexpected error occurs
 */
public class ScanActivityMultipleTest extends AbstractIT {

    @Rule public ActivityScenarioRule<ScanActivity> activityRule = new ActivityScenarioRule<>(ScanActivity.class);

    @Rule
    public final GrantPermissionRule permissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA);

    /**
     * variable to define max number of scans to test
     */
    private static final int MAX_NUMBER_OF_SCAN = 40;
    private int docScanCount = 0;

    @Test
    public void runAllScanTests() {
        captureAndVerifyDocScan();
        for (int i=0;i<MAX_NUMBER_OF_SCAN;i++) {
            System.out.println("Scan no: "+docScanCount);
            verifyScanMoreDocument();
        }
    }

    public void captureAndVerifyDocScan() {
        onView(withId(R.id.shutterButton)).perform(click());
        shortSleep();
        shortSleep();
        shortSleep();
        shortSleep();
        docScanCount++;
        assertEquals(docScanCount, ScanActivity.originalScannedImages.size());
    }

    public void verifyScanMoreDocument() {
        onView(withId(R.id.scanMoreButton)).perform(click());
        captureAndVerifyDocScan();
    }
}
