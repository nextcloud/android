/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Benedek Major <benedek@major.onl>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils;

import org.junit.Test;

import org.lukhnos.nnio.file.FileVisitResult;
import org.lukhnos.nnio.file.Path;
import org.lukhnos.nnio.file.SimpleFileVisitor;
import org.lukhnos.nnio.file.attribute.BasicFileAttributes;
import org.mockito.Mock;
import org.mockito.Mockito;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static com.owncloud.android.utils.FilesSyncHelper.walkFileTreeRandomly;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;



public class FilesSyncHelperTest {
    @Mock
    private File mockFile;

    @Mock
    private Path mockPath;

    @Test
    public void testWalkFileTreeNullHandling(){
        mockFile = mock(File.class);
        mockPath = mock(Path.class);
        Mockito.when(mockFile.isDirectory()).thenReturn(true);
        Mockito.when(mockPath.toFile()).thenReturn(mockFile);
        Mockito.when(mockFile.listFiles()).thenReturn(null);
        Mockito.when(mockFile.canRead()).thenReturn(true);

        SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        };
        try{
            walkFileTreeRandomly(mockPath,visitor);
        } catch (Exception e){
            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter( writer );
            e.printStackTrace( printWriter );
            printWriter.flush();

            String stackTrace = writer.toString();
            fail("walkFileTree throws an exception: \n" + stackTrace);
        }
    }
    
}