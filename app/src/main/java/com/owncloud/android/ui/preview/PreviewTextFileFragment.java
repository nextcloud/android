/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.preview;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.ui.fileactions.FileActionsBottomSheet;
import com.nextcloud.utils.extensions.BundleExtensionsKt;
import com.nextcloud.utils.extensions.FileExtensionsKt;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import org.mozilla.universalchardet.ReaderFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.FragmentManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class PreviewTextFileFragment extends PreviewTextFragment {
    private static final String EXTRA_FILE = "FILE";
    private static final String EXTRA_USER = "USER";
    private static final String EXTRA_OPEN_SEARCH = "SEARCH";
    private static final String EXTRA_SEARCH_QUERY = "SEARCH_QUERY";

    private static final String TAG = PreviewTextFileFragment.class.getSimpleName();

    private TextLoadAsyncTask textLoadAsyncTask;
    private User user;

    @Inject UserAccountManager accountManager;
    @Inject ViewThemeUtils viewThemeUtils;

    public static PreviewTextFileFragment create(User user, OCFile file, boolean openSearch, String searchQuery) {
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_FILE, file);
        args.putParcelable(EXTRA_USER, user);
        args.putBoolean(EXTRA_OPEN_SEARCH, openSearch);
        args.putString(EXTRA_SEARCH_QUERY, searchQuery);
        PreviewTextFileFragment fragment = new PreviewTextFileFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Creates an empty fragment for previews.
     * <p>
     * MUST BE KEPT: the system uses it when tries to re-instantiate a fragment automatically (for instance, when the
     * device is turned a aside).
     * <p>
     * DO NOT CALL IT: an {@link OCFile} and {@link User} must be provided for a successful construction
     */
    public PreviewTextFileFragment() {
        super();
        user = null;
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
            file = BundleExtensionsKt.getParcelableArgument(args, EXTRA_FILE, OCFile.class);
        }

        if (user == null) {
            user = BundleExtensionsKt.getParcelableArgument(args, EXTRA_USER, User.class);
        }

        if (args.containsKey(EXTRA_SEARCH_QUERY)) {
            searchQuery = args.getString(EXTRA_SEARCH_QUERY);
        }
        searchOpen = args.getBoolean(EXTRA_OPEN_SEARCH, false);

        if (savedInstanceState == null) {
            if (file == null) {
                throw new IllegalStateException("Instanced with a NULL OCFile");
            }
            if (user == null) {
                throw new IllegalStateException("Instanced with a NULL ownCloud Account");
            }
        } else {
            file = BundleExtensionsKt.getParcelableArgument(savedInstanceState, EXTRA_FILE, OCFile.class);
            user = BundleExtensionsKt.getParcelableArgument(savedInstanceState, EXTRA_USER, User.class);
        }

        handler = new Handler();
        setFile(file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        FileExtensionsKt.logFileSize(getFile(), TAG);
        outState.putParcelable(PreviewTextFileFragment.EXTRA_FILE, getFile());
        outState.putParcelable(PreviewTextFileFragment.EXTRA_USER, user);
        super.onSaveInstanceState(outState);
    }

    @Override
    void loadAndShowTextPreview() {
        textLoadAsyncTask = new TextLoadAsyncTask(new WeakReference<>(binding.textPreview),
                                                  new WeakReference<>(binding.emptyListProgress));
        textLoadAsyncTask.execute(getFile().getStoragePath());
    }

    /**
     * Reads the file to preview and shows its contents. Too critical to be anonymous.
     */
    private class TextLoadAsyncTask extends AsyncTask<Object, Void, StringWriter> {
        private static final int PARAMS_LENGTH = 1;
        private final WeakReference<TextView> textViewReference;
        private final WeakReference<FrameLayout> progressViewReference;

        private TextLoadAsyncTask(WeakReference<TextView> textView, WeakReference<FrameLayout> progressView) {
            textViewReference = textView;
            progressViewReference = progressView;
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
        @SuppressFBWarnings("STT")
        protected void onPostExecute(final StringWriter stringWriter) {
            final TextView textView = textViewReference.get();

            if (textView != null) {
                originalText = stringWriter.toString();
                setText(textView, originalText, getFile(), requireActivity(), false, false, viewThemeUtils);

                if (searchView != null) {
                    searchView.setOnQueryTextListener(PreviewTextFileFragment.this);

                    if (searchOpen) {
                        searchView.setQuery(searchQuery, true);
                    }
                }

                textView.setVisibility(View.VISIBLE);
            }

            final FrameLayout progress = progressViewReference.get();
            if (progress != null) {
                progress.setVisibility(View.GONE);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.custom_menu_placeholder, menu);

        MenuItem menuItem = menu.findItem(R.id.action_search);
        menuItem.setVisible(true);
        searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        viewThemeUtils.androidx.themeToolbarSearchView(searchView);

        if (searchOpen) {
            searchView.setIconified(false);
            searchView.setQuery(searchQuery, false);
            searchView.clearFocus();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.custom_menu_placeholder_item) {
            final OCFile file = getFile();
            if (containerActivity.getStorageManager() != null && file != null) {
                // Update the file
                final OCFile updatedFile = containerActivity.getStorageManager().getFileById(file.getFileId());
                setFile(updatedFile);

                final OCFile fileNew = getFile();
                if (fileNew != null) {
                    showFileActions(file);
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showFileActions(OCFile file) {
        final List<Integer> additionalFilter = new ArrayList<>(
            Arrays.asList(
                R.id.action_rename_file,
                R.id.action_sync_file,
                R.id.action_move_or_copy,
                R.id.action_favorite,
                R.id.action_unset_favorite,
                R.id.action_pin_to_homescreen
                         ));
        if (getFile() != null && getFile().isSharedWithMe() && !getFile().canReshare()) {
            additionalFilter.add(R.id.action_send_share_file);
        }
        final FragmentManager fragmentManager = getChildFragmentManager();
        FileActionsBottomSheet.newInstance(file, false, additionalFilter)
            .setResultListener(fragmentManager, this, this::onFileActionChosen)
            .show(fragmentManager, "actions");
    }

    private void onFileActionChosen(final int itemId) {
        if (itemId == R.id.action_send_share_file) {
            if (getFile().isSharedWithMe() && !getFile().canReshare()) {
                DisplayUtils.showSnackMessage(getView(), R.string.resharing_is_not_allowed);
            } else {
                containerActivity.getFileOperationsHelper().sendShareFile(getFile());
            }
        } else if (itemId == R.id.action_open_file_with) {
            openFile();
        } else if (itemId == R.id.action_remove_file) {
            RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(getFile());
            dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
        } else if (itemId == R.id.action_see_details) {
            seeDetails();
        } else if (itemId == R.id.action_sync_file) {
            containerActivity.getFileOperationsHelper().syncFile(getFile());
        } else if(itemId == R.id.action_cancel_sync){
            containerActivity.getFileOperationsHelper().cancelTransference(getFile());
        } else if (itemId == R.id.action_edit) {
            containerActivity.getFileOperationsHelper().openFileWithTextEditor(getFile(), getContext());
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
