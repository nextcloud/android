package com.owncloud.android.widgets;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Denis Dijak on 25.4.2016.
 * Recycler view implementation with possibility to set empty View
 */
public class OCRecyclerView extends RecyclerView {
    private View recyclerView;

    /**
     * Observe changes made on Recycler View
     */
    final private AdapterDataObserver adobserver = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            checkIfEmpty();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            checkIfEmpty();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            if(itemCount != 0) {
                checkIfEmpty();
            }
        }
    };

    public OCRecyclerView(Context context) {
        super(context);
    }

    public OCRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OCRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Check if Recycler view is empty, and show empty View instead of the content
     */
    private void checkIfEmpty() {
        if (this.recyclerView != null && getAdapter() != null) {
            final boolean emptyViewVisible = getAdapter().getItemCount() == 0;
            this.recyclerView.setVisibility(emptyViewVisible ? VISIBLE : GONE);
            setVisibility(emptyViewVisible ? GONE : VISIBLE);
        }
    }

    /**
     * Set adapter for OCRecyclerView, register AdapterDataObserver
     * that will check if we are dealing with some content or we have to show empty view
     * @param adapter Adapter
     */
    @Override
    public void setAdapter(Adapter adapter) {
        final Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(adobserver);
        }
        super.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerAdapterDataObserver(adobserver);
        }

        checkIfEmpty();
    }

    /**
     * Set a View that will be shown on empty content
     * @param eView empty View
     */
    public void setEmptyView(View eView) {
        this.recyclerView = eView;
        checkIfEmpty();
    }
}