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
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.hasTextColor;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.core.IsNot.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
/**
 *Scan test to test the full flow of document scan from Scanning to Save page.
 */
public class ScanActivityTest extends AbstractIT {

    @Rule public ActivityScenarioRule<ScanActivity> activityRule = new ActivityScenarioRule<>(ScanActivity.class);

    @Rule
    public final GrantPermissionRule permissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA);

    private int docScanCount = 0;

    @Test
    /**
     * running all test in one test will create a flow from scanning to saving the scans
     */
    public void runAllScanTests() {
        verifyIfToolbarHidden();
        verifyIfScanFragmentReplaced();
        verifyToggleAutomatic();
        verifyToggleFlash();
        captureAndVerifyDocScan();
        verifyScanMoreDocument();
        verifyApplyFilter();
        verifyRotateDocument();
        verifyImageCrop();
        verifyImageDeletion();
        verifySaveScannedDocs();
        verifyPasswordSwitch();
        verifyPdfPasswordSwitchToggle();
    }

    public void verifyIfToolbarHidden() {
        onView(withId(R.id.toolbar)).check(matches(not(isDisplayed())));
    }


    public void verifyIfScanFragmentReplaced() {
        onView(withId(R.id.scan_doc_btn_automatic)).check(matches(isDisplayed()));
        onView(withId(R.id.scan_doc_btn_flash)).check(matches(isDisplayed()));
        onView(withId(R.id.scan_doc_btn_cancel)).check(matches(isDisplayed()));
        onView(withId(R.id.shutterButton)).check(matches(isDisplayed()));
    }


    public void verifyToggleAutomatic() {
        onView(withId(R.id.scan_doc_btn_automatic)).perform(click());
        onView(withId(R.id.scan_doc_btn_automatic)).check(matches(hasTextColor(R.color.grey_60)));

        onView(withId(R.id.scan_doc_btn_automatic)).perform(click());
        onView(withId(R.id.scan_doc_btn_automatic)).check(matches(hasTextColor(R.color.primary)));
    }


    public void verifyToggleFlash() {
        onView(withId(R.id.scan_doc_btn_flash)).perform(click());
        onView(withId(R.id.scan_doc_btn_flash)).check(matches(hasTextColor(R.color.primary)));

        onView(withId(R.id.scan_doc_btn_flash)).perform(click());
        onView(withId(R.id.scan_doc_btn_flash)).check(matches(hasTextColor(R.color.grey_60)));
    }


    public void captureAndVerifyDocScan() {
        onView(withId(R.id.shutterButton)).perform(click());
        shortSleep();
        shortSleep();
        shortSleep();
        docScanCount++;
        assertEquals(docScanCount, ScanActivity.scannedImages.size());
    }

    public void verifyScanMoreDocument() {
        onView(withId(R.id.scanMoreButton)).perform(click());
        captureAndVerifyDocScan();
    }

    public void verifyApplyFilter() {
        onView(withId(R.id.filterDocButton)).perform(click());

        onView(withText(R.string.edit_scan_filter_dialog_title))
            .inRoot(isDialog())
            .check(matches(isDisplayed()));

        onView(withText(R.string.edit_scan_filter_b_n_w))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
            .perform(click());

        shortSleep();
        shortSleep();
        shortSleep();
    }


    public void verifyRotateDocument() {
        onView(withId(R.id.rotateDocButton)).perform(click());
    }


    public void verifyImageCrop() {
        onView(withId(R.id.cropDocButton)).perform(click());

        onView(withId(R.id.crop_polygon_view)).check(matches(isDisplayed()));
        onView(withId(R.id.crop_btn_reset_borders)).check(matches(isDisplayed()));

        onView(withId(R.id.action_save)).perform(click());
    }


    public void verifyImageDeletion() {
        onView(withId(R.id.deleteDocButton)).perform(click());
        docScanCount--;
        assertEquals(docScanCount, ScanActivity.scannedImages.size());
    }


    public void verifySaveScannedDocs() {
        onView(withId(R.id.action_save)).perform(click());

        onView(withId(R.id.scan_save_filename_input)).check(matches(isDisplayed()));
        onView(withId(R.id.scan_save_location_input)).check(matches(isDisplayed()));
        onView(withId(R.id.scan_save_nested_scroll_view)).perform(swipeUp());

        onView(withId(R.id.scan_save_without_txt_recognition_pdf_checkbox)).check(matches(isDisplayed()));
        onView(withId(R.id.scan_save_without_txt_recognition_png_checkbox)).check(matches(isDisplayed()));
        onView(withId(R.id.scan_save_without_txt_recognition_jpg_checkbox)).check(matches(isDisplayed()));
        onView(withId(R.id.scan_save_with_txt_recognition_pdf_checkbox)).check(matches(isDisplayed()));
        onView(withId(R.id.scan_save_with_txt_recognition_txt_checkbox)).check(matches(isDisplayed()));

        onView(withId(R.id.scan_save_without_txt_recognition_pdf_checkbox)).check(matches(not(isChecked())));
        onView(withId(R.id.scan_save_without_txt_recognition_png_checkbox)).check(matches(not(isChecked())));
        onView(withId(R.id.scan_save_without_txt_recognition_jpg_checkbox)).check(matches(not(isChecked())));
        onView(withId(R.id.scan_save_with_txt_recognition_pdf_checkbox)).check(matches(isChecked()));
        onView(withId(R.id.scan_save_with_txt_recognition_txt_checkbox)).check(matches(not(isChecked())));

        onView(withId(R.id.scan_save_pdf_password_switch)).check(matches(isDisplayed()));
        onView(withId(R.id.scan_save_pdf_password_switch)).check(matches(isEnabled()));
        onView(withId(R.id.scan_save_pdf_password_switch)).check(matches(not(isChecked())));
        onView(withId(R.id.scan_save_pdf_password_text_input)).check(matches(not(isDisplayed())));

        onView(withId(R.id.save_scan_btn_cancel)).check(matches(isDisplayed()));
        onView(withId(R.id.save_scan_btn_save)).check(matches(isDisplayed()));
    }


    public void verifyPasswordSwitch() {
        onView(withId(R.id.scan_save_with_txt_recognition_pdf_checkbox)).perform(click());
        onView(withId(R.id.scan_save_pdf_password_switch)).check(matches(not(isEnabled())));
        onView(withId(R.id.scan_save_pdf_password_switch)).check(matches(not(isChecked())));

        onView(withId(R.id.scan_save_without_txt_recognition_pdf_checkbox)).perform(click());
        onView(withId(R.id.scan_save_pdf_password_switch)).check(matches(isEnabled()));
        onView(withId(R.id.scan_save_pdf_password_switch)).check(matches(not(isChecked())));

    }


    public void verifyPdfPasswordSwitchToggle() {
        onView(withId(R.id.scan_save_pdf_password_switch)).perform(click());
        onView(withId(R.id.scan_save_pdf_password_text_input)).check(matches(isDisplayed()));

        onView(withId(R.id.scan_save_pdf_password_switch)).perform(click());
        onView(withId(R.id.scan_save_pdf_password_text_input)).check(matches(not(isDisplayed())));
    }

}
