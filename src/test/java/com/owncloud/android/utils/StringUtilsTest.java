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

    @Test
    public void prefixBothEmpty() {
        String text = "";
        String prefix = "";
        String expectedReturn = "";

        assertEquals("returned text without prefix was incorrect",
                     expectedReturn, StringUtils.removePrefix(text, prefix));
    }

    @Test
    public void noPrefix() {
        String text = "/this/is/some/path";
        String prefix = "/that/is/another/path";
        String expectedReturn = "/this/is/some/path";

        assertEquals("returned text without prefix was incorrect",
                     expectedReturn, StringUtils.removePrefix(text, prefix));
    }

    @Test
    public void simplePrefix() {
        String text = "/path/and/subpath";
        String prefix = "/path";
        String expectedReturn = "/and/subpath";

        assertEquals("returned text without prefix was incorrect",
                     expectedReturn, StringUtils.removePrefix(text, prefix));
    }

    @Test
    public void prefixEqual() {
        String text = "/path/and/subpath";
        String prefix = "/path/and/subpath";
        String expectedReturn = "";

        assertEquals("returned text without prefix was incorrect",
                     expectedReturn, StringUtils.removePrefix(text, prefix));
    }
}
