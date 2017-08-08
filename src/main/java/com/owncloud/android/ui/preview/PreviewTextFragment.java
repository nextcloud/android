/**
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

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.utils.MimeTypeUtil;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class PreviewTextFragment extends FileFragment {
    private static final String EXTRA_FILE = "FILE";
    private static final String EXTRA_ACCOUNT = "ACCOUNT";
    private static final String TAG = PreviewTextFragment.class.getSimpleName();

    private static final String SCREEN_NAME = "Text Preview";

    private Account mAccount;
    private TextView mTextPreview;
    private TextLoadAsyncTask mTextLoadTask;

    private RelativeLayout mMultiView;

    protected LinearLayout mMultiListContainer;
    protected TextView mMultiListMessage;
    protected TextView mMultiListHeadline;
    protected ImageView mMultiListIcon;
    protected ProgressBar mMultiListProgress;


    /**
     * Creates an empty fragment for previews.
     * <p/>
     * MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically
     * (for instance, when the device is turned a aside).
     * <p/>
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log_OC.e(TAG, "onCreateView");


        View ret = inflater.inflate(R.layout.text_file_preview, container, false);

        mTextPreview = (TextView) ret.findViewById(R.id.text_preview);

        mMultiView = (RelativeLayout) ret.findViewById(R.id.multi_view);

        setupMultiView(ret);
        setMultiListLoadingMessage();

        return ret;
    }

    protected void setupMultiView(View view) {
        mMultiListContainer = (LinearLayout) view.findViewById(R.id.empty_list_view);
        mMultiListMessage = (TextView) view.findViewById(R.id.empty_list_view_text);
        mMultiListHeadline = (TextView) view.findViewById(R.id.empty_list_view_headline);
        mMultiListIcon = (ImageView) view.findViewById(R.id.empty_list_icon);
        mMultiListProgress = (ProgressBar) view.findViewById(R.id.empty_list_progress);
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

        OCFile file = getFile();

        Bundle args = getArguments();

        if (file == null) {
            file = args.getParcelable(FileDisplayActivity.EXTRA_FILE);
        }

        if (mAccount == null) {
            mAccount = args.getParcelable(FileDisplayActivity.EXTRA_ACCOUNT);
        }

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
        setFile(file);
        setHasOptionsMenu(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PreviewTextFragment.EXTRA_FILE, getFile());
        outState.putParcelable(PreviewTextFragment.EXTRA_ACCOUNT, mAccount);
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


    /**
     * Reads the file to preview and shows its contents. Too critical to be anonymous.
     */
    private class TextLoadAsyncTask extends AsyncTask<Object, Void, StringWriter> {
        private final WeakReference<TextView> mTextViewReference;

        private TextLoadAsyncTask(WeakReference<TextView> textView) {
            mTextViewReference = textView;
        }

        @Override
        protected void onPreExecute() {
            // not used at the moment
        }

        @Override
        protected StringWriter doInBackground(java.lang.Object... params) {
            if (params.length != 1) {
                throw new IllegalArgumentException("The parameter to " + TextLoadAsyncTask.class.getName() + " must be (1) the file location");
            }
            final String location = (String) params[0];

            InputStreamReader inputStream = null;
            Scanner sc = null;
            StringWriter source = new StringWriter();
            BufferedWriter bufferedWriter = new BufferedWriter(source);
            try {
                inputStream = new InputStreamReader(new FileInputStream(location), "UTF8");
                sc = new Scanner(inputStream);
                while (sc.hasNextLine()) {
                    bufferedWriter.append(sc.nextLine());
                    if (sc.hasNextLine()) {
                        bufferedWriter.append("\n");
                    }
                }
                bufferedWriter.close();
                IOException exc = sc.ioException();
                if (exc != null) {
                    throw exc;
                }
            } catch (IOException e) {
                Log_OC.e(TAG, e.getMessage(), e);
                finish();
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log_OC.e(TAG, e.getMessage(), e);
                        finish();
                    }
                }
                if (sc != null) {
                    sc.close();
                }
            }
            return source;
        }

        @Override
        protected void onPostExecute(final StringWriter stringWriter) {
            final TextView textView = mTextViewReference.get();

            if (textView != null) {
                textView.setText(new String(stringWriter.getBuffer()));
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mContainerActivity.getStorageManager() != null) {
            FileMenuFilter mf = new FileMenuFilter(
                    getFile(),
                    mContainerActivity.getStorageManager().getAccount(),
                    mContainerActivity,
                    getActivity()
            );
            mf.filter(menu);
        }

        // additional restriction for this fragment
        MenuItem item = menu.findItem(R.id.action_rename_file);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_move);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // this one doesn't make sense since the file has to be down in order to be previewed
        item = menu.findItem(R.id.action_download_file);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        item = menu.findItem(R.id.action_sync_file);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        item = menu.findItem(R.id.action_sync_account);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        Boolean dualPane = getResources().getBoolean(R.bool.large_land_layout);

        item = menu.findItem(R.id.action_switch_view);
        if (item != null && !dualPane){
            item.setVisible(false);
            item.setEnabled(false);
        }

        item = menu.findItem(R.id.action_sort);
        if (item != null && !dualPane) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_favorite);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_unset_favorite);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        if(getFile().isSharedWithMe() && !getFile().canReshare()){
            // additional restriction for this fragment
            item = menu.findItem(R.id.action_share_file);
            if(item != null){
                item.setVisible(false);
                item.setEnabled(false);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share_file: {
                mContainerActivity.getFileOperationsHelper().showShareFile(getFile());
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
            case R.id.action_send_file: {
                sendFile();
                return true;
            }
            case R.id.action_sync_file: {
                mContainerActivity.getFileOperationsHelper().syncFile(getFile());
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

    private void sendFile() {
        mContainerActivity.getFileOperationsHelper().sendDownloadedFile(getFile());
    }

    private void seeDetails() {
        mContainerActivity.showDetails(getFile());
    }

    @Override
    public void onPause() {
        Log_OC.e(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            AnalyticsUtils.setCurrentScreenName(getActivity(), SCREEN_NAME, TAG);
        }
        Log_OC.e(TAG, "onResume");
    }

    @Override
    public void onDestroy() {
        Log_OC.e(TAG, "onDestroy");
        super.onDestroy();
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
        mContainerActivity.getFileOperationsHelper().openFile(getFile());
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
        unsupportedTypes.add("text/vnd.abc");
        unsupportedTypes.add("text/vnd.fmi.flexstor");
        unsupportedTypes.add("text/vnd.rn-realtext");
        unsupportedTypes.add("text/vnd.wap.wml");
        unsupportedTypes.add("text/vnd.wap.wmlscript");
        return (file != null && file.isDown() && MimeTypeUtil.isText(file) &&
                !unsupportedTypes.contains(file.getMimetype()) &&
                !unsupportedTypes.contains(MimeTypeUtil.getMimeTypeFromPath(file.getRemotePath()))
        );
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
