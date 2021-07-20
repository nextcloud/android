package com.owncloud.android.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringUtilsTest {

    @Test
    public void assertCorrectSingleSearchMatch() {

        String text = "this is a simple test";
        String searchText = "simple";
        int dummyColorInt = 44221;
        String expectedReturn = String.format("this is a <font color='%d'><b>%s</b></font> test",
                                              dummyColorInt, searchText);

        assertEquals("returned parsed text value was incorrect",
                     expectedReturn, StringUtils.searchAndColor(text, searchText, dummyColorInt));
    }

    @Test
    public void assertTextReturnedIfSearchTextIsEmpty() {
        String helloWorld = "hello world";
        assertEquals("text returned when searchText was not empty",
                     helloWorld, StringUtils.searchAndColor(helloWorld, "", 0));
    }

    @Test
    public void searchStringNull() {
        String text = "this is a simple test";
        String searchText = null;
        int dummyColorInt = 44221;
        String expectedReturn = "this is a simple test";

        assertEquals("returned parsed text value was incorrect",
                     expectedReturn, StringUtils.searchAndColor(text, searchText, dummyColorInt));
    }

    @Test
    public void searchStringEmpty() {
        String text = "this is a simple test";
        String searchText = "";
        int dummyColorInt = 44221;
        String expectedReturn = "this is a simple test";

        assertEquals("returned parsed text value was incorrect",
                     expectedReturn, StringUtils.searchAndColor(text, searchText, dummyColorInt));
    }

    @Test
    public void textNull() {
        String text = null;
        String searchText = "test";
        int dummyColorInt = 44221;
        String expectedReturn = "";

        assertEquals("returned parsed text value was incorrect",
                     expectedReturn, StringUtils.searchAndColor(text, searchText, dummyColorInt));
    }

    @Test
    public void textEmpty() {
        String text = "";
        String searchText = "test";
        int dummyColorInt = 44221;
        String expectedReturn = "";

        assertEquals("returned parsed text value was incorrect",
                     expectedReturn, StringUtils.searchAndColor(text, searchText, dummyColorInt));
    }

    @Test
    public void bothNull() {
        String text = null;
        String searchText = null;
        int dummyColorInt = 44221;
        String expectedReturn = "";

        assertEquals("returned parsed text value was incorrect",
                     expectedReturn, StringUtils.searchAndColor(text, searchText, dummyColorInt));
    }

    @Test
    public void bothEmpty() {
        String text = "";
        String searchText = "";
        int dummyColorInt = 44221;
        String expectedReturn = "";

        assertEquals("returned parsed text value was incorrect",
                     expectedReturn, StringUtils.searchAndColor(text, searchText, dummyColorInt));
    }
}
