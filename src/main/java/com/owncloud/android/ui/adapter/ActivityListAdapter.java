package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.lib.resources.activities.models.Activity;
import com.owncloud.android.utils.DisplayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alejandro on 28/03/17.
 */

public class ActivityListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<Object> mValues;

    public ActivityListAdapter(Context context) {
        this.mValues = new ArrayList<>();
        this.context = context;
    }

    public void setActivityItems(List<Object> activityItems) {
        mValues.clear();
        mValues.addAll(activityItems);
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_list_item, parent, false);
        return new ActivityViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Activity activity =(Activity) mValues.get(position);
        ((ActivityViewHolder)holder).dateTime.setText(DisplayUtils.getRelativeTimestamp(context, activity.getDatetime().getTime()));
        ((ActivityViewHolder)holder).subject.setText(activity.getSubject());
        ((ActivityViewHolder)holder).message.setText(activity.getMessage());

        // Todo set proper action icon (to be clarified how to pick)
        //((ActivityViewHolder)holder).activityIcon.setImageResource(R.drawable.ic_action_share);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }


    class HeaderViewHolder extends RecyclerView.ViewHolder {

        public HeaderViewHolder(View v) {
            super(v);
        }

    }

    class ActivityViewHolder extends RecyclerView.ViewHolder {

        private final ImageView activityIcon;
        private final TextView subject;
        private final TextView message;
        private final TextView dateTime;

        private ActivityViewHolder(View itemView) {
            super(itemView);
            activityIcon = (ImageView) itemView.findViewById(R.id.activity_icon);
            subject = (TextView) itemView.findViewById(R.id.activity_subject);
            message = (TextView) itemView.findViewById(R.id.activity_message);
            dateTime = (TextView) itemView.findViewById(R.id.activity_datetime);
        }

    }


}
