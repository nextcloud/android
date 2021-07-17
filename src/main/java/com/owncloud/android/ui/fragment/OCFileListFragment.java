/*
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author masensio
 * @author David A. Velasco
 * @author Andy Scherzinger
 * @author Chris Narkiewicz
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2016 ownCloud Inc.
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.PopupMenu;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.android.lib.richWorkspace.RichWorkspaceDirectEditingRemoteOperation;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.device.DeviceInfo;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.VirtualFolderType;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.Creator;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.e2ee.ToggleEncryptionRemoteOperation;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.lib.resources.files.ToggleFavoriteRemoteOperation;
import com.owncloud.android.lib.resources.shares.GetSharesRemoteOperation;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener;
import com.owncloud.android.ui.activity.ToolbarActivity;
import com.owncloud.android.ui.activity.UploadFilesActivity;
import com.owncloud.android.ui.adapter.OCFileListAdapter;
import com.owncloud.android.ui.dialog.ChooseRichDocumentsTemplateDialogFragment;
import com.owncloud.android.ui.dialog.ChooseTemplateDialogFragment;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.CreateFolderDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.dialog.RenameFileDialogFragment;
import com.owncloud.android.ui.dialog.SetupEncryptionDialogFragment;
import com.owncloud.android.ui.dialog.SyncFileNotEnoughSpaceDialogFragment;
import com.owncloud.android.ui.events.ChangeMenuEvent;
import com.owncloud.android.ui.events.CommentsEvent;
import com.owncloud.android.ui.events.EncryptionEvent;
import com.owncloud.android.ui.events.FavoriteEvent;
import com.owncloud.android.ui.events.SearchEvent;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.ui.preview.PreviewMediaFragment;
import com.owncloud.android.ui.preview.PreviewTextFileFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeFabUtils;
import com.owncloud.android.utils.theme.ThemeToolbarUtils;
import com.owncloud.android.utils.theme.ThemeUtils;

import org.apache.commons.httpclient.HttpStatus;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.parceler.Parcels;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.owncloud.android.datamodel.OCFile.ROOT_PATH;
import static com.owncloud.android.utils.DisplayUtils.openSortingOrderDialogFragment;

/**
 * A Fragment that lists all files and folders in a given path.
 * TODO refactor to get rid of direct dependency on FileDisplayActivity
 */
