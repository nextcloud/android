package com.owncloud.android.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Created by tobi on 24.05.15.
 */
public class TextDrawable extends Drawable {

    private final String text;
    private final Integer color;
    private final Float size;

    public TextDrawable(String text, int r, int g, int b, float size) {

        this.text = text;
        this.color = Color.rgb(r, g, b);
        this.size = size;

//        this.paint = new Paint();
////        paint.setColor(Color.BLACK);
////        paint.setTextSize(18f);
////        paint.setAntiAlias(true);
////        paint.setFakeBoldText(true);
//
////        paint.setARGB(255, r, g, b);
////        paint.setStyle(Paint.Style.FILL);
////        paint.setTextAlign(Paint.Align.CENTER);
//
//
//        paint.setColor(Color.BLACK);
//        paint.setTextSize(22f);
//        paint.setAntiAlias(true);
//        paint.setFakeBoldText(true);
//        paint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public void draw(Canvas canvas) {
        // TODO Paint in Constructor
        Paint bg = new Paint();
        bg.setStyle(Paint.Style.FILL);
        bg.setColor(color);
        canvas.drawRect(0,-20,20,40,bg);

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(20);
        paint.setAntiAlias(true);
        paint.setFakeBoldText(true);
//        paint.setTextAlign(Paint.Align.CENTER);

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        int x = (canvas.getWidth() / 2) - (bounds.width() / 2);
        int y = (canvas.getHeight() / 2) - (bounds.height() / 2);

//        canvas.drawText(text, x, y, paint);
        canvas.drawText(text, 4, 6, paint);
    }

    @Override
    public void setAlpha(int alpha) {
//        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
//        paint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
