package com.owncloud.android.utils;

import android.content.Context;

import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.*;


public class CacheManagerTest {




    public static File createFile(String name, int iteration) throws IOException {
        Context instrumentationCtx = ApplicationProvider.getApplicationContext();

        File file = new File(instrumentationCtx.getCacheDir() + File.separator + name);
        if (!file.getParentFile().exists()) {
            assertTrue(file.getParentFile().mkdirs());
        }

        file.createNewFile();

        FileWriter writer = new FileWriter(file);

        for (int i = 0; i < iteration; i++) {
            writer.write("123123123123123123123123123\n");
        }
        writer.flush();
        writer.close();

        return file;
    }

    /**
     *
     * @throws IOException
     */
    @Test
    public void testCacheManager() throws IOException {
        File testFile = createFile("text.txt", 1);
        Context instrumentationCtx = ApplicationProvider.getApplicationContext();

        assertTrue(testFile.exists());

        CacheManager cache = new CacheManager();
        cache.deleteCache(instrumentationCtx);

        assertFalse(testFile.exists());
    }


}