public class OCFileListFragment extends ExtendedListFragment implements
        OCFileListFragmentInterface,
        OCFileListBottomSheetActions,
        Injectable {

    protected static final String TAG = OCFileListFragment.class.getSimpleName();

    private static final String MY_PACKAGE = OCFileListFragment.class.getPackage() != null ?
            OCFileListFragment.class.getPackage().getName() : "com.owncloud.android.ui.fragment";

    public final static String ARG_ONLY_FOLDERS_CLICKABLE = MY_PACKAGE + ".ONLY_FOLDERS_CLICKABLE";
    public final static String ARG_FILE_SELECTABLE = MY_PACKAGE + ".FILE_SELECTABLE";
    public final static String ARG_ALLOW_CONTEXTUAL_ACTIONS = MY_PACKAGE + ".ALLOW_CONTEXTUAL";
    public final static String ARG_HIDE_FAB = MY_PACKAGE + ".HIDE_FAB";
    public final static String ARG_HIDE_ITEM_OPTIONS = MY_PACKAGE + ".HIDE_ITEM_OPTIONS";
    public final static String ARG_SEARCH_ONLY_FOLDER = MY_PACKAGE + ".SEARCH_ONLY_FOLDER";
    public final static String ARG_MIMETYPE = MY_PACKAGE + ".MIMETYPE";

    public static final String DOWNLOAD_BEHAVIOUR = "DOWNLOAD_BEHAVIOUR";
    public static final String DOWNLOAD_SEND = "DOWNLOAD_SEND";

    public static final String FOLDER_LAYOUT_LIST = "LIST";
    public static final String FOLDER_LAYOUT_GRID = "GRID";

    public static final String SEARCH_EVENT = "SEARCH_EVENT";

    private static final String KEY_FILE = MY_PACKAGE + ".extra.FILE";

    protected static final String KEY_CURRENT_SEARCH_TYPE = "CURRENT_SEARCH_TYPE";

    private static final String DIALOG_CREATE_FOLDER = "DIALOG_CREATE_FOLDER";
    private static final String DIALOG_CREATE_DOCUMENT = "DIALOG_CREATE_DOCUMENT";

    private static final int SINGLE_SELECTION = 1;
    private static final int NOT_ENOUGH_SPACE_FRAG_REQUEST_CODE = 2;

    @Inject AppPreferences preferences;
    @Inject UserAccountManager accountManager;
    @Inject ClientFactory clientFactory;
    protected FileFragment.ContainerActivity mContainerActivity;

    protected OCFile mFile;
    protected OCFileListAdapter mAdapter;
    protected boolean mOnlyFoldersClickable;
    protected boolean mFileSelectable;

    protected boolean mHideFab = true;
    protected ActionMode mActiveActionMode;
    protected boolean mIsActionModeNew;
    protected OCFileListFragment.MultiChoiceModeListener mMultiChoiceModeListener;

    protected SearchType currentSearchType;
    protected boolean searchFragment;
    protected SearchEvent searchEvent;
    protected AsyncTask<Void, Void, Boolean> remoteOperationAsyncTask;
    protected String mLimitToMimeType;
    private FloatingActionButton mFabMain;

    @Inject DeviceInfo deviceInfo;

    protected enum MenuItemAddRemove {
        DO_NOTHING,
        REMOVE_SORT,
        REMOVE_GRID_AND_SORT,
        ADD_GRID_AND_SORT_WITH_SEARCH
    }

    protected MenuItemAddRemove menuItemAddRemoveValue = MenuItemAddRemove.ADD_GRID_AND_SORT_WITH_SEARCH;

    private List<MenuItem> mOriginalMenuItems = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mMultiChoiceModeListener = new MultiChoiceModeListener();

        if (savedInstanceState != null) {
            currentSearchType = Parcels.unwrap(savedInstanceState.getParcelable(KEY_CURRENT_SEARCH_TYPE));
            searchEvent = Parcels.unwrap(savedInstanceState.getParcelable(OCFileListFragment.SEARCH_EVENT));
            mFile = savedInstanceState.getParcelable(KEY_FILE);
        }

        searchFragment = currentSearchType != null && isSearchEventSet(searchEvent);
    }

    @Override
    public void onResume() {
        if (getActivity() == null) {
            return;
        }

        Intent intent = getActivity().getIntent();

        if (intent.getParcelableExtra(OCFileListFragment.SEARCH_EVENT) != null) {
            searchEvent = Parcels.unwrap(intent.getParcelableExtra(OCFileListFragment.SEARCH_EVENT));
            onMessageEvent(searchEvent);
        }

        super.onResume();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log_OC.i(TAG, "onAttach");
        try {
            mContainerActivity = (FileFragment.ContainerActivity) context;

        } catch (ClassCastException e) {
            throw new IllegalArgumentException(context.toString() + " must implement " +
                    FileFragment.ContainerActivity.class.getSimpleName(), e);
        }
        try {
            setOnRefreshListener((OnEnforceableRefreshListener) context);

        } catch (ClassCastException e) {
            throw new IllegalArgumentException(context.toString() + " must implement " +
                                                   OnEnforceableRefreshListener.class.getSimpleName(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.i(TAG, "onCreateView() start");
        View v = super.onCreateView(inflater, container, savedInstanceState);

        if (savedInstanceState != null
                && Parcels.unwrap(savedInstanceState.getParcelable(KEY_CURRENT_SEARCH_TYPE)) != null &&
                Parcels.unwrap(savedInstanceState.getParcelable(OCFileListFragment.SEARCH_EVENT)) != null) {
            searchFragment = true;
            currentSearchType = Parcels.unwrap(savedInstanceState.getParcelable(KEY_CURRENT_SEARCH_TYPE));
            searchEvent = Parcels.unwrap(savedInstanceState.getParcelable(OCFileListFragment.SEARCH_EVENT));
        } else {
            currentSearchType = SearchType.NO_SEARCH;
        }

        Bundle args = getArguments();
        boolean allowContextualActions = args != null && args.getBoolean(ARG_ALLOW_CONTEXTUAL_ACTIONS, false);
        if (allowContextualActions) {
            setChoiceModeAsMultipleModal(savedInstanceState);
        }

        mFabMain = requireActivity().findViewById(R.id.fab_main);

        if (mFabMain != null) { // is not available in FolderPickerActivity
            ThemeFabUtils.colorFloatingActionButton(mFabMain, R.drawable.ic_plus, requireContext());
        }

        Log_OC.i(TAG, "onCreateView() end");
        return v;
    }

    @Override
    public void onDetach() {
        setOnRefreshListener(null);
        mContainerActivity = null;

        if (remoteOperationAsyncTask != null) {
            remoteOperationAsyncTask.cancel(true);
        }
        super.onDetach();
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.cancelAllPendingTasks();

        if (getActivity() != null) {
            getActivity().getIntent().removeExtra(OCFileListFragment.SEARCH_EVENT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log_OC.i(TAG, "onActivityCreated() start");

        if (savedInstanceState != null) {
            mFile = savedInstanceState.getParcelable(KEY_FILE);
        }

        Bundle args = getArguments();
        mOnlyFoldersClickable = args != null && args.getBoolean(ARG_ONLY_FOLDERS_CLICKABLE, false);
        mFileSelectable = args != null && args.getBoolean(ARG_FILE_SELECTABLE, false);
        mLimitToMimeType = args != null ? args.getString(ARG_MIMETYPE, "") : "";
        boolean hideItemOptions = args != null && args.getBoolean(ARG_HIDE_ITEM_OPTIONS, false);

        mAdapter = new OCFileListAdapter(
            getActivity(),
            accountManager.getUser(),
            preferences,
            mContainerActivity,
            this,
            hideItemOptions,
            isGridViewPreferred(mFile)
        );
        setRecyclerViewAdapter(mAdapter);

        mHideFab = args != null && args.getBoolean(ARG_HIDE_FAB, false);

        if (mHideFab) {
            setFabVisible(false);
        } else {
            setFabVisible(true);
            registerFabListener();
        }

        if (getArguments() == null) {
            searchEvent = null;
        } else {
            searchEvent = Parcels.unwrap(getArguments().getParcelable(OCFileListFragment.SEARCH_EVENT));
        }
        prepareCurrentSearch(searchEvent);

        if (mSortButton != null) {
            mSortButton.setOnClickListener(v -> openSortingOrderDialogFragment(requireFragmentManager(),
                                                                               preferences.getSortOrderByFolder(mFile)));
        }

        if (mSwitchGridViewButton != null) {
            mSwitchGridViewButton.setOnClickListener(v -> {
                if (isGridEnabled()) {
                    setListAsPreferred();
                } else {
                    setGridAsPreferred();
                }
                setGridSwitchButton();
            });
        }

        setTitle();

        if (searchEvent != null) {
            onMessageEvent(searchEvent);
        }

        FragmentActivity fragmentActivity;
        if ((fragmentActivity = getActivity()) != null && fragmentActivity instanceof FileDisplayActivity) {
            FileDisplayActivity fileDisplayActivity = (FileDisplayActivity) fragmentActivity;
            fileDisplayActivity.updateActionBarTitleAndHomeButton(fileDisplayActivity.getCurrentDir());
        }
        listDirectory(false, false);
    }

    protected void prepareCurrentSearch(SearchEvent event) {
        if (isSearchEventSet(event)) {

            switch (event.getSearchType()) {
                case FILE_SEARCH:
                    currentSearchType = SearchType.FILE_SEARCH;
                    break;

                case FAVORITE_SEARCH:
                    currentSearchType = SearchType.FAVORITE_SEARCH;
                    break;

                case RECENTLY_MODIFIED_SEARCH:
                    currentSearchType = SearchType.RECENTLY_MODIFIED_SEARCH;
                    break;

                case SHARED_FILTER:
                    currentSearchType = SearchType.SHARED_FILTER;
                    break;

                default:
                    // do nothing
                    break;
            }

            prepareActionBarItems(event);
        }
    }

    /**
     * register listener on FAB.
     */
    public void registerFabListener() {
        FileActivity activity = (FileActivity) getActivity();

        if (mFabMain != null) { // is not available in FolderPickerActivity
            ThemeFabUtils.colorFloatingActionButton(mFabMain, R.drawable.ic_plus, requireContext());
            mFabMain.setOnClickListener(v -> new OCFileListBottomSheetDialog(activity,
                                                                             this,
                                                                             deviceInfo,
                                                                             accountManager.getUser(),
                                                                             getCurrentFile())
                .show());
        }
    }

    @Override
    public void createFolder() {
        CreateFolderDialogFragment.newInstance(mFile)
                .show(getActivity().getSupportFragmentManager(), DIALOG_CREATE_FOLDER);
    }

    @Override
    public void uploadFromApp() {
        Intent action = new Intent(Intent.ACTION_GET_CONTENT);
        action = action.setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
        action.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        getActivity().startActivityForResult(
                Intent.createChooser(action, getString(R.string.upload_chooser_title)),
                FileDisplayActivity.REQUEST_CODE__SELECT_CONTENT_FROM_APPS
        );
    }

    @Override
    public void directCameraUpload() {
        FileDisplayActivity fileDisplayActivity = (FileDisplayActivity) getActivity();

        if (fileDisplayActivity != null) {
            fileDisplayActivity.getFileOperationsHelper()
                .uploadFromCamera(fileDisplayActivity, FileDisplayActivity.REQUEST_CODE__UPLOAD_FROM_CAMERA);
        } else {
            DisplayUtils.showSnackMessage(getView(), getString(R.string.error_starting_direct_camera_upload));
        }
    }

    @Override
    public void uploadFiles() {
        UploadFilesActivity.startUploadActivityForResult(
                getActivity(),
                ((FileActivity) getActivity()).getAccount(),
                FileDisplayActivity.REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM
        );
    }

    @Override
    public void createRichWorkspace() {
        new Thread(() -> {
            RemoteOperationResult result = new RichWorkspaceDirectEditingRemoteOperation(mFile.getRemotePath())
                .execute(accountManager.getUser().toPlatformAccount(), requireContext());

            if (result.isSuccess()) {
                String url = (String) result.getSingleData();
                mContainerActivity.getFileOperationsHelper().openRichWorkspaceWithTextEditor(mFile,
                                                                                             url,
                                                                                             requireContext());
            } else {
                DisplayUtils.showSnackMessage(getView(), R.string.failed_to_start_editor);
            }
        }).start();
    }

    @Override
    public void onShareIconClick(OCFile file) {
        if (file.isFolder()) {
            mContainerActivity.showDetails(file, 1);
        } else {
            mContainerActivity.getFileOperationsHelper().sendShareFile(file);
        }
    }

    @Override
    public void showShareDetailView(OCFile file) {
        mContainerActivity.showDetails(file, 1);
    }

    @Override
    public void showActivityDetailView(OCFile file) {
        mContainerActivity.showDetails(file, 0);
    }

    @Override
    public void onOverflowIconClicked(OCFile file, View view) {
        PopupMenu popup = new PopupMenu(getActivity(), view);
        popup.inflate(R.menu.item_file);
        FileMenuFilter mf = new FileMenuFilter(mAdapter.getFiles().size(),
                                               Collections.singleton(file),
                                               mContainerActivity, getActivity(),
                                               true,
                                               accountManager.getUser());
        mf.filter(popup.getMenu(), true);
        popup.setOnMenuItemClickListener(item -> {
            Set<OCFile> checkedFiles = new HashSet<>();
            checkedFiles.add(file);
            return onFileActionChosen(item, checkedFiles);
        });
        popup.show();
    }

    @Override
    public void newDocument() {
        ChooseRichDocumentsTemplateDialogFragment.newInstance(mFile,
                                                              ChooseRichDocumentsTemplateDialogFragment.Type.DOCUMENT)
                .show(requireActivity().getSupportFragmentManager(), DIALOG_CREATE_DOCUMENT);
    }

    @Override
    public void newSpreadsheet() {
        ChooseRichDocumentsTemplateDialogFragment.newInstance(mFile,
                                                              ChooseRichDocumentsTemplateDialogFragment.Type.SPREADSHEET)
                .show(requireActivity().getSupportFragmentManager(), DIALOG_CREATE_DOCUMENT);
    }

    @Override
    public void newPresentation() {
        ChooseRichDocumentsTemplateDialogFragment.newInstance(mFile,
                                                              ChooseRichDocumentsTemplateDialogFragment.Type.PRESENTATION)
                .show(requireActivity().getSupportFragmentManager(), DIALOG_CREATE_DOCUMENT);
    }

    @Override
    public void onHeaderClicked() {
        if (!getAdapter().isMultiSelect() && mContainerActivity instanceof FileDisplayActivity) {
            ((FileDisplayActivity) mContainerActivity).startRichWorkspacePreview(getCurrentFile());
        }
    }

    @Override
    public void showTemplate(Creator creator, String headline) {
        ChooseTemplateDialogFragment.newInstance(mFile, creator, headline).show(requireActivity().getSupportFragmentManager(),
                                                                      DIALOG_CREATE_DOCUMENT);
    }

    /**
     * Handler for multiple selection mode.
     * <p>
     * Manages input from the user when one or more files or folders are selected in the list.
     * <p>
     * Also listens to changes in navigation drawer to hide and recover multiple selection when it's opened
     * and closed.
     */
    private class MultiChoiceModeListener implements AbsListView.MultiChoiceModeListener, DrawerLayout.DrawerListener {

        private static final String KEY_ACTION_MODE_CLOSED_BY_DRAWER = "KILLED_ACTION_MODE";

        /**
         * True when action mode is finished because the drawer was opened
         */
        private boolean mActionModeClosedByDrawer;

        /**
         * Selected items in list when action mode is closed by drawer
         */
        private Set<OCFile> mSelectionWhenActionModeClosedByDrawer = new HashSet<>();

        @Override
        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
            // nothing to do
        }

        @Override
        public void onDrawerOpened(@NonNull View drawerView) {
            // nothing to do
        }

        /**
         * When the navigation drawer is closed, action mode is recovered in the same state as was
         * when the drawer was (started to be) opened.
         *
         * @param drawerView Navigation drawer just closed.
         */
        @Override
        public void onDrawerClosed(@NonNull View drawerView) {
            if (mActionModeClosedByDrawer && mSelectionWhenActionModeClosedByDrawer.size() > 0) {
                FragmentActivity actionBarActivity = getActivity();
                actionBarActivity.startActionMode(mMultiChoiceModeListener);

                getAdapter().setCheckedItem(mSelectionWhenActionModeClosedByDrawer);

                mActiveActionMode.invalidate();

                mSelectionWhenActionModeClosedByDrawer.clear();
            }
        }

        /**
         * If the action mode is active when the navigation drawer starts to move, the action
         * mode is closed and the selection stored to be recovered when the drawer is closed.
         *
         * @param newState One of STATE_IDLE, STATE_DRAGGING or STATE_SETTLING.
         */
        @Override
        public void onDrawerStateChanged(int newState) {
            if (DrawerLayout.STATE_DRAGGING == newState && mActiveActionMode != null) {
                mSelectionWhenActionModeClosedByDrawer.addAll(((OCFileListAdapter) getRecyclerView().getAdapter())
                        .getCheckedItems());
                mActiveActionMode.finish();
                mActionModeClosedByDrawer = true;
            }
        }

        /**
         * Update action mode bar when an item is selected / unselected in the list
         */
        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            // nothing to do here
        }

        /**
         * Load menu and customize UI when action mode is started.
         */
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActiveActionMode = mode;
            // Determine if actionMode is "new" or not (already affected by item-selection)
            mIsActionModeNew = true;

            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.item_file, menu);
            mode.invalidate();

            //set actionMode color
            ThemeToolbarUtils.colorStatusBar(getActivity(), ThemeColorUtils.actionModeColor(requireContext()));

            // hide FAB in multi selection mode
            setFabVisible(false);

            mAdapter.setMultiSelect(true);
            return true;
        }

        /**
         * Updates available action in menu depending on current selection.
         */
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            final int checkedCount = mAdapter.getCheckedItems().size();
            Set<OCFile> checkedFiles = mAdapter.getCheckedItems();
            String title = getResources().getQuantityString(R.plurals.items_selected_count, checkedCount, checkedCount);
            mode.setTitle(title);
            FileMenuFilter mf = new FileMenuFilter(
                mAdapter.getFiles().size(),
                checkedFiles,
                mContainerActivity,
                getActivity(),
                false,
                accountManager.getUser()
            );

            mf.filter(menu, false);

            // Determine if we need to finish the action mode because there are no items selected
            if (checkedCount == 0 && !mIsActionModeNew) {
                exitSelectionMode();
            }

            return true;
        }

        /**
         * Starts the corresponding action when a menu item is tapped by the user.
         */
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Set<OCFile> checkedFiles = mAdapter.getCheckedItems();
            return onFileActionChosen(item, checkedFiles);
        }

        /**
         * Restores UI.
         */
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActiveActionMode = null;

            // reset to previous color
            FragmentActivity fragmentActivity;
            if ((fragmentActivity = getActivity()) != null && fragmentActivity instanceof FileDisplayActivity) {
                FileDisplayActivity fileDisplayActivity = (FileDisplayActivity) fragmentActivity;
                fileDisplayActivity.updateActionBarTitleAndHomeButton(fileDisplayActivity.getCurrentDir());
            }

            // show FAB on multi selection mode exit
            if (!mHideFab && !searchFragment) {
                setFabVisible(true);
            }

            mAdapter.setMultiSelect(false);
            mAdapter.clearCheckedItems();
        }

        public void storeStateIn(Bundle outState) {
            outState.putBoolean(KEY_ACTION_MODE_CLOSED_BY_DRAWER, mActionModeClosedByDrawer);
        }

        public void loadStateFrom(Bundle savedInstanceState) {
            mActionModeClosedByDrawer = savedInstanceState.getBoolean(KEY_ACTION_MODE_CLOSED_BY_DRAWER,
                    mActionModeClosedByDrawer);
        }
    }

    /**
     * Init listener that will handle interactions in multiple selection mode.
     */
    protected void setChoiceModeAsMultipleModal(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mMultiChoiceModeListener.loadStateFrom(savedInstanceState);
        }
        ((FileActivity) getActivity()).addDrawerListener(mMultiChoiceModeListener);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {
            searchEvent = Parcels.unwrap(savedInstanceState.getParcelable(SEARCH_EVENT));
        }

        if (isSearchEventSet(searchEvent)) {
            onMessageEvent(searchEvent);
        }
    }

    /**
     * Saves the current listed folder.
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(KEY_FILE, mFile);
        if (searchFragment) {
            outState.putParcelable(KEY_CURRENT_SEARCH_TYPE, Parcels.wrap(currentSearchType));
            if (isSearchEventSet(searchEvent)) {
                outState.putParcelable(OCFileListFragment.SEARCH_EVENT, Parcels.wrap(searchEvent));
            }
        }
        mMultiChoiceModeListener.storeStateIn(outState);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        if (mOriginalMenuItems.size() == 0) {
            mOriginalMenuItems.add(menu.findItem(R.id.action_search));
        }

        switch (menuItemAddRemoveValue) {
            case ADD_GRID_AND_SORT_WITH_SEARCH:
                mSwitchGridViewButton.setVisibility(View.VISIBLE);
                mSortButton.setVisibility(View.VISIBLE);
                break;

            case REMOVE_SORT:
                mSortButton.setVisibility(View.GONE);
                break;

            case REMOVE_GRID_AND_SORT:
                menu.removeItem(R.id.action_search);
                mSortButton.setVisibility(View.GONE);
                mSwitchGridViewButton.setVisibility(View.GONE);
                break;

            case DO_NOTHING:
            default:
                Log_OC.v(TAG, "Kept the options menu default structure");
                break;
        }
    }

    /**
     * Call this, when the user presses the up button.
     *
     * Tries to move up the current folder one level. If the parent folder was removed from the
     * database, it continues browsing up until finding an existing folders.
     *
     * return       Count of folder levels browsed up.
     */
    public int onBrowseUp() {
        OCFile parentDir;
        int moveCount = 0;

        if (mFile != null) {
            FileDataStorageManager storageManager = mContainerActivity.getStorageManager();

            String parentPath = null;
            if (mFile.getParentId() != FileDataStorageManager.ROOT_PARENT_ID) {
                parentPath = new File(mFile.getRemotePath()).getParent();
                parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath :
                        parentPath + OCFile.PATH_SEPARATOR;
                parentDir = storageManager.getFileByPath(parentPath);
                moveCount++;
            } else {
                parentDir = storageManager.getFileByPath(ROOT_PATH);
            }
            while (parentDir == null) {
                parentPath = new File(parentPath).getParent();
                parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath :
                        parentPath + OCFile.PATH_SEPARATOR;
                parentDir = storageManager.getFileByPath(parentPath);
                moveCount++;
            }   // exit is granted because storageManager.getFileByPath("/") never returns null
            mFile = parentDir;

            listDirectory(mFile, MainApp.isOnlyOnDevice(), false);

            onRefresh(false);

            // restore index and top position
            restoreIndexAndTopPosition();

        }   // else - should never happen now

        return moveCount;
    }

    /**
     * Will toggle a file selection status from the action mode
     * @param file The concerned OCFile by the selection/deselection
     */
    private void toggleItemToCheckedList(OCFile file) {
        if (getAdapter().isCheckedFile(file)) {
            getAdapter().removeCheckedFile(file);
        } else {
            getAdapter().addCheckedFile(file);
        }
        updateActionModeFile(file);
    }

    /**
     * Will update (invalidate) the action mode adapter/mode to refresh an item selection change
     * @param file The concerned OCFile to refresh in adapter
     */
    private void updateActionModeFile(OCFile file) {
        mIsActionModeNew = false;
        if (mActiveActionMode != null) {
            mActiveActionMode.invalidate();
            mAdapter.notifyItemChanged(getAdapter().getItemPosition(file));
        }
    }

    @Override
    public boolean onLongItemClicked(OCFile file) {
        FragmentActivity actionBarActivity = getActivity();
        if (actionBarActivity != null) {
            // Create only once instance of action mode
            if (mActiveActionMode != null) {
                toggleItemToCheckedList(file);
            } else {
                actionBarActivity.startActionMode(mMultiChoiceModeListener);
                getAdapter().addCheckedFile(file);
            }
            updateActionModeFile(file);
        }

        return true;
    }

    @Override
    public void onItemClicked(OCFile file) {
        if (getAdapter().isMultiSelect()) {
            toggleItemToCheckedList(file);
        } else {
            if (file != null) {
                int position = mAdapter.getItemPosition(file);

                if (file.isFolder()) {
                    resetHeaderScrollingState();

                    if (file.isEncrypted()) {
                        User user = ((FileActivity) mContainerActivity).getUser().orElseThrow(RuntimeException::new);

                        // check if e2e app is enabled
                        OCCapability ocCapability = mContainerActivity.getStorageManager()
                            .getCapability(user.getAccountName());

                        if (ocCapability.getEndToEndEncryption().isFalse() ||
                            ocCapability.getEndToEndEncryption().isUnknown()) {
                            Snackbar.make(getRecyclerView(), R.string.end_to_end_encryption_not_enabled,
                                          Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        // check if keys are stored
                        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(
                            getContext().getContentResolver());


                        String publicKey = arbitraryDataProvider.getValue(user, EncryptionUtils.PUBLIC_KEY);
                        String privateKey = arbitraryDataProvider.getValue(user, EncryptionUtils.PRIVATE_KEY);

                        if (publicKey.isEmpty() || privateKey.isEmpty()) {
                            Log_OC.d(TAG, "no public key for " + user.getAccountName());

                            SetupEncryptionDialogFragment dialog = SetupEncryptionDialogFragment.newInstance(user,
                                                                                                             position);
                            dialog.setTargetFragment(this, SetupEncryptionDialogFragment.SETUP_ENCRYPTION_REQUEST_CODE);
                            dialog.show(getFragmentManager(), SetupEncryptionDialogFragment.SETUP_ENCRYPTION_DIALOG_TAG);
                        } else {
                            // update state and view of this fragment
                            searchFragment = false;
                            mHideFab = false;

                            if (mContainerActivity instanceof FolderPickerActivity &&
                                    ((FolderPickerActivity) mContainerActivity)
                                            .isDoNotEnterEncryptedFolder()) {
                                Snackbar.make(getRecyclerView(),
                                        R.string.copy_move_to_encrypted_folder_not_supported,
                                        Snackbar.LENGTH_LONG).show();
                            } else {
                                listDirectory(file, MainApp.isOnlyOnDevice(), false);
                                // then, notify parent activity to let it update its state and view
                                mContainerActivity.onBrowsedDownTo(file);
                                // save index and top position
                                saveIndexAndTopPosition(position);
                            }
                        }
                    } else {
                        // update state and view of this fragment
                        searchFragment = false;
                        setEmptyListLoadingMessage();
                        listDirectory(file, MainApp.isOnlyOnDevice(), false);
                        // then, notify parent activity to let it update its state and view
                        mContainerActivity.onBrowsedDownTo(file);
                        // save index and top position
                        saveIndexAndTopPosition(position);
                    }

                } else if (mFileSelectable) {
                    Intent intent = new Intent();
                    intent.putExtra(FolderPickerActivity.EXTRA_FILES, file);
                    getActivity().setResult(Activity.RESULT_OK, intent);
                    getActivity().finish();
                } else if (!mOnlyFoldersClickable) { // Click on a file
                    if (PreviewImageFragment.canBePreviewed(file)) {
                        // preview image - it handles the download, if needed
                        if (searchFragment) {
                            VirtualFolderType type;
                            switch (currentSearchType) {
                                case FAVORITE_SEARCH:
                                    type = VirtualFolderType.FAVORITE;
                                    break;
                                case GALLERY_SEARCH:
                                    type = VirtualFolderType.GALLERY;
                                    break;
                                default:
                                    type = VirtualFolderType.NONE;
                                    break;
                            }
                            ((FileDisplayActivity) mContainerActivity).startImagePreview(file, type, !file.isDown());
                        } else {
                            ((FileDisplayActivity) mContainerActivity).startImagePreview(file, !file.isDown());
                        }
                    } else if (file.isDown() && MimeTypeUtil.isVCard(file)) {
                        ((FileDisplayActivity) mContainerActivity).startContactListFragment(file);
                    } else if (PreviewTextFileFragment.canBePreviewed(file)) {
                        setFabVisible(false);
                        resetHeaderScrollingState();
                        ((FileDisplayActivity) mContainerActivity).startTextPreview(file, false);
                    } else if (file.isDown()) {
                        if (PreviewMediaFragment.canBePreviewed(file)) {
                            // media preview
                            setFabVisible(false);
                            resetHeaderScrollingState();
                            ((FileDisplayActivity) mContainerActivity).startMediaPreview(file, 0, true, true, false);
                        } else {
                            mContainerActivity.getFileOperationsHelper().openFile(file);
                        }
                    } else {
                        // file not downloaded, check for streaming, remote editing
                        User account = accountManager.getUser();
                        OCCapability capability = mContainerActivity.getStorageManager()
                            .getCapability(account.getAccountName());

                        if (PreviewMediaFragment.canBePreviewed(file) && !file.isEncrypted()) {
                            // stream media preview on >= NC14
                            setFabVisible(false);
                            resetHeaderScrollingState();
                            ((FileDisplayActivity) mContainerActivity).startMediaPreview(file, 0, true, true, true);
                        } else if (FileMenuFilter.isEditorAvailable(requireContext().getContentResolver(),
                                                                    accountManager.getUser(),
                                                                    file.getMimeType()) &&
                            !file.isEncrypted()) {
                            mContainerActivity.getFileOperationsHelper().openFileWithTextEditor(file, getContext());
                        } else if (capability.getRichDocumentsMimeTypeList().contains(file.getMimeType()) &&
                            capability.getRichDocumentsDirectEditing().isTrue() && !file.isEncrypted()) {
                            mContainerActivity.getFileOperationsHelper().openFileAsRichDocument(file, getContext());
                        } else {
                            // automatic download, preview on finish
                            ((FileDisplayActivity) mContainerActivity).startDownloadForPreview(file);
                        }
                    }
                }
        } else {
                Log_OC.d(TAG, "Null object in ListAdapter!");
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SetupEncryptionDialogFragment.SETUP_ENCRYPTION_REQUEST_CODE &&
                resultCode == SetupEncryptionDialogFragment.SETUP_ENCRYPTION_RESULT_CODE &&
                data.getBooleanExtra(SetupEncryptionDialogFragment.SUCCESS, false)) {

            int position = data.getIntExtra(SetupEncryptionDialogFragment.ARG_POSITION, -1);
            OCFile file = mAdapter.getItem(position);

            // update state and view of this fragment
            searchFragment = false;
            listDirectory(file, MainApp.isOnlyOnDevice(), false);
            // then, notify parent activity to let it update its state and view
            mContainerActivity.onBrowsedDownTo(file);
            // save index and top position
            saveIndexAndTopPosition(position);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Start the appropriate action(s) on the currently selected files given menu selected by the user.
     *
     * @param item       MenuItem selected by the user
     * @param checkedFiles List of files selected by the user on which the action should be performed
     * @return 'true' if the menu selection started any action, 'false' otherwise.
     */
    public boolean onFileActionChosen(MenuItem item, Set<OCFile> checkedFiles) {
        if (checkedFiles.isEmpty()) {
            return false;
        }

        if (checkedFiles.size() == SINGLE_SELECTION) {
            /// action only possible on a single file
            OCFile singleFile = checkedFiles.iterator().next();
            int itemId = item.getItemId();

            if (itemId == R.id.action_send_share_file) {
                mContainerActivity.getFileOperationsHelper().sendShareFile(singleFile);
                return true;
            } else if (itemId == R.id.action_open_file_with) {
                mContainerActivity.getFileOperationsHelper().openFile(singleFile);
                return true;
            } else if (itemId == R.id.action_stream_media) {
                mContainerActivity.getFileOperationsHelper().streamMediaFile(singleFile);
                return true;
            } else if (itemId == R.id.action_edit) {
                // should not be necessary, as menu item is filtered, but better play safe
                if (FileMenuFilter.isEditorAvailable(requireContext().getContentResolver(),
                                                     accountManager.getUser(),
                                                     singleFile.getMimeType())) {
                    mContainerActivity.getFileOperationsHelper().openFileWithTextEditor(singleFile, getContext());
                } else {
                    mContainerActivity.getFileOperationsHelper().openFileAsRichDocument(singleFile, getContext());
                }

                return true;
            } else if (itemId == R.id.action_rename_file) {
                RenameFileDialogFragment dialog = RenameFileDialogFragment.newInstance(singleFile);
                dialog.show(getFragmentManager(), FileDetailFragment.FTAG_RENAME_FILE);
                return true;
            } else if (itemId == R.id.action_see_details) {
                if (mActiveActionMode != null) {
                    mActiveActionMode.finish();
                }

                mContainerActivity.showDetails(singleFile);
                mContainerActivity.showSortListGroup(false);
                return true;
            } else if (itemId == R.id.action_set_as_wallpaper) {
                mContainerActivity.getFileOperationsHelper().setPictureAs(singleFile, getView());
                return true;
            } else if (itemId == R.id.action_encrypted) {
                mContainerActivity.getFileOperationsHelper().toggleEncryption(singleFile, true);
                return true;
            } else if (itemId == R.id.action_unset_encrypted) {
                mContainerActivity.getFileOperationsHelper().toggleEncryption(singleFile, false);
                return true;
            }
        }

        /// actions possible on a batch of files
        int itemId = item.getItemId();
        if (itemId == R.id.action_remove_file) {
            RemoveFilesDialogFragment dialog =
                RemoveFilesDialogFragment.newInstance(new ArrayList<>(checkedFiles), mActiveActionMode);
            dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
            return true;
        } else if (itemId == R.id.action_download_file || itemId == R.id.action_sync_file) {
            syncAndCheckFiles(checkedFiles);
            exitSelectionMode();
            return true;
        } else if (itemId == R.id.action_cancel_sync) {
            ((FileDisplayActivity) mContainerActivity).cancelTransference(checkedFiles);
            return true;
        } else if (itemId == R.id.action_favorite) {
            mContainerActivity.getFileOperationsHelper().toggleFavoriteFiles(checkedFiles, true);
            return true;
        } else if (itemId == R.id.action_unset_favorite) {
            mContainerActivity.getFileOperationsHelper().toggleFavoriteFiles(checkedFiles, false);
            return true;
        } else if (itemId == R.id.action_move) {
            Intent action = new Intent(getActivity(), FolderPickerActivity.class);
            action.putParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES, new ArrayList<>(checkedFiles));
            action.putExtra(FolderPickerActivity.EXTRA_CURRENT_FOLDER, mFile);
            action.putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.MOVE);
            getActivity().startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__MOVE_FILES);
            return true;
        } else if (itemId == R.id.action_copy) {
            Intent action = new Intent(getActivity(), FolderPickerActivity.class);
            action.putParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES, new ArrayList<>(checkedFiles));
            action.putExtra(FolderPickerActivity.EXTRA_CURRENT_FOLDER, mFile);
            action.putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.COPY);
            getActivity().startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__COPY_FILES);
            return true;
        } else if (itemId == R.id.action_select_all_action_menu) {
            selectAllFiles(true);
            return true;
        } else if (itemId == R.id.action_deselect_all_action_menu) {
            selectAllFiles(false);
            return true;
        } else if (itemId == R.id.action_send_file) {
            mContainerActivity.getFileOperationsHelper().sendFiles(checkedFiles);
            return true;
        }

        return false;
    }

    /**
     * Use this to query the {@link OCFile} that is currently
     * being displayed by this fragment
     *
     * @return The currently viewed OCFile
     */
    public OCFile getCurrentFile() {
        return mFile;
    }

    /**
     * Calls {@link OCFileListFragment#listDirectory(OCFile, boolean, boolean)} with a null parameter
     */
    public void listDirectory(boolean onlyOnDevice, boolean fromSearch) {
        listDirectory(null, onlyOnDevice, fromSearch);
    }

    public void refreshDirectory() {
        searchFragment = false;

        setFabVisible(true);
        listDirectory(getCurrentFile(), MainApp.isOnlyOnDevice(), false);
    }

    public void listDirectory(OCFile directory, boolean onlyOnDevice, boolean fromSearch) {
        listDirectory(directory, null, onlyOnDevice, fromSearch);
    }

    /**
     * Lists the given directory on the view. When the input parameter is null,
     * it will either refresh the last known directory. list the root
     * if there never was a directory.
     *
     * @param directory File to be listed
     */
    public void listDirectory(OCFile directory, OCFile file, boolean onlyOnDevice, boolean fromSearch) {
        if (!searchFragment) {
            FileDataStorageManager storageManager = mContainerActivity.getStorageManager();
            if (storageManager != null) {
                // Check input parameters for null
                if (directory == null) {
                    if (mFile != null) {
                        directory = mFile;
                    } else {
                        directory = storageManager.getFileByPath(ROOT_PATH);
                        if (directory == null) {
                            return; // no files, wait for sync
                        }
                    }
                }

                // If that's not a directory -> List its parent
                if (!directory.isFolder()) {
                    Log_OC.w(TAG, "You see, that is not a directory -> " + directory.toString());
                    directory = storageManager.getFileById(directory.getParentId());
                }

                if (searchView != null && !searchView.isIconified() && !fromSearch) {
                    searchView.post(() -> {
                        searchView.setQuery("", false);
                        searchView.onActionViewCollapsed();
                        Activity activity;
                        if ((activity = getActivity()) != null && activity instanceof FileDisplayActivity) {
                            FileDisplayActivity fileDisplayActivity = (FileDisplayActivity) activity;
                            fileDisplayActivity.hideSearchView(fileDisplayActivity.getCurrentDir());
                            if (getCurrentFile() != null) {
                                fileDisplayActivity
                                    .setDrawerIndicatorEnabled(fileDisplayActivity.isRoot(getCurrentFile()));
                            }
                        }
                    });
                }

                mAdapter.swapDirectory(
                    accountManager.getUser(),
                    directory,
                    storageManager,
                    onlyOnDevice,
                    mLimitToMimeType
                );

                OCFile previousDirectory = mFile;
                mFile = directory;

                updateLayout();

                if (file != null) {
                    mAdapter.setHighlightedItem(file);
                    int position = mAdapter.getItemPosition(file);
                    if (position != -1) {
                        getRecyclerView().scrollToPosition(position);
                    }
                } else if (previousDirectory == null || !previousDirectory.equals(directory)) {
                    getRecyclerView().scrollToPosition(0);
                }
            }
        }
    }

    private void updateLayout() {
        // decide grid vs list view
        if (isGridViewPreferred(mFile)) {
            switchToGridView();
        } else {
            switchToListView();
        }

        if (mSortButton != null) {
            mSortButton.setText(DisplayUtils.getSortOrderStringId(preferences.getSortOrderByFolder(mFile)));
        }
        if (mSwitchGridViewButton != null) {
            setGridSwitchButton();
        }

        if (mHideFab) {
            setFabVisible(false);
        } else {
            setFabVisible(true);
            // registerFabListener();
        }

        // FAB
        setFabEnabled(mFile != null && mFile.canWrite());

        invalidateActionMode();
    }

    private void invalidateActionMode() {
        if (mActiveActionMode != null) {
            mActiveActionMode.invalidate();
        }
    }

    public void sortFiles(FileSortOrder sortOrder) {
        mSortButton.setText(DisplayUtils.getSortOrderStringId(sortOrder));
        mAdapter.setSortOrder(mFile, sortOrder);
    }

    /**
     * Determines if user set folder to grid or list view. If folder is not set itself,
     * it finds a parent that is set (at least root is set).
     *
     * @param folder Folder to check or null for root folder
     * @return 'true' is folder should be shown in grid mode, 'false' if list mode is preferred.
     */
    public boolean isGridViewPreferred(@Nullable OCFile folder) {
        return FOLDER_LAYOUT_GRID.equals(preferences.getFolderLayout(folder));
    }

    public void setListAsPreferred() {
        preferences.setFolderLayout(mFile, FOLDER_LAYOUT_LIST);
        switchToListView();
    }

    public void switchToListView() {
        if (isGridEnabled()) {
            switchLayoutManager(false);
        }
    }

    public void setGridAsPreferred() {
        preferences.setFolderLayout(mFile, FOLDER_LAYOUT_GRID);
        switchToGridView();
    }

    public void switchToGridView() {
        if (!isGridEnabled()) {
            switchLayoutManager(true);
        }
    }

    public void switchLayoutManager(boolean grid) {
        int position = 0;

        if (getRecyclerView().getLayoutManager() != null) {
            position = ((LinearLayoutManager) getRecyclerView().getLayoutManager())
                    .findFirstCompletelyVisibleItemPosition();
        }

        RecyclerView.LayoutManager layoutManager;
        if (grid) {
            layoutManager = new GridLayoutManager(getContext(), getColumnsCount());
            ((GridLayoutManager) layoutManager).setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (position == getAdapter().getItemCount() - 1 ||
                        position == 0 && getAdapter().shouldShowHeader()) {
                        return ((GridLayoutManager) layoutManager).getSpanCount();
                    } else {
                        return 1;
                    }
                }
            });

        } else {
            layoutManager = new LinearLayoutManager(getContext());
        }

        getRecyclerView().setLayoutManager(layoutManager);
        getRecyclerView().scrollToPosition(position);
        getAdapter().setGridView(grid);
        getRecyclerView().setAdapter(getAdapter());
        getAdapter().notifyDataSetChanged();
    }

    public OCFileListAdapter getAdapter() {
        return mAdapter;
    }

    private void setTitle() {
        // set title

        if (getActivity() instanceof FileDisplayActivity && currentSearchType != null) {
            switch (currentSearchType) {
                case FAVORITE_SEARCH:
                    setTitle(R.string.drawer_item_favorites);
                    break;
                case GALLERY_SEARCH:
                    setTitle(R.string.drawer_item_gallery);
                    break;
                case RECENTLY_ADDED_SEARCH:
                    setTitle(R.string.drawer_item_recently_added);
                    break;
                case RECENTLY_MODIFIED_SEARCH:
                    setTitle(R.string.drawer_item_recently_modified);
                    break;
                case SHARED_FILTER:
                    setTitle(R.string.drawer_item_shared);
                    break;
                default:
                    setTitle(ThemeUtils.getDefaultDisplayNameForRootFolder(getContext()));
                    break;
            }
        }

    }

    protected void prepareActionBarItems(SearchEvent event) {
        if (event != null) {
            switch (event.getSearchType()) {
                case FAVORITE_SEARCH:
                    menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_SORT;
                    break;

                case RECENTLY_MODIFIED_SEARCH:
                    menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_SORT;
                    break;

                default:
                    // do nothing
                    break;
            }
        }

        if (SearchType.FILE_SEARCH != currentSearchType && getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private void setEmptyView(SearchEvent event) {
        if (event != null) {
            switch (event.getSearchType()) {
                case FILE_SEARCH:
                    setEmptyListMessage(SearchType.FILE_SEARCH);
                    break;

                case FAVORITE_SEARCH:
                    setEmptyListMessage(SearchType.FAVORITE_SEARCH);
                    break;

                case RECENTLY_MODIFIED_SEARCH:
                    setEmptyListMessage(SearchType.RECENTLY_MODIFIED_SEARCH);
                    break;

                default:
                    setEmptyListMessage(SearchType.NO_SEARCH);
                    break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ChangeMenuEvent changeMenuEvent) {
        searchFragment = false;
        searchEvent = new SearchEvent();

        menuItemAddRemoveValue = MenuItemAddRemove.ADD_GRID_AND_SORT_WITH_SEARCH;
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
            setTitle(ThemeUtils.getDefaultDisplayNameForRootFolder(getContext()));
        }

        getActivity().getIntent().removeExtra(OCFileListFragment.SEARCH_EVENT);
        getArguments().putParcelable(OCFileListFragment.SEARCH_EVENT, null);

        setFabVisible(true);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(CommentsEvent event) {
        mAdapter.refreshCommentsCount(event.remoteId);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(FavoriteEvent event) {
        try {
            User user = accountManager.getUser();
            OwnCloudClient client = clientFactory.create(user);

            ToggleFavoriteRemoteOperation toggleFavoriteOperation = new ToggleFavoriteRemoteOperation(
                event.shouldFavorite, event.remotePath);
            RemoteOperationResult remoteOperationResult = toggleFavoriteOperation.execute(client);

            if (remoteOperationResult.isSuccess()) {
                mAdapter.setFavoriteAttributeForItemID(event.remoteId, event.shouldFavorite);
            }

        } catch (ClientFactory.CreationException e) {
            Log_OC.e(TAG, "Error processing event", e);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(final SearchEvent event) {
        if (SearchRemoteOperation.SearchType.PHOTO_SEARCH == event.searchType) {
            return;
        }

        prepareCurrentSearch(event);
        searchFragment = true;
        setEmptyListLoadingMessage();
        mAdapter.setData(new ArrayList<>(), SearchType.NO_SEARCH, mContainerActivity.getStorageManager(), mFile, true);

        setFabVisible(false);

        Runnable switchViewsRunnable = () -> {
            if (isGridViewPreferred(mFile) && !isGridEnabled()) {
                switchToGridView();
            } else if (!isGridViewPreferred(mFile) && isGridEnabled()) {
                switchToListView();
            }
        };

        new Handler(Looper.getMainLooper()).post(switchViewsRunnable);

        final User currentUser = accountManager.getUser();

        final RemoteOperation remoteOperation;
        if (currentSearchType != SearchType.SHARED_FILTER) {
            boolean searchOnlyFolders = false;
            if (getArguments() != null && getArguments().getBoolean(ARG_SEARCH_ONLY_FOLDER, false)) {
                searchOnlyFolders = true;
            }

            remoteOperation = new SearchRemoteOperation(event.getSearchQuery(), event.getSearchType(),
                                                        searchOnlyFolders);
        } else {
            remoteOperation = new GetSharesRemoteOperation();
        }

        remoteOperationAsyncTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                setTitle();
                if (getContext() != null && !isCancelled()) {
                    RemoteOperationResult remoteOperationResult = remoteOperation.execute(
                        currentUser.toPlatformAccount(), getContext());

                    FileDataStorageManager storageManager = null;
                    if (mContainerActivity != null && mContainerActivity.getStorageManager() != null) {
                        storageManager = mContainerActivity.getStorageManager();
                    }

                    if (remoteOperationResult.isSuccess() && remoteOperationResult.getResultData() != null
                        && !isCancelled() && searchFragment) {
                        if (remoteOperationResult.getResultData() == null || ((List) remoteOperationResult.getResultData()).isEmpty()) {
                            setEmptyView(event);
                        } else {
                            mAdapter.setData(((RemoteOperationResult<List>) remoteOperationResult).getResultData(),
                                             currentSearchType,
                                             storageManager,
                                             mFile,
                                             true);
                            searchEvent = event;
                        }

                        final ToolbarActivity fileDisplayActivity = (ToolbarActivity) getActivity();
                        if (fileDisplayActivity != null) {
                            fileDisplayActivity.runOnUiThread(() -> {
                                if (fileDisplayActivity != null) {
                                    setLoading(false);
                                }
                            });
                        }
                    }

                    return remoteOperationResult.isSuccess();
                } else {
                    return Boolean.FALSE;
                }
            }

            @Override
            protected void onPostExecute(Boolean bool) {
                if (!isCancelled()) {
                    mAdapter.notifyDataSetChanged();
                }
            }
        };

        remoteOperationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(EncryptionEvent event) {
        final User user = accountManager.getUser();

        try {
            OwnCloudClient client = clientFactory.create(user);
            RemoteOperationResult remoteOperationResult = new ToggleEncryptionRemoteOperation(event.localId,
                                                                                              event.remotePath,
                                                                                              event.shouldBeEncrypted)
                .execute(client);

            if (remoteOperationResult.isSuccess()) {
                mAdapter.setEncryptionAttributeForItemID(event.remoteId, event.shouldBeEncrypted);

                // check if keys are stored
                ArbitraryDataProvider arbitraryDataProvider =
                    new ArbitraryDataProvider(requireContext().getContentResolver());


                String publicKey = arbitraryDataProvider.getValue(user, EncryptionUtils.PUBLIC_KEY);
                String privateKey = arbitraryDataProvider.getValue(user, EncryptionUtils.PRIVATE_KEY);

                if (publicKey.isEmpty() || privateKey.isEmpty()) {
                    Log_OC.d(TAG, "no public key for " + user.getAccountName());


                    FileDataStorageManager storageManager = mContainerActivity.getStorageManager();
                    int position = mAdapter.getItemPosition(storageManager.getFileByRemoteId(event.remoteId));
                    SetupEncryptionDialogFragment dialog = SetupEncryptionDialogFragment.newInstance(user, position);
                    dialog.setTargetFragment(this, SetupEncryptionDialogFragment.SETUP_ENCRYPTION_REQUEST_CODE);
                    dialog.show(getParentFragmentManager(), SetupEncryptionDialogFragment.SETUP_ENCRYPTION_DIALOG_TAG);
                }
            } else if (remoteOperationResult.getHttpCode() == HttpStatus.SC_FORBIDDEN) {
                Snackbar.make(getRecyclerView(),
                              R.string.end_to_end_encryption_folder_not_empty,
                              Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(getRecyclerView(),
                              R.string.common_error_unknown,
                              Snackbar.LENGTH_LONG).show();
            }

        } catch (ClientFactory.CreationException e) {
            Log_OC.e(TAG, "Cannot create client", e);
        }
    }

    protected void setTitle(@StringRes final int title) {
        getActivity().runOnUiThread(() -> {
            if (getActivity() != null && ((FileDisplayActivity) getActivity()).getSupportActionBar() != null) {
                ThemeToolbarUtils.setColoredTitle(((FileDisplayActivity) getActivity()).getSupportActionBar(),
                                           title, getContext());
            }
        });
    }

    protected void setTitle(final String title) {
        getActivity().runOnUiThread(() -> {
            if (getActivity() != null) {
                ActionBar actionBar = ((FileDisplayActivity) getActivity()).getSupportActionBar();

                if (actionBar != null) {
                    ThemeToolbarUtils.setColoredTitle(actionBar, title, getContext());
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }


    @Override
    public void onRefresh() {
        if (isSearchEventSet(searchEvent) && searchFragment) {
            onMessageEvent(searchEvent);

            mRefreshListLayout.setRefreshing(false);
        } else {
            searchFragment = false;
            super.onRefresh();
        }
    }

    public void setSearchFragment(boolean searchFragment) {
        this.searchFragment = searchFragment;
    }

    public boolean isSearchFragment() {
        return searchFragment;
    }

    /**
     * De-/select all elements in the current list view.
     *
     * @param select <code>true</code> to select all, <code>false</code> to deselect all
     */
    public void selectAllFiles(boolean select) {
        OCFileListAdapter ocFileListAdapter = (OCFileListAdapter) getRecyclerView().getAdapter();

        if (select) {
            ocFileListAdapter.addAllFilesToCheckedFiles();
        } else {
            ocFileListAdapter.removeAllFilesFromCheckedFiles();
        }

        for (int i = 0; i < mAdapter.getItemCount(); i++) {
            mAdapter.notifyItemChanged(i);
        }

        mActiveActionMode.invalidate();
    }

    /**
     * Exits the multi file selection mode.
     */
    public void exitSelectionMode() {
        if (mActiveActionMode != null) {
            mActiveActionMode.finish();
        }
    }

    private boolean isSearchEventSet(SearchEvent event) {
        return event != null &&
            event.getSearchType() != null &&
            (!TextUtils.isEmpty(event.getSearchQuery()) ||
                event.searchType == SearchRemoteOperation.SearchType.SHARED_SEARCH ||
                event.searchType == SearchRemoteOperation.SearchType.SHARED_FILTER ||
                event.searchType == SearchRemoteOperation.SearchType.FAVORITE_SEARCH);
    }

    private void syncAndCheckFiles(Collection<OCFile> files) {
        for (OCFile file : files) {
            // Get the remaining space on device
            long availableSpaceOnDevice = FileOperationsHelper.getAvailableSpaceOnDevice();

            // Determine if space is enough to download the file, -1 available space if there in error while computing
            boolean isSpaceEnough = true;
            if (availableSpaceOnDevice >= 0) {
                isSpaceEnough = checkIfEnoughSpace(availableSpaceOnDevice, file);
            }

            if (isSpaceEnough) {
                mContainerActivity.getFileOperationsHelper().syncFile(file);
            } else {
                showSpaceErrorDialog(file, availableSpaceOnDevice);
            }
        }
    }

    @VisibleForTesting
    public boolean checkIfEnoughSpace(long availableSpaceOnDevice, OCFile file) {
        if (file.isFolder()) {
            // on folders we assume that we only need difference
            return availableSpaceOnDevice > (file.getFileLength() - localFolderSize(file));
        } else {
            // on files complete file must first be stored, then target gets overwritten
            return availableSpaceOnDevice > file.getFileLength();
        }
    }

    private long localFolderSize(OCFile file) {
        if (file.getStoragePath() == null) {
            // not yet downloaded anything
            return 0;
        } else {
            return FileStorageUtils.getFolderSize(new File(file.getStoragePath()));
        }
    }

    private void showSpaceErrorDialog(OCFile file, long availableSpaceOnDevice) {
        SyncFileNotEnoughSpaceDialogFragment dialog =
            SyncFileNotEnoughSpaceDialogFragment.newInstance(file, availableSpaceOnDevice);
        dialog.setTargetFragment(this, NOT_ENOUGH_SPACE_FRAG_REQUEST_CODE);

        if (getFragmentManager() != null) {
            dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
        }
    }

    @Override
    public boolean isLoading() {
        return false;
    }

    /**
     * Sets the 'visibility' state of the FAB contained in the fragment.
     * <p>
     * When 'false' is set, FAB visibility is set to View.GONE programmatically.
     *
     * @param visible Desired visibility for the FAB.
     */
    public void setFabVisible(final boolean visible) {
        if (mFabMain == null) {
            // is not available in FolderPickerActivity
            return;
        }

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (visible) {
                    mFabMain.show();
                    ThemeFabUtils.colorFloatingActionButton(mFabMain, requireContext());
                } else {
                    mFabMain.hide();
                }

                showFabWithBehavior(visible);
            });
        }
    }

    /**
     * Remove this, if HideBottomViewOnScrollBehavior is fix by Google
     *
     * @param visible flag if FAB should be shown or hidden
     */
    private void showFabWithBehavior(boolean visible) {
        ViewGroup.LayoutParams layoutParams = mFabMain.getLayoutParams();
        if (layoutParams instanceof CoordinatorLayout.LayoutParams) {
            CoordinatorLayout.Behavior coordinatorLayoutBehavior =
                ((CoordinatorLayout.LayoutParams) layoutParams).getBehavior();
            if (coordinatorLayoutBehavior instanceof HideBottomViewOnScrollBehavior) {
                @SuppressWarnings("unchecked")
                HideBottomViewOnScrollBehavior<FloatingActionButton> behavior =
                    (HideBottomViewOnScrollBehavior<FloatingActionButton>) coordinatorLayoutBehavior;
                if (visible) {
                    behavior.slideUp(mFabMain);
                } else {
                    behavior.slideDown(mFabMain);
                }
            }
        }
    }

    /**
     * Sets the 'visibility' state of the FAB contained in the fragment.
     * <p>
     * When 'false' is set, FAB is greyed out
     *
     * @param enabled Desired visibility for the FAB.
     */
    public void setFabEnabled(final boolean enabled) {
        if (mFabMain == null) {
            // is not available in FolderPickerActivity
            return;
        }

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (enabled) {
                    mFabMain.setEnabled(true);
                    ThemeFabUtils.colorFloatingActionButton(mFabMain, requireContext());
                } else {
                    mFabMain.setEnabled(false);
                    ThemeFabUtils.colorFloatingActionButton(mFabMain, requireContext(), Color.GRAY);
                }
            });
        }
    }

    private void resetHeaderScrollingState() {
        if (requireActivity() instanceof FileDisplayActivity) {
            AppBarLayout appBarLayout = ((FileDisplayActivity) requireActivity()).findViewById(R.id.appbar);

            if (appBarLayout != null) {
                appBarLayout.setExpanded(true);
            }
        }
    }
}
