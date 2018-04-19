/*
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author masensio
 * @author David A. Velasco
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2016 ownCloud Inc.
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.VirtualFolderType;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.SearchOperation;
import com.owncloud.android.lib.resources.files.ToggleEncryptionOperation;
import com.owncloud.android.lib.resources.files.ToggleFavoriteOperation;
import com.owncloud.android.lib.resources.shares.GetRemoteSharesOperation;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener;
import com.owncloud.android.ui.activity.ToolbarActivity;
import com.owncloud.android.ui.activity.UploadFilesActivity;
import com.owncloud.android.ui.adapter.OCFileListAdapter;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.CreateFolderDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.dialog.RenameFileDialogFragment;
import com.owncloud.android.ui.dialog.SetupEncryptionDialogFragment;
import com.owncloud.android.ui.events.ChangeMenuEvent;
import com.owncloud.android.ui.events.DummyDrawerEvent;
import com.owncloud.android.ui.events.EncryptionEvent;
import com.owncloud.android.ui.events.FavoriteEvent;
import com.owncloud.android.ui.events.SearchEvent;
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.ui.preview.PreviewMediaFragment;
import com.owncloud.android.ui.preview.PreviewTextFragment;
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ThemeUtils;

import org.apache.commons.httpclient.HttpStatus;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.parceler.Parcels;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A Fragment that lists all files and folders in a given path.
 * TODO refactor to get rid of direct dependency on FileDisplayActivity
 */
public class OCFileListFragment extends ExtendedListFragment implements OCFileListFragmentInterface {

    private static final String TAG = OCFileListFragment.class.getSimpleName();

    private static final String MY_PACKAGE = OCFileListFragment.class.getPackage() != null ?
            OCFileListFragment.class.getPackage().getName() : "com.owncloud.android.ui.fragment";

    public final static String ARG_ONLY_FOLDERS_CLICKABLE = MY_PACKAGE + ".ONLY_FOLDERS_CLICKABLE";
    public final static String ARG_ALLOW_CONTEXTUAL_ACTIONS = MY_PACKAGE + ".ALLOW_CONTEXTUAL";
    public final static String ARG_HIDE_FAB = MY_PACKAGE + ".HIDE_FAB";
    public final static String ARG_HIDE_ITEM_OPTIONS = MY_PACKAGE + ".HIDE_ITEM_OPTIONS";
    public final static String ARG_SEARCH_ONLY_FOLDER = MY_PACKAGE + ".SEARCH_ONLY_FOLDER";

    public static final String DOWNLOAD_BEHAVIOUR = "DOWNLOAD_BEHAVIOUR";
    public static final String DOWNLOAD_SEND = "DOWNLOAD_SEND";

    public static final String FOLDER_LAYOUT_LIST = "LIST";
    public static final String FOLDER_LAYOUT_GRID = "GRID";

    public static final String SEARCH_EVENT = "SEARCH_EVENT";

    private static final String KEY_FILE = MY_PACKAGE + ".extra.FILE";

    private static final String KEY_CURRENT_SEARCH_TYPE = "CURRENT_SEARCH_TYPE";

    private static final String DIALOG_CREATE_FOLDER = "DIALOG_CREATE_FOLDER";

    private static final String SCREEN_NAME = "Remote/Server file browser";

    private FileFragment.ContainerActivity mContainerActivity;

    private OCFile mFile = null;
    private OCFileListAdapter mAdapter;
    private boolean mOnlyFoldersClickable;

    private int mSystemBarActionModeColor;
    private int mSystemBarColor;
    private int mProgressBarActionModeColor;
    private int mProgressBarColor;

    private boolean mHideFab = true;
    private ActionMode mActiveActionMode;
    private OCFileListFragment.MultiChoiceModeListener mMultiChoiceModeListener;

    private BottomNavigationView bottomNavigationView;

    private SearchType currentSearchType;
    private boolean searchFragment = false;
    private SearchEvent searchEvent;
    private AsyncTask remoteOperationAsyncTask;

    private enum MenuItemAddRemove {
        DO_NOTHING, REMOVE_SORT, REMOVE_GRID_AND_SORT, ADD_SORT, ADD_GRID_AND_SORT, ADD_GRID_AND_SORT_WITH_SEARCH,
        REMOVE_SEARCH
    }

    private MenuItemAddRemove menuItemAddRemoveValue = MenuItemAddRemove.DO_NOTHING;

    private ArrayList<MenuItem> mOriginalMenuItems = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mSystemBarActionModeColor = getResources().getColor(R.color.action_mode_status_bar_background);
        mSystemBarColor = ThemeUtils.primaryDarkColor();
        mProgressBarActionModeColor = getResources().getColor(R.color.action_mode_background);
        mProgressBarColor = ThemeUtils.primaryColor();
        mMultiChoiceModeListener = new MultiChoiceModeListener();

        if (savedInstanceState != null) {
            currentSearchType = Parcels.unwrap(savedInstanceState.getParcelable(KEY_CURRENT_SEARCH_TYPE));
            searchEvent = Parcels.unwrap(savedInstanceState.getParcelable(OCFileListFragment.SEARCH_EVENT));
            mFile = savedInstanceState.getParcelable(KEY_FILE);
        }

