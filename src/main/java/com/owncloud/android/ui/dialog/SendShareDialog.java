package com.owncloud.android.ui.dialog;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.adapter.SendButtonAdapter;
import com.owncloud.android.ui.components.SendButtonData;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ThemeUtils;

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
        TextView sharePeopleText = view.findViewById(R.id.share_people_button);
        sharePeopleText.setOnClickListener(v -> shareFile(file));

        ImageView sharePeopleImageView = view.findViewById(R.id.share_people_icon);
        themeShareButtonImage(sharePeopleImageView);
        sharePeopleImageView.setOnClickListener(v -> shareFile(file));

        // Share via link button
        TextView shareLinkText = view.findViewById(R.id.share_link_button);
        shareLinkText.setOnClickListener(v -> shareFile(file));

        ImageView shareLinkImageView = view.findViewById(R.id.share_link_icon);
        themeShareButtonImage(shareLinkImageView);
        shareLinkImageView.setOnClickListener(v -> shareFile(file));

        if (file.isSharedWithMe() && !file.canReshare()) {
            showResharingNotAllowedSnackbar();

            if (file.isFolder()) {
                shareLinkText.setVisibility(View.GONE);
                shareLinkImageView.setVisibility(View.GONE);
                sharePeopleText.setVisibility(View.GONE);
                sharePeopleImageView.setVisibility(View.GONE);
                getDialog().hide();
            } else {
                shareLinkText.setEnabled(false);
                shareLinkText.setAlpha(0.3f);
                shareLinkImageView.setEnabled(false);
                shareLinkImageView.setAlpha(0.3f);
                sharePeopleText.setEnabled(false);
                sharePeopleText.setAlpha(0.3f);
                sharePeopleImageView.setEnabled(false);
                sharePeopleImageView.setAlpha(0.3f);
            }
        }

        // populate send apps
        Intent sendIntent = createSendIntent();

        List<SendButtonData> sendButtonDataList = setupSendButtonData(sendIntent);

        if (getContext().getString(R.string.send_files_to_other_apps).equalsIgnoreCase("off")) {
            sharePeopleText.setVisibility(View.GONE);
        }

        SendButtonAdapter.ClickListener clickListener = setupSendButtonClickListener(sendIntent);

        RecyclerView sendButtonsView = view.findViewById(R.id.send_button_recycler_view);
        sendButtonsView.setHasFixedSize(true);
        sendButtonsView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        sendButtonsView.setAdapter(new SendButtonAdapter(sendButtonDataList, clickListener));

        return view;
    }

    private void themeShareButtonImage(ImageView shareImageView) {
        shareImageView.getBackground().setColorFilter(ThemeUtils.elementColor(), PorterDuff.Mode.SRC_IN);
        shareImageView.getDrawable().mutate().setColorFilter(ThemeUtils.fontColor(), PorterDuff.Mode.SRC_IN);
    }

    private void showResharingNotAllowedSnackbar() {
        Snackbar snackbar = Snackbar.make(view, R.string.resharing_is_not_allowed, Snackbar.LENGTH_LONG);
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                super.onDismissed(transientBottomBar, event);

                if (file.isFolder()) {
                    dismiss();
                }
            }
        });

        snackbar.show();
    }

    @NonNull
    private SendButtonAdapter.ClickListener setupSendButtonClickListener(Intent sendIntent) {
        return sendButtonDataData -> {
            String packageName = sendButtonDataData.getPackageName();
            String activityName = sendButtonDataData.getActivityName();

            if (MimeTypeUtil.isImage(file) && !file.isDown()) {
                fileOperationsHelper.sendCachedImage(file, packageName, activityName);
            } else {
                // Obtain the file
                if (file.isDown()) {
                    sendIntent.setComponent(new ComponentName(packageName, activityName));
                    getActivity().startActivity(Intent.createChooser(sendIntent, getString(R.string.send)));
                } else {  // Download the file
                    Log_OC.d(TAG, file.getRemotePath() + ": File must be downloaded");
                    ((SendShareDialog.SendShareDialogDownloader) getActivity()).downloadFile(file, packageName,
                            activityName);
                }
            }

            dismiss();
        };
    }

    @NonNull
    private List<SendButtonData> setupSendButtonData(Intent sendIntent) {
        List<SendButtonData> sendButtonDataList = new ArrayList<>();
        for (ResolveInfo match : getActivity().getPackageManager().queryIntentActivities(sendIntent, 0)) {
            Drawable icon = match.loadIcon(getActivity().getPackageManager());
            CharSequence label = match.loadLabel(getActivity().getPackageManager());
            SendButtonData sendButtonData = new SendButtonData(icon, label,
                    match.activityInfo.packageName,
                    match.activityInfo.name);

            sendButtonDataList.add(sendButtonData);
        }
        return sendButtonDataList;
    }

    @NonNull
    private Intent createSendIntent() {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType(file.getMimetype());
        sendIntent.putExtra(Intent.EXTRA_STREAM, file.getExposedFileUri(getActivity()));
        sendIntent.putExtra(Intent.ACTION_SEND, true);
        return sendIntent;
    }

    private void shareFile(OCFile file) {
        fileOperationsHelper.showShareFile(file);
        dismiss();
    }

    public void setFileOperationsHelper(FileOperationsHelper fileOperationsHelper) {
        this.fileOperationsHelper = fileOperationsHelper;
    }

    public interface SendShareDialogDownloader {
        void downloadFile(OCFile file, String packageName, String activityName);
    }
}
