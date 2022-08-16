/*
 *  ownCloud Android client application
 *
 *  @author Bartek Przybylski
 *  @author masensio
 *  @author Juan Carlos González Cabrero
 *  @author David A. Velasco
 *  @author Chris Narkiewicz
 *  Copyright (C) 2012  Bartek Przybylski
 *  Copyright (C) 2016 ownCloud Inc.
 *  Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources.NotFoundException;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nextcloud.client.account.User;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ReceiveExternalFilesBinding;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.NameCollisionPolicy;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.syncadapter.FileSyncAdapter;
import com.owncloud.android.ui.adapter.UploaderAdapter;
import com.owncloud.android.ui.asynctasks.CopyAndUploadContentUrisTask;
import com.owncloud.android.ui.dialog.AccountChooserInterface;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.CreateFolderDialogFragment;
import com.owncloud.android.ui.dialog.MultipleAccountsDialog;
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment;
import com.owncloud.android.ui.fragment.TaskRetainerFragment;
import com.owncloud.android.ui.helpers.UriUploader;
import com.owncloud.android.utils.DataHolderUtil;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ErrorMessageAdapter;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.MimeType;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeTextInputUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import javax.inject.Inject;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static com.owncloud.android.utils.DisplayUtils.openSortingOrderDialogFragment;

/**
 * This can be used to upload things to an ownCloud instance.
 */
