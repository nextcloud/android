package com.owncloud.android.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Created by tobi on 24.05.15.
 */
public class TextDrawable extends Drawable {

    private final String text;
    private final Paint paint;
    private final Paint bg;

    public TextDrawable(String text, int r, int g, int b) {

        this.text = text;
        Integer color = Color.rgb(r, g, b);

        bg = new Paint();
        bg.setStyle(Paint.Style.FILL);
        bg.setColor(color);

        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(20);
        paint.setAntiAlias(true);
        paint.setFakeBoldText(true);
    }

    @Override
    public void draw(Canvas canvas) {
        //RectF rf = new RectF(0,24,24,0);
        //canvas.drawRoundRect(rf,24,24,bg);
        canvas.drawRect(0,-20,20,40,bg);
        canvas.drawText(text, 4, 6, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
