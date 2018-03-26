package com.owncloud.android.utils;

import junit.framework.Assert;

import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import static junit.framework.Assert.assertEquals;

/**
 * Unit tests for the ({@link IOHelper}) class.
 */

public class IOHelperTest {

    private final static String outputFileName = "output.txt";

    @Test
    public void testInputStreamIsClosed() throws Exception {
        // Define FileOutputStream for testing.
        FileOutputStream mOutputStream = null;
        StringWriter test = null;
        try {
            // init object
            mOutputStream = new FileOutputStream(outputFileName);
            test = new StringWriter();

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            // Close output stream.
            IOHelper.close(mOutputStream);
        }
        // try to write to stream. This should fail since we have now closed the output stream.
        try {
            mOutputStream.write("Hello World".getBytes());
            Assert.fail("No exception was thrown");
        } catch (IOException e) {
            assertEquals(e.getMessage(), "Stream Closed");
        }

        Files.deleteIfExists(Paths.get(outputFileName));
    }
}
