package com.owncloud.android.ui.dialog;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.adapter.SendButtonAdapter;
import com.owncloud.android.ui.components.SendButtonData;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.utils.MimeTypeUtil;

import java.util.ArrayList;
import java.util.List;

/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

public class SendShareDialog extends BottomSheetDialogFragment {

    private static final String KEY_OCFILE = "KEY_OCFILE";
    private static final String TAG = SendShareDialog.class.getSimpleName();
    public static final String PACKAGE_NAME = "PACKAGE_NAME";
    public static final String ACTIVITY_NAME = "ACTIVITY_NAME";

    private View view;
    private OCFile file;
    private FileOperationsHelper fileOperationsHelper;
    private FileDisplayActivity fileDisplayActivity;

    public static SendShareDialog newInstance(OCFile file) {

        SendShareDialog dialogFragment = new SendShareDialog();

        Bundle args = new Bundle();
        args.putParcelable(KEY_OCFILE, file);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // keep the state of the fragment on configuration changes
        setRetainInstance(true);

        view = null;

        file = getArguments().getParcelable(KEY_OCFILE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.send_share_fragment, container, false);

        // Share with people
        TextView sharePeopleButton = (TextView) view.findViewById(R.id.share_people_button);

        sharePeopleButton.setOnClickListener(v -> fileOperationsHelper.showShareFile(file));


        // Share via link button
        TextView shareLinkButton = (TextView) view.findViewById(R.id.share_link_button);

        if (file.isSharedWithMe() && !file.canReshare()) {
            Snackbar.make(view, R.string.resharing_is_not_allowed, Snackbar.LENGTH_LONG).show();
            shareLinkButton.setVisibility(View.GONE);
        }

        shareLinkButton.setOnClickListener(v -> fileOperationsHelper.showShareFile(file));


        // populate send apps
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType(file.getMimetype());
        sendIntent.putExtra(Intent.EXTRA_STREAM, file.getExposedFileUri(getActivity()));
        sendIntent.putExtra(Intent.ACTION_SEND, true);

        List<SendButtonData> sendButtonDataList = new ArrayList<>();
        for (ResolveInfo match : getActivity().getPackageManager().queryIntentActivities(sendIntent, 0)) {
            Drawable icon = match.loadIcon(getActivity().getPackageManager());
            CharSequence label = match.loadLabel(getActivity().getPackageManager());
            SendButtonData sendButtonData = new SendButtonData(icon, label,
                    match.activityInfo.packageName,
                    match.activityInfo.name);

            sendButtonDataList.add(sendButtonData);
        }

        if (getContext().getString(R.string.send_files_to_other_apps).equalsIgnoreCase("off")) {
            sharePeopleButton.setVisibility(View.GONE);
        }

        SendButtonAdapter.ClickListener clickListener = sendButtonDataData -> {

            if (MimeTypeUtil.isImage(file) && !file.isDown()) {
                fileOperationsHelper.sendCachedImage(file);
            } else {
                String packageName = sendButtonDataData.getPackageName();
                String activityName = sendButtonDataData.getActivityName();
                
                // Obtain the file
                if (file.isDown()) {
                    sendIntent.setComponent(new ComponentName(packageName, activityName));
                    getActivity().startActivity(Intent.createChooser(sendIntent, getString(R.string.send)));

                } else {  // Download the file
                    Log_OC.d(TAG, file.getRemotePath() + ": File must be downloaded");
                    fileDisplayActivity.startDownloadForSending(file, OCFileListFragment.DOWNLOAD_SEND,
                            packageName, activityName);
                }
            }

            dismiss();
        };

        RecyclerView sendButtonsView = (RecyclerView) view.findViewById(R.id.send_button_recycler_view);
        sendButtonsView.setHasFixedSize(true);
        sendButtonsView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        sendButtonsView.setAdapter(new SendButtonAdapter(sendButtonDataList, clickListener));


        return view;
    }

    public void setFileOperationsHelper(FileOperationsHelper fileOperationsHelper) {
        this.fileOperationsHelper = fileOperationsHelper;
    }

    public void setFileDisplayActivity(FileDisplayActivity fileDisplayActivity) {
        this.fileDisplayActivity = fileDisplayActivity;
    }
}
