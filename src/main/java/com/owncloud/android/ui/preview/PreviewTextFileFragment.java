/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.preview;

import android.accounts.Account;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;

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

public class PreviewTextFileFragment extends PreviewTextFragment {
    private static final String EXTRA_FILE = "FILE";
    private static final String EXTRA_ACCOUNT = "ACCOUNT";
    private static final String TAG = PreviewTextFileFragment.class.getSimpleName();

    private TextLoadAsyncTask textLoadAsyncTask;
    private Account account;

    @Inject UserAccountManager accountManager;

    /**
     * Creates an empty fragment for previews.
     * <p>
     * MUST BE KEPT: the system uses it when tries to re-instantiate a fragment automatically (for instance, when the
     * device is turned a aside).
     * <p>
     * DO NOT CALL IT: an {@link OCFile} and {@link Account} must be provided for a successful construction
     */
    public PreviewTextFileFragment() {
        super();
        account = null;
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

        if (account == null) {
            account = args.getParcelable(FileDisplayActivity.EXTRA_ACCOUNT);
        }

        if (args.containsKey(FileDisplayActivity.EXTRA_SEARCH_QUERY)) {
            mSearchQuery = args.getString(FileDisplayActivity.EXTRA_SEARCH_QUERY);
        }
        mSearchOpen = args.getBoolean(FileDisplayActivity.EXTRA_SEARCH, false);

        if (savedInstanceState == null) {
            if (file == null) {
                throw new IllegalStateException("Instanced with a NULL OCFile");
            }
            if (account == null) {
                throw new IllegalStateException("Instanced with a NULL ownCloud Account");
            }
        } else {
            file = savedInstanceState.getParcelable(EXTRA_FILE);
            account = savedInstanceState.getParcelable(EXTRA_ACCOUNT);
        }

        mHandler = new Handler();
        setFile(file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(PreviewTextFileFragment.EXTRA_FILE, getFile());
        outState.putParcelable(PreviewTextFileFragment.EXTRA_ACCOUNT, account);

        super.onSaveInstanceState(outState);
    }

    @Override
    void loadAndShowTextPreview() {
        textLoadAsyncTask = new TextLoadAsyncTask(new WeakReference<>(mTextPreview));
        textLoadAsyncTask.execute(getFile().getStoragePath());
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
                setText(textView, mOriginalText, getFile(), requireActivity());

                if (mSearchView != null) {
                    mSearchView.setOnQueryTextListener(PreviewTextFileFragment.this);

                    if (mSearchOpen) {
                        mSearchView.setQuery(mSearchQuery, true);
                    }
                }

                textView.setVisibility(View.VISIBLE);
            }

            if (mMultiListContainer != null) {
                mMultiListContainer.setVisibility(View.GONE);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.item_file, menu);

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
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (containerActivity.getStorageManager() != null) {
            User user = accountManager.getUser();
            FileMenuFilter mf = new FileMenuFilter(
                getFile(),
                containerActivity,
                getActivity(),
                false,
                deviceInfo,
                user
            );
            mf.filter(menu, true);
        }

        // additional restriction for this fragment
        FileMenuFilter.hideMenuItems(
            menu.findItem(R.id.action_rename_file),
            menu.findItem(R.id.action_select_all),
            menu.findItem(R.id.action_move),
            menu.findItem(R.id.action_download_file),
            menu.findItem(R.id.action_sync_file),
            menu.findItem(R.id.action_favorite),
            menu.findItem(R.id.action_unset_favorite)
        );

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            FileMenuFilter.hideMenuItem(menu.findItem(R.id.action_edit));
        }

        if (getFile().isSharedWithMe() && !getFile().canReshare()) {
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
                if (getFile().isSharedWithMe() && !getFile().canReshare()) {
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

            case R.id.action_edit:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    containerActivity.getFileOperationsHelper().openFileWithTextEditor(getFile(), getContext());
                    return true;
                }
                return false;

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

    /**
     * Opens the previewed file with an external application.
     */
    private void openFile() {
        containerActivity.getFileOperationsHelper().openFile(getFile());
        finish();
    }

    /**
     * Helper method to test if an {@link OCFile} can be passed to a {@link PreviewTextFileFragment} to be previewed.
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
        unsupportedTypes.add("text/html");
        return file != null && file.isDown() && MimeTypeUtil.isText(file) &&
            !unsupportedTypes.contains(file.getMimeType()) &&
            !unsupportedTypes.contains(MimeTypeUtil.getMimeTypeFromPath(file.getRemotePath()));
    }


    @Override
    public void onStop() {
        super.onStop();
        Log_OC.e(TAG, "onStop");

        if (textLoadAsyncTask != null) {
            textLoadAsyncTask.cancel(true);
        }
    }

}
