package com.owncloud.android.ui.activities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

import com.owncloud.android.ui.adapter.ActivityListAdapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ActivityListItemDecoration extends RecyclerView.ItemDecoration {
    private final String TAG = this.getClass().getSimpleName();
    private View currentHeader;

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(c, parent, state);


        View topChild = parent.getChildAt(0);
        int topChildPosition = parent.getChildAdapterPosition(topChild);
        if (topChildPosition == -1) {
            return;
        }
        ActivityListAdapter adapter = (ActivityListAdapter) parent.getAdapter();

        Object topElement = adapter.getActivityAtByPosition(topChildPosition);

        if (topElement instanceof ActivityListHeader) {
            currentHeader = parent.getChildAt(0);
            currentHeader.setBackgroundColor(Color.WHITE);
        }
        drawHeader(c, currentHeader);
    }

    private void drawHeader(Canvas c, View header) {
        c.save();
        c.translate(0, 0);
        header.draw(c);
        c.restore();
    }
}
