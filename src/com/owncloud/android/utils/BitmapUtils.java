/* ownCloud Android client application
 *   Copyright (C) 2012-2014 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.utils;

import com.owncloud.android.lib.common.utils.Log_OC;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.BitmapFactory.Options;
import android.media.ExifInterface;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;

/**
 * Utility class with methods for decoding Bitmaps.
 * 
 * @author David A. Velasco
 */
public class BitmapUtils {
    
    
    /**
     * Decodes a bitmap from a file containing it minimizing the memory use, known that the bitmap
     * will be drawn in a surface of reqWidth x reqHeight
     * 
     * @param srcPath       Absolute path to the file containing the image.
     * @param reqWidth      Width of the surface where the Bitmap will be drawn on, in pixels.
     * @param reqHeight     Height of the surface where the Bitmap will be drawn on, in pixels.
     * @return
     */
    public static Bitmap decodeSampledBitmapFromFile(String srcPath, int reqWidth, int reqHeight) {
    
        // set desired options that will affect the size of the bitmap
        final Options options = new Options();
        options.inScaled = true;
        options.inPurgeable = true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
            options.inPreferQualityOverSpeed = false;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            options.inMutable = false;
        }
        
        // make a false load of the bitmap to get its dimensions
        options.inJustDecodeBounds = true;
        
        BitmapFactory.decodeFile(srcPath, options);   
        
        // calculate factor to subsample the bitmap
        options.inSampleSize = calculateSampleFactor(options, reqWidth, reqHeight);

        // decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(srcPath, options);
        
    }    


    /**
     * Calculates a proper value for options.inSampleSize in order to decode a Bitmap minimizing 
     * the memory overload and covering a target surface of reqWidth x reqHeight if the original
     * image is big enough. 
     * 
     * @param options       Bitmap decoding options; options.outHeight and options.inHeight should
     *                      be set. 
     * @param reqWidth      Width of the surface where the Bitmap will be drawn on, in pixels.
     * @param reqHeight     Height of the surface where the Bitmap will be drawn on, in pixels.
     * @return              The largest inSampleSize value that is a power of 2 and keeps both
     *                      height and width larger than reqWidth and reqHeight.
     */
    private static int calculateSampleFactor(Options options, int reqWidth, int reqHeight) {
        
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
    
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
    
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        
        return inSampleSize;
    }
    
    /**
     * Rotate bitmap according to EXIF orientation. 
     * Cf. http://www.daveperrett.com/articles/2012/07/28/exif-orientation-handling-is-a-ghetto/ 
     * @param bitmap Bitmap to be rotated
     * @param storagePath Path to source file of bitmap. Needed for EXIF information. 
     * @return correctly EXIF-rotated bitmap
     */
    public static Bitmap rotateImage(Bitmap bitmap, String storagePath){
        Bitmap resultBitmap = bitmap;

        try
        {
            ExifInterface exifInterface = new ExifInterface(storagePath);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

            Matrix matrix = new Matrix();

            // 1: nothing to do
            
            // 2
            if (orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL)
            {
                matrix.postScale(-1.0f, 1.0f);
            }
            // 3
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
            {
                matrix.postRotate(180);
            }
            // 4
            else if (orientation == ExifInterface.ORIENTATION_FLIP_VERTICAL)
            {
                matrix.postScale(1.0f, -1.0f);
            }
            // 5
            else if (orientation == ExifInterface.ORIENTATION_TRANSPOSE)
            {
                matrix.postRotate(-90);
                matrix.postScale(1.0f, -1.0f);
            }
            // 6
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_90)
            {
                matrix.postRotate(90);
            }
            // 7
            else if (orientation == ExifInterface.ORIENTATION_TRANSVERSE)
            {
                matrix.postRotate(90);
                matrix.postScale(1.0f, -1.0f);
            }
            // 8
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270)
            {
                matrix.postRotate(270);
            } 
            
            // Rotate the bitmap
            resultBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (resultBitmap != bitmap) {
                bitmap.recycle();
            }
        }
        catch (Exception exception)
        {
            Log_OC.e("BitmapUtil", "Could not rotate the image: " + storagePath);
        }
        return resultBitmap;
    }

    /**
     * Checks if file passed is an image
     * @param file
     * @return true/false
     */
    public static boolean isImage(File file) {
        Uri selectedUri = Uri.fromFile(file);
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString().toLowerCase());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);

        return (mimeType != null && mimeType.startsWith("image/"));
    }
    
}
