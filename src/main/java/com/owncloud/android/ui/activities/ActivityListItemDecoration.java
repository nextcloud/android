package com.owncloud.android.ui.activities;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ActivityListItemDecoration extends RecyclerView.ItemDecoration {
    private final String TAG = this.getClass().getSimpleName();
    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(c, parent, state);

        View topChild = parent.getChildAt(0);
        View currentHeader = topChild;



        if (currentHeader != null) {
            drawHeader(c, currentHeader);
            Log.d(TAG,"Attach new Header" );
        }
    }

    private void drawHeader(Canvas c, View header) {
        c.save();
        c.translate(0, 0);
        header.draw(c);
        c.restore();
    }
}
