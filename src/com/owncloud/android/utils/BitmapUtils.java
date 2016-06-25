/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
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

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.utils.Log_OC;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.BitmapFactory.Options;
import android.media.ExifInterface;
import android.net.Uri;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Utility class with methods for decoding Bitmaps.
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

            // calculates the largest inSampleSize value (for smallest sample) that is a power of 2 and keeps both
            // height and width **larger** than the requested height and width.
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
     *  Convert HSL values to a RGB Color.
     *
     *  @param h Hue is specified as degrees in the range 0 - 360.
     *  @param s Saturation is specified as a percentage in the range 1 - 100.
     *  @param l Lumanance is specified as a percentage in the range 1 - 100.
     *  @paran alpha  the alpha value between 0 - 1
     *  adapted from https://svn.codehaus.org/griffon/builders/gfxbuilder/tags/GFXBUILDER_0.2/
     *  gfxbuilder-core/src/main/com/camick/awt/HSLColor.java
     */
    public static int[] HSLtoRGB(float h, float s, float l, float alpha)
    {
        if (s <0.0f || s > 100.0f)
        {
            String message = "Color parameter outside of expected range - Saturation";
            throw new IllegalArgumentException( message );
        }

        if (l <0.0f || l > 100.0f)
        {
            String message = "Color parameter outside of expected range - Luminance";
            throw new IllegalArgumentException( message );
        }

        if (alpha <0.0f || alpha > 1.0f)
        {
            String message = "Color parameter outside of expected range - Alpha";
            throw new IllegalArgumentException( message );
        }

        //  Formula needs all values between 0 - 1.

        h = h % 360.0f;
        h /= 360f;
        s /= 100f;
        l /= 100f;

        float q = 0;

        if (l < 0.5)
            q = l * (1 + s);
        else
            q = (l + s) - (s * l);

        float p = 2 * l - q;

        int r = Math.round(Math.max(0, HueToRGB(p, q, h + (1.0f / 3.0f)) * 256));
        int g = Math.round(Math.max(0, HueToRGB(p, q, h) * 256));
        int b = Math.round(Math.max(0, HueToRGB(p, q, h - (1.0f / 3.0f)) * 256));

        int[] array = {r, g, b};
        return array;
    }

    private static float HueToRGB(float p, float q, float h){
		if (h < 0) h += 1;

		if (h > 1 ) h -= 1;

		if (6 * h < 1)
		{
			return p + ((q - p) * 6 * h);
		}

		if (2 * h < 1 )
		{
			return  q;
		}

		if (3 * h < 2)
		{
			return p + ( (q - p) * 6 * ((2.0f / 3.0f) - h) );
		}

   		return p;
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

    /**
     * calculates the RGB value based on a given account name.
     *
     * @param accountName The account name
     * @return corresponding RGB color
     * @throws UnsupportedEncodingException if the charset is not supported
     * @throws NoSuchAlgorithmException if the specified algorithm is not available
     */
    public static int[] calculateRGB(String accountName) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        // using adapted algorithm from /core/js/placeholder.js:50
        String username = AccountUtils.getUsernameOfAccount(accountName);
        byte[] seed = username.getBytes("UTF-8");
        MessageDigest md = MessageDigest.getInstance("MD5");
//      Integer seedMd5Int = Math.abs(new String(Hex.encodeHex(seedMd5)).hashCode());
        Integer seedMd5Int = String.format(Locale.ROOT, "%032x",
                new BigInteger(1, md.digest(seed))).hashCode();

        double maxRange = Integer.MAX_VALUE;
        float hue = (float) (seedMd5Int / maxRange * 360);

        return BitmapUtils.HSLtoRGB(hue, 90.0f, 65.0f, 1.0f);
    }

    /**
     * Returns a new circular bitmap drawable by creating it from a bitmap, setting initial target density based on
     * the display metrics of the resources.
     *
     * @param resources the resources for initial target density
     * @param bitmap the original bitmap
     * @return the circular bitmap
     */
    public static RoundedBitmapDrawable bitmapToCircularBitmapDrawable(Resources resources, Bitmap bitmap) {
        RoundedBitmapDrawable roundedBitmap = RoundedBitmapDrawableFactory.create(resources, bitmap);
        roundedBitmap.setCircular(true);
        return roundedBitmap;
    }
}