        searchFragment = currentSearchType != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log_OC.i(TAG, "onAttach");
        try {
            mContainerActivity = (FileFragment.ContainerActivity) context;

        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " +
                    FileFragment.ContainerActivity.class.getSimpleName());
        }
        try {
            setOnRefreshListener((OnEnforceableRefreshListener) context);

        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " +
                    SwipeRefreshLayout.OnRefreshListener.class.getSimpleName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.i(TAG, "onCreateView() start");
        View v = super.onCreateView(inflater, container, savedInstanceState);
        bottomNavigationView = v.findViewById(R.id.bottom_navigation_view);

        if (savedInstanceState != null
                && Parcels.unwrap(savedInstanceState.getParcelable(KEY_CURRENT_SEARCH_TYPE)) != null) {
            currentSearchType = Parcels.unwrap(savedInstanceState.getParcelable(KEY_CURRENT_SEARCH_TYPE));
            searchFragment = true;
        } else {
            currentSearchType = SearchType.NO_SEARCH;
        }

        if (getResources().getBoolean(R.bool.bottom_toolbar_enabled)) {
            bottomNavigationView.setVisibility(View.VISIBLE);
            DisplayUtils.setupBottomBar(bottomNavigationView, getResources(), getActivity(), R.id.nav_bar_files);
        }

        if (!getResources().getBoolean(R.bool.bottom_toolbar_enabled) || savedInstanceState != null) {

            final View fabView = v.findViewById(R.id.fab_main);
            final RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    fabView.getLayoutParams();
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1);
            Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    fabView.setLayoutParams(layoutParams);
                    fabView.invalidate();
                }
            });
        }

        Bundle args = getArguments();
        boolean allowContextualActions = (args != null) && args.getBoolean(ARG_ALLOW_CONTEXTUAL_ACTIONS, false);
        if (allowContextualActions) {
            setChoiceModeAsMultipleModal(savedInstanceState);
        }

        Log_OC.i(TAG, "onCreateView() end");
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() != null) {
            AnalyticsUtils.setCurrentScreenName(getActivity(), SCREEN_NAME, TAG);
        }
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
        mOnlyFoldersClickable = (args != null) && args.getBoolean(ARG_ONLY_FOLDERS_CLICKABLE, false);
        boolean hideItemOptions = (args != null) && args.getBoolean(ARG_HIDE_ITEM_OPTIONS, false);

        mAdapter = new OCFileListAdapter(getActivity(), mContainerActivity, this, hideItemOptions,
                isGridViewPreferred(mFile));
        setRecyclerViewAdapter(mAdapter);

        mHideFab = (args != null) && args.getBoolean(ARG_HIDE_FAB, false);

        if (mHideFab) {
            setFabEnabled(false);
        } else {
            setFabEnabled(true);
            registerFabListener();
        }

        searchEvent = Parcels.unwrap(getArguments().getParcelable(OCFileListFragment.SEARCH_EVENT));
        prepareCurrentSearch(searchEvent);
        if (searchEvent != null && savedInstanceState == null) {
            onMessageEvent(searchEvent);
        }

        if (isGridViewPreferred(getCurrentFile())) {
            switchToGridView();
        }

        setTitle();
    }

    private void prepareCurrentSearch(SearchEvent event) {
        if (event != null) {
            if (event.getSearchType().equals(SearchOperation.SearchType.FILE_SEARCH)) {
                currentSearchType = SearchType.FILE_SEARCH;

            } else if (event.getSearchType().equals(SearchOperation.SearchType.CONTENT_TYPE_SEARCH)) {
                if (event.getSearchQuery().equals("image/%")) {
                    currentSearchType = SearchType.PHOTO_SEARCH;
                } else if (event.getSearchQuery().equals("video/%")) {
                    currentSearchType = SearchType.VIDEO_SEARCH;
                }
            } else if (event.getSearchType().equals(SearchOperation.SearchType.FAVORITE_SEARCH)) {
                currentSearchType = SearchType.FAVORITE_SEARCH;
            } else if (event.getSearchType().equals(SearchOperation.SearchType.RECENTLY_ADDED_SEARCH)) {
                currentSearchType = SearchType.RECENTLY_ADDED_SEARCH;
            } else if (event.getSearchType().equals(SearchOperation.SearchType.RECENTLY_MODIFIED_SEARCH)) {
                currentSearchType = SearchType.RECENTLY_MODIFIED_SEARCH;
            } else if (event.getSearchType().equals(SearchOperation.SearchType.SHARED_SEARCH)) {
                currentSearchType = SearchType.SHARED_FILTER;
            }

            prepareActionBarItems(event);
        }
    }

    /**
     * register listener on FAB.
     */
    private void registerFabListener() {
        getFabMain().setOnClickListener(v -> {
            final View fileListActionsBottomSheet = getLayoutInflater()
                    .inflate(R.layout.file_list_actions_bottom_sheet_fragment, null);
            final BottomSheetDialog dialog = new BottomSheetDialog(getContext());
            dialog.setContentView(fileListActionsBottomSheet);

            ThemeUtils.tintDrawable(((ImageView) fileListActionsBottomSheet.findViewById(R.id.menu_icon_upload_files))
                    .getDrawable(),ThemeUtils.primaryColor());
            ThemeUtils.tintDrawable(((ImageView) fileListActionsBottomSheet.findViewById(R.id.menu_icon_upload_from_app))
                    .getDrawable(),ThemeUtils.primaryColor());
            ThemeUtils.tintDrawable(((ImageView) fileListActionsBottomSheet.findViewById(R.id.menu_icon_mkdir))
                    .getDrawable(),ThemeUtils.primaryColor());

            fileListActionsBottomSheet.findViewById(R.id.menu_upload_files)
                    .setOnClickListener(view -> uploadFiles(dialog));
            fileListActionsBottomSheet.findViewById(R.id.menu_upload_from_app)
                    .setOnClickListener(view -> uploadFromApp(dialog));
            fileListActionsBottomSheet.findViewById(R.id.menu_mkdir)
                    .setOnClickListener(view -> createFolder(dialog));

            dialog.setOnShowListener(d ->
                    BottomSheetBehavior.from((View) fileListActionsBottomSheet.getParent())
                            .setPeekHeight(fileListActionsBottomSheet.getMeasuredHeight())
            );
            dialog.show();
        });
    }

    private void createFolder(BottomSheetDialog dialog) {
        CreateFolderDialogFragment.newInstance(mFile)
                .show(getActivity().getSupportFragmentManager(), DIALOG_CREATE_FOLDER);
        dialog.dismiss();
    }

    private void uploadFromApp(BottomSheetDialog dialog) {
        Intent action = new Intent(Intent.ACTION_GET_CONTENT);
        action = action.setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
        //Intent.EXTRA_ALLOW_MULTIPLE is only supported on api level 18+, Jelly Bean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            action.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        getActivity().startActivityForResult(
                Intent.createChooser(action, getString(R.string.upload_chooser_title)),
                FileDisplayActivity.REQUEST_CODE__SELECT_CONTENT_FROM_APPS
        );
        dialog.dismiss();
    }

    private void uploadFiles(BottomSheetDialog dialog) {
        UploadFilesActivity.startUploadActivityForResult(
                getActivity(),
                ((FileActivity) getActivity()).getAccount(),
                FileDisplayActivity.REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM
        );
        dialog.dismiss();
    }

    @Override
    public void onShareIconClick(OCFile file) {
        mContainerActivity.getFileOperationsHelper().sendShareFile(file);
    }

    @Override
    public void onOverflowIconClicked(OCFile file, View view) {
        PopupMenu popup = new PopupMenu(getActivity(), view);
        popup.inflate(R.menu.file_actions_menu);
        FileMenuFilter mf = new FileMenuFilter(mAdapter.getFiles().size(), Collections.singleton(file),
                ((FileActivity) getActivity()).getAccount(), mContainerActivity, getActivity(), true);
        mf.filter(popup.getMenu(), true);
        popup.setOnMenuItemClickListener(item -> {
            Set<OCFile> checkedFiles = new HashSet<>();
            checkedFiles.add(file);
            return onFileActionChosen(item.getItemId(), checkedFiles);
        });
        popup.show();
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
        private boolean mActionModeClosedByDrawer = false;

        /**
         * Selected items in list when action mode is closed by drawer
         */
        private HashSet<OCFile> mSelectionWhenActionModeClosedByDrawer = new HashSet<>();

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            // nothing to do
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            // nothing to do
        }

        /**
         * When the navigation drawer is closed, action mode is recovered in the same state as was
         * when the drawer was (started to be) opened.
         *
         * @param drawerView Navigation drawer just closed.
         */
        @Override
        public void onDrawerClosed(View drawerView) {
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

            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.file_actions_menu, menu);
            mode.invalidate();

            //set gray color
            ThemeUtils.colorStatusBar(getActivity(), mSystemBarActionModeColor);
            ThemeUtils.colorToolbarProgressBar(getActivity(), mProgressBarActionModeColor);

            // hide FAB in multi selection mode
            setFabEnabled(false);

            mAdapter.setMultiSelect(true);
            return true;
        }

        /**
         * Updates available action in menu depending on current selection.
         */
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Set<OCFile> checkedFiles = mAdapter.getCheckedItems();
            final int checkedCount = mAdapter.getCheckedItems().size();
            String title = getResources().getQuantityString(R.plurals.items_selected_count, checkedCount, checkedCount);
            mode.setTitle(title);
            FileMenuFilter mf = new FileMenuFilter(
                    mAdapter.getFiles().size(),
                    checkedFiles,
                    ((FileActivity) getActivity()).getAccount(),
                    mContainerActivity,
                    getActivity(),
                    false
            );
            mf.filter(menu, false);
            return true;
        }

        /**
         * Starts the corresponding action when a menu item is tapped by the user.
         */
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Set<OCFile> checkedFiles = mAdapter.getCheckedItems();
            return onFileActionChosen(item.getItemId(), checkedFiles);
        }

        /**
         * Restores UI.
         */
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActiveActionMode = null;

            // reset to previous color
            ThemeUtils.colorStatusBar(getActivity(), mSystemBarColor);
            ThemeUtils.colorToolbarProgressBar(getActivity(), mProgressBarColor);

            // show FAB on multi selection mode exit
            if (!mHideFab && !searchFragment) {
                setFabEnabled(true);
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
    private void setChoiceModeAsMultipleModal(Bundle savedInstanceState) {
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

        if (searchEvent != null) {
            onMessageEvent(searchEvent);
        }
    }

    /**
     * Saves the current listed folder.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(KEY_FILE, mFile);
        if (searchFragment) {
            outState.putParcelable(KEY_CURRENT_SEARCH_TYPE, Parcels.wrap(currentSearchType));
            outState.putParcelable(OCFileListFragment.SEARCH_EVENT, Parcels.wrap(searchEvent));
        }
        mMultiChoiceModeListener.storeStateIn(outState);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Menu mMenu = menu;

        if (mOriginalMenuItems.size() == 0) {
            mOriginalMenuItems.add(mMenu.findItem(R.id.action_switch_view));
            mOriginalMenuItems.add(mMenu.findItem(R.id.action_sort));
            mOriginalMenuItems.add(mMenu.findItem(R.id.action_search));
        }

        changeGridIcon(menu);   // this is enough if the option stays out of the action bar

        MenuItem menuItemOrig;

        if (menuItemAddRemoveValue.equals(MenuItemAddRemove.ADD_SORT)) {
            if (menu.findItem(R.id.action_sort) == null) {
                menuItemOrig = mOriginalMenuItems.get(1);
                menu.add(menuItemOrig.getGroupId(), menuItemOrig.getItemId(), menuItemOrig.getOrder(),
                        menuItemOrig.getTitle());
            }

        } else if (menuItemAddRemoveValue.equals(MenuItemAddRemove.ADD_GRID_AND_SORT)) {
            if (menu.findItem(R.id.action_switch_view) == null) {
                menuItemOrig = mOriginalMenuItems.get(0);
                menu.add(menuItemOrig.getGroupId(), menuItemOrig.getItemId(), menuItemOrig.getOrder(),
                        menuItemOrig.getTitle());
            }

            if (menu.findItem(R.id.action_sort) == null) {
                menuItemOrig = mOriginalMenuItems.get(1);
                menu.add(menuItemOrig.getGroupId(), menuItemOrig.getItemId(), menuItemOrig.getOrder(),
                        menuItemOrig.getTitle());
            }
        } else if (menuItemAddRemoveValue.equals(MenuItemAddRemove.REMOVE_SEARCH)) {
            menu.removeItem(R.id.action_search);
        } else if (menuItemAddRemoveValue.equals(MenuItemAddRemove.ADD_GRID_AND_SORT_WITH_SEARCH)) {
            if (menu.findItem(R.id.action_switch_view) == null) {
                menuItemOrig = mOriginalMenuItems.get(0);
                menu.add(menuItemOrig.getGroupId(), menuItemOrig.getItemId(), menuItemOrig.getOrder(),
                        menuItemOrig.getTitle());
            }

            if (menu.findItem(R.id.action_sort) == null) {
                menuItemOrig = mOriginalMenuItems.get(1);
                menu.add(menuItemOrig.getGroupId(), menuItemOrig.getItemId(), menuItemOrig.getOrder(),
                        menuItemOrig.getTitle());
            }

            if (menu.findItem(R.id.action_search) == null) {
                menuItemOrig = mOriginalMenuItems.get(2);
                menu.add(menuItemOrig.getGroupId(), menuItemOrig.getItemId(), menuItemOrig.getOrder(),
                        menuItemOrig.getTitle());
            }
        } else if (menuItemAddRemoveValue.equals(MenuItemAddRemove.REMOVE_SORT)) {
            menu.removeItem(R.id.action_sort);
            menu.removeItem(R.id.action_search);
        } else if (menuItemAddRemoveValue.equals(MenuItemAddRemove.REMOVE_GRID_AND_SORT)) {
            menu.removeItem(R.id.action_sort);
            menu.removeItem(R.id.action_switch_view);
            menu.removeItem(R.id.action_search);
        }

    }

    /**
     * Call this, when the user presses the up button.
     * <p>
     * Tries to move up the current folder one level. If the parent folder was removed from the
     * database, it continues browsing up until finding an existing folders.
     * <p/>
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
                parentDir = storageManager.getFileByPath(OCFile.ROOT_PATH);
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


    @Override
    public boolean onLongItemClicked(OCFile file) {
        FragmentActivity actionBarActivity = getActivity();
        getAdapter().addCheckedFile(file);
        actionBarActivity.startActionMode(mMultiChoiceModeListener);

        return true;
    }

    @Override
    public void onItemClicked(OCFile file) {
        if (getAdapter().isMultiSelect()) {
            if (getAdapter().isCheckedFile(file)) {
                getAdapter().removeCheckedFile(file);
            } else {
                getAdapter().addCheckedFile(file);
            }
            mActiveActionMode.invalidate();
            mAdapter.notifyItemChanged(getAdapter().getItemPosition(file));
        } else {
            if (file != null) {
                int position = mAdapter.getItemPosition(file);
                
                if (file.isFolder()) {
                    if (file.isEncrypted()) {
                        // check if API >= 19
                        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
                            Snackbar.make(getRecyclerView(), R.string.end_to_end_encryption_not_supported,
                                    Snackbar.LENGTH_LONG).show();
                            return;
                        }

                    Account account = ((FileActivity) mContainerActivity).getAccount();

                    // check if e2e app is enabled
                    OCCapability ocCapability = mContainerActivity.getStorageManager().getCapability(account.name);

                    if (ocCapability.getEndToEndEncryption().isFalse() ||
                            ocCapability.getEndToEndEncryption().isUnknown()) {
                        Snackbar.make(getRecyclerView(), R.string.end_to_end_encryption_not_enabled,
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }// check if keys are stored
                    ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(
                            getContext().getContentResolver());

                    
                    String publicKey = arbitraryDataProvider.getValue(account, EncryptionUtils.PUBLIC_KEY);
                    String privateKey = arbitraryDataProvider.getValue(account, EncryptionUtils.PRIVATE_KEY);

                        if (publicKey.isEmpty() || privateKey.isEmpty()) {
                            Log_OC.d(TAG, "no public key for " + account.name);

                            SetupEncryptionDialogFragment dialog = SetupEncryptionDialogFragment.newInstance(account,
                                    position);
                            dialog.setTargetFragment(this, SetupEncryptionDialogFragment.SETUP_ENCRYPTION_REQUEST_CODE);
                            dialog.show(getFragmentManager(), SetupEncryptionDialogFragment.SETUP_ENCRYPTION_DIALOG_TAG);
                        } else {
                            // update state and view of this fragment
                            searchFragment = false;

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
                    listDirectory(file, MainApp.isOnlyOnDevice(), false);
                    // then, notify parent activity to let it update its state and view
                    mContainerActivity.onBrowsedDownTo(file);
                    // save index and top position
                    saveIndexAndTopPosition(position);
                }

            } else if (!mOnlyFoldersClickable){ // Click on a file
                if (PreviewImageFragment.canBePreviewed(file)) {
                    // preview image - it handles the download, if needed
                    if (searchFragment) {
                        VirtualFolderType type;
                        switch (currentSearchType) {
                            case FAVORITE_SEARCH:
                                type = VirtualFolderType.FAVORITE;
                                break;
                            case PHOTO_SEARCH:
                                type = VirtualFolderType.PHOTOS;
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
                } else if (PreviewTextFragment.canBePreviewed(file)) {
                    ((FileDisplayActivity) mContainerActivity).startTextPreview(file, false);
                } else if (file.isDown()) {
                    if (PreviewMediaFragment.canBePreviewed(file)) {
                        // media preview
                        ((FileDisplayActivity) mContainerActivity).startMediaPreview(file, 0,
                                true, false);
                    } else {
                        mContainerActivity.getFileOperationsHelper().openFile(file);
                    }

                } else {
                    // automatic download, preview on finish
                    ((FileDisplayActivity) mContainerActivity).startDownloadForPreview(file);
                

            }
}
        } else {
            Log_OC.d(TAG, "Null object in ListAdapter!!");}
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
     * @param menuId       Identifier of the action menu selected by the user
     * @param checkedFiles List of files selected by the user on which the action should be performed
     * @return 'true' if the menu selection started any action, 'false' otherwise.
     */
    public boolean onFileActionChosen(int menuId, Set<OCFile> checkedFiles) {
        if (checkedFiles.size() <= 0) {
            return false;
        }

        if (checkedFiles.size() == 1) {
            /// action only possible on a single file
            OCFile singleFile = checkedFiles.iterator().next();
            switch (menuId) {
                case R.id.action_send_share_file: {
                    mContainerActivity.getFileOperationsHelper().sendShareFile(singleFile);
                    return true;
                }
                case R.id.action_open_file_with: {
                    mContainerActivity.getFileOperationsHelper().openFile(singleFile);
                    return true;
                }
                case R.id.action_rename_file: {
                    RenameFileDialogFragment dialog = RenameFileDialogFragment.newInstance(singleFile);
                    dialog.show(getFragmentManager(), FileDetailFragment.FTAG_RENAME_FILE);
                    return true;
                }
                case R.id.action_see_details: {
                    if (mActiveActionMode != null) {
                        mActiveActionMode.finish();
                    }
                    mContainerActivity.showDetails(singleFile);
                    return true;
                }
                case R.id.action_set_as_wallpaper: {
                    mContainerActivity.getFileOperationsHelper().setPictureAs(singleFile, getView());
                    return true;
                }
                case R.id.action_encrypted: {
                    mContainerActivity.getFileOperationsHelper().toggleEncryption(singleFile, true);
                    return true;
                }
                case R.id.action_unset_encrypted: {
                    mContainerActivity.getFileOperationsHelper().toggleEncryption(singleFile, false);
                    return true;
                }
            }
        }

        /// actions possible on a batch of files
        switch (menuId) {
            case R.id.action_remove_file: {
                RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(new ArrayList<>(checkedFiles));
                dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
                return true;
            }
            case R.id.action_download_file:
            case R.id.action_sync_file: {
                mContainerActivity.getFileOperationsHelper().syncFiles(checkedFiles);
                return true;
            }
            case R.id.action_cancel_sync: {
                ((FileDisplayActivity) mContainerActivity).cancelTransference(checkedFiles);
                return true;
            }
            case R.id.action_keep_files_offline: {
                mContainerActivity.getFileOperationsHelper().toggleOfflineFiles(checkedFiles, true);
                return true;
            }
            case R.id.action_unset_keep_files_offline: {
                mContainerActivity.getFileOperationsHelper().toggleOfflineFiles(checkedFiles, false);
                return true;
            }
            case R.id.action_favorite: {
                mContainerActivity.getFileOperationsHelper().toggleFavoriteFiles(checkedFiles, true);
                return true;
            }
            case R.id.action_unset_favorite: {
                mContainerActivity.getFileOperationsHelper().toggleFavoriteFiles(checkedFiles, false);
                return true;
            }
            case R.id.action_move: {
                Intent action = new Intent(getActivity(), FolderPickerActivity.class);
                action.putParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES, new ArrayList<>(checkedFiles));
                action.putExtra(FolderPickerActivity.EXTRA_CURRENT_FOLDER, mFile);
                action.putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.MOVE);
                getActivity().startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__MOVE_FILES);
                return true;
            }
            case R.id.action_copy: {
                Intent action = new Intent(getActivity(), FolderPickerActivity.class);
                action.putParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES, new ArrayList<>(checkedFiles));
                action.putExtra(FolderPickerActivity.EXTRA_CURRENT_FOLDER, mFile);
                action.putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.COPY);
                getActivity().startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__COPY_FILES);
                return true;
            }
            case R.id.action_select_all_action_menu: {
                selectAllFiles(true);
                return true;
            }
            case R.id.action_deselect_all_action_menu: {
                selectAllFiles(false);
                return true;
            }
            default:
                return false;
        }
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

        setFabEnabled(true);
        listDirectory(getCurrentFile(), MainApp.isOnlyOnDevice(), false);
    }

    /**
     * Lists the given directory on the view. When the input parameter is null,
     * it will either refresh the last known directory. list the root
     * if there never was a directory.
     *
     * @param directory File to be listed
     */
    public void listDirectory(OCFile directory, boolean onlyOnDevice, boolean fromSearch) {
        if (!searchFragment) {
            FileDataStorageManager storageManager = mContainerActivity.getStorageManager();
            if (storageManager != null) {

                // Check input parameters for null
                if (directory == null) {
                    if (mFile != null) {
                        directory = mFile;
                    } else {
                        directory = storageManager.getFileByPath("/");
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

                    searchView.post(new Runnable() {
                        @Override
                        public void run() {
                            searchView.setQuery("", false);
                            searchView.onActionViewCollapsed();
                            Activity activity;
                            if ((activity = getActivity()) != null && activity instanceof FileDisplayActivity) {
                                FileDisplayActivity fileDisplayActivity = (FileDisplayActivity) activity;
                                if (getCurrentFile() != null) {
                                    fileDisplayActivity.setDrawerIndicatorEnabled(
                                            fileDisplayActivity.isRoot(getCurrentFile()));
                                }
                            }

                        }
                    });
                }

                mAdapter.swapDirectory(directory, storageManager, onlyOnDevice);
                if (mFile == null || !mFile.equals(directory)) {
                    getRecyclerView().scrollToPosition(0);
                }
                mFile = directory;

                updateLayout();
            }
        }
    }

    private void updateLayout() {
        // decide grid vs list view
        if (AccountUtils.getServerVersion(((FileActivity) mContainerActivity).getAccount())
                .supportsRemoteThumbnails() && isGridViewPreferred(mFile)) {
            switchToGridView();
        } else {
            switchToListView();

        }

        invalidateActionMode();
    }

    private void invalidateActionMode() {
        if (mActiveActionMode != null) {
            mActiveActionMode.invalidate();
        }
    }


    public void sortFiles(FileSortOrder sortOrder) {
        mAdapter.setSortOrder(mFile, sortOrder);
    }

    /**
     * Determines if user set folder to grid or list view. If folder is not set itself,
     * it finds a parent that is set (at least root is set).
     *
     * @param folder Folder to check.
     * @return 'true' is folder should be shown in grid mode, 'false' if list mode is preferred.
     */
    public boolean isGridViewPreferred(OCFile folder) {
        return PreferenceManager.getFolderLayout(getActivity(), folder).equals(FOLDER_LAYOUT_GRID);
    }

    public void setListAsPreferred() {
        PreferenceManager.setFolderLayout(getActivity(), mFile, FOLDER_LAYOUT_LIST);
        switchToListView();
    }

    public void switchToListView() {
        if (isGridEnabled()) {
            switchLayoutManager(false);
        }
    }

    public void setGridAsPreferred() {
        PreferenceManager.setFolderLayout(getActivity(), mFile, FOLDER_LAYOUT_GRID);
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
            layoutManager = new GridLayoutManager(getContext(), getColumnSize());
            ((GridLayoutManager) layoutManager).setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (position == getAdapter().getItemCount() - 1) {
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

    private void changeGridIcon(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.action_switch_view);
        if (menuItem != null) {
            if (isGridViewPreferred(mFile)) {
                menuItem.setTitle(getString(R.string.action_switch_list_view));
                menuItem.setIcon(R.drawable.ic_view_list);
            } else {
                menuItem.setTitle(getString(R.string.action_switch_grid_view));
                menuItem.setIcon(R.drawable.ic_view_module);
            }
        }
    }

    private void unsetAllMenuItems(final boolean unsetDrawer) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (unsetDrawer) {
                    EventBus.getDefault().post(new DummyDrawerEvent());
                } else {
                    if (bottomNavigationView != null) {
                        bottomNavigationView.getMenu().findItem(R.id.nav_bar_files).setChecked(true);
                    }
                }
            }
        });

    }

    public void setTitleFromSearchEvent(SearchEvent event) {
        prepareCurrentSearch(event);
        setTitle();
    }

    private void setTitle() {
        // set title

        if (getActivity() instanceof FileDisplayActivity && currentSearchType != null) {
            switch (currentSearchType) {
                case FAVORITE_SEARCH:
                    setTitle(R.string.drawer_item_favorites);
                    break;
                case PHOTO_SEARCH:
                    setTitle(R.string.drawer_item_photos);
                    break;
                case VIDEO_SEARCH:
                    setTitle(R.string.drawer_item_videos);
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
                    setTitle(ThemeUtils.getDefaultDisplayNameForRootFolder());
                    break;
            }
        }

    }

    private void prepareActionBarItems(SearchEvent event) {
        if (event != null) {
            if (event.getSearchType().equals(SearchOperation.SearchType.CONTENT_TYPE_SEARCH)) {
                if (event.getSearchQuery().equals("image/%")) {
                    menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_GRID_AND_SORT;
                } else if (event.getSearchQuery().equals("video/%")) {
                    menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_SEARCH;
                }
            } else if (event.getSearchType().equals(SearchOperation.SearchType.FAVORITE_SEARCH)) {
                menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_SORT;
            } else if (event.getSearchType().equals(SearchOperation.SearchType.RECENTLY_ADDED_SEARCH)) {
                menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_SORT;
            } else if (event.getSearchType().equals(SearchOperation.SearchType.RECENTLY_MODIFIED_SEARCH)) {
                menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_SORT;
            } else if (event.getSearchType().equals(SearchOperation.SearchType.SHARED_SEARCH)) {
                menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_SEARCH;
            }

        }

        if (currentSearchType != null && !currentSearchType.equals(SearchType.FILE_SEARCH) && getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private void setEmptyView(SearchEvent event) {

        if (event != null) {
            if (event.getSearchType().equals(SearchOperation.SearchType.FILE_SEARCH)) {
                setEmptyListMessage(SearchType.FILE_SEARCH);
            } else if (event.getSearchType().equals(SearchOperation.SearchType.CONTENT_TYPE_SEARCH)) {
                if (event.getSearchQuery().equals("image/%")) {
                    setEmptyListMessage(SearchType.PHOTO_SEARCH);
                } else if (event.getSearchQuery().equals("video/%")) {
                    setEmptyListMessage(SearchType.VIDEO_SEARCH);
                }
            } else if (event.getSearchType().equals(SearchOperation.SearchType.FAVORITE_SEARCH)) {
                setEmptyListMessage(SearchType.FAVORITE_SEARCH);
            } else if (event.getSearchType().equals(SearchOperation.SearchType.RECENTLY_ADDED_SEARCH)) {
                setEmptyListMessage(SearchType.RECENTLY_ADDED_SEARCH);
            } else if (event.getSearchType().equals(SearchOperation.SearchType.RECENTLY_MODIFIED_SEARCH)) {
                setEmptyListMessage(SearchType.RECENTLY_MODIFIED_SEARCH);
            } else if (event.getSearchType().equals(SearchOperation.SearchType.SHARED_SEARCH)) {
                setEmptyListMessage(SearchType.SHARED_FILTER);
            }

        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ChangeMenuEvent changeMenuEvent) {
        menuItemAddRemoveValue = MenuItemAddRemove.ADD_GRID_AND_SORT_WITH_SEARCH;
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
            setTitle(ThemeUtils.getDefaultDisplayNameForRootFolder());
        }

        getActivity().getIntent().removeExtra(OCFileListFragment.SEARCH_EVENT);
        getArguments().putParcelable(OCFileListFragment.SEARCH_EVENT, null);

        setFabEnabled(true);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(FavoriteEvent event) {
        Account currentAccount = AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext());

        OwnCloudAccount ocAccount = null;
        AccountManager mAccountMgr = AccountManager.get(getActivity());

        try {
            ocAccount = new OwnCloudAccount(
                    currentAccount,
                    MainApp.getAppContext()
            );

            OwnCloudClient mClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                    getClientFor(ocAccount, MainApp.getAppContext());

            String userId = mAccountMgr.getUserData(currentAccount,
                    com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID);

            if (TextUtils.isEmpty(userId)) {
                userId = mClient.getCredentials().getUsername();
            }

            ToggleFavoriteOperation toggleFavoriteOperation = new ToggleFavoriteOperation(event.shouldFavorite,
                    event.remotePath, userId);
            RemoteOperationResult remoteOperationResult = toggleFavoriteOperation.execute(mClient);

            if (remoteOperationResult.isSuccess()) {
                mAdapter.setFavoriteAttributeForItemID(event.remoteId, event.shouldFavorite);
            }

        } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
            Log_OC.e(TAG, "Account not found", e);
        } catch (AuthenticatorException e) {
            Log_OC.e(TAG, "Authentication failed", e);
        } catch (IOException e) {
            Log_OC.e(TAG, "IO error", e);
        } catch (OperationCanceledException e) {
            Log_OC.e(TAG, "Operation has been canceled", e);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(final SearchEvent event) {
        searchFragment = true;
        setEmptyListLoadingMessage();
        mAdapter.setData(new ArrayList<>(), SearchType.NO_SEARCH, mContainerActivity.getStorageManager(), mFile);

        setFabEnabled(false);

        if (event.getUnsetType().equals(SearchEvent.UnsetType.UNSET_BOTTOM_NAV_BAR)) {
            unsetAllMenuItems(false);
        } else if (event.getUnsetType().equals(SearchEvent.UnsetType.UNSET_DRAWER)) {
            unsetAllMenuItems(true);
        }


        if (bottomNavigationView != null && searchEvent != null) {
            switch (currentSearchType) {
                case FAVORITE_SEARCH:
                    DisplayUtils.setBottomBarItem(bottomNavigationView, R.id.nav_bar_favorites);
                    break;
                case PHOTO_SEARCH:
                    DisplayUtils.setBottomBarItem(bottomNavigationView, R.id.nav_bar_photos);
                    break;

                default:
                    DisplayUtils.setBottomBarItem(bottomNavigationView, -1);
                    break;
            }
        }

        Runnable switchViewsRunnable = new Runnable() {
            @Override
            public void run() {
                if (isGridViewPreferred(mFile) && !isGridEnabled()) {
                    switchToGridView();
                } else if (!isGridViewPreferred(mFile) && isGridEnabled()) {
                    switchToListView();
                }
            }
        };

        if (currentSearchType.equals(SearchType.PHOTO_SEARCH)) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    switchToGridView();
                }
            });
        } else if (currentSearchType.equals(SearchType.NO_SEARCH) || currentSearchType.equals(
                SearchType.REGULAR_FILTER)) {
            new Handler(Looper.getMainLooper()).post(switchViewsRunnable);
        } else {
            new Handler(Looper.getMainLooper()).post(switchViewsRunnable);
        }

        final RemoteOperation remoteOperation;
        if (!currentSearchType.equals(SearchType.SHARED_FILTER)) {
            boolean searchOnlyFolders = false;
            if (getArguments() != null && getArguments().getBoolean(ARG_SEARCH_ONLY_FOLDER, false)) {
                searchOnlyFolders = true;
            }

            remoteOperation = new SearchOperation(event.getSearchQuery(), event.getSearchType(), searchOnlyFolders);
        } else {
            remoteOperation = new GetRemoteSharesOperation();
        }

        final Account currentAccount = AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext());

        remoteOperationAsyncTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                if (getContext() != null && !isCancelled()) {
                    RemoteOperationResult remoteOperationResult = remoteOperation.execute(currentAccount, getContext());

                    FileDataStorageManager storageManager = null;
                    if (mContainerActivity != null && mContainerActivity.getStorageManager() != null) {
                        storageManager = mContainerActivity.getStorageManager();
                    }

                    if (remoteOperationResult.isSuccess() && remoteOperationResult.getData() != null
                            && !isCancelled() && searchFragment) {
                        if (remoteOperationResult.getData() == null || remoteOperationResult.getData().size() == 0) {
                            setEmptyView(event);
                        } else {
                            mAdapter.setData(remoteOperationResult.getData(), currentSearchType, storageManager, mFile);
                        }

                        final ToolbarActivity fileDisplayActivity = (ToolbarActivity) getActivity();
                        if (fileDisplayActivity != null) {
                            fileDisplayActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (fileDisplayActivity != null) {
                                        fileDisplayActivity.setIndeterminate(false);
                                    }
                                }
                            });
                        }
                    }

                    return remoteOperationResult.isSuccess();
                } else {
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Object o) {
                if (!isCancelled()) {
                    mAdapter.notifyDataSetChanged();
                }
            }
        };

        remoteOperationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, true);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(EncryptionEvent event) {
        Account currentAccount = AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext());

        OwnCloudAccount ocAccount = null;
        try {
            ocAccount = new OwnCloudAccount(currentAccount, MainApp.getAppContext());

            OwnCloudClient mClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                    getClientFor(ocAccount, MainApp.getAppContext());

            ToggleEncryptionOperation toggleEncryptionOperation = new ToggleEncryptionOperation(event.localId,
                    event.remotePath, event.shouldBeEncrypted);
            RemoteOperationResult remoteOperationResult = toggleEncryptionOperation.execute(mClient, true);

            if (remoteOperationResult.isSuccess()) {
                mAdapter.setEncryptionAttributeForItemID(event.remoteId, event.shouldBeEncrypted);
            } else if (remoteOperationResult.getHttpCode() == HttpStatus.SC_FORBIDDEN) {
                Snackbar.make(getRecyclerView(), R.string.end_to_end_encryption_folder_not_empty, Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(getRecyclerView(), R.string.common_error_unknown, Snackbar.LENGTH_LONG).show();
            }

        } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
            Log_OC.e(TAG, "Account not found", e);
        } catch (AuthenticatorException e) {
            Log_OC.e(TAG, "Authentication failed", e);
        } catch (IOException e) {
            Log_OC.e(TAG, "IO error", e);
        } catch (OperationCanceledException e) {
            Log_OC.e(TAG, "Operation has been canceled", e);
        }
    }

    private void setTitle(@StringRes final int title) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null && ((FileDisplayActivity) getActivity()).getSupportActionBar() != null) {
                    ThemeUtils.setColoredTitle(((FileDisplayActivity) getActivity()).getSupportActionBar(),
                            title, getContext());
                }
            }
        });
    }

    private void setTitle(final String title) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null) {
                    ActionBar actionBar = ((FileDisplayActivity) getActivity()).getSupportActionBar();

                    if (actionBar != null) {
                            ThemeUtils.setColoredTitle(actionBar, title);
                    }
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
        if (searchEvent != null && searchFragment) {
            onMessageEvent(searchEvent);

            mRefreshListLayout.setRefreshing(false);
        } else {
            super.onRefresh();
        }
    }

    public void setSearchFragment(boolean searchFragment) {
        this.searchFragment = searchFragment;
    }

    public boolean getIsSearchFragment() {
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
}
