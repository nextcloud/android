package com.owncloud.android.ui.preview;

import android.accounts.Account;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.LoadingDialog;
import com.owncloud.android.ui.dialog.RemoveFileDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class PreviewTextFragment extends FileFragment {
    private static final String EXTRA_FILE = "FILE";
    private static final String EXTRA_ACCOUNT = "ACCOUNT";
    private static final String TAG = PreviewTextFragment.class.getSimpleName();

    private Account mAccount;
    private TextView mTextPreview;
    private TextLoadAsyncTask mTextLoadTask;

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

        return ret;
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
        mTextLoadTask = new TextLoadAsyncTask(new WeakReference<TextView>(mTextPreview));
        mTextLoadTask.execute(getFile().getStoragePath());
    }


    /**
     * Reads the file to preview and shows its contents. Too critical to be anonymous.
     */
    private class TextLoadAsyncTask extends AsyncTask<Object, Void, StringWriter> {
        private final String DIALOG_WAIT_TAG = "DIALOG_WAIT";
        private final WeakReference<TextView> mTextViewReference;

        private TextLoadAsyncTask(WeakReference<TextView> textView) {
            mTextViewReference = textView;
        }


        @Override
        protected void onPreExecute() {
            showLoadingDialog();
        }

        @Override
        protected StringWriter doInBackground(java.lang.Object... params) {
            if (params.length != 1) {
                throw new IllegalArgumentException("The parameter to " + TextLoadAsyncTask.class.getName() + " must be (1) the file location");
            }
            final String location = (String) params[0];

            FileInputStream inputStream = null;
            Scanner sc = null;
            StringWriter source = new StringWriter();
            BufferedWriter bufferedWriter = new BufferedWriter(source);
            try {
                inputStream = new FileInputStream(location);
                sc = new Scanner(inputStream);
                while (sc.hasNextLine()) {
                    bufferedWriter.append(sc.nextLine());
                    if (sc.hasNextLine()) bufferedWriter.append("\n");
                }
                bufferedWriter.close();
                IOException exc = sc.ioException();
                if (exc != null) throw exc;
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

            dismissLoadingDialog();
        }

        /**
         * Show loading dialog
         */
        public void showLoadingDialog() {
            // only once
            Fragment frag = getActivity().getSupportFragmentManager().findFragmentByTag(DIALOG_WAIT_TAG);
            LoadingDialog loading = null;
            if (frag == null) {
                // Construct dialog
                loading = new LoadingDialog(getResources().getString(R.string.wait_a_moment));
                FragmentManager fm = getActivity().getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                loading.show(ft, DIALOG_WAIT_TAG);
            } else {
                loading = (LoadingDialog) frag;
                loading.setShowsDialog(true);
            }

        }

        /**
         * Dismiss loading dialog
         */
        public void dismissLoadingDialog() {
            final Fragment frag = getActivity().getSupportFragmentManager().findFragmentByTag(DIALOG_WAIT_TAG);
            if (frag != null) {
                LoadingDialog loading = (LoadingDialog) frag;
                loading.dismiss();
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
                RemoveFileDialogFragment dialog = RemoveFileDialogFragment.newInstance(getFile());
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
                return false;
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
        if (mTextLoadTask != null)
            mTextLoadTask.cancel(Boolean.TRUE);
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
        final List<String> unsupportedTypes = new LinkedList<String>();
        unsupportedTypes.add("text/richtext");
        unsupportedTypes.add("text/rtf");
        unsupportedTypes.add("text/vnd.abc");
        unsupportedTypes.add("text/vnd.fmi.flexstor");
        unsupportedTypes.add("text/vnd.rn-realtext");
        unsupportedTypes.add("text/vnd.wap.wml");
        unsupportedTypes.add("text/vnd.wap.wmlscript");
        return (file != null && file.isDown() && file.isText() &&
                !unsupportedTypes.contains(file.getMimetype()) &&
                !unsupportedTypes.contains(file.getMimeTypeFromName())
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
