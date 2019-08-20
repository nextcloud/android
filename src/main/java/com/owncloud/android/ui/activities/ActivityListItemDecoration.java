package com.owncloud.android.ui.activities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.owncloud.android.ui.adapter.StickyHeaderAdapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ActivityListItemDecoration extends RecyclerView.ItemDecoration {
    private final String TAG = this.getClass().getSimpleName();
    private final StickyHeaderAdapter adapter;


    public ActivityListItemDecoration(StickyHeaderAdapter stickyHeaderAdapter) {
        this.adapter = stickyHeaderAdapter;
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(c, parent, state);

        View topChild = parent.getChildAt(0);
        if (topChild == null) {
            return;
        }
        int topChildPosition = parent.getChildAdapterPosition(topChild);

        if (topChildPosition == RecyclerView.NO_POSITION) {
            return;
        }
        View currentHeader = getHeaderViewForItem(topChildPosition, parent);
        fixLayoutSize(parent, currentHeader);
        int contactPoint = currentHeader.getBottom();
        View childInContact = getChildInContact(parent, contactPoint);

        if (childInContact == null) {
            return;
        }

        if (adapter.isHeader(parent.getChildAdapterPosition(childInContact))) {
            moveHeader(c, currentHeader, childInContact);
            return;
        }

        drawHeader(c, currentHeader);
    }

    private void drawHeader(Canvas c, View header) {
        c.save();
        c.translate(0, 0);
        header.draw(c);
        c.restore();
    }

    private void moveHeader(Canvas c, View currentHeader, View nextHeader) {
        c.save();
        c.translate(0, nextHeader.getTop() - currentHeader.getHeight());
        currentHeader.draw(c);
        c.restore();
    }

    private View getChildInContact(RecyclerView parent, int contactPoint) {
        View childInContact = null;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View currentChild = parent.getChildAt(i);
            if (currentChild.getBottom() > contactPoint) {
                if (currentChild.getTop() <= contactPoint) {
                    childInContact = currentChild;
                    break;
                }
            }
        }
        return childInContact;
    }

    private View getHeaderViewForItem(int itemPosition, RecyclerView parent) {
        int headerPosition = adapter.getHeaderPositionForItem(itemPosition);
        int layoutId = adapter.getHeaderLayout(itemPosition);
        View header = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        header.setBackgroundColor(Color.WHITE);
        adapter.bindHeaderData(header, headerPosition);
        return header;
    }

    private void fixLayoutSize(ViewGroup parent, View view) {

        // Specs for parent (RecyclerView)
        int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.UNSPECIFIED);

        // Specs for children (headers)
        int childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec, parent.getPaddingLeft() + parent.getPaddingRight(), view.getLayoutParams().width);
        int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec, parent.getPaddingTop() + parent.getPaddingBottom(), view.getLayoutParams().height);

        view.measure(childWidthSpec, childHeightSpec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    }


}
