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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

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
    
}
