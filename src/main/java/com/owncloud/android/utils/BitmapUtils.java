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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.Status;
import com.owncloud.android.lib.resources.users.StatusType;
import com.owncloud.android.ui.StatusDrawable;

import org.apache.commons.codec.binary.Hex;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.exifinterface.media.ExifInterface;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class with methods for decoding Bitmaps.
 */
public final class BitmapUtils {
    public static final String TAG = BitmapUtils.class.getSimpleName();

    private BitmapUtils() {
        // utility class -> private constructor
    }

    /**
     * Decodes a bitmap from a file containing it minimizing the memory use, known that the bitmap will be drawn in a
     * surface of reqWidth x reqHeight
     *
     * @param srcPath   Absolute path to the file containing the image.
     * @param reqWidth  Width of the surface where the Bitmap will be drawn on, in pixels.
     * @param reqHeight Height of the surface where the Bitmap will be drawn on, in pixels.
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
     * Calculates a proper value for options.inSampleSize in order to decode a Bitmap minimizing the memory overload and
     * covering a target surface of reqWidth x reqHeight if the original image is big enough.
     *
     * @param options   Bitmap decoding options; options.outHeight and options.inHeight should be set.
     * @param reqWidth  Width of the surface where the Bitmap will be drawn on, in pixels.
     * @param reqHeight Height of the surface where the Bitmap will be drawn on, in pixels.
     * @return The largest inSampleSize value that is a power of 2 and keeps both height and width larger than reqWidth
     * and reqHeight.
     */
    public static int calculateSampleFactor(Options options, int reqWidth, int reqHeight) {

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
     * Rotate bitmap according to EXIF orientation. Cf. http://www.daveperrett.com/articles/2012/07/28/exif-orientation-handling-is-a-ghetto/
     *
     * @param bitmap      Bitmap to be rotated
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

    public static Color usernameToColor(String name) {
        String hash = name.toLowerCase(Locale.ROOT);

        // already a md5 hash?
        if (!hash.matches("([0-9a-f]{4}-?){8}$")) {
            try {
                hash = md5(hash);
            } catch (NoSuchAlgorithmException e) {
                int color = getResources().getColor(R.color.primary_dark);
                return new Color(android.graphics.Color.red(color),
                                 android.graphics.Color.green(color),
                                 android.graphics.Color.blue(color));
            }
        }

        hash = hash.replaceAll("[^0-9a-f]", "");
        int steps = 6;

        Color[] finalPalette = generateColors(steps);

        return finalPalette[hashToInt(hash, steps * 3)];
    }

    private static int hashToInt(String hash, int maximum) {
        int finalInt = 0;
        int[] result = new int[hash.length()];

        // splitting evenly the string
        for (int i = 0; i < hash.length(); i++) {
            // chars in md5 goes up to f, hex: 16
            result[i] = Integer.parseInt(String.valueOf(hash.charAt(i)), 16) % 16;
        }

        // adds up all results
        for (int value : result) {
            finalInt += value;
        }

        // chars in md5 goes up to f, hex:16
        // make sure we're always using int in our operation
        return Integer.parseInt(String.valueOf(Integer.parseInt(String.valueOf(finalInt), 10) % maximum), 10);
    }

    private static Color[] generateColors(int steps) {
        Color red = new Color(182, 70, 157);
        Color yellow = new Color(221, 203, 85);
        Color blue = new Color(0, 130, 201); // Nextcloud blue

        Color[] palette1 = mixPalette(steps, red, yellow);
        Color[] palette2 = mixPalette(steps, yellow, blue);
        Color[] palette3 = mixPalette(steps, blue, red);

        Color[] resultPalette = new Color[palette1.length + palette2.length + palette3.length];
        System.arraycopy(palette1, 0, resultPalette, 0, palette1.length);
        System.arraycopy(palette2, 0, resultPalette, palette1.length, palette2.length);
        System.arraycopy(palette3,
                         0,
                         resultPalette,
                         palette1.length + palette2.length,
                         palette1.length);

        return resultPalette;
    }

    @SuppressFBWarnings("CLI_CONSTANT_LIST_INDEX")
    private static Color[] mixPalette(int steps, Color color1, Color color2) {
        Color[] palette = new Color[steps];
        palette[0] = color1;

        float[] step = stepCalc(steps, color1, color2);
        for (int i = 1; i < steps; i++) {
            int r = (int) (color1.r + step[0] * i);
            int g = (int) (color1.g + step[1] * i);
            int b = (int) (color1.b + step[2] * i);

            palette[i] = new Color(r, g, b);
        }

        return palette;
    }

    private static float[] stepCalc(int steps, Color color1, Color color2) {
        float[] step = new float[3];

        step[0] = (color2.r - color1.r) / (float) steps;
        step[1] = (color2.g - color1.g) / (float) steps;
        step[2] = (color2.b - color1.b) / (float) steps;

        return step;
    }

    public static class Color {
        public int a = 255;
        public int r;
        public int g;
        public int b;

        public Color(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public Color(int a, int r, int g, int b) {
            this.a = a;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof Color)) {
                return false;
            }

            Color other = (Color) obj;
            return this.r == other.r && this.g == other.g && this.b == other.b;
        }

        @Override
        public int hashCode() {
            return r * 10000 + g * 1000 + b;
        }
    }

