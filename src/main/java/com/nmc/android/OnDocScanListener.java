package com.nmc.android;

import android.graphics.Bitmap;

import java.io.File;
import java.util.List;

public interface OnDocScanListener {
    void addScannedDoc(File file);

    List<File> getScannedDocs();

    boolean removedScannedDoc(File file);

    File replaceScannedDoc(int index, File newFile);
}
