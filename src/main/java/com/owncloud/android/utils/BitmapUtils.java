/*
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2015 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.codec.binary.Hex;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.exifinterface.media.ExifInterface;

/**
 * Utility class with methods for decoding Bitmaps.
 */
public final class BitmapUtils {
    public static final String TAG = BitmapUtils.class.getSimpleName();

    private static final int INDEX_RED = 0;
    private static final int INDEX_GREEN = 1;
    private static final int INDEX_BLUE = 2;
    private static final int INDEX_HUE = 0;
    private static final int INDEX_SATURATION = 1;
    private static final int INDEX_LUMINATION = 2;

    private BitmapUtils() {
        // utility class -> private constructor
    }

    /**
     * Decodes a bitmap from a file containing it minimizing the memory use, known that the bitmap
     * will be drawn in a surface of reqWidth x reqHeight
     *
     * @param srcPath       Absolute path to the file containing the image.
     * @param reqWidth      Width of the surface where the Bitmap will be drawn on, in pixels.
     * @param reqHeight     Height of the surface where the Bitmap will be drawn on, in pixels.
     * @return decoded bitmap
     */
    public static Bitmap decodeSampledBitmapFromFile(String srcPath, int reqWidth, int reqHeight) {

        // set desired options that will affect the size of the bitmap
        final Options options = new Options();
        options.inScaled = true;
        options.inPurgeable = true;
        options.inPreferQualityOverSpeed = false;
        options.inMutable = false;

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
     * @return The largest inSampleSize value that is a power of 2 and keeps both
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
            while ((halfHeight / inSampleSize) > reqHeight || (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * scales a given bitmap depending on the given size parameters.
     *
     * @param bitmap the bitmap to be scaled
     * @param px     the target pixel size
     * @param width  the width
     * @param height the height
     * @param max    the max(height, width)
     * @return the scaled bitmap
     */
    public static Bitmap scaleBitmap(Bitmap bitmap, float px, int width, int height, int max) {
        float scale = px / max;
        int w = Math.round(scale * width);
        int h = Math.round(scale * height);
        return Bitmap.createScaledBitmap(bitmap, w, h, true);
    }

    /**
     * Rotate bitmap according to EXIF orientation.
     * Cf. http://www.daveperrett.com/articles/2012/07/28/exif-orientation-handling-is-a-ghetto/
     * @param bitmap Bitmap to be rotated
     * @param storagePath Path to source file of bitmap. Needed for EXIF information.
     * @return correctly EXIF-rotated bitmap
     */
    public static Bitmap rotateImage(Bitmap bitmap, String storagePath) {
        Bitmap resultBitmap = bitmap;

        try {
            ExifInterface exifInterface = new ExifInterface(storagePath);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

            Matrix matrix = new Matrix();

            // 1: nothing to do

            // 2
            if (orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL) {
                matrix.postScale(-1.0f, 1.0f);
            }
            // 3
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                matrix.postRotate(180);
            }
            // 4
            else if (orientation == ExifInterface.ORIENTATION_FLIP_VERTICAL) {
                matrix.postScale(1.0f, -1.0f);
            }
            // 5
            else if (orientation == ExifInterface.ORIENTATION_TRANSPOSE) {
                matrix.postRotate(-90);
                matrix.postScale(1.0f, -1.0f);
            }
            // 6
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                matrix.postRotate(90);
            }
            // 7
            else if (orientation == ExifInterface.ORIENTATION_TRANSVERSE) {
                matrix.postRotate(90);
                matrix.postScale(1.0f, -1.0f);
            }
            // 8
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                matrix.postRotate(270);
            }

            // Rotate the bitmap
            resultBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (!resultBitmap.equals(bitmap)) {
                bitmap.recycle();
            }
        } catch (Exception exception) {
            Log_OC.e("BitmapUtil", "Could not rotate the image: " + storagePath);
        }
        return resultBitmap;
    }

    /**
     *  Convert HSL values to a RGB Color.
     *
     *  @param h Hue is specified as degrees in the range 0 - 360.
     *  @param s Saturation is specified as a percentage in the range 1 - 100.
     *  @param l Luminance is specified as a percentage in the range 1 - 100.
     *  @param alpha  the alpha value between 0 - 1
     *  adapted from https://svn.codehaus.org/griffon/builders/gfxbuilder/tags/GFXBUILDER_0.2/
     *  gfxbuilder-core/src/main/com/camick/awt/HSLColor.java
     */
    @SuppressWarnings("PMD.MethodNamingConventions")
    public static int[] HSLtoRGB(float h, float s, float l, float alpha) {
        if (s < 0.0f || s > 100.0f) {
            String message = "Color parameter outside of expected range - Saturation";
            throw new IllegalArgumentException(message);
        }

        if (l < 0.0f || l > 100.0f) {
            String message = "Color parameter outside of expected range - Luminance";
            throw new IllegalArgumentException(message);
        }

        if (alpha < 0.0f || alpha > 1.0f) {
            String message = "Color parameter outside of expected range - Alpha";
            throw new IllegalArgumentException(message);
        }

        //  Formula needs all values between 0 - 1.

        h = h % 360.0f;
        h /= 360f;
        s /= 100f;
        l /= 100f;

        float q;

        if (l < 0.5) {
            q = l * (1 + s);
        } else {
            q = (l + s) - (s * l);
        }

        float p = 2 * l - q;

        int r = Math.round(Math.max(0, HueToRGB(p, q, h + (1.0f / 3.0f)) * 256));
        int g = Math.round(Math.max(0, HueToRGB(p, q, h) * 256));
        int b = Math.round(Math.max(0, HueToRGB(p, q, h - (1.0f / 3.0f)) * 256));

        return new int[]{r, g, b};
    }