    public static String md5(String string) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(string.getBytes(Charset.defaultCharset()));

        return new String(Hex.encodeHex(md5.digest()));
    }

    /**
     * Returns a new circular bitmap drawable by creating it from a bitmap, setting initial target density based on the
     * display metrics of the resources.
     *
     * @param resources the resources for initial target density
     * @param bitmap    the original bitmap
     * @return the circular bitmap
     */
    public static RoundedBitmapDrawable bitmapToCircularBitmapDrawable(Resources resources,
                                                                       Bitmap bitmap,
                                                                       float radius) {
        if (bitmap == null) {
            return null;
        }

        RoundedBitmapDrawable roundedBitmap = RoundedBitmapDrawableFactory.create(resources, bitmap);
        roundedBitmap.setCircular(true);

        if (radius != -1) {
            roundedBitmap.setCornerRadius(radius);
        }

        return roundedBitmap;
    }

    public static RoundedBitmapDrawable bitmapToCircularBitmapDrawable(Resources resources, Bitmap bitmap) {
        return bitmapToCircularBitmapDrawable(resources, bitmap, -1);
    }

    public static void setRoundedBitmap(Resources resources, Bitmap bitmap, float radius, ImageView imageView) {

        imageView.setImageDrawable(BitmapUtils.bitmapToCircularBitmapDrawable(resources,
                                                                              bitmap,
                                                                              radius));
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
            if (drawable.getBounds().width() > 0 && drawable.getBounds().height() > 0) {
                bitmap = Bitmap.createBitmap(drawable.getBounds().width(),
                                             drawable.getBounds().height(),
                                             Bitmap.Config.ARGB_8888);
            } else {
                bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            }
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                                         drawable.getIntrinsicHeight(),
                                         Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static void setRoundedBitmap(Bitmap thumbnail, ImageView imageView) {
        BitmapUtils.setRoundedBitmap(getResources(),
                                     thumbnail,
                                     getResources().getDimension(R.dimen.file_icon_rounded_corner_radius),
                                     imageView);
    }

    public static void setRoundedBitmapForGridMode(Bitmap thumbnail, ImageView imageView) {
        BitmapUtils.setRoundedBitmap(getResources(),
                                     thumbnail,
                                     getResources().getDimension(R.dimen.file_icon_rounded_corner_radius_for_grid_mode),
                                     imageView);
    }

    public static Bitmap createAvatarWithStatus(Bitmap avatar, StatusType statusType, String icon, Context context) {
        float avatarRadius = getResources().getDimension(R.dimen.list_item_avatar_icon_radius);
        int width = DisplayUtils.convertDpToPixel(2 * avatarRadius, context);

        Bitmap output = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // avatar
        Bitmap croppedBitmap = getCroppedBitmap(avatar, width);

        canvas.drawBitmap(croppedBitmap, 0f, 0f, null);

        // status
        int statusSize = width / 4;

        Status status = new Status(statusType, "", icon, -1);
        StatusDrawable statusDrawable = new StatusDrawable(status, statusSize, context);

        canvas.translate(width / 2f, width / 2f);
        statusDrawable.draw(canvas);

        return output;
    }

    /**
     * from https://stackoverflow.com/a/12089127
     */
    private static Bitmap getCroppedBitmap(Bitmap bitmap, int width) {
        Bitmap output = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        int color = -0xbdbdbe;
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, width, width);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);

        canvas.drawCircle(width / 2f, width / 2f, width / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        canvas.drawBitmap(Bitmap.createScaledBitmap(bitmap, width, width, false), rect, rect, paint);

        return output;
    }

    private static Resources getResources() {
        return MainApp.getAppContext().getResources();
    }
}
