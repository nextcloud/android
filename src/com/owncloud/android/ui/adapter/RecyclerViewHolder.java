package com.owncloud.android.ui.adapter;

import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.dialog.FileActionsDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.ui.preview.PreviewMediaFragment;
import com.owncloud.android.ui.preview.PreviewTextFragment;

/**
 * Created by Denis Dijak on 25.4.2016.
 */
public class RecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener, View.OnCreateContextMenuListener {

    private static final String TAG = RecyclerViewHolder.class.getSimpleName();

    private FileListListAdapter mAdapter;
    private OCFileListFragment mFileListFragment;
    private FileFragment.ContainerActivity mComponentsGetter;

    protected TextView fileName;
    protected ImageView fileIcon;
    protected TextView fileSizeV;
    protected TextView lastModV;
    protected ImageView checkBoxV;
    protected ImageView favoriteIcon;
    protected ImageView localStateView;
    protected ImageView sharedIconV;

    /**
     * Recycler view holder implements view holder logic for all the elements shown in recycler view.
     * They are found on the layout only once and then they are recycled for each row.
     *
     * Implement onClick listener for item click actions
     * Implement onLongClick listener for item long click actions
     * Implement onCreateContextMenu listener for item actions
     *
     * @param itemView current view
     * @param mAdapter adapter
     * @param mComponentsGetter ContainerActivity
     * @param mFileListFragment fileListFragment
     */
    public RecyclerViewHolder(View itemView, FileListListAdapter mAdapter, FileFragment.ContainerActivity mComponentsGetter, OCFileListFragment mFileListFragment) {
        super(itemView);
        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
        itemView.setOnCreateContextMenuListener(this);

        this.mAdapter = mAdapter;
        this.mFileListFragment = mFileListFragment;
        this.mComponentsGetter = mComponentsGetter;

        this.fileName = (TextView) itemView.findViewById(R.id.Filename);
        this.fileIcon = (ImageView) itemView.findViewById(R.id.thumbnail);
        this.fileSizeV = (TextView) itemView.findViewById(R.id.file_size);
        this.lastModV = (TextView) itemView.findViewById(R.id.last_mod);
        this.checkBoxV = (ImageView) itemView.findViewById(R.id.custom_checkbox);
        this.favoriteIcon = (ImageView) itemView.findViewById(R.id.favoriteIcon);
        this.localStateView = (ImageView) itemView.findViewById(R.id.localFileIndicator);
        this.sharedIconV = (ImageView) itemView.findViewById(R.id.sharedIcon);

    }

    /**
     * This is executed when user clicks on item in recycler view
     * @param v clicked View
     */
    @Override
    public void onClick(View v) {
        try {
            final OCFile file = mAdapter.getCurrentFiles().get(getAdapterPosition());
            mFileListFragment.setCurrentFile(file);

            if (file != null) {

                // save list position
                // TODO : save recycler view position

                if (file.isFolder()) {

                    // update state and view of this fragment
                    mFileListFragment.listDirectory(file);
                    // then, notify parent activity to let it update its state and view
                    mComponentsGetter.onBrowsedDownTo(file);

                } else { /// Click on a file
                    if (PreviewImageFragment.canBePreviewed(file)) {
                        // preview image - it handles the download, if needed
                        ((FileDisplayActivity) this.mComponentsGetter).startImagePreview(file);
                    } else if (PreviewTextFragment.canBePreviewed(file)) {
                        ((FileDisplayActivity) this.mComponentsGetter).startTextPreview(file);
                    } else if (file.isDown()) {
                        if (PreviewMediaFragment.canBePreviewed(file)) {
                            // media preview
                            ((FileDisplayActivity) this.mComponentsGetter).startMediaPreview(file, 0, true);
                        } else {
                            this.mComponentsGetter.getFileOperationsHelper().openFile(file);
                        }

                    } else {
                        // automatic download, preview on finish
                        ((FileDisplayActivity) this.mComponentsGetter).startDownloadForPreview(file);
                    }
                }

            } else {
                Log_OC.d(TAG, "Selected OCFile instance could not be found!");
            }
        }catch (Exception e)
        {
            Log_OC.d(TAG, "Something went wrong. Message : " + e.getMessage());
        }
    }

    /**
     * This is executed when user long clicks on item in recycler view
     * @param v selected view
     * @return always true
     */
    @Override
    public boolean onLongClick(View v) {
        showFileAction(getAdapterPosition());
        return true;
    }

    /**
     * This executes when recycler view holder is initialized
     * @param menu context menu
     * @param v selected view
     * @param menuInfo context menu info
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

        Bundle args = mFileListFragment.getArguments();
        boolean allowContextualActions =
                (args == null) || args.getBoolean(OCFileListFragment.ARG_ALLOW_CONTEXTUAL_ACTIONS, true);
        if (allowContextualActions) {
            MenuInflater inflater = mFileListFragment.getActivity().getMenuInflater();
            inflater.inflate(R.menu.file_actions_menu, menu);
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            OCFile targetFile = (OCFile) mAdapter.getItem(info.position);

            if (mComponentsGetter != null && mComponentsGetter.getStorageManager() != null) {
                FileMenuFilter mf = new FileMenuFilter(
                        targetFile,
                        mComponentsGetter.getStorageManager().getAccount(),
                        mComponentsGetter,
                        mFileListFragment.getActivity()
                );
                mf.filter(menu);
            }
        }

    }

    /**
     * Show dialog that represents actions that can be made on selected item
     * @param fileIndex file at index
     */
    private void showFileAction(int fileIndex) {
        Bundle args = mFileListFragment.getArguments();
        PopupMenu pm = new PopupMenu(mFileListFragment.getActivity(),null);
        Menu menu = pm.getMenu();

        boolean allowContextualActions =
                (args == null) || args.getBoolean(OCFileListFragment.ARG_ALLOW_CONTEXTUAL_ACTIONS, true);

        if (allowContextualActions) {
            MenuInflater inflater = mFileListFragment.getActivity().getMenuInflater();

            inflater.inflate(R.menu.file_actions_menu, menu);
            OCFile targetFile = (OCFile) mAdapter.getItem(fileIndex);

            if (mComponentsGetter.getStorageManager() != null) {
                FileMenuFilter mf = new FileMenuFilter(
                        targetFile,
                        mComponentsGetter.getStorageManager().getAccount(),
                        mComponentsGetter,
                        mFileListFragment.getActivity()
                );
                mf.filter(menu);
            }

            FileActionsDialogFragment dialog = FileActionsDialogFragment.newInstance(menu, fileIndex, targetFile);
            dialog.setTargetFragment(mFileListFragment, 0);
            dialog.show(mFileListFragment.getFragmentManager(), FileActionsDialogFragment.FTAG_FILE_ACTIONS);
        }
    }
}