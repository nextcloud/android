package com.nmc.android.interfaces;

import android.graphics.Bitmap;

import java.io.File;
import java.util.List;

public interface OnDocScanListener {
    void addScannedDoc(Bitmap file);

    List<Bitmap> getScannedDocs();

    boolean removedScannedDoc(Bitmap file, int index);

    //isFilterApplied will tell whether the filter is applied to the image or not
    Bitmap replaceScannedDoc(int index, Bitmap newFile, boolean isFilterApplied);

    void replaceFilterIndex(int index, int filterIndex);
}
