package com.owncloud.android.ui.preview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.Build;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.View;

import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.FileInputStream;
import java.io.InputStream;

public class ImageViewCustom extends AppCompatImageView {

    private static final String TAG = ImageViewCustom.class.getSimpleName();

    private static final boolean IS_ICS_OR_HIGHER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    private static final boolean IS_VERSION_BUGGY_ON_RECYCLES =
            Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1 ||
            Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2;

    private int mBitmapHeight;
    private int mBitmapWidth;

    private Movie mGifMovie;
    private int mMovieWidth, mMovieHeight;
    private long mMovieDuration;
    private long mMovieRunDuration;
    private long mLastTick;

    public ImageViewCustom(Context context) {
        super(context);
    }
    
    public ImageViewCustom(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public ImageViewCustom(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

	@Override
    protected void onDraw(Canvas canvas) {
        if(IS_ICS_OR_HIGHER && checkIfMaximumBitmapExceed(canvas) || IS_VERSION_BUGGY_ON_RECYCLES ) {
            // Software type is set with two targets:
            // 1. prevent that bitmaps larger than maximum textures allowed are shown as black views in devices
            //  with LAYER_TYPE_HARDWARE enabled by default;
            // 2. grant that bitmaps are correctly de-allocated from memory in versions suffering the bug fixed in
            //  https://android.googlesource.com/platform/frameworks/base/+/034de6b1ec561797a2422314e6ef03e3cd3e08e0;
            //
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        if(mGifMovie != null) {
            long nowTick = android.os.SystemClock.uptimeMillis();
            if (mLastTick == 0) {
                mMovieRunDuration = 0;
            } else {
                mMovieRunDuration += nowTick - mLastTick;
                if(mMovieRunDuration > mMovieDuration) {
                        mMovieRunDuration = 0;
                }
            }

            mGifMovie.setTime((int) mMovieRunDuration);

            float scale = getScaleToViewFactor(mGifMovie, canvas);

            canvas.scale(scale, scale);
            canvas.translate(((float) getWidth() / scale - (float) mGifMovie.width()) / 2f,
                    ((float) getHeight() / scale - (float) mGifMovie.height()) /2f);

            mGifMovie.draw(canvas, 0, 0);

            mLastTick = nowTick;
            invalidate();
        } else {
            super.onDraw(canvas);
        }
    }

    private float getScaleToViewFactor(Movie movie, Canvas canvas) {
        if (movie.height() > getHeight() || movie.width() > getWidth()) {
            float offset = 0.25f;
            return (1f / Math.min(canvas.getHeight() / movie.height(), canvas.getWidth() / movie.width())) + offset;
        }

        return Math.min(canvas.getHeight() / movie.height(), canvas.getWidth() / movie.width());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mGifMovie == null) {
            setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        } else {
            setMeasuredDimension(mMovieWidth, mMovieHeight);
        }
    }

    /**
     * Checks if current bitmaps exceed the maximum OpenGL texture size limit
     * @param canvas        Canvas where the view will be drawn into.
     * @return boolean      True means that the bitmap is too big for the canvas.
     */
	private boolean checkIfMaximumBitmapExceed(Canvas canvas) {
        return mBitmapWidth > canvas.getMaximumBitmapWidth() || mBitmapHeight > canvas.getMaximumBitmapHeight();

    }
    
    @Override
    /**
     * Keeps the size of the bitmap cached in member variables for faster access in {@link #onDraw(Canvas)},
     * but without keeping another reference to the {@link Bitmap}
     */
    public void setImageBitmap(Bitmap bm) {
        mBitmapWidth = bm.getWidth();
        mBitmapHeight = bm.getHeight();
        super.setImageBitmap(bm);
    }

    /**
     * sets the GIF image of the given storage path.
     *
     * @param storagePath the storage path of the GIF image
     */
    public void setGIFImageFromStoragePath(String storagePath) {
        try {
            InputStream gifInputStream = new FileInputStream(storagePath);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setFocusable(true);

            mGifMovie = Movie.decodeStream(gifInputStream);
            mMovieWidth = mGifMovie.width();
            mMovieHeight = mGifMovie.height();
            mMovieDuration = mGifMovie.duration();
        } catch (Exception e) {
            Log_OC.e(TAG, "Failed to set GIF image");
        }
    }

}
