package com.nmc.android;

import android.graphics.Bitmap;

import java.io.File;
import java.util.List;

public interface OnDocScanListener {
    void addScannedDoc(Bitmap file);

    List<Bitmap> getScannedDocs();

    boolean removedScannedDoc(Bitmap file);

    Bitmap replaceScannedDoc(int index, Bitmap newFile);
}
