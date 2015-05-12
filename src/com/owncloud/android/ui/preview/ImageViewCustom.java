package com.owncloud.android.ui.preview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.owncloud.android.lib.common.utils.Log_OC;

public class ImageViewCustom extends ImageView {

    private static final String TAG = ImageViewCustom.class.getSimpleName();

    private static final boolean IS_ICS_OR_HIGHER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;

    private int mBitmapHeight;
    private int mBitmapWidth;

    
    public ImageViewCustom(Context context) {
        super(context);
    }
    
    public ImageViewCustom(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public ImageViewCustom(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @SuppressLint("NewApi")
	@Override
    protected void onDraw(Canvas canvas) {

        if(IS_ICS_OR_HIGHER && checkIfMaximumBitmapExceed(canvas)) {
            // Set layer type to software one for avoiding exceed
            // and problems in visualization
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        super.onDraw(canvas);
    }

    /**
     * Checks if current bitmaps exceed the maximum OpenGL texture size limit
     * @param canvas        Canvas where the view will be drawn into.
     * @return boolean      True means that the bitmap is too big for the canvas.
     */
    @SuppressLint("NewApi")
	private boolean checkIfMaximumBitmapExceed(Canvas canvas) {
        Log_OC.v(TAG, "Canvas maximum: " + canvas.getMaximumBitmapWidth() + " - " + canvas.getMaximumBitmapHeight());
        if (mBitmapWidth > canvas.getMaximumBitmapWidth()
                || mBitmapHeight > canvas.getMaximumBitmapHeight()) {
            return true;
        }
        
        return false;
    }
    
    @Override
    /**
     * Keeps the size of the bitmap cached in member variables for faster access in {@link #onDraw(Canvas)} ,
     * but without keeping another reference to the {@link Bitmap}
     */
    public void setImageBitmap (Bitmap bm) {
        mBitmapWidth = bm.getWidth();
        mBitmapHeight = bm.getHeight();
        super.setImageBitmap(bm);
    }

}
