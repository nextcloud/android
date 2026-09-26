/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author masensio
 * @author Juan Carlos González Cabrero
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author Shashvat Kedia
 * @author Abel García de Prada
 * @author John Kalimeris
 * @author Aitor Ballesteros Pavón
 * @author Jorge Aguado Recio
 *
 * Copyright (C) 2012  Bartek Przybylski
 * Copyright (C) 2026 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.data.providers.implementation.OCSharedPreferencesProvider;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.domain.exceptions.UnauthorizedException;
import com.owncloud.android.domain.files.model.OCFile;
import com.owncloud.android.extensions.ActivityExtKt;
import com.owncloud.android.extensions.ThrowableExtKt;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.presentation.common.UIResult;
import com.owncloud.android.presentation.files.SortBottomSheetFragment;
import com.owncloud.android.presentation.files.SortOptionsView;
import com.owncloud.android.presentation.files.SortOrder;
import com.owncloud.android.presentation.files.SortType;
import com.owncloud.android.presentation.files.ViewType;
import com.owncloud.android.presentation.files.createfolder.CreateFolderDialogFragment;
import com.owncloud.android.presentation.files.operations.FileOperation;
import com.owncloud.android.presentation.files.operations.FileOperationsViewModel;
import com.owncloud.android.presentation.security.LockType;
import com.owncloud.android.presentation.security.SecurityEnforced;
import com.owncloud.android.presentation.spaces.SpacesListFragment;
import com.owncloud.android.presentation.transfers.TransfersViewModel;
import com.owncloud.android.ui.ReceiveExternalFilesViewModel;
import com.owncloud.android.ui.adapter.ReceiveExternalFilesAdapter;
import com.owncloud.android.ui.asynctasks.CopyAndUploadContentUrisTask;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.fragment.TaskRetainerFragment;
import com.owncloud.android.ui.helpers.UriUploader;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.SortFilesUtils;
import kotlin.Lazy;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.owncloud.android.presentation.settings.advanced.SettingsAdvancedFragment.PREF_SHOW_HIDDEN_FILES;
import static org.koin.core.parameter.ParametersHolderKt.parametersOf;
import static org.koin.java.KoinJavaComponent.get;
import static org.koin.java.KoinJavaComponent.inject;

/**
 * This can be used to upload things to an ownCloud instance.
 */
