package com.owncloud.android.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilsTest {

    @Test
    public void searchAndColor_assertCorrectSingleSearchMatch() {

        String text = "this is a simple test";
        String searchText = "simple";
        int dummyColorInt = 44221;
        String expectedReturn = String.format("this is a <font color='%d'><b>%s</b></font> test",
                                              dummyColorInt, searchText);

        assertEquals("returned parsed text value was incorrect",
                     expectedReturn, StringUtils.searchAndColor(text, searchText, dummyColorInt));
    }

    @Test
    public void searchAndColor_assertTextReturnedIfSearchTextIsEmpty() {
        String helloWorld = "hello world";
        assertEquals("text returned when searchText was not empty",
                     helloWorld, StringUtils.searchAndColor(helloWorld, "", 0));
    }
}
