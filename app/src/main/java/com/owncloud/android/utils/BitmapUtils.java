/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 23017-2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.ImageView;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.Status;
import com.owncloud.android.lib.resources.users.StatusType;
import com.owncloud.android.ui.StatusDrawable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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

    public static Bitmap addColorFilter(Bitmap originalBitmap, int filterColor, int opacity) {
        Bitmap resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(resultBitmap, 0, 0, null);

        Paint paint = new Paint();
        paint.setColor(filterColor);

        paint.setAlpha(opacity);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        canvas.drawRect(0, 0, resultBitmap.getWidth(), resultBitmap.getHeight(), paint);

        return resultBitmap;
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private static Bitmap decodeSampledBitmapViaImageDecoder(@NonNull File file, int reqWidth, int reqHeight) {
        try {
            Log_OC.i(TAG, "Decoding Bitmap via ImageDecoder");

            final var imageDecoderSource = ImageDecoder.createSource(file);

            final var onDecoderListener = new ImageDecoder.OnHeaderDecodedListener() {
                @Override
                public void onHeaderDecoded(@NonNull ImageDecoder decoder, @NonNull ImageDecoder.ImageInfo info, @NonNull ImageDecoder.Source source) {
                    decoder.setTargetSize(reqWidth, reqHeight);
                }
            };

            return ImageDecoder.decodeBitmap(imageDecoderSource, onDecoderListener);
        } catch (IOException exception) {
            Log_OC.w(TAG, "Decoding Bitmap via ImageDecoder failed, BitmapFactory.decodeFile will be used");
            return null;
        }
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
        final var file = new File(srcPath);
        if (!file.exists()) {
            Log_OC.w(TAG, "File does not exists, returning empty bitmap");
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            final var result = decodeSampledBitmapViaImageDecoder(file, reqWidth, reqHeight);
            if (result != null) {
                return result;
            }
        }

        Log_OC.i(TAG, "Decoding Bitmap via BitmapFactory.decodeFile");

        // set desired options that will affect the size of the bitmap
        final Options options = new Options();

        // make a false load of the bitmap to get its dimensions
        options.inJustDecodeBounds = true;

        // FIXME after auto-rename can't generate thumbnail from localPath
        BitmapFactory.decodeFile(srcPath, options);

        // calculate factor to subsample the bitmap
        options.inSampleSize = calculateSampleFactor(options, reqWidth, reqHeight);

        // decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(srcPath, options);
    }

    /**
     * Decodes a bitmap from a file containing it minimizing the memory use. Scales image to screen size.
     *
     * @param storagePath   Absolute path to the file containing the image.
     */
    public static Bitmap retrieveBitmapFromFile(String storagePath, int minWidth, int minHeight){
        // Get the original dimensions of the bitmap
        var bitmapResolution = getImageResolution(storagePath);
        var originalWidth = bitmapResolution[0];
        var originalHeight = bitmapResolution[1];

        // Detect Orientation and swap height/width if the image is to be rotated
        var shouldRotate = detectRotateImage(storagePath);
        if (shouldRotate) {
            // Swap the width and height
            var tempWidth = originalWidth;
            originalWidth = originalHeight;
            originalHeight = tempWidth;
        }

        var bitmapResult = decodeSampledBitmapFromFile(storagePath, originalWidth, originalHeight);
        if (bitmapResult == null) {
            return null;
        }

        // Calculate the scaling factors based on screen dimensions
        var widthScaleFactor = (float) minWidth/ bitmapResult.getWidth();
        var heightScaleFactor = (float) minHeight / bitmapResult.getHeight();

        // Use the smaller scaling factor to maintain aspect ratio
        var scaleFactor = Math.min(widthScaleFactor, heightScaleFactor);

        // Calculate the new scaled width and height
        var scaledWidth = (int) (bitmapResult.getWidth() * scaleFactor);
        var scaledHeight = (int) (bitmapResult.getHeight() * scaleFactor);

        bitmapResult = scaleBitmap(bitmapResult,scaledWidth,scaledHeight);

        return bitmapResult;
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
     * scales a given bitmap depending on the given size parameters.
     *
     * @param bitmap the bitmap to be scaled
     * @param width  the width
     * @param height the height
     * @return the scaled bitmap
     */
    public static Bitmap scaleBitmap(Bitmap bitmap, int width, int height) {
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
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

            if (orientation != ExifInterface.ORIENTATION_NORMAL) {
                Matrix matrix = new Matrix();
                switch (orientation) {
                    // 2
                    case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: {
                        matrix.postScale(-1.0f, 1.0f);
                        break;
                    }
                    // 3
                    case ExifInterface.ORIENTATION_ROTATE_180: {
                        matrix.postRotate(180);
                        break;
                    }
                    // 4
                    case ExifInterface.ORIENTATION_FLIP_VERTICAL: {
                        matrix.postScale(1.0f, -1.0f);
                        break;
                    }
                    // 5
                    case ExifInterface.ORIENTATION_TRANSPOSE: {
                        matrix.postRotate(-90);
                        matrix.postScale(1.0f, -1.0f);
                        break;
                    }
                    // 6
                    case ExifInterface.ORIENTATION_ROTATE_90: {
                        matrix.postRotate(90);
                        break;
                    }
                    // 7
                    case ExifInterface.ORIENTATION_TRANSVERSE: {
                        matrix.postRotate(90);
                        matrix.postScale(1.0f, -1.0f);
                        break;
                    }
                    // 8
                    case ExifInterface.ORIENTATION_ROTATE_270: {
                        matrix.postRotate(270);
                        break;
                    }
                }

                // Rotate the bitmap
                resultBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (!resultBitmap.equals(bitmap)) {
                    bitmap.recycle();
                }
            }
        } catch (Exception exception) {
            Log_OC.e("BitmapUtil", "Could not rotate the image: " + storagePath);
        }
        return resultBitmap;
    }

    /**
     * Detect if Image will be rotated according to EXIF orientation. Cf. http://www.daveperrett.com/articles/2012/07/28/exif-orientation-handling-is-a-ghetto/
     *
     * @param storagePath Path to source file of bitmap. Needed for EXIF information.
     * @return true if image's orientation determines it will be rotated to where height and width change
     */
    public static boolean detectRotateImage(String storagePath) {
        try {
            ExifInterface exifInterface = new ExifInterface(storagePath);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

            if (orientation != ExifInterface.ORIENTATION_NORMAL) {
                switch (orientation) {
                    // 5
                    case ExifInterface.ORIENTATION_TRANSPOSE: {
                        return true;
                    }
                    // 6
                    case ExifInterface.ORIENTATION_ROTATE_90: {
                        return true;
                    }
                    // 7
                    case ExifInterface.ORIENTATION_TRANSVERSE: {
                        return true;
                    }
                    // 8
                    case ExifInterface.ORIENTATION_ROTATE_270: {
                        return true;
                    }
                }
            }
        }
        catch (Exception exception) {
            Log_OC.e("BitmapUtil", "Could not read orientation at: " + storagePath);
        }
        return false;
    }

    public static int[] getImageResolution(String srcPath) {
        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(srcPath, options);
        return new int [] {options.outWidth, options.outHeight};
    }

    public static Color usernameToColor(String name) {
        String hash = name.toLowerCase(Locale.ROOT);

        // Check if the input is already a valid MD5 hash (32 hex characters)
        if (hash.length() != 32 || !hash.matches("[0-9a-f]+")) {
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

        // Sum the values of the hexadecimal digits
        for (int i = 0; i < hash.length(); i++) {
            // Efficient hex char-to-int conversion
            finalInt += Character.digit(hash.charAt(i), 16);
        }

        // Return the sum modulo maximum
        return finalInt % maximum;
    }

    private static Color[] generateColors(int steps) {
        Color red = new Color(182, 70, 157);
        Color yellow = new Color(221, 203, 85);
        Color blue = new Color(0, 130, 201); // Nextcloud blue

        Color[] palette1 = mixPalette(steps, red, yellow);
        Color[] palette2 = mixPalette(steps, yellow, blue);
        Color[] palette3 = mixPalette(steps, blue, red);

        Color[] resultPalette = new Color[palette1.length + palette2.length + palette3.length];
        System.arraycopy(palette1, 0, resultPalette, 0, steps);
        System.arraycopy(palette2, 0, resultPalette, steps, steps);
        System.arraycopy(palette3, 0, resultPalette, steps * 2, steps);

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
            if (!(obj instanceof Color other)) {
                return false;
            }

            return this.r == other.r && this.g == other.g && this.b == other.b;
        }

        @Override
        public int hashCode() {
            return (r << 16) + (g << 8) + b;
        }
    }

    public static String md5(String string) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        // Use UTF-8 for consistency
        byte[] hashBytes = md5.digest(string.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder(32);
        for (byte b : hashBytes) {
            // Convert each byte to a 2-digit hex string
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    /**
     * Returns a new circular bitmap drawable by creating it from a bitmap, setting initial target density based on the
     * display metrics of the resources.
     *
     * @param resources the resources for initial target density
     * @param bitmap    the original bitmap
     * @return the circular bitmap
     */
    @Nullable
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

    @Nullable
    public static RoundedBitmapDrawable bitmapToCircularBitmapDrawable(Resources resources, Bitmap bitmap) {
        return bitmapToCircularBitmapDrawable(resources, bitmap, -1);
    }

    public static void setRoundedBitmap(Resources resources, Bitmap bitmap, float radius, ImageView imageView) {

        imageView.setImageDrawable(BitmapUtils.bitmapToCircularBitmapDrawable(resources,
                                                                              bitmap,
                                                                              radius));
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        return drawableToBitmap(drawable, -1, -1);
    }

    @NonNull
    public static Bitmap drawableToBitmap(Drawable drawable, int desiredWidth, int desiredHeight) {
        if (drawable instanceof BitmapDrawable bitmapDrawable) {
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        Bitmap bitmap;
        int width;
        int height;

        if (desiredWidth > 0 && desiredHeight > 0) {
            width = desiredWidth;
            height = desiredHeight;
        } else {
            if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
                if (drawable.getBounds().width() > 0 && drawable.getBounds().height() > 0) {
                    width = drawable.getBounds().width();
                    height = drawable.getBounds().height();
                } else {
                    width = 1;
                    height = 1;
                }
            } else {
                width = drawable.getIntrinsicWidth();
                height = drawable.getIntrinsicHeight();
            }
        }

        bitmap = Bitmap.createBitmap(width,
                                     height,
                                     Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static void setRoundedBitmapAccordingToListType(boolean gridView, Bitmap thumbnail, ImageView thumbnailView) {
        if (gridView) {
            BitmapUtils.setRoundedBitmapForGridMode(thumbnail, thumbnailView);
        } else {
            BitmapUtils.setRoundedBitmap(thumbnail, thumbnailView);
        }
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

    public static Bitmap createAvatarWithStatus(Bitmap avatar, StatusType statusType, @NonNull String icon, Context context) {
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
     * Inspired from https://www.demo2s.com/android/android-bitmap-get-a-round-version-of-the-bitmap.html
     */
    public static Bitmap roundBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        final Canvas canvas = new Canvas(output);

        final int color = R.color.white;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(getResources().getColor(color, null));
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    /**
     * from https://stackoverflow.com/a/38249623
     **/
    public static Bitmap tintImage(Bitmap bitmap, int color) {
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        Bitmap bitmapResult = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapResult);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return bitmapResult;
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
