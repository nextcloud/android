/*
 *   ownCloud Android client application
 *
 *   @author Bartek Przybylski
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nextcloud.client.account.User;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ThemeUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


/**
 * Dialog which will be displayed to user upon keep-in-sync file conflict.
 */
public class ConflictsResolveDialog extends DialogFragment {

    private OCFile existingFile;
    private File newFile;
    private OnConflictDecisionMadeListener listener;
    private User user;
    private List<ThumbnailsCacheManager.ThumbnailGenerationTask> asyncTasks = new ArrayList<>();

    public enum Decision {
        CANCEL,
        KEEP_BOTH,
        KEEP_LOCAL,
        KEEP_SERVER,
    }

    public ConflictsResolveDialog() {
        // needed by Android
        existingFile = null;
        newFile = null;
        listener = null;
        user = null;
    }

    public ConflictsResolveDialog(OnConflictDecisionMadeListener listener,
                                  OCFile file,
                                  OCUpload conflictUpload, Optional<User> user) {
        this.listener = listener;
        this.existingFile = file;
        this.newFile = new File(conflictUpload.getLocalPath());
        this.user = user.get();
    }

    @Override
    public void onStart() {
        super.onStart();

        int color = ThemeUtils.primaryAccentColor(getContext());

        AlertDialog alertDialog = (AlertDialog) getDialog();

        if (alertDialog == null || user == null) {
            Toast.makeText(getContext(), "Failed to create conflict dialog", Toast.LENGTH_LONG).show();
            return;
        }

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            newFile = (File) savedInstanceState.getSerializable("file");
            existingFile = savedInstanceState.getParcelable("ocfile");
            user = savedInstanceState.getParcelable("user");
            listener = (OnConflictDecisionMadeListener) getActivity();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("file", newFile);
        outState.putParcelable("ocfile", existingFile);
        outState.putParcelable("user", user);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Inflate the layout for the dialog
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.conflict_resolve_dialog, null);
        int accentColor = ThemeUtils.primaryAccentColor(getContext());

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view)
            .setPositiveButton(R.string.common_ok, ((dialog, which) -> {
                if (listener != null) {
                    CheckBox newFile = view.findViewById(R.id.new_checkbox);
                    CheckBox existingFile = view.findViewById(R.id.existing_checkbox);

                    if (newFile.isSelected() && existingFile.isSelected()) {
                        listener.conflictDecisionMade(Decision.KEEP_BOTH);
                    } else if (newFile.isSelected()) {
                        listener.conflictDecisionMade(Decision.KEEP_LOCAL);
                    } else {
                        listener.conflictDecisionMade(Decision.KEEP_SERVER);
                    }
                }
            }))
            .setNegativeButton(R.string.common_cancel, ((dialog, which) -> {
                if (listener != null) {
                    listener.conflictDecisionMade(Decision.CANCEL);
                }
            }))
            .setTitle(ThemeUtils.getColoredTitle(getResources().getString(R.string.conflict_message_headline),
                                                 accentColor));

        // set info for new file
        TextView newSize = view.findViewById(R.id.new_size);
        newSize.setText(DisplayUtils.bytesToHumanReadable(newFile.length()));

        TextView newTimestamp = view.findViewById(R.id.new_timestamp);
        newTimestamp.setText(DisplayUtils.getRelativeTimestamp(getContext(), newFile.lastModified()));

        ImageView newThumbnail = view.findViewById(R.id.new_thumbnail);
        newThumbnail.setTag(newFile.hashCode());
        setThumbnail(newFile, newThumbnail);

        // set info for existing file
        TextView existingSize = view.findViewById(R.id.existing_size);
        existingSize.setText(DisplayUtils.bytesToHumanReadable(existingFile.getFileLength()));

        TextView existingTimestamp = view.findViewById(R.id.existing_timestamp);
        existingTimestamp.setText(DisplayUtils.getRelativeTimestamp(getContext(),
                                                                    existingFile.getModificationTimestamp()));

        ImageView existingThumbnail = view.findViewById(R.id.existing_thumbnail);
        existingThumbnail.setTag(existingFile.getFileId());
        setThumbnail(existingFile, view.findViewById(R.id.existing_thumbnail));

        return builder.create();
    }

    public void showDialog(AppCompatActivity activity) {
        Fragment prev = activity.getSupportFragmentManager().findFragmentByTag("dialog");
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        this.show(ft, "dialog");
    }

    @Override
    public void onCancel(@NotNull DialogInterface dialog) {
        if (listener != null) {
            listener.conflictDecisionMade(Decision.CANCEL);
        }
    }

    public interface OnConflictDecisionMadeListener {
        void conflictDecisionMade(Decision decision);
    }

    private void setThumbnail(OCFile file, ImageView thumbnailView) {
        if (file.isFolder()) {
            thumbnailView.setImageDrawable(MimeTypeUtil
                                               .getFolderTypeIcon(file.isSharedWithMe() || file.isSharedWithSharee(),
                                                                  file.isSharedViaLink(),
                                                                  file.isEncrypted(),
                                                                  file.getMountType(),
                                                                  getContext()));
        } else {
            if (file.getRemoteId() != null && file.isPreviewAvailable()) {
                // Thumbnail in cache?
                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.getRemoteId()
                );

                if (thumbnail != null && !file.isUpdateThumbnailNeeded()) {
                    if (MimeTypeUtil.isVideo(file)) {
                        Bitmap withOverlay = ThumbnailsCacheManager.addVideoOverlay(thumbnail);
                        thumbnailView.setImageBitmap(withOverlay);
                    } else {
                        BitmapUtils.setRoundedBitmap(thumbnail, thumbnailView);
                    }
                } else {
                    // generate new thumbnail
                    if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, thumbnailView)) {
                        try {
                            FileDataStorageManager storageManager =
                                new FileDataStorageManager(user.toPlatformAccount(),
                                                           requireContext().getContentResolver());
                            final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                                new ThumbnailsCacheManager.ThumbnailGenerationTask(thumbnailView,
                                                                                   storageManager,
                                                                                   user.toPlatformAccount(),
                                                                                   asyncTasks,
                                                                                   true);

                            if (thumbnail == null) {
                                thumbnail = BitmapUtils.drawableToBitmap(
                                    MimeTypeUtil.getFileTypeIcon(file.getMimeType(),
                                                                 file.getFileName(),
                                                                 user.toPlatformAccount(),
                                                                 getContext()));
                            }
                            final ThumbnailsCacheManager.AsyncThumbnailDrawable asyncDrawable =
                                new ThumbnailsCacheManager.AsyncThumbnailDrawable(getResources(),
                                                                                  thumbnail, task);
                            thumbnailView.setImageDrawable(asyncDrawable);
                            task.execute(new ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file,
                                                                                                  file.getRemoteId()));
                        } catch (IllegalArgumentException e) {
                            Log_OC.d(this, "ThumbnailGenerationTask : " + e.getMessage());
                        }
                    }
                }

                if ("image/png".equalsIgnoreCase(file.getMimeType())) {
                    thumbnailView.setBackgroundColor(getResources().getColor(R.color.bg_default));
                }
            } else {
                thumbnailView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(file.getMimeType(),
                                                                            file.getFileName(),
                                                                            user.toPlatformAccount(),
                                                                            getContext()));
            }
        }
    }

    private void setThumbnail(File file, ImageView thumbnailView) {
        if (file.isDirectory()) {
            thumbnailView.setImageDrawable(MimeTypeUtil.getDefaultFolderIcon(getContext()));
        } else {
            thumbnailView.setImageResource(R.drawable.file);

            /* Cancellation needs do be checked and done before changing the drawable in fileIcon, or
             * {@link ThumbnailsCacheManager#cancelPotentialThumbnailWork} will NEVER cancel any task.
             */
            boolean allowedToCreateNewThumbnail = ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, thumbnailView);


            // get Thumbnail if file is image
            if (MimeTypeUtil.isImage(file)) {
                // Thumbnail in Cache?
                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.hashCode()
                );
                if (thumbnail != null) {
                    thumbnailView.setImageBitmap(thumbnail);
                } else {

                    // generate new Thumbnail
                    if (allowedToCreateNewThumbnail) {
                        final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                            new ThumbnailsCacheManager.ThumbnailGenerationTask(thumbnailView);
                        if (MimeTypeUtil.isVideo(file)) {
                            thumbnail = ThumbnailsCacheManager.mDefaultVideo;
                        } else {
                            thumbnail = ThumbnailsCacheManager.mDefaultImg;
                        }
                        final ThumbnailsCacheManager.AsyncThumbnailDrawable asyncDrawable =
                            new ThumbnailsCacheManager.AsyncThumbnailDrawable(
                                getResources(),
                                thumbnail,
                                task
                            );
                        thumbnailView.setImageDrawable(asyncDrawable);
                        task.execute(new ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file, null));
                        Log_OC.v(this, "Executing task to generate a new thumbnail");

                    } // else, already being generated, don't restart it
                }
            } else {
                thumbnailView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(null, file.getName(), getContext()));
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        for (ThumbnailsCacheManager.ThumbnailGenerationTask task : asyncTasks) {
            if (task != null) {
                task.cancel(true);
                if (task.getGetMethod() != null) {
                    Log_OC.d(this, "cancel: abort get method directly");
                    task.getGetMethod().abort();
                }
            }
        }

        asyncTasks.clear();
    }
}
