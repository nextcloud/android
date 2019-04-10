package com.owncloud.android.utils;

import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.*;

public class BitmapUtilsTest {


    @Test
    public void calculateHSL_assertCorrectRGBArrayReturned() throws Exception {
        int[] rgbArray = BitmapUtils.calculateHSL("Edvard");
    }

    @Test
    public void HSLtoRGB_assertCorrectRGBArrayReturned() throws Exception {
        int[] rgbArray = BitmapUtils.HSLtoRGB(300.0f, 90.0f, 99.0f, 1.0f);
        String format = "hello";
    }

}