public class ReceiveExternalFilesActivity extends FileActivity
        implements OnItemClickListener,
        android.view.View.OnClickListener,
        CopyAndUploadContentUrisTask.OnCopyTmpFilesTaskListener,
        SortOptionsView.SortOptionsListener,
        SortBottomSheetFragment.SortDialogListener,
        SortOptionsView.CreateFolderListener,
        SearchView.OnQueryTextListener,
        ReceiveExternalFilesAdapter.OnSearchQueryUpdateListener,
        SecurityEnforced,
        CreateFolderDialogFragment.CreateFolderListener {

    private static final String FTAG_ERROR_FRAGMENT = "ERROR_FRAGMENT";

    private AccountManager mAccountManager;
    private Stack<String> mParents;
    private ArrayList<Uri> mStreamsToUpload = new ArrayList<>();
    private String mUploadPath;
    private OCFile mFile;
    private SortOptionsView mSortOptionsView;

    private View mEmptyListView;
    private ImageView mEmptyListImage;
    private TextView mEmptyListTitle;
    private FrameLayout fragmentContainer;
    private Button uploaderButton;
    private Button cancelButton;
    private TextView noPermissionsMessage;
    private String currentSpaceId;

    private boolean haveMultiAccount = false;

    // this is inited lazily, when an account is selected. If no account is selected but an instance of this would
    // be crated it would result in an null pointer exception.
    private ReceiveExternalFilesAdapter mAdapter = null;
    private ListView mListView;
    private boolean mSyncInProgress = false;
    private boolean mAccountSelected;
    private boolean mAccountSelectionShowing;

    private final static int DIALOG_NO_ACCOUNT = 0;
    private final static int DIALOG_MULTIPLE_ACCOUNT = 1;

    private final static int REQUEST_CODE__SETUP_ACCOUNT = REQUEST_CODE__LAST_SHARED + 1;
    private final static int MAX_FILENAME_LENGTH = 223;

    private final static String KEY_PARENTS = "PARENTS";
    private final static String KEY_FILE = "FILE";
    private final static String KEY_ACCOUNT_SELECTED = "ACCOUNT_SELECTED";
    private final static String KEY_ACCOUNT_SELECTION_SHOWING = "ACCOUNT_SELECTION_SHOWING";

    private static final String DIALOG_WAIT_COPY_FILE = "DIALOG_WAIT_COPY_FILE";
    private static final String TAG_SPACE_LIST_FRAGMENT = "TAG_SPACE_LIST_FRAGMENT";

    private boolean showHiddenFiles;
    private OCSharedPreferencesProvider sharedPreferencesProvider;
    private boolean areSpacesAllowed;
    private boolean isMultiPersonal;


    Pattern pattern = Pattern.compile("[/\\\\]");

    private ReceiveExternalFilesViewModel mReceiveExternalFilesViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prepareStreamsToUpload(); //streams need to be prepared before super.onCreate() is called

        ActivityExtKt.checkPasscodeEnforced(this, this);

        if (savedInstanceState == null) {
            mParents = new Stack<>();
            mAccountSelected = false;
            mAccountSelectionShowing = false;

        } else {
            mParents = (Stack<String>) savedInstanceState.getSerializable(KEY_PARENTS);
            mFile = savedInstanceState.getParcelable(KEY_FILE);
            mAccountSelected = savedInstanceState.getBoolean(KEY_ACCOUNT_SELECTED);
            mAccountSelectionShowing = savedInstanceState.getBoolean(KEY_ACCOUNT_SELECTION_SHOWING);
        }

        super.onCreate(savedInstanceState);

        if (mAccountSelected) {
            setAccount(savedInstanceState.getParcelable(FileActivity.EXTRA_ACCOUNT));
        }

        //init ui
        setContentView(R.layout.uploader_layout);

        View receiveExternalFilesRoot = findViewById(R.id.coordinator_layout);
        ActivityExtKt.adaptInfiniteEdges(this, receiveExternalFilesRoot);

        mSortOptionsView = findViewById(R.id.options_layout);
        if (mSortOptionsView != null) {
            mSortOptionsView.setOnSortOptionsListener(this);
            mSortOptionsView.setOnCreateFolderListener(this);
            mSortOptionsView.selectAdditionalView(SortOptionsView.AdditionalView.CREATE_FOLDER);
        }
        mSortOptionsView.setVisibility(View.GONE);


        initPickerListener();

    }
    private void subscribeToViewModels() {
        mReceiveExternalFilesViewModel.isMultiPersonal().observe(this, isMultiPersonalCapability ->
                isMultiPersonal = isMultiPersonalCapability);
        mReceiveExternalFilesViewModel.getSpacesAreAllowed().observe(this, spaces -> {
            areSpacesAllowed = spaces;

            if (!areSpacesAllowed) { // OC10 Server
                showListOfFiles();
                showRetainerFragment();
                updateDirectoryList();
                if (mParents.size() == 1) {
                    updateToolbar(getString(R.string.uploader_top_message));
                }
            } else { // OCIS Server
                if (haveMultiAccount) { // Multi account
                    mListView = findViewById(android.R.id.list);
                    fragmentContainer = findViewById(R.id.fragment_container);
                    mListView.setVisibility(View.GONE);
                    fragmentContainer.setVisibility(View.VISIBLE);
                }
                initAndShowListOfSpaces();
                getSupportFragmentManager().setFragmentResultListener(SpacesListFragment.REQUEST_KEY_CLICK_SPACE, this, (requestKey, bundle) -> {
                    removeSpaceListFragment();
                    OCFile rootSpaceFolder = bundle.getParcelable(SpacesListFragment.BUNDLE_KEY_CLICK_SPACE);
                    mFile = rootSpaceFolder;
                    currentSpaceId = mFile.getSpaceId();
                    startSyncFolderOperation(mFile);
                    showListOfFiles();
                    updateDirectoryList();
                    showRetainerFragment();
                });
            }
        }
        );
    }

    private void removeSpaceListFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_SPACE_LIST_FRAGMENT);
        getSupportFragmentManager().beginTransaction().remove(fragment).commit();
    }

    private void showListOfFiles() {
        fragmentContainer = findViewById(R.id.fragment_container);
        mListView = findViewById(android.R.id.list);
        fragmentContainer.setVisibility(View.GONE);
        mListView.setVisibility(View.VISIBLE);
        mSortOptionsView.setVisibility(View.VISIBLE);

        mListView.setOnItemClickListener(this);

        mEmptyListView = findViewById(R.id.empty_list_view);
        mEmptyListImage = findViewById(R.id.list_empty_dataset_icon);
        mEmptyListImage.setImageResource(R.drawable.ic_folder);
        mEmptyListTitle = findViewById(R.id.list_empty_dataset_title);
        mEmptyListTitle.setText(R.string.file_list_empty_title_all_files);
    }

    private void showRetainerFragment() {
        FragmentManager fm = getSupportFragmentManager();
        TaskRetainerFragment taskRetainerFragment =
                (TaskRetainerFragment) fm.findFragmentByTag(TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT);
        if (taskRetainerFragment == null) {
            taskRetainerFragment = new TaskRetainerFragment();
            fm.beginTransaction()
                    .add(taskRetainerFragment, TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT).commit();
        }   // else, Fragment already created and retained across configuration change
    }

    private void initAndShowListOfSpaces() {
        SpacesListFragment listOfSpaces = SpacesListFragment.Companion.newInstance(true, true, getAccount().name);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, listOfSpaces, TAG_SPACE_LIST_FRAGMENT);
        transaction.commit();
        uploaderButton = findViewById(R.id.uploader_choose_folder);
        uploaderButton.setVisibility(View.GONE);
        updateToolbar(getString(R.string.choose_upload_space));
    }

    private void initPickerListener() {
        cancelButton = findViewById(R.id.uploader_cancel);
        cancelButton.setOnClickListener(view -> finish());
    }

    @Override
    protected void setAccount(Account account, boolean savedAccount) {
        if (somethingToUpload()) {
            mAccountManager = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
            Account[] accounts = mAccountManager.getAccountsByType(MainApp.Companion.getAccountType());
            if (accounts.length == 0) {
                Timber.i("No ownCloud account is available");
                showDialog(DIALOG_NO_ACCOUNT);
            } else if (accounts.length > 1 && !mAccountSelected && !mAccountSelectionShowing) {
                Timber.i("More than one ownCloud is available");
                showDialog(DIALOG_MULTIPLE_ACCOUNT);
                mAccountSelectionShowing = true;
            } else {
                if (!savedAccount) {
                    setAccount(accounts[0]);
                }
            }
        } else {
            showErrorDialog(
                    R.string.uploader_error_message_no_file_to_upload,
                    R.string.uploader_error_title_no_file_to_upload
            );
        }

        super.setAccount(account, savedAccount);
    }

    @Override
    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(mAccountWasRestored);
        mReceiveExternalFilesViewModel = get(ReceiveExternalFilesViewModel.class, null , () -> parametersOf(getAccount().name));
        subscribeToViewModels();
        initTargetFolder();

        mReceiveExternalFilesViewModel.getSyncFolderLiveData().observe(this, eventUiResult -> {
            UIResult<Unit> uiResult = eventUiResult.getContentIfNotHandled();
            if (uiResult != null) {
                if (uiResult instanceof UIResult.Loading) {
                    mSyncInProgress = true;
                } else if (uiResult instanceof UIResult.Error) {
                    mSyncInProgress = false;
                    Throwable throwable = ((UIResult.Error<Unit>) uiResult).getError();
                    if (throwable != null) {
                        if (throwable instanceof UnauthorizedException) {
                            requestCredentialsUpdate();
                        } else if (throwable instanceof CertificateCombinedException) {
                            showUntrustedCertDialogForThrowable(throwable);
                        } else {
                            ActivityExtKt.showErrorInSnackbar(this, R.string.sync_fail_ticker, throwable);
                        }
                    }
                } else if (uiResult instanceof UIResult.Success) {
                    mSyncInProgress = false;
                    updateDirectoryList();
                    if (mParents.size() == 1 && !areSpacesAllowed) {
                        updateToolbar(getString(R.string.uploader_top_message));
                    }
                    if(fragmentContainer.getVisibility() == View.VISIBLE) {
                        updateToolbar(getString(R.string.choose_upload_space));
                        mListView.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Timber.v("onSaveInstanceState() start");
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_PARENTS, mParents);
        //outState.putParcelable(KEY_ACCOUNT, mAccount);
        outState.putParcelable(KEY_FILE, mFile);
        outState.putBoolean(KEY_ACCOUNT_SELECTED, mAccountSelected);
        outState.putBoolean(KEY_ACCOUNT_SELECTION_SHOWING, mAccountSelectionShowing);
        outState.putParcelable(FileActivity.EXTRA_ACCOUNT, getAccount());

        Timber.v("onSaveInstanceState() end");
    }

    @Override
    protected Dialog onCreateDialog(final int id) {
        final AlertDialog.Builder builder = new Builder(this);
        switch (id) {
            case DIALOG_NO_ACCOUNT:
                builder.setIcon(R.drawable.ic_warning);
                builder.setTitle(R.string.uploader_wrn_no_account_title);
                builder.setMessage(String.format(
                        getString(R.string.uploader_wrn_no_account_text),
                        getString(R.string.app_name)));
                builder.setCancelable(false);
                builder.setPositiveButton(R.string.uploader_wrn_no_account_setup_btn_text, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_ADD_ACCOUNT);
                        intent.putExtra("authorities", new String[]{MainApp.Companion.getAuthTokenType()});
                        startActivityForResult(intent, REQUEST_CODE__SETUP_ACCOUNT);
                    }
                });
                builder.setNegativeButton(R.string.uploader_wrn_no_account_quit_btn_text, (dialog, which) -> finish());
                return builder.create();
            case DIALOG_MULTIPLE_ACCOUNT:
                Account[] accounts = mAccountManager.getAccountsByType(MainApp.Companion.getAccountType());
                CharSequence[] dialogItems = new CharSequence[accounts.length];
                OwnCloudAccount oca;
                haveMultiAccount = true;
                for (int i = 0; i < dialogItems.length; ++i) {
                    try {
                        oca = new OwnCloudAccount(accounts[i], this);
                        dialogItems[i] =
                                oca.getDisplayName() + " @ " +
                                        DisplayUtils.convertIdn(
                                                accounts[i].name.substring(accounts[i].name.lastIndexOf("@") + 1),
                                                false
                                        );

                    } catch (Exception e) {
                        Timber.w("Couldn't read display name of account; using account name instead");
                        dialogItems[i] = DisplayUtils.convertIdn(accounts[i].name, false);
                    }
                }
                builder.setTitle(R.string.common_choose_account);
                builder.setItems(dialogItems, (dialog, which) -> {
                    setAccount(mAccountManager.getAccountsByType(MainApp.Companion.getAccountType())[which]);
                    onAccountSet(mAccountWasRestored);
                    dialog.dismiss();
                    PreferenceManager.setLastUploadPath("/", this);
                    mAccountSelected = true;
                    mAccountSelectionShowing = false;
                });
                builder.setCancelable(true);
                builder.setOnCancelListener(dialog -> {
                    mAccountSelectionShowing = false;
                    dialog.cancel();
                    finish();
                });
                return builder.create();
            default:
                throw new IllegalArgumentException("Unknown dialog id: " + id);
        }
    }

    @Override
    public void onBackPressed() {
        if (mParents.size() <= 1) {
            super.onBackPressed();
        } else {
            mParents.pop();
            String full_path = generatePath(mParents);
            OCFile folder = getStorageManager().getFileByPath(full_path, currentSpaceId);
            if (folder == null) {
                Timber.i("Folder %s not found, fetching the folder again.", full_path);
                super.onBackPressed();
                return;
            }
            startSyncFolderOperation(folder);
            updateDirectoryList();
            if (mParents.size() <= 1 && !areSpacesAllowed) {
                updateToolbar(getString(R.string.uploader_top_message));
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // click on folder in the list
        Timber.d("on item click");
        // get current representation of files:
        // This does not necessarily mean this is the content of the current folder.
        // If the user searches for a folder mAdapter.getFiles() returns only the folders/files
        // that match the currently entered search query.
        List<OCFile> tmpfiles = mAdapter.getFiles();
        tmpfiles = sortFileList(tmpfiles);

        if (tmpfiles.size() <= 0) {
            return;
        }
        // filter on dirtype
        Vector<OCFile> files = new Vector<>(tmpfiles);
        if (files.size() < position) {
            throw new IndexOutOfBoundsException("Incorrect item selected");
        }
        if (files.get(position).isFolder()) {
            OCFile folderToEnter = files.get(position);
            startSyncFolderOperation(folderToEnter);
            mParents.push(folderToEnter.getFileName());
            updateDirectoryList();
        }
    }

    @Override
    public void onClick(View v) {
        // click on button
        // Using an if / else if / else block because Java switch statements
        // with resource ID comparisons are no longer supported in AGP 9
        if (v.getId() == R.id.uploader_choose_folder) {
            mUploadPath = "";   // first element in mParents is root dir, represented by "";
            // init mUploadPath with "/" results in a "//" prefix
            for (String p : mParents) {
                mUploadPath += p + File.separator;
            }
            if (!isPlainTextUpload()) {
                Timber.d("Uploading file to dir %s", mUploadPath);
                uploadFiles();
            } else {
                showUploadTextDialog();
            }
        } else {
            throw new IllegalArgumentException("Wrong element clicked");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Timber.i("result received. req: " + requestCode + " res: " + resultCode);
        if (requestCode == REQUEST_CODE__SETUP_ACCOUNT) {
            dismissDialog(DIALOG_NO_ACCOUNT);
            if (resultCode == RESULT_CANCELED) {
                finish();
            }
            Account[] accounts = mAccountManager.getAccountsByType(MainApp.Companion.getAuthTokenType());
            if (accounts.length == 0) {
                showDialog(DIALOG_NO_ACCOUNT);
            } else {
                // there is no need for checking for is there more then one
                // account at this point
                // since account setup can set only one account at time
                setAccount(accounts[0]);
                updateDirectoryList();
            }
        }
    }

    private void updateDirectoryList() {
        initToolbar(mParents.peek());

        String full_path = generatePath(mParents);
        Timber.d("Populating view with content of : %s", full_path);

        sharedPreferencesProvider = new OCSharedPreferencesProvider(getApplicationContext());
        showHiddenFiles = sharedPreferencesProvider.getBoolean(PREF_SHOW_HIDDEN_FILES, false);
        mFile = getStorageManager().getFileByPath(full_path, currentSpaceId);

        if (mFile != null) {
            if (mAdapter == null) {
                mAdapter = new ReceiveExternalFilesAdapter(
                        this, getStorageManager(), getAccount(), showHiddenFiles, isMultiPersonal);
                mListView.setAdapter(mAdapter);
            }
            Vector<OCFile> files = new Vector<>(sortFileList(getStorageManager().getFolderContent(mFile)));
            mAdapter.setNewItemVector(files);
        }

        Button btnChooseFolder = findViewById(R.id.uploader_choose_folder);
        noPermissionsMessage = findViewById(R.id.uploader_no_permissions_message);
        if (getCurrentFolder().getHasAddFilePermission()) {
            btnChooseFolder.setOnClickListener(this);
            btnChooseFolder.setVisibility(View.VISIBLE);
            noPermissionsMessage.setVisibility(View.GONE);
        } else {
            btnChooseFolder.setVisibility(View.GONE);
            noPermissionsMessage.setVisibility(View.VISIBLE);
        }

        initPickerListener();

        if (getCurrentFolder().getHasAddSubdirectoriesPermission()) {
            mSortOptionsView.selectAdditionalView(SortOptionsView.AdditionalView.CREATE_FOLDER);
        } else {
            mSortOptionsView.selectAdditionalView(SortOptionsView.AdditionalView.HIDDEN);
        }

        updateEmptyListMessage(getString(R.string.file_list_empty_title_all_files));
    }

    /**
     * This activity is special, so we won't use the ToolbarActivity method to initialize the actionbar.
     */
    private void initToolbar(String current_dir) {
        Toolbar toolbar = findViewById(R.id.standard_toolbar);
        ConstraintLayout rootToolbar = findViewById(R.id.root_toolbar);
        toolbar.setVisibility(View.VISIBLE);
        rootToolbar.setVisibility(View.GONE);

        String actionBarTitle;
        if (current_dir.equals("")) {
            actionBarTitle = getString(R.string.uploader_top_message);
        } else {
            actionBarTitle = current_dir;
        }
        toolbar.setTitle(actionBarTitle);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    private void updateToolbar(String toolbarName) {
        updateStandardToolbar(toolbarName,
                false,
                false
        );
    }

    @Override
    public void onSavedCertificate() {
        startSyncFolderOperation(getCurrentDir());
    }

    private void startSyncFolderOperation(OCFile folder) {
        if (folder == null) {
            Timber.w("startSyncFolderOperation called with a null folder; skipping sync");
            return;
        }

        mSyncInProgress = true;

        if (mReceiveExternalFilesViewModel != null) {
            mReceiveExternalFilesViewModel.refreshFolderUseCase(folder);
        }
    }

    private List<OCFile> sortFileList(List<OCFile> files) {
        // Read sorting order, default to sort by name ascending
        FileStorageUtils.mSortOrderFileDisp = PreferenceManager.getSortOrder(this, FileStorageUtils.FILE_DISPLAY_SORT);
        FileStorageUtils.mSortAscendingFileDisp = PreferenceManager.getSortAscending(this,
                FileStorageUtils.FILE_DISPLAY_SORT);

        files = new SortFilesUtils().sortFiles(files, FileStorageUtils.mSortOrderFileDisp,
                FileStorageUtils.mSortAscendingFileDisp);
        return files;
    }

    private String generatePath(Stack<String> dirs) {
        StringBuilder full_path = new StringBuilder();

        for (String a : dirs) {
            full_path.append(a).append("/");
        }
        return full_path.toString();
    }

    private void prepareStreamsToUpload() {
        if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_SEND)) {
            Uri safeStream = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            if (safeStream != null) {
                mStreamsToUpload.add(safeStream);
            }
        } else if (getIntent().getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
            ArrayList<Uri> safeArrayList = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (safeArrayList != null) {
                mStreamsToUpload = safeArrayList;
            }
        }

        if (isPlainTextUpload()) {
            return;
        }

        for (Uri stream : mStreamsToUpload) {
            String streamToUpload;
            try {
                streamToUpload = new File(stream.toString()).getCanonicalPath();
            } catch (IOException e) {
                finish();
                return;
            }
            if (streamToUpload.contains("/data") &&
                    streamToUpload.contains(getPackageName()) &&
                    !streamToUpload.contains(getCacheDir().getPath())
            ) {
                finish();
            }
        }
    }

    private boolean somethingToUpload() {
        return (!mStreamsToUpload.isEmpty() || isPlainTextUpload());
    }

    /**
     * Checks if the intent contains plain text and no other stream has been added yet.
     *
     * @return true/false
     */
    private boolean isPlainTextUpload() {
        return mStreamsToUpload.isEmpty() &&
                getIntent().getStringExtra(Intent.EXTRA_TEXT) != null;
    }

    public void uploadFiles() {

        UriUploader uploader = new UriUploader(
                this,
                mStreamsToUpload,
                mUploadPath,
                getAccount(),
                currentSpaceId,
                true, // Show waiting dialog while file is being copied from private storage
                this  // Copy temp task listener
        );

        UriUploader.UriUploaderResultCode resultCode = uploader.uploadUris();

        // Save the path to shared preferences; even if upload is not possible, user chose the folder
        PreferenceManager.setLastUploadPath(mUploadPath, this);

        if (resultCode == UriUploader.UriUploaderResultCode.OK) {
            finish();
        } else if (resultCode != UriUploader.UriUploaderResultCode.COPY_THEN_UPLOAD) {

            int messageResTitle = R.string.uploader_error_title_file_cannot_be_uploaded;
            int messageResId = R.string.common_error_unknown;

            if (resultCode == UriUploader.UriUploaderResultCode.ERROR_NO_FILE_TO_UPLOAD) {
                messageResId = R.string.uploader_error_message_no_file_to_upload;
                messageResTitle = R.string.uploader_error_title_no_file_to_upload;
            } else if (resultCode == UriUploader.UriUploaderResultCode.ERROR_READ_PERMISSION_NOT_GRANTED) {
                messageResId = R.string.uploader_error_message_read_permission_not_granted;
            }

            showErrorDialog(
                    messageResId,
                    messageResTitle
            );
        }
    }

    /**
     * Loads the target folder initialize shown to the user.
     * <p/>
     * The target account has to be chosen before this method is called.
     */
    private void initTargetFolder() {
        if (getStorageManager() == null) {
            throw new IllegalStateException("Do not call this method before " +
                    "initializing mStorageManager");
        }

        String lastPath = PreferenceManager.getLastUploadPath(this);
        // "/" equals root-directory
        if (lastPath.equals("/")) {
            mParents.add("");
        } else {
            String[] dir_names = lastPath.split("/");
            mParents.clear();
            mParents.addAll(Arrays.asList(dir_names));
        }
        //Make sure that path still exists, if it doesn't pop the stack and try the previous path
        while (!getStorageManager().fileExists(generatePath(mParents)) && mParents.size() > 1) {
            mParents.pop();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem menuItem = menu.findItem(R.id.action_search);
        SearchView mSearchView = (SearchView) menuItem.getActionView();
        mSearchView.setOnQueryTextListener(this);
        menu.removeItem(menu.findItem(R.id.action_share_current_folder).getItemId());

        Toolbar toolbar = findViewById(R.id.standard_toolbar);
        if (getString(R.string.choose_upload_space) != toolbar.getTitle()) {
            mSearchView.setQueryHint(getString(R.string.actionbar_search));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home:
                if ((mParents.size() > 1)) {
                    onBackPressed();
                } else {
                    mFile = null;
                    initAndShowListOfSpaces();
                    updateToolbar(getString(R.string.choose_upload_space));
                    fragmentContainer.setVisibility(View.VISIBLE);
                    mEmptyListView.setVisibility(View.GONE);
                    mListView.setVisibility(View.GONE);
                    noPermissionsMessage.setVisibility(View.GONE);
                }
                break;
            default:
                retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void sortByName(boolean isAscending) {
        mAdapter.setSortOrder(FileStorageUtils.SORT_NAME, isAscending);
    }

    private void sortBySize(boolean isAscending) {
        mAdapter.setSortOrder(FileStorageUtils.SORT_SIZE, isAscending);
    }

    private void sortByDate(boolean isAscending) {
        mAdapter.setSortOrder(FileStorageUtils.SORT_DATE, isAscending);
    }

    private OCFile getCurrentFolder() {
        OCFile file = mFile;
        if (file != null) {
            if (file.isFolder()) {
                return file;
            } else if (getStorageManager() != null) {
                return getStorageManager().getFileByPath(file.getParentRemotePath(), currentSpaceId);
            }
        }
        return null;
    }

    private void browseToRoot() {
        OCFile root = getStorageManager().getRootPersonalFolder();
        mFile = root;
        startSyncFolderOperation(root);
    }

    @Override
    public void onSortTypeListener(@NotNull SortType sortType, @NotNull SortOrder sortOrder) {
        SortBottomSheetFragment sortBottomSheetFragment = SortBottomSheetFragment.Companion.newInstance(sortType, sortOrder);
        sortBottomSheetFragment.setSortDialogListener(this);
        sortBottomSheetFragment.show(getSupportFragmentManager(), SortBottomSheetFragment.TAG);
    }

    @Override
    public void onViewTypeListener(@NotNull ViewType viewType) {

    }

    @Override
    public void onSortSelected(@NotNull SortType sortType) {
        mSortOptionsView.setSortTypeSelected(sortType);

        boolean isAscending = mSortOptionsView.getSortOrderSelected().equals(SortOrder.SORT_ORDER_ASCENDING);
        if (sortType == SortType.SORT_TYPE_BY_NAME) {
            sortByName(isAscending);
        } else if (sortType == SortType.SORT_TYPE_BY_DATE) {
            sortByDate(isAscending);
        } else if (sortType == SortType.SORT_TYPE_BY_SIZE) {
            sortBySize(isAscending);
        }
    }

    @Override
    public void onCreateFolderListener() {
        CreateFolderDialogFragment dialog = CreateFolderDialogFragment.newInstance(mFile, this::onFolderNameSet);
        dialog.show(getSupportFragmentManager(), CreateFolderDialogFragment.CREATE_FOLDER_FRAGMENT);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if (mAdapter != null) {
            mAdapter.filterBySearch(query);
        }
        return true;
    }

    @Override
    public void updateEmptyListMessage(String updateTxt) {
        if (mAdapter.getFiles().isEmpty()) {
            mEmptyListView.setVisibility(View.VISIBLE);
            mListView = findViewById(android.R.id.list);
            mListView.setVisibility(View.GONE);
        } else {
            mEmptyListView.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
        }
        mEmptyListTitle.setText(updateTxt);
    }

    @Override
    public void optionLockSelected(@NonNull LockType type) {
        ActivityExtKt.manageOptionLockSelected(this, type);
    }

    @Override
    public void onFolderNameSet(@NotNull String newFolderName, @NotNull OCFile parentFolder) {
        FileOperationsViewModel fileOperationsViewModel = get(FileOperationsViewModel.class);

        fileOperationsViewModel.performOperation(new FileOperation.CreateFolder(newFolderName, parentFolder));
        fileOperationsViewModel.getCreateFolder().observe(this, uiResultEvent -> {
            UIResult<Unit> uiResult = uiResultEvent.peekContent();
            if (uiResult.isSuccess()) {
                updateDirectoryList();
            } else if (uiResult.isError()) {
                Throwable throwable = uiResult.getThrowableOrNull();
                CharSequence errorMessage = ThrowableExtKt.parseError(throwable,
                        getResources().getString(R.string.create_dir_fail_msg),
                        getResources(), false);

                showSnackMessage(errorMessage.toString());
            }
        });
    }

    /**
     * Process the result of CopyAndUploadContentUrisTask
     */
    @Override
    public void onTmpFilesCopied(ResultCode result) {
        try {
            dismissLoadingDialog();
        } catch (IllegalStateException illegalStateException) {
            Timber.e(illegalStateException);
        }
        finish();
    }

    /**
     * Show an error dialog, forcing the user to click a single button to exit the activity
     *
     * @param messageResId    DataResult id of the message to show in the dialog.
     * @param messageResTitle DataResult id of the title to show in the dialog. 0 to show default alert message.
     *                        -1 to show no title.
     */
    private void showErrorDialog(int messageResId, int messageResTitle) {

        ConfirmationDialogFragment errorDialog = ConfirmationDialogFragment.newInstance(
                messageResId,
                new String[]{getString(R.string.app_name)}, // see uploader_error_message_* in strings.xml
                messageResTitle,
                R.string.common_back,
                -1,
                -1
        );
        errorDialog.setCancelable(false);
        errorDialog.setOnConfirmationListener(
                new ConfirmationDialogFragment.ConfirmationDialogFragmentListener() {
                    @Override
                    public void onConfirmation(String callerTag) {
                        finish();
                    }

                    @Override
                    public void onNeutral(String callerTag) {
                    }

                    @Override
                    public void onCancel(String callerTag) {
                    }
                }
        );
        errorDialog.show(getSupportFragmentManager(), FTAG_ERROR_FRAGMENT);
    }

    /**
     * Show a dialog where the user can enter a filename for the file he wants to place the text in.
     */
    private void showUploadTextDialog() {
        final AlertDialog.Builder builder = new Builder(this);

        final View dialogView = getLayoutInflater().inflate(R.layout.dialog_upload_text, null);
        builder.setView(dialogView);
        builder.setTitle(R.string.uploader_upload_text_dialog_title);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.uploader_btn_upload_text, null);
        builder.setNegativeButton(android.R.string.cancel, null);

        final TextInputEditText input = dialogView.findViewById(R.id.inputFileName);
        final TextInputLayout inputLayout = dialogView.findViewById(R.id.inputTextLayout);

        final AlertDialog alertDialog = builder.create();
        setFileNameFromIntent(alertDialog, input);
        alertDialog.setOnShowListener(dialog -> {
            Button button = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String fileName = input.getText().toString();
                String error = null;
                fileName += ".txt";
                String filePath = savePlainTextToFile(fileName);
                ArrayList<String> fileToUpload = new ArrayList<>();
                fileToUpload.add(filePath);
                @NotNull Lazy<TransfersViewModel> transfersViewModelLazy = inject(TransfersViewModel.class);
                TransfersViewModel transfersViewModel = transfersViewModelLazy.getValue();
                transfersViewModel.uploadFilesFromSystem(getAccount().name, fileToUpload, mUploadPath, currentSpaceId, false);
                finish();

                inputLayout.setErrorEnabled(error != null);
                inputLayout.setError(error);
            });
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Button okButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                String fileName = input.getText().toString();
                String error = null;
                Matcher matcher = pattern.matcher(fileName);
                if (charSequence == null || charSequence.toString().trim().isEmpty()) {
                    okButton.setEnabled(false);
                    error = getString(R.string.uploader_upload_text_dialog_filename_error_empty);
                } else if (charSequence.length() > MAX_FILENAME_LENGTH) {
                    error = String.format(getString(R.string.uploader_upload_text_dialog_filename_error_length_max),
                            MAX_FILENAME_LENGTH);
                } else if (matcher.find()) {
                    error = getString(R.string.filename_forbidden_characters);
                } else {
                    okButton.setEnabled(true);
                    error = null;
                    inputLayout.setError(error);
                }

                if (error != null) {
                    okButton.setEnabled(false);
                    inputLayout.setError(error);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        alertDialog.show();
    }

    /**
     * Store plain text from intent to a new file in cache dir.
     *
     * @param fileName The name of the file.
     * @return Path of the created file.
     */
    private String savePlainTextToFile(String fileName) {
        File tmpFile = null;
        String content = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        try {
            tmpFile = new File(getCacheDir(), fileName);
            FileOutputStream outputStream = new FileOutputStream(tmpFile);
            outputStream.write(content.getBytes());
            outputStream.close();

        } catch (IOException e) {
            Timber.w(e, "Failed to create temp file for uploading plain text: %s", e.getMessage());
        }
        return tmpFile.getAbsolutePath();
    }

    /**
     * Suggest a filename based on the extras in the intent.
     * Show soft keyboard when no filename could be suggested.
     *
     * @param alertDialog AlertDialog
     * @param input       EditText The view where to place the filename in.
     */
    private void setFileNameFromIntent(AlertDialog alertDialog, EditText input) {
        String subject = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);
        String title = getIntent().getStringExtra(Intent.EXTRA_TITLE);
        String fileName = subject != null ? subject : title;

        input.setText(fileName);
        input.selectAll();

        if (fileName == null) {
            // Show soft keyboard
            Window window = alertDialog.getWindow();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }
        }
    }
}
