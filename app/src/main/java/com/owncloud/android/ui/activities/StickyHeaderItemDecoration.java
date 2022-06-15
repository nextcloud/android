/*
 * Nextcloud Android client application
 *

 * Copyright (C) 2019 Sevastyan Savanyuk
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activities;

import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.owncloud.android.ui.adapter.StickyHeaderAdapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class StickyHeaderItemDecoration extends RecyclerView.ItemDecoration {
    private final StickyHeaderAdapter adapter;


    public StickyHeaderItemDecoration(StickyHeaderAdapter stickyHeaderAdapter) {
        this.adapter = stickyHeaderAdapter;
    }

    @Override
    public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(canvas, parent, state);

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
            moveHeader(canvas, currentHeader, childInContact);
            return;
        }

        drawHeader(canvas, currentHeader);
    }

    private void drawHeader(Canvas canvas, View header) {
        canvas.save();
        canvas.translate(0, 0);
        header.draw(canvas);
        canvas.restore();
    }

    private void moveHeader(Canvas canvas, View currentHeader, View nextHeader) {
        canvas.save();
        canvas.translate(0, nextHeader.getTop() - currentHeader.getHeight());
        currentHeader.draw(canvas);
        canvas.restore();
    }

    private View getChildInContact(RecyclerView parent, int contactPoint) {
        View childInContact = null;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View currentChild = parent.getChildAt(i);
            if (currentChild.getBottom() > contactPoint && currentChild.getTop() <= contactPoint) {
                childInContact = currentChild;
                break;
            }
        }
        return childInContact;
    }

    private View getHeaderViewForItem(int itemPosition, RecyclerView parent) {
        int headerPosition = adapter.getHeaderPositionForItem(itemPosition);
        int layoutId = adapter.getHeaderLayout(itemPosition);
        View header = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
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
