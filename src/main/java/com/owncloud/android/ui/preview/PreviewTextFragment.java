/*
 *   ownCloud Android client application
 *
 *   Copyright (C) 2016 ownCloud Inc.
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

package com.owncloud.android.ui.preview;

import android.accounts.Account;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.StringUtils;

import org.mozilla.universalchardet.ReaderFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;

public class PreviewTextFragment extends FileFragment implements SearchView.OnQueryTextListener, Injectable {
    private static final String EXTRA_FILE = "FILE";
    private static final String EXTRA_ACCOUNT = "ACCOUNT";
    private static final String TAG = PreviewTextFragment.class.getSimpleName();

    private Account mAccount;
    private TextView mTextPreview;
    private TextLoadAsyncTask mTextLoadTask;

    private String mOriginalText;

    private Handler mHandler;
    private SearchView mSearchView;
    private RelativeLayout mMultiView;

    private TextView mMultiListMessage;
    private TextView mMultiListHeadline;
    private ImageView mMultiListIcon;
    private ProgressBar mMultiListProgress;


    private String mSearchQuery = "";
    private boolean mSearchOpen;

    @Inject UserAccountManager accountManager;

    /**
     * Creates an empty fragment for previews.
     *
     * MUST BE KEPT: the system uses it when tries to re-instantiate a fragment automatically
     * (for instance, when the device is turned a aside).
     *
     * DO NOT CALL IT: an {@link OCFile} and {@link Account} must be provided for a successful
     * construction
     */
    public PreviewTextFragment() {
        super();
        mAccount = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log_OC.e(TAG, "onCreateView");


        View ret = inflater.inflate(R.layout.text_file_preview, container, false);
        mTextPreview = ret.findViewById(R.id.text_preview);

        mTextPreview = ret.findViewById(R.id.text_preview);

        mMultiView = ret.findViewById(R.id.multi_view);

        setupMultiView(ret);
        setMultiListLoadingMessage();

        return ret;
    }

    private void setupMultiView(View view) {
        mMultiListMessage = view.findViewById(R.id.empty_list_view_text);
        mMultiListHeadline = view.findViewById(R.id.empty_list_view_headline);
        mMultiListIcon = view.findViewById(R.id.empty_list_icon);
        mMultiListProgress = view.findViewById(R.id.empty_list_progress);
    }

    private void setMultiListLoadingMessage() {
        if (mMultiView != null) {
            mMultiListHeadline.setText(R.string.file_list_loading);
            mMultiListMessage.setText("");

            mMultiListIcon.setVisibility(View.GONE);
            mMultiListProgress.setVisibility(View.VISIBLE);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        OCFile file = getFile();

        Bundle args = getArguments();

        if (file == null) {
            file = args.getParcelable(FileDisplayActivity.EXTRA_FILE);
        }

        if (mAccount == null) {
            mAccount = args.getParcelable(FileDisplayActivity.EXTRA_ACCOUNT);
        }

        if (args.containsKey(FileDisplayActivity.EXTRA_SEARCH_QUERY)) {
            mSearchQuery = args.getString(FileDisplayActivity.EXTRA_SEARCH_QUERY);
        }
        mSearchOpen = args.getBoolean(FileDisplayActivity.EXTRA_SEARCH, false);

        if (savedInstanceState == null) {
            if (file == null) {
                throw new IllegalStateException("Instanced with a NULL OCFile");
            }
            if (mAccount == null) {
                throw new IllegalStateException("Instanced with a NULL ownCloud Account");
            }
        } else {
            file = savedInstanceState.getParcelable(EXTRA_FILE);
            mAccount = savedInstanceState.getParcelable(EXTRA_ACCOUNT);
        }

        mHandler = new Handler();
        setFile(file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(PreviewTextFragment.EXTRA_FILE, getFile());
        outState.putParcelable(PreviewTextFragment.EXTRA_ACCOUNT, mAccount);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log_OC.e(TAG, "onStart");

        loadAndShowTextPreview();
    }

    private void loadAndShowTextPreview() {
        mTextLoadTask = new TextLoadAsyncTask(new WeakReference<>(mTextPreview));
        mTextLoadTask.execute(getFile().getStoragePath());
    }


    @Override
    public boolean onQueryTextSubmit(String query) {
        performSearch(query, 0);
        return true;
    }

    @Override
    public boolean onQueryTextChange(final String newText) {
        performSearch(newText, 500);
        return true;
    }


    private void performSearch(final String query, int delay) {
        mHandler.removeCallbacksAndMessages(null);

        if (mOriginalText != null) {
            if (getActivity() instanceof FileDisplayActivity) {
                FileDisplayActivity fileDisplayActivity = (FileDisplayActivity) getActivity();
                fileDisplayActivity.setSearchQuery(query);
            }
            mHandler.postDelayed(() -> {
                if (query != null && !query.isEmpty()) {
                    if (getContext() != null && getContext().getResources() != null) {
                        String coloredText = StringUtils.searchAndColor(mOriginalText, query,
                            getContext().getResources().getColor(R.color.primary));
                        mTextPreview.setText(Html.fromHtml(coloredText.replace("\n", "<br \\>")));
                    }
                } else {
                    mTextPreview.setText(mOriginalText);
                }
            }, delay);
        }

        if (delay == 0 && mSearchView != null) {
            mSearchView.clearFocus();
        }
    }

    /**
     * Reads the file to preview and shows its contents. Too critical to be anonymous.
     */
    private class TextLoadAsyncTask extends AsyncTask<Object, Void, StringWriter> {
        private static final int PARAMS_LENGTH = 1;
        private final WeakReference<TextView> mTextViewReference;

        private TextLoadAsyncTask(WeakReference<TextView> textView) {
            mTextViewReference = textView;
        }

        @Override
        protected void onPreExecute() {
            // not used at the moment
        }

        @Override
        protected StringWriter doInBackground(Object... params) {
            if (params.length != PARAMS_LENGTH) {
                throw new IllegalArgumentException("The parameter to " + TextLoadAsyncTask.class.getName()
                        + " must be (1) the file location");
            }
            String location = (String) params[0];

            Scanner scanner = null;
            StringWriter source = new StringWriter();
            BufferedWriter bufferedWriter = new BufferedWriter(source);
            Reader reader = null;

            try {
                File file = new File(location);
                reader = ReaderFactory.createReaderFromFile(file);
                scanner = new Scanner(reader);

                while (scanner.hasNextLine()) {
                    bufferedWriter.append(scanner.nextLine());
                    if (scanner.hasNextLine()) {
                        bufferedWriter.append("\n");
                    }
                }
                bufferedWriter.close();
                IOException exc = scanner.ioException();
                if (exc != null) {
                    throw exc;
                }
            } catch (IOException e) {
                Log_OC.e(TAG, e.getMessage(), e);
                finish();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log_OC.e(TAG, e.getMessage(), e);
                        finish();
                    }
                }
                if (scanner != null) {
                    scanner.close();
                }
            }
            return source;
        }

        @Override
        protected void onPostExecute(final StringWriter stringWriter) {
            final TextView textView = mTextViewReference.get();

            if (textView != null) {
                mOriginalText = stringWriter.toString();
                mSearchView.setOnQueryTextListener(PreviewTextFragment.this);
                textView.setText(mOriginalText);

                if (mSearchOpen) {
                    mSearchView.setQuery(mSearchQuery, true);
                }
                textView.setVisibility(View.VISIBLE);
            }

            if (mMultiView != null) {
                mMultiView.setVisibility(View.GONE);
            }

        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.file_actions_menu, menu);

        MenuItem menuItem = menu.findItem(R.id.action_search);
        menuItem.setVisible(true);
        mSearchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        mSearchView.setMaxWidth(Integer.MAX_VALUE);

        if (mSearchOpen) {
            mSearchView.setIconified(false);
            mSearchView.setQuery(mSearchQuery, false);
            mSearchView.clearFocus();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (containerActivity.getStorageManager() != null) {
            Account currentAccount = containerActivity.getStorageManager().getAccount();
            FileMenuFilter mf = new FileMenuFilter(
                    getFile(),
                    currentAccount,
                containerActivity,
                    getActivity(),
                    false
            );
            mf.filter(menu,
                      true,
                      accountManager.isMediaStreamingSupported(currentAccount));
        }

        // additional restriction for this fragment
        FileMenuFilter.hideMenuItems(
                menu.findItem(R.id.action_rename_file),
                menu.findItem(R.id.action_select_all),
                menu.findItem(R.id.action_move),
                menu.findItem(R.id.action_download_file),
                menu.findItem(R.id.action_sync_file),
                menu.findItem(R.id.action_sync_account),
                menu.findItem(R.id.action_favorite),
                menu.findItem(R.id.action_unset_favorite)
        );

        Boolean dualPane = getResources().getBoolean(R.bool.large_land_layout);

        if (!dualPane) {
            FileMenuFilter.hideMenuItems(menu.findItem(R.id.action_switch_view),
                    menu.findItem(R.id.action_sort)
            );
        }

        if(getFile().isSharedWithMe() && !getFile().canReshare()){
            FileMenuFilter.hideMenuItem(menu.findItem(R.id.action_send_share_file));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send_share_file: {
                if(getFile().isSharedWithMe() && !getFile().canReshare()){
                    DisplayUtils.showSnackMessage(getView(), R.string.resharing_is_not_allowed);
                } else {
                    containerActivity.getFileOperationsHelper().sendShareFile(getFile());
                }
                return true;
            }
            case R.id.action_open_file_with: {
                openFile();
                return true;
            }
            case R.id.action_remove_file: {
                RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(getFile());
                dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
                return true;
            }
            case R.id.action_see_details: {
                seeDetails();
                return true;
            }
            case R.id.action_sync_file: {
                containerActivity.getFileOperationsHelper().syncFile(getFile());
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Update the file of the fragment with file value
     *
     * @param file The new file to set
     */
    public void updateFile(OCFile file) {
        setFile(file);
    }

    private void seeDetails() {
        containerActivity.showDetails(getFile());
    }

    @Override
    public void onStop() {
        super.onStop();
        Log_OC.e(TAG, "onStop");
        if (mTextLoadTask != null) {
            mTextLoadTask.cancel(Boolean.TRUE);
        }
    }

    /**
     * Opens the previewed file with an external application.
     */
    private void openFile() {
        containerActivity.getFileOperationsHelper().openFile(getFile());
        finish();
    }

    /**
     * Helper method to test if an {@link OCFile} can be passed to a {@link PreviewTextFragment} to be previewed.
     *
     * @param file File to test if can be previewed.
     * @return 'True' if the file can be handled by the fragment.
     */
    public static boolean canBePreviewed(OCFile file) {
        final List<String> unsupportedTypes = new LinkedList<>();
        unsupportedTypes.add("text/richtext");
        unsupportedTypes.add("text/rtf");
        unsupportedTypes.add("text/calendar");
        unsupportedTypes.add("text/vnd.abc");
        unsupportedTypes.add("text/vnd.fmi.flexstor");
        unsupportedTypes.add("text/vnd.rn-realtext");
        unsupportedTypes.add("text/vnd.wap.wml");
        unsupportedTypes.add("text/vnd.wap.wmlscript");
        return file != null && file.isDown() && MimeTypeUtil.isText(file) &&
                !unsupportedTypes.contains(file.getMimeType()) &&
                !unsupportedTypes.contains(MimeTypeUtil.getMimeTypeFromPath(file.getRemotePath()));
    }

    /**
     * Finishes the preview
     */
    private void finish() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().onBackPressed();
            }
        });
    }
}
