package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.ui.activity.UploadFilesActivity;

import java.io.File;

/**
 * Created by ddijak on 4.12.2015.
 */
public class LocalRecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    protected TextView fileName;
    protected ImageView fileIcon;
    protected TextView fileSizeV;
    protected TextView lastModV;
    protected CheckBox checkBoxV;
    protected ImageView favoriteIcon;
    protected ImageView localStateView;
    protected ImageView sharedIconV;
    protected ImageView sharedWithMeIconV;
    protected UploadFilesActivity activity;
    protected LocalFileListAdapter mAdapter;
    private Context mContext;

    public LocalRecyclerViewHolder(View itemView, LocalFileListAdapter adapter, Context context) {
        super(itemView);
        itemView.setOnClickListener(this);

        mContext = context;
        this.fileName = (TextView) itemView.findViewById(R.id.Filename);
        this.fileIcon = (ImageView) itemView.findViewById(R.id.thumbnail);
        this.fileSizeV = (TextView) itemView.findViewById(R.id.file_size);
        this.lastModV = (TextView) itemView.findViewById(R.id.last_mod);
        this.checkBoxV = (CheckBox) itemView.findViewById(R.id.custom_checkbox);
        this.favoriteIcon = (ImageView) itemView.findViewById(R.id.favoriteIcon);
        this.localStateView = (ImageView) itemView.findViewById(R.id.localFileIndicator);
        this.sharedIconV = (ImageView) itemView.findViewById(R.id.sharedIcon);
        this.mAdapter = adapter;

        this.checkBoxV.setClickable(false);
    }

    @Override
    public void onClick(View v) {

        File file = (File) mAdapter.getItem(getAdapterPosition());
        if (file != null) {
            /// Click on a directory
            if (file.isDirectory()) {
                // just local updates
                ((UploadFilesActivity) mContext).onDirectoryClick(file);
                mAdapter.swapDirectory(file);
                // notify the click to container Activity
            } else {
                if (checkBoxV.isChecked()) {
                    checkBoxV.setChecked(false);
                    mAdapter.removeCheckedFile(file.getAbsolutePath());

                } else {
                    checkBoxV.setChecked(true);
                    mAdapter.setCheckedFile(file.getAbsolutePath());
                }
            }
        }
    }
}
