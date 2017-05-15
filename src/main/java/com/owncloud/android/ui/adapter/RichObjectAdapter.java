package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.ui.interfaces.ActivityListInterface;

import java.util.ArrayList;

/**
 * Created by alejandro on 11/05/17.
 */

public class RichObjectAdapter extends RecyclerView.Adapter<RichObjectAdapter.RichObjectViewHolder> {

    private final Context mContext;
    private ArrayList<String> mValues;
    private final ActivityListInterface mActivityListInterface;

    public RichObjectAdapter(Context context, ActivityListInterface activityListInterface){
        this.mContext=context;
        this.mValues=new ArrayList<>();
        this.mActivityListInterface=activityListInterface;
    }


    public void setValues(ArrayList<String> values){
        this.mValues=values;
        notifyDataSetChanged();
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
        Glide.with(mContext)
                .load(url)
                .into(holder.image);
        holder.image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivityListInterface.onActivityClicked();
            }
        });
    }

    static class RichObjectViewHolder extends RecyclerView.ViewHolder{

        ImageView image;
        RichObjectViewHolder(View v){
            super(v);
            image=(ImageView)v.findViewById(R.id.image);
        }

    }

}