public class ReceiveExternalFilesActivity extends FileActivity
    implements OnItemClickListener, View.OnClickListener, CopyAndUploadContentUrisTask.OnCopyTmpFilesTaskListener,
    SortingOrderDialogFragment.OnSortingOrderListener, Injectable, AccountChooserInterface {

    private static final String TAG = ReceiveExternalFilesActivity.class.getSimpleName();

    private static final String FTAG_ERROR_FRAGMENT = "ERROR_FRAGMENT";
    public static final String TEXT_FILE_SUFFIX = ".txt";
    public static final String URL_FILE_SUFFIX = ".url";
    public static final String WEBLOC_FILE_SUFFIX = ".webloc";
    public static final String DESKTOP_FILE_SUFFIX = ".desktop";
    public static final int SINGLE_PARENT = 1;

    @Inject AppPreferences preferences;
    @Inject LocalBroadcastManager localBroadcastManager;
    private AccountManager mAccountManager;
    private Stack<String> mParents = new Stack<>();
    private List<Parcelable> mStreamsToUpload;
    private String mUploadPath;
    private OCFile mFile;

    private SyncBroadcastReceiver mSyncBroadcastReceiver;
    private boolean mSyncInProgress;

    private final static int REQUEST_CODE__SETUP_ACCOUNT = REQUEST_CODE__LAST_SHARED + 1;

    private final static String KEY_PARENTS = "PARENTS";
    private final static String KEY_FILE = "FILE";

    private boolean mUploadFromTmpFile;
    private String mSubjectText;
    private String mExtraText;

    private final static Charset FILENAME_ENCODING = Charset.forName("UTF-8");

    private ViewGroup mEmptyListContainer;
    private TextView mEmptyListMessage;
    private TextView mEmptyListHeadline;
    private ImageView mEmptyListIcon;
    private MaterialButton sortButton;
    private ReceiveExternalFilesBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prepareStreamsToUpload();

        if (savedInstanceState != null) {
            String parentPath = savedInstanceState.getString(KEY_PARENTS);

            if (parentPath != null) {
                mParents.addAll(Arrays.asList(parentPath.split(OCFile.PATH_SEPARATOR)));
            }

            mFile = savedInstanceState.getParcelable(KEY_FILE);
        }
        mAccountManager = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);

        super.onCreate(savedInstanceState);
        binding = ReceiveExternalFilesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Listen for sync messages
        IntentFilter syncIntentFilter = new IntentFilter(RefreshFolderOperation.
                                                             EVENT_SINGLE_FOLDER_CONTENTS_SYNCED);
        syncIntentFilter.addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED);
        mSyncBroadcastReceiver = new SyncBroadcastReceiver();
        localBroadcastManager.registerReceiver(mSyncBroadcastReceiver, syncIntentFilter);

        // Init Fragment without UI to retain AsyncTask across configuration changes
        FragmentManager fm = getSupportFragmentManager();
        TaskRetainerFragment taskRetainerFragment =
                (TaskRetainerFragment) fm.findFragmentByTag(TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT);
        if (taskRetainerFragment == null) {
            taskRetainerFragment = new TaskRetainerFragment();
            fm.beginTransaction()
                    .add(taskRetainerFragment, TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT).commit();
        }   // else, Fragment already created and retained across configuration change
    }

    @Override
    protected void setAccount(Account account, boolean savedAccount) {
        Account[] accounts = mAccountManager.getAccountsByType(MainApp.getAccountType(this));
        if (accounts.length == 0) {
            Log_OC.i(TAG, "No ownCloud account is available");
            DialogNoAccount dialog = new DialogNoAccount();
            dialog.show(getSupportFragmentManager(), null);
        }

        if (!somethingToUpload()) {
            showErrorDialog(
                    R.string.uploader_error_message_no_file_to_upload,
                    R.string.uploader_error_title_no_file_to_upload
            );
        }

        super.setAccount(account, savedAccount);
    }

    private void showAccountChooserDialog() {
        MultipleAccountsDialog dialog = new MultipleAccountsDialog();
        dialog.show(getSupportFragmentManager(), null);
    }

    private Activity getActivity() {
        return this;
    }

    @Override
    public void onAccountChosen(@NonNull User user) {
        setAccount(user.toPlatformAccount(), false);
        initTargetFolder();
        populateDirectoryList();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mAccountManager.getAccountsByType(MainApp.getAccountType(this)).length == 0) {
            Toast.makeText(this,
                           String.format(getString(R.string.uploader_wrn_no_account_text),
                                         getString(R.string.app_name)),
                           Toast.LENGTH_LONG).show();
            return;
        }

        initTargetFolder();
        populateDirectoryList();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        Log_OC.d(TAG, "onSaveInstanceState() start");
        super.onSaveInstanceState(outState);
        outState.putString(KEY_PARENTS, generatePath(mParents));
        outState.putParcelable(KEY_FILE, mFile);
        if (getUser().isPresent()) {
            outState.putParcelable(FileActivity.EXTRA_USER, getUser().orElseThrow(RuntimeException::new));
        }

        Log_OC.d(TAG, "onSaveInstanceState() end");
    }

    @Override
    protected void onDestroy() {
        if (mSyncBroadcastReceiver != null) {
            localBroadcastManager.unregisterReceiver(mSyncBroadcastReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onSortingOrderChosen(FileSortOrder newSortOrder) {
        preferences.setSortOrder(mFile, newSortOrder);
        sortButton.setText(DisplayUtils.getSortOrderStringId(newSortOrder));
        populateDirectoryList();
    }

    public static class DialogNoAccount extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new Builder(getActivity());
            builder.setIcon(R.drawable.ic_warning);
            builder.setTitle(R.string.uploader_wrn_no_account_title);
            builder.setMessage(String.format(getString(R.string.uploader_wrn_no_account_text),
                    getString(R.string.app_name)));
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.uploader_wrn_no_account_setup_btn_text, (dialog, which) -> {
                // using string value since in API7 this
                // constant is not defined
                // in API7 < this constant is defined in
                // Settings.ADD_ACCOUNT_SETTINGS
                // and Settings.EXTRA_AUTHORITIES
                Intent intent = new Intent(android.provider.Settings.ACTION_ADD_ACCOUNT);
                intent.putExtra("authorities", new String[]{MainApp.getAuthTokenType()});
                startActivityForResult(intent, REQUEST_CODE__SETUP_ACCOUNT);
            });
            builder.setNeutralButton(R.string.uploader_wrn_no_account_quit_btn_text,
                                 (dialog, which) -> getActivity().finish());
            return builder.create();
        }
    }

    public static class DialogInputUploadFilename extends DialogFragment implements Injectable {
        private static final String KEY_SUBJECT_TEXT = "SUBJECT_TEXT";
        private static final String KEY_EXTRA_TEXT = "EXTRA_TEXT";

        private static final int CATEGORY_URL = 1;
        private static final int CATEGORY_MAPS_URL = 2;
        private static final int EXTRA_TEXT_LENGTH = 3;
        private static final int SINGLE_SPINNER_ENTRY = 1;

        private List<String> mFilenameBase;
        private List<String> mFilenameSuffix;
        private List<String> mText;
        private int mFileCategory;

        private Spinner mSpinner;
        @Inject AppPreferences preferences;
        @Inject ThemeColorUtils themeColorUtils;
        @Inject ThemeTextInputUtils themeTextInputUtils;

        public static DialogInputUploadFilename newInstance(String subjectText, String extraText) {
            DialogInputUploadFilename dialog = new DialogInputUploadFilename();
            Bundle args = new Bundle();
            args.putString(KEY_SUBJECT_TEXT, subjectText);
            args.putString(KEY_EXTRA_TEXT, extraText);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mFilenameBase = new ArrayList<>();
            mFilenameSuffix = new ArrayList<>();
            mText = new ArrayList<>();

            String subjectText = "";
            String extraText = "";
            if (getArguments() != null) {
                if (getArguments().getString(KEY_SUBJECT_TEXT) != null) {
                    subjectText = getArguments().getString(KEY_SUBJECT_TEXT);
                }
                if (getArguments().getString(KEY_EXTRA_TEXT) != null) {
                    extraText = getArguments().getString(KEY_EXTRA_TEXT);
                }
            }

            LayoutInflater layout = getLayoutInflater();
            View view = layout.inflate(R.layout.upload_file_dialog, null);

            ArrayAdapter<String> adapter
                    = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            int selectPos = 0;
            String filename = renameSafeFilename(subjectText);
            if (filename == null) {
                filename = "";
            }
            adapter.add(getString(R.string.upload_file_dialog_filetype_snippet_text));
            mText.add(extraText);
            mFilenameBase.add(filename);
            mFilenameSuffix.add(TEXT_FILE_SUFFIX);
            if (isIntentStartWithUrl(extraText)) {
                String str = getString(R.string.upload_file_dialog_filetype_internet_shortcut);
                mText.add(internetShortcutUrlText(extraText));
                mFilenameBase.add(filename);
                mFilenameSuffix.add(URL_FILE_SUFFIX);
                adapter.add(String.format(str,URL_FILE_SUFFIX));

                mText.add(internetShortcutWeblocText(extraText));
                mFilenameBase.add(filename);
                mFilenameSuffix.add(WEBLOC_FILE_SUFFIX);
                adapter.add(String.format(str,WEBLOC_FILE_SUFFIX));

                mText.add(internetShortcutDesktopText(extraText, filename));
                mFilenameBase.add(filename);
                mFilenameSuffix.add(DESKTOP_FILE_SUFFIX);
                adapter.add(String.format(str,DESKTOP_FILE_SUFFIX));

                selectPos = preferences.getUploadUrlFileExtensionUrlSelectedPos();
                mFileCategory = CATEGORY_URL;
            } else if (isIntentFromGoogleMap(subjectText, extraText)) {
                String str = getString(R.string.upload_file_dialog_filetype_googlemap_shortcut);
                String texts[] = extraText.split("\n");
                mText.add(internetShortcutUrlText(texts[2]));
                mFilenameBase.add(texts[0]);
                mFilenameSuffix.add(URL_FILE_SUFFIX);
                adapter.add(String.format(str,URL_FILE_SUFFIX));

                mText.add(internetShortcutWeblocText(texts[2]));
                mFilenameBase.add(texts[0]);
                mFilenameSuffix.add(WEBLOC_FILE_SUFFIX);
                adapter.add(String.format(str,WEBLOC_FILE_SUFFIX));

                mText.add(internetShortcutDesktopText(texts[2], texts[0]));
                mFilenameBase.add(texts[0]);
                mFilenameSuffix.add(DESKTOP_FILE_SUFFIX);
                adapter.add(String.format(str, DESKTOP_FILE_SUFFIX));

                selectPos = preferences.getUploadMapFileExtensionUrlSelectedPos();
                mFileCategory = CATEGORY_MAPS_URL;
            }

            final TextInputEditText userInput = view.findViewById(R.id.user_input);
            final TextInputLayout userInputContainer = view.findViewById(R.id.user_input_container);
            setFilename(userInput, selectPos);
            userInput.requestFocus();
            themeTextInputUtils.colorTextInput(userInputContainer,
                                               userInput,
                                               themeColorUtils.primaryColor(getContext()),
                                               themeColorUtils.primaryAccentColor(getContext()));

            final Spinner spinner = view.findViewById(R.id.file_type);
            setupSpinner(adapter, selectPos, userInput, spinner);
            if (adapter.getCount() == SINGLE_SPINNER_ENTRY) {
                view.findViewById(R.id.label_file_type).setVisibility(View.GONE);
                spinner.setVisibility(View.GONE);
            }
            mSpinner = spinner;

            Dialog filenameDialog = createFilenameDialog(view, userInput, spinner);
            if (filenameDialog.getWindow() != null) {
                filenameDialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }
            return filenameDialog;
        }

        private void setupSpinner(ArrayAdapter<String> adapter, int selectPos, final EditText userInput, Spinner spinner) {
            spinner.setAdapter(adapter);
            spinner.setSelection(selectPos, false);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView parent, View view, int position, long id) {
                    Spinner spinner = (Spinner) parent;
                    int selectPos = spinner.getSelectedItemPosition();
                    setFilename(userInput, selectPos);
                    saveSelection(selectPos);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // nothing to do
                }
            });
        }

        @NonNull
        private Dialog createFilenameDialog(View view, final EditText userInput, final Spinner spinner) {
            Builder builder = new Builder(requireActivity());
            builder.setView(view);
            builder.setTitle(R.string.upload_file_dialog_title);
            builder.setPositiveButton(R.string.common_ok, (dialog, id) -> {
                int selectPos = spinner.getSelectedItemPosition();

                // verify if file name has suffix
                String filename = userInput.getText().toString();
                String suffix = mFilenameSuffix.get(selectPos);
                if (!filename.endsWith(suffix)) {
                    filename += suffix;
                }

                File file = createTempFile(mText.get(selectPos));

                if (file == null) {
                    getActivity().finish();
                } else {
                    String tmpName = file.getAbsolutePath();

                    ((ReceiveExternalFilesActivity) getActivity()).uploadFile(tmpName, filename);
                }
            });
            builder.setNeutralButton(R.string.common_cancel, (dialog, id) -> dialog.cancel());

            return builder.create();
        }

        public void onPause() {
            hideSpinnerDropDown(mSpinner);
            super.onPause();
        }

        private void saveSelection(int selectPos) {
            switch (mFileCategory) {
                case CATEGORY_URL:
                    preferences.setUploadUrlFileExtensionUrlSelectedPos(selectPos);
                    break;
                case CATEGORY_MAPS_URL:
                    preferences.setUploadMapFileExtensionUrlSelectedPos(selectPos);
                    break;
                default:
                    Log_OC.d(TAG, "Simple text snippet only: no selection to be persisted");
                    break;
            }
        }

        private void hideSpinnerDropDown(Spinner spinner) {
            try {
                Method method = Spinner.class.getDeclaredMethod("onDetachedFromWindow");
                method.setAccessible(true);
                method.invoke(spinner);
            } catch (Exception e) {
                Log_OC.e(TAG, "onDetachedFromWindow", e);
            }
        }

        private void setFilename(EditText inputText, int selectPos)
        {
            String filename = mFilenameBase.get(selectPos) + mFilenameSuffix.get(selectPos);
            inputText.setText(filename);
            int selectionStart = 0;
            int extensionStart = filename.lastIndexOf('.');
            int selectionEnd = extensionStart >= 0 ? extensionStart : filename.length();
            if (selectionEnd >= 0) {
                inputText.setSelection(
                        Math.min(selectionStart, selectionEnd),
                        Math.max(selectionStart, selectionEnd));
            }
        }

        private boolean isIntentFromGoogleMap(String subjectText, String extraText) {
            String texts[] = extraText.split("\n");
            if (texts.length != EXTRA_TEXT_LENGTH) {
                return false;
            }

            if (texts[0].length() == 0 || !subjectText.equals(texts[0])) {
                return false;
            }

            return texts[2].startsWith("https://goo.gl/maps/");
        }

        private boolean isIntentStartWithUrl(String extraText) {
            return extraText.startsWith("http://") || extraText.startsWith("https://");
        }

        @Nullable
        private String renameSafeFilename(String filename) {
            String safeFilename = filename;
            safeFilename = safeFilename.replaceAll("[?]", "_");
            safeFilename = safeFilename.replaceAll("\"", "_");
            safeFilename = safeFilename.replaceAll("/", "_");
            safeFilename = safeFilename.replaceAll("<", "_");
            safeFilename = safeFilename.replaceAll(">", "_");
            safeFilename = safeFilename.replaceAll("[*]", "_");
            safeFilename = safeFilename.replaceAll("[|]", "_");
            safeFilename = safeFilename.replaceAll(";", "_");
            safeFilename = safeFilename.replaceAll("=", "_");
            safeFilename = safeFilename.replaceAll(",", "_");

            int maxLength = 128;
            if (safeFilename.getBytes(FILENAME_ENCODING).length > maxLength) {
                safeFilename = new String(safeFilename.getBytes(FILENAME_ENCODING), 0, maxLength, FILENAME_ENCODING);
            }
            return safeFilename;
        }

        private String internetShortcutUrlText(String url) {
            return "[InternetShortcut]\r\n" +
                    "URL=" + url + "\r\n";
        }

        private String internetShortcutWeblocText(String url) {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
                    "<plist version=\"1.0\">\n" +
                    "<dict>\n" +
                    "<key>URL</key>\n" +
                    "<string>" + url + "</string>\n" +
                    "</dict>\n" +
                    "</plist>\n";
        }

        private String internetShortcutDesktopText(String url, String filename) {
            return "[Desktop Entry]\n" +
                "Encoding=UTF-8\n" +
                "Name=" + filename + "\n" +
                "Type=Link\n" +
                "URL=" + url + "\n" +
                "Icon=text-html";
        }

        @Nullable
        private File createTempFile(String text) {
            File file = new File(getActivity().getCacheDir(), "tmp.tmp");
            FileWriter fw = null;
            try {
                fw = new FileWriter(file);
                fw.write(text);
            } catch (IOException e) {
                Log_OC.d(TAG, "Error ", e);
                return null;
            } finally {
                if (fw != null) {
                    try {
                        fw.close();
                    } catch (IOException e) {
                        Log_OC.d(TAG, "Error closing file writer ", e);
                    }
                }
            }
            return file;
        }
    }

    @Override
    public void onBackPressed() {
        if (mParents.size() <= SINGLE_PARENT) {
            super.onBackPressed();
        } else {
            mParents.pop();
            String full_path = generatePath(mParents);
            startSyncFolderOperation(getStorageManager().getFileByPath(full_path));
            populateDirectoryList();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // click on folder in the list
        Log_OC.d(TAG, "on item click");
        List<OCFile> tmpFiles = getStorageManager().getFolderContent(mFile, false);
        tmpFiles = sortFileList(tmpFiles);

        if (tmpFiles.isEmpty()) {
            return;
        }
        // filter on dirtype
        Vector<OCFile> files = new Vector<>();
        files.addAll(tmpFiles);

        if (files.size() < position) {
            throw new IndexOutOfBoundsException("Incorrect item selected");
        }
        if (files.get(position).isFolder()){
            OCFile folderToEnter = files.get(position);
            startSyncFolderOperation(folderToEnter);
            mParents.push(folderToEnter.getFileName());
            populateDirectoryList();
        }
    }

    @Override
    public void onClick(View v) {
        // click on button
        int id = v.getId();

        if (id == R.id.uploader_choose_folder) {
            mUploadPath = "";   // first element in mParents is root dir, represented by "";
            // init mUploadPath with "/" results in a "//" prefix
            for (String p : mParents) {
                mUploadPath += p + OCFile.PATH_SEPARATOR;
            }

            if (mUploadFromTmpFile) {
                DialogInputUploadFilename dialog = DialogInputUploadFilename.newInstance(mSubjectText, mExtraText);
                dialog.show(getSupportFragmentManager(), null);
            } else {
                Log_OC.d(TAG, "Uploading file to dir " + mUploadPath);
                uploadFiles();
            }
        } else if (id == R.id.uploader_cancel) {
            finish();
        } else {
            throw new IllegalArgumentException("Wrong element clicked");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log_OC.i(TAG, "result received. req: " + requestCode + " res: " + resultCode);
        if (requestCode == REQUEST_CODE__SETUP_ACCOUNT) {
            if (resultCode == RESULT_CANCELED) {
                finish();
            }
            Account[] accounts = mAccountManager.getAccountsByType(MainApp.getAuthTokenType());
            if (accounts.length == 0) {
                DialogNoAccount dialog = new DialogNoAccount();
                dialog.show(getSupportFragmentManager(), null);
            } else {
                // there is no need for checking for is there more then one
                // account at this point
                // since account setup can set only one account at time
                setAccount(accounts[0], false);
                populateDirectoryList();
            }
        }
    }

    private void setupActionBarSubtitle() {
        ActionBar actionBar = getSupportActionBar();

        if (isHaveMultipleAccount()) {
            themeToolbarUtils.setColoredSubtitle(actionBar, getAccount().name, this);
        } else if (actionBar != null) {
            actionBar.setSubtitle(null);
        }
    }

    private void populateDirectoryList() {
        setupEmptyList();
        setupToolbar();
        ActionBar actionBar = getSupportActionBar();
        setupActionBarSubtitle();

        binding.toolbarLayout.sortListButtonGroup.setVisibility(View.VISIBLE);
        binding.toolbarLayout.switchGridViewButton.setVisibility(View.GONE);

        String current_dir = mParents.peek();
        boolean notRoot = mParents.size() > 1;

        if (actionBar != null) {
            if (TextUtils.isEmpty(current_dir)) {
                themeToolbarUtils.setColoredTitle(actionBar, R.string.uploader_top_message, this);
            } else {
                themeToolbarUtils.setColoredTitle(actionBar, current_dir, this);
            }

            actionBar.setDisplayHomeAsUpEnabled(notRoot);
            actionBar.setHomeButtonEnabled(notRoot);
        }

        String full_path = generatePath(mParents);

        Log_OC.d(TAG, "Populating view with content of : " + full_path);

        mFile = getStorageManager().getFileByPath(full_path);
        if (mFile != null) {
            List<OCFile> files = getStorageManager().getFolderContent(mFile, false);

            if (files.isEmpty()) {
                setMessageForEmptyList(R.string.file_list_empty_headline, R.string.empty,
                        R.drawable.uploads);
            } else {
                mEmptyListContainer.setVisibility(View.GONE);

                files = sortFileList(files);

                List<Map<String, Object>> data = new LinkedList<>();
                for (OCFile f : files) {
                    Map<String, Object> h = new HashMap<>();
                    h.put("dirname", f);
                    data.add(h);
                }

                UploaderAdapter sa = new UploaderAdapter(this,
                                                         data,
                                                         R.layout.uploader_list_item_layout,
                                                         new String[]{"dirname"},
                                                         new int[]{R.id.filename},
                                                         getStorageManager(),
                                                         getUser().get(),
                                                         themeColorUtils,
                                                         themeDrawableUtils);

                binding.list.setAdapter(sa);
            }
            MaterialButton btnChooseFolder = binding.uploaderChooseFolder;
            themeButtonUtils.colorPrimaryButton(btnChooseFolder, this, themeColorUtils);
            btnChooseFolder.setOnClickListener(this);

            if (mFile.canWrite()) {
                btnChooseFolder.setEnabled(true);
                themeButtonUtils.colorPrimaryButton(btnChooseFolder, this, themeColorUtils);
            } else {
                btnChooseFolder.setEnabled(false);
                btnChooseFolder.setBackgroundColor(Color.GRAY);
            }

            themeToolbarUtils.colorStatusBar(this);

            themeToolbarUtils.tintBackButton(actionBar, this);

            Button btnNewFolder = binding.uploaderCancel;
            btnNewFolder.setTextColor(themeColorUtils.primaryColor(this, true));
            btnNewFolder.setOnClickListener(this);

            binding.list.setOnItemClickListener(this);

            sortButton = binding.toolbarLayout.sortButton;
            FileSortOrder sortOrder = preferences.getSortOrderByFolder(mFile);
            sortButton.setText(DisplayUtils.getSortOrderStringId(sortOrder));
            sortButton.setOnClickListener(l -> openSortingOrderDialogFragment(getSupportFragmentManager(), sortOrder));
        }
    }

    protected void setupEmptyList() {
        mEmptyListContainer = binding.emptyView.emptyListView;
        mEmptyListMessage = binding.emptyView.emptyListViewText;
        mEmptyListHeadline = binding.emptyView.emptyListViewHeadline;
        mEmptyListIcon = binding.emptyView.emptyListIcon;
    }

    public void setMessageForEmptyList(@StringRes final int headline, @StringRes final int message,
                                       @DrawableRes final int icon) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mEmptyListContainer != null && mEmptyListMessage != null) {
                mEmptyListHeadline.setText(headline);
                mEmptyListMessage.setText(message);
                mEmptyListIcon.setImageDrawable(
                    themeDrawableUtils.tintDrawable(icon, themeColorUtils.primaryColor(this, true)));
                mEmptyListIcon.setVisibility(View.VISIBLE);
                mEmptyListMessage.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onSavedCertificate() {
        startSyncFolderOperation(getCurrentDir());
    }

    private void startSyncFolderOperation(OCFile folder) {
        long currentSyncTime = System.currentTimeMillis();

        mSyncInProgress = true;

        // perform folder synchronization
        RemoteOperation syncFolderOp = new RefreshFolderOperation(folder,
                                                                        currentSyncTime,
                                                                        false,
                                                                        false,
                                                                        getStorageManager(),
                                                                        getUser().orElseThrow(RuntimeException::new),
                                                                        getApplicationContext()
                                                                      );
        syncFolderOp.execute(getAccount(), this, null, null);
    }

    private List<OCFile> sortFileList(List<OCFile> files) {
        FileSortOrder sortOrder = preferences.getSortOrderByFolder(mFile);
        return sortOrder.sortCloudFiles(files);
    }

    private String generatePath(Stack<String> dirs) {
        String full_path = "";

        for (String a : dirs) {
            full_path += a + OCFile.PATH_SEPARATOR;
        }
        return full_path;
    }

    private void prepareStreamsToUpload() {
        Intent intent = getIntent();

        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            mStreamsToUpload = new ArrayList<>();
            mStreamsToUpload.add(intent.getParcelableExtra(Intent.EXTRA_STREAM));
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            mStreamsToUpload = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        }

        if (mStreamsToUpload == null || mStreamsToUpload.isEmpty() || mStreamsToUpload.get(0) == null) {
            mStreamsToUpload = null;
            saveTextsFromIntent(intent);
        }
    }

    private void saveTextsFromIntent(Intent intent) {
        if (!MimeType.TEXT_PLAIN.equals(intent.getType())) {
            return;
        }
        mUploadFromTmpFile = true;

        mSubjectText = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (mSubjectText == null) {
            mSubjectText = intent.getStringExtra(Intent.EXTRA_TITLE);
            if (mSubjectText == null) {
                mSubjectText = DateFormat.format("yyyyMMdd_kkmmss", Calendar.getInstance()).toString();
            }
        }
        mExtraText = intent.getStringExtra(Intent.EXTRA_TEXT);
    }

    private boolean somethingToUpload() {
        return (mStreamsToUpload != null && mStreamsToUpload.size() > 0 && mStreamsToUpload.get(0) != null ||
                mUploadFromTmpFile);
    }

    public void uploadFile(String tmpName, String filename) {
        FileUploader.uploadNewFile(
                getBaseContext(),
                getUser().orElseThrow(RuntimeException::new),
                tmpName,
                mFile.getRemotePath() + filename,
                FileUploader.LOCAL_BEHAVIOUR_COPY,
                null,
                true,
                UploadFileOperation.CREATED_BY_USER,
                false,
                false,
                NameCollisionPolicy.ASK_USER
        );
        finish();
    }

    public void uploadFiles() {

        UriUploader uploader = new UriUploader(
            this,
            mStreamsToUpload,
            mUploadPath,
            getUser().orElseThrow(RuntimeException::new),
            FileUploader.LOCAL_BEHAVIOUR_DELETE,
            true, // Show waiting dialog while file is being copied from private storage
            this  // Copy temp task listener
        );

        UriUploader.UriUploaderResultCode resultCode = uploader.uploadUris();

        // Save the path to shared preferences; even if upload is not possible, user chose the folder
        preferences.setLastUploadPath(mUploadPath);

        if (resultCode == UriUploader.UriUploaderResultCode.OK) {
            finish();
        } else {

            int messageResTitle = R.string.uploader_error_title_file_cannot_be_uploaded;
            int messageResId = R.string.common_error_unknown;

            if (resultCode == UriUploader.UriUploaderResultCode.ERROR_NO_FILE_TO_UPLOAD) {
                messageResId = R.string.uploader_error_message_no_file_to_upload;
                messageResTitle = R.string.uploader_error_title_no_file_to_upload;
            } else if (resultCode == UriUploader.UriUploaderResultCode.ERROR_READ_PERMISSION_NOT_GRANTED) {
                messageResId = R.string.uploader_error_message_read_permission_not_granted;
            } else if (resultCode == UriUploader.UriUploaderResultCode.ERROR_UNKNOWN) {
                messageResId = R.string.common_error_unknown;
            }

            showErrorDialog(
                    messageResId,
                    messageResTitle
            );
        }
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        super.onRemoteOperationFinish(operation, result);


        if (operation instanceof CreateFolderOperation) {
            onCreateFolderOperationFinish((CreateFolderOperation) operation, result);
        }

    }

    /**
     * Updates the view associated to the activity after the finish of an operation
     * trying create a new folder
     *
     * @param operation Creation operation performed.
     * @param result    Result of the creation.
     */
    private void onCreateFolderOperationFinish(CreateFolderOperation operation,
                                               RemoteOperationResult result) {
        if (result.isSuccess()) {
            String remotePath = operation.getRemotePath().substring(0, operation.getRemotePath().length() - 1);
            String newFolder = remotePath.substring(remotePath.lastIndexOf('/') + 1);
            mParents.push(newFolder);
            populateDirectoryList();
        } else {
            try {
                DisplayUtils.showSnackMessage(
                        this, ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources())
                );

            } catch (NotFoundException e) {
                Log_OC.e(TAG, "Error while trying to show fail message ", e);
            }
        }
    }


    /**
     * Loads the target folder initialize shown to the user.
     * <p/>
     * The target account has to be chosen before this method is called.
     */
    private void initTargetFolder() {
        if (getStorageManager() == null) {
            throw new IllegalStateException("Do not call this method before initializing mStorageManager");
        }

        if (mParents.empty()) {
            String lastPath = preferences.getLastUploadPath();
            // "/" equals root-directory
            if (OCFile.ROOT_PATH.equals(lastPath)) {
                mParents.add("");
            } else {
                String[] dir_names = lastPath.split(OCFile.PATH_SEPARATOR);
                mParents.clear();
                mParents.addAll(Arrays.asList(dir_names));
            }
        }

        // make sure that path still exists, if it doesn't pop the stack and try the previous path
        while (!getStorageManager().fileExists(generatePath(mParents)) && mParents.size() > 1) {
            mParents.pop();
        }
    }

    private boolean isHaveMultipleAccount() {
        return mAccountManager.getAccountsByType(MainApp.getAccountType(this)).length > 1;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_receive_external_files, menu);

        if (!isHaveMultipleAccount()) {
            MenuItem switchAccountMenu = menu.findItem(R.id.action_switch_account);
            switchAccountMenu.setVisible(false);
        }

        // tint search event
        final MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);

        MenuItem newFolderMenuItem = menu.findItem(R.id.action_create_dir);
        newFolderMenuItem.setEnabled(mFile.canWrite());

        // hacky as no default way is provided
        themeToolbarUtils.themeSearchView(searchView, this);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        int itemId = item.getItemId();

        if (itemId == R.id.action_create_dir) {
            CreateFolderDialogFragment dialog = CreateFolderDialogFragment.newInstance(mFile);
            dialog.show(getSupportFragmentManager(), CreateFolderDialogFragment.CREATE_FOLDER_FRAGMENT);
        } else if (itemId == android.R.id.home) {
            if (mParents.size() > SINGLE_PARENT) {
                onBackPressed();
            }
        } else if (itemId == R.id.action_switch_account) {
            showAccountChooserDialog();
        } else {
            retval = super.onOptionsItemSelected(item);
        }

        return retval;
    }

    private OCFile getCurrentFolder(){
        OCFile file = mFile;
        if (file != null) {
            if (file.isFolder()) {
                return file;
            } else if (getStorageManager() != null) {
                return getStorageManager().getFileByPath(file.getParentRemotePath());
            }
        }
        return null;
    }

    private void browseToRoot() {
        OCFile root = getStorageManager().getFileByPath(OCFile.ROOT_PATH);
        mFile = root;
        startSyncFolderOperation(root);
    }

    private class SyncBroadcastReceiver extends BroadcastReceiver {

        /**
         * {@link BroadcastReceiver} to enable syncing feedback in UI
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String event = intent.getAction();
                Log_OC.d(TAG, "Received broadcast " + event);
                String accountName = intent.getStringExtra(FileSyncAdapter.EXTRA_ACCOUNT_NAME);
                String syncFolderRemotePath = intent.getStringExtra(FileSyncAdapter.EXTRA_FOLDER_PATH);
                RemoteOperationResult syncResult = (RemoteOperationResult)
                        DataHolderUtil.getInstance().retrieve(intent.getStringExtra(FileSyncAdapter.EXTRA_RESULT));
                boolean sameAccount = getAccount() != null && accountName.equals(getAccount().name)
                        && getStorageManager() != null;

                if (sameAccount) {

                    if (FileSyncAdapter.EVENT_FULL_SYNC_START.equals(event)) {
                        mSyncInProgress = true;

                    } else {
                        OCFile currentFile = (mFile == null) ? null :
                            getStorageManager().getFileByPath(mFile.getRemotePath());
                        OCFile currentDir = (getCurrentFolder() == null) ? null :
                            getStorageManager().getFileByPath(getCurrentFolder().getRemotePath());

                        if (currentDir == null) {
                            // current folder was removed from the server
                            DisplayUtils.showSnackMessage(
                                    getActivity(),
                                    R.string.sync_current_folder_was_removed,
                                    getCurrentFolder().getFileName()
                            );
                            browseToRoot();

                        } else {
                            if (currentFile == null && !mFile.isFolder()) {
                                // currently selected file was removed in the server, and now we know it
                                currentFile = currentDir;
                            }

                            if (currentDir.getRemotePath().equals(syncFolderRemotePath)) {
                                populateDirectoryList();
                            }
                            mFile = currentFile;
                        }

                        mSyncInProgress = !FileSyncAdapter.EVENT_FULL_SYNC_END.equals(event) &&
                                !RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED.equals(event);

                        if (RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED.equals(event)
                                /// TODO refactor and make common
                                && syncResult != null && !syncResult.isSuccess()) {

                            if (syncResult.getCode() == ResultCode.UNAUTHORIZED ||
                                (syncResult.isException() && syncResult.getException()
                                    instanceof AuthenticatorException)) {

                                requestCredentialsUpdate(context);

                            } else if (ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED == syncResult.getCode()) {

                                showUntrustedCertDialog(syncResult);
                            }
                        }
                    }
                    Log_OC.d(TAG, "Setting progress visibility to " + mSyncInProgress);

                }
            } catch (RuntimeException e) {
                // avoid app crashes after changing the serial id of RemoteOperationResult
                // in owncloud library with broadcast notifications pending to process
                DataHolderUtil.getInstance().delete(intent.getStringExtra(FileSyncAdapter.EXTRA_RESULT));
            }
        }
    }

    /**
     * Process the result of CopyAndUploadContentUrisTask
     */
    @Override
    public void onTmpFilesCopied(ResultCode result) {
        dismissLoadingDialog();
        finish();
    }

    /**
     * Show an error dialog, forcing the user to click a single button to exit the activity
     *
     * @param messageResId      Resource id of the message to show in the dialog.
     * @param messageResTitle   Resource id of the title to show in the dialog. 0 to show default alert message.
     *                          -1 to show no title.
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
                    // not used at the moment
                }

                @Override
                public void onCancel(String callerTag) {
                    // not used at the moment
                }
            }
        );
        errorDialog.show(getSupportFragmentManager(), FTAG_ERROR_FRAGMENT);
    }
}

