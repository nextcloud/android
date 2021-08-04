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

    @Test
    public void escapeString() {
        // we cannot use R.string.report_issue_template
        // filled with a random stacktrace
        String raw = "### Stacktrace\n" +
            "java.lang.RuntimeException\n" +
            "at com.owncloud.android.ui.activity.DrawerActivity.onNavigationItemClicked(DrawerActivity.java:423)\n" +
            "at com.owncloud.android.ui.activity.DrawerActivity.lambda$null$0$DrawerActivity(DrawerActivity.java:383)\n" +
            "at com.owncloud.android.ui.activity.-$$Lambda$DrawerActivity$WuFXuKRKoNjsgnTzctIqSi-LJ_I.run(Unknown Source:4)\n" +
            "at android.os.Handler.handleCallback(Handler.java:883)\n" +
            "at android.os.Handler.dispatchMessage(Handler.java:100)\n" +
            "at android.os.Looper.loop(Looper.java:214)\n" +
            "at android.app.ActivityThread.main(ActivityThread.java:7356)\n" +
            "at java.lang.reflect.Method.invoke(Native Method)\n" +
            "at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:492)\n" +
            "at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:930)\n" +
            "\n" +
            "### Steps to reproduce\n" +
            "1. \n" +
            "2. \n" +
            "3. \n" +
            "\n" +
            "### Expected behaviour\n" +
            "- Tell us what should happen\n" +
            "\n" +
            "### Actual behaviour\n" +
            "- Tell us what happens\n" +
            "\n" +
            "### Can you reproduce this problem on https://try.nextcloud.com?\n" +
            "- Please create a test demo account and see if this still happens there.\n" +
            "- If yes, please open up a bug report\n" +
            "- If not, please verify server setup and ask for help on forum\n" +
            "\n" +
            "### Environment data\n" +
            "Android version:\n" +
            "\n" +
            "Device model: \n" +
            "\n" +
            "Stock or customized system:\n" +
            "\n" +
            "Nextcloud app version:\n" +
            "\n" +
            "Nextcloud server version:\n" +
            "\n" +
            "Reverse proxy:\n" +
            "\n" +
            "### Logs\n" +
            "#### Web server error log\n" +
            "```\n" +
            "Insert your webserver log here\n" +
            "```\n" +
            "\n" +
            "#### Nextcloud log (data/nextcloud.log)\n" +
            "```\n" +
            "Insert your Nextcloud log here\n" +
            "```\n" +
            "**NOTE:** Be super sure to remove sensitive data like passwords, note that everybody can look here! You " +
            "can use the Issue Template application to prefill some of the required information: https://apps.nextcloud.com/apps/issuetemplate\n";

        String escaped = "###+Stacktrace%0A" +
            "java.lang.RuntimeException%0A" +
            "at+com.owncloud.android.ui.activity.DrawerActivity.onNavigationItemClicked(DrawerActivity.java:423)%0A" +
            "at+com.owncloud.android.ui.activity.DrawerActivity.lambda$null$0$DrawerActivity(DrawerActivity.java:383)%0A" +
            "at+com.owncloud.android.ui.activity.-$$Lambda$DrawerActivity$WuFXuKRKoNjsgnTzctIqSi-LJ_I.run(Unknown+Source:4)%0A" +
            "at+android.os.Handler.handleCallback(Handler.java:883)%0A" +
            "at+android.os.Handler.dispatchMessage(Handler.java:100)%0A" +
            "at+android.os.Looper.loop(Looper.java:214)%0A" +
            "at+android.app.ActivityThread.main(ActivityThread.java:7356)%0A" +
            "at+java.lang.reflect.Method.invoke(Native+Method)%0A" +
            "at+com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:492)%0A" +
            "at+com.android.internal.os.ZygoteInit.main(ZygoteInit.java:930)%0A" +
            "%0A" +
            "###+Steps+to+reproduce%0A" +
            "1.+%0A" +
            "2.+%0A" +
            "3.+%0A" +
            "%0A" +
            "###+Expected+behaviour%0A" +
            "-+Tell+us+what+should+happen%0A" +
            "%0A" +
            "###+Actual+behaviour%0A" +
            "-+Tell+us+what+happens%0A" +
            "%0A" +
            "###+Can+you+reproduce+this+problem+on+https://try.nextcloud.com?%0A" +
            "-+Please+create+a+test+demo+account+and+see+if+this+still+happens+there.%0A" +
            "-+If+yes,+please+open+up+a+bug+report%0A" +
            "-+If+not,+please+verify+server+setup+and+ask+for+help+on+forum%0A" +
            "%0A" +
            "###+Environment+data%0A" +
            "Android+version:%0A" +
            "%0A" +
            "Device+model:+%0A" +
            "%0A" +
            "Stock+or+customized+system:%0A" +
            "%0A" +
            "Nextcloud+app+version:%0A" +
            "%0A" +
            "Nextcloud+server+version:%0A" +
            "%0A" +
            "Reverse+proxy:%0A" +
            "%0A" +
            "###+Logs%0A" +
            "####+Web+server+error+log%0A" +
            "```%0A" +
            "Insert+your+webserver+log+here%0A" +
            "```%0A" +
            "%0A" +
            "####+Nextcloud+log+(data/nextcloud.log)%0A" +
            "```%0A" +
            "Insert+your+Nextcloud+log+here%0A" +
            "```%0A" +
            "**NOTE:**+Be+super+sure+to+remove+sensitive+data+like+passwords,+note+that+everybody+can+look+here!+You+" +
            "can+use+the+Issue+Template+application+to+prefill+some+of+the+required+information:+https://apps.nextcloud.com/apps/issuetemplate%0A";

        assertEquals(escaped, StringUtils.escapeStacktrace(raw));
    }
}
