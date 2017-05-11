package com.owncloud.android.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;

import java.util.ArrayList;

/**
 * Created by alejandro on 11/05/17.
 */

public class RichObjectAdapter extends RecyclerView.Adapter<RichObjectAdapter.RichObjectViewHolder> {

    private final ArrayList<String> mValues;

    public RichObjectAdapter(ArrayList<String> values){
        this.mValues=values;
    }

    @Override
    public RichObjectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rich_object_item, parent, false);
        return new RichObjectViewHolder(v);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    @Override
    public void onBindViewHolder(RichObjectViewHolder holder, int position) {
        String url=mValues.get(position);
        Glide.with(MainApp.getAppContext())
                .load(url)
                .into(holder.image);
    }

    static class RichObjectViewHolder extends RecyclerView.ViewHolder{

        ImageView image;
        RichObjectViewHolder(View v){
            super(v);
            image=(ImageView)v.findViewById(R.id.image);
        }

    }

}
