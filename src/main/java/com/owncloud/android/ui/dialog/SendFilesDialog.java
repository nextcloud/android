package com.owncloud.android.ui.dialog;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.adapter.SendButtonAdapter;
import com.owncloud.android.ui.components.SendButtonData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH.
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
public class SendFilesDialog extends BottomSheetDialogFragment {

    private static final String KEY_OCFILES = "KEY_OCFILES";

    private OCFile[] files;

    public static SendFilesDialog newInstance(Set<OCFile> files) {

        SendFilesDialog dialogFragment = new SendFilesDialog();

        Bundle args = new Bundle();
        args.putParcelableArray(KEY_OCFILES, files.toArray(new OCFile[0]));
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // keep the state of the fragment on configuration changes
        setRetainInstance(true);

        files = (OCFile[]) requireArguments().getParcelableArray(KEY_OCFILES);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.send_files_fragment, container, false);

        // populate send apps
        Intent sendIntent = createSendIntent();

        List<SendButtonData> sendButtonDataList = setupSendButtonData(sendIntent);

        SendButtonAdapter.ClickListener clickListener = setupSendButtonClickListener(sendIntent);

        RecyclerView sendButtonsView = view.findViewById(R.id.send_button_recycler_view);
        sendButtonsView.setHasFixedSize(true);
        sendButtonsView.setLayoutManager(new GridLayoutManager(getActivity(), 4));
        sendButtonsView.setAdapter(new SendButtonAdapter(sendButtonDataList, clickListener));

        return view;
    }

    @NonNull
    private SendButtonAdapter.ClickListener setupSendButtonClickListener(Intent sendIntent) {
        return sendButtonDataData -> {
            String packageName = sendButtonDataData.getPackageName();
            String activityName = sendButtonDataData.getActivityName();

            sendIntent.setComponent(new ComponentName(packageName, activityName));
            requireActivity().startActivity(Intent.createChooser(sendIntent, getString(R.string.send)));

            dismiss();
        };
    }

    @NonNull
    private List<SendButtonData> setupSendButtonData(Intent sendIntent) {
        Drawable icon;
        SendButtonData sendButtonData;
        CharSequence label;
        List<ResolveInfo> matches = requireActivity().getPackageManager().queryIntentActivities(sendIntent, 0);
        List<SendButtonData> sendButtonDataList = new ArrayList<>(matches.size());
        for (ResolveInfo match : matches) {
            icon = match.loadIcon(requireActivity().getPackageManager());
            label = match.loadLabel(requireActivity().getPackageManager());
            sendButtonData = new SendButtonData(icon, label,
                                                match.activityInfo.packageName,
                                                match.activityInfo.name);

            sendButtonDataList.add(sendButtonData);
        }
        return sendButtonDataList;
    }

    @NonNull
    private Intent createSendIntent() {
        Intent sendIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        sendIntent.setType(getUniqueMimetype());
        sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, getExposedFileUris());
        sendIntent.putExtra(Intent.ACTION_SEND, true);
        return sendIntent;
    }

    @Nullable
    private String getUniqueMimetype() {
        String mimetype = files[0].getMimeType();

        for (OCFile file : files) {
            if (!mimetype.equals(file.getMimeType())) {
                return null;
            }
        }

        return mimetype;
    }

    private ArrayList<Uri> getExposedFileUris() {
        ArrayList<Uri> uris = new ArrayList<>();

        for (OCFile file : files) {
            uris.add(file.getExposedFileUri(requireContext()));
        }

        return uris;
    }
}