    @SuppressWarnings("PMD.MethodNamingConventions")
    private static float HueToRGB(float p, float q, float h) {
        if (h < 0) {
            h += 1;
        }

        if (h > 1) {
            h -= 1;
        }

        if (6 * h < 1) {
            return p + ((q - p) * 6 * h);
        }

        if (2 * h < 1) {
            return q;
        }

        if (3 * h < 2) {
            return p + ((q - p) * 6 * (2.0f / 3.0f - h));
        }

        return p;
    }

    /**
     * calculates the RGB value based on a given account name.
     *
     * @param name The name
     * @return corresponding RGB color
     * @throws NoSuchAlgorithmException     if the specified algorithm is not available
     */
    public static int[] calculateHSL(String name) throws NoSuchAlgorithmException {
        // using adapted algorithm from https://github.com/nextcloud/server/blob/master/core/js/placeholder.js#L126

        String[] result = new String[]{"0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0"};
        double[] rgb = new double[]{0, 0, 0};
        int sat = 70;
        int lum = 68;
        int modulo = 16;

        String hash = name.toLowerCase(Locale.ROOT).replaceAll("[^0-9a-f]", "");

        if (!hash.matches("^[0-9a-f]{32}")) {
            hash = md5(hash);
        }

        // Splitting evenly the string
        for (int i = 0; i < hash.length(); i++) {
            result[i % modulo] = result[i % modulo] + Integer.parseInt(hash.substring(i, i + 1), 16);
        }

        // Converting our data into a usable rgb format
        // Start at 1 because 16%3=1 but 15%3=0 and makes the repartition even
        for (int count = 1; count < modulo; count++) {
            rgb[count % 3] += Integer.parseInt(result[count]);
        }

        // Reduce values bigger than rgb requirements
        rgb[INDEX_RED] = rgb[INDEX_RED] % 255;
        rgb[INDEX_GREEN] = rgb[INDEX_GREEN] % 255;
        rgb[INDEX_BLUE] = rgb[INDEX_BLUE] % 255;

        double[] hsl = rgbToHsl(rgb[INDEX_RED], rgb[INDEX_GREEN], rgb[INDEX_BLUE]);

        // Classic formula to check the brightness for our eye
        // If too bright, lower the sat
        double bright = Math.sqrt(0.299 * Math.pow(rgb[INDEX_RED], 2) + 0.587 * Math.pow(rgb[INDEX_GREEN], 2) + 0.114
                * Math.pow(rgb[INDEX_BLUE], 2));

        if (bright >= 200) {
            sat = 60;
        }

        return new int[]{(int) (hsl[INDEX_HUE] * 360), sat, lum};
    }

    private static double[] rgbToHsl(double rUntrimmed, double gUntrimmed, double bUntrimmed) {
        double r = rUntrimmed / 255;
        double g = gUntrimmed / 255;
        double b = bUntrimmed / 255;

        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double h = (max + min) / 2;
        double s;
        double l = (max + min) / 2;

        if (max == min) {
            h = s = 0; // achromatic
        } else {
            double d = max - min;
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);

            if (max == r) {
                h = (g - b) / d + (g < b ? 6 : 0);
            } else if (max == g) {
                h = (b - r) / d + 2;
            } else if (max == b) {
                h = (r - g) / d + 4;
            }
            h /= 6;
        }

        double[] hsl = new double[]{0.0, 0.0, 0.0};
        hsl[INDEX_HUE] = h;
        hsl[INDEX_SATURATION] = s;
        hsl[INDEX_LUMINATION] = l;

        return hsl;
    }

    public static String md5(String string) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(string.getBytes(Charset.defaultCharset()));

        return new String(Hex.encodeHex(md5.digest()));
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
        if (bitmap == null) {
            return null;
        }

        RoundedBitmapDrawable roundedBitmap = RoundedBitmapDrawableFactory.create(resources, bitmap);
        roundedBitmap.setCircular(true);
        return roundedBitmap;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        Bitmap bitmap;
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
