/**
 *   ownCloud Android client application
 *
 *   @author masensio
 *   Copyright (C) 2015 ownCloud Inc.
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

package com.owncloud.android.ui.dialog;

import android.accounts.Account;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimetypeIconUtil;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ShareFileDialogFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ShareFileDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 * Dialog Fragment to show the share options of a file/folder
 *
 * Search the users and share with them
 */
public class ShareFileDialogFragment extends DialogFragment
        implements DialogInterface.OnClickListener{
    private static final String TAG = ShareFileDialogFragment.class.getSimpleName();

    // the fragment initialization parameters
    private static final String ARG_FILE = "FILE";
    private static final String ARG_ACCOUNT = "ACCOUNT";

    // Parameters
    private OCFile mFile;
    private Account mAccount;

    private OnFragmentInteractionListener mListener;

    /**
     * Public factory method to create new ShareFileDialogFragment instances.
     *
     * @param fileToShare   An {@link OCFile} to show in the fragment
     * @param account       An ownCloud account
     * @return A new instance of fragment ShareFragment.
     */
    public static ShareFileDialogFragment newInstance(OCFile fileToShare, Account account) {
        ShareFileDialogFragment fragment = new ShareFileDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, fileToShare);
        args.putParcelable(ARG_ACCOUNT, account);
        fragment.setArguments(args);
        return fragment;
    }

    public ShareFileDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mFile = getArguments().getParcelable(ARG_FILE);
            mAccount = getArguments().getParcelable(ARG_ACCOUNT);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Inflate the layout for the dialog
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.share_file_dialog, null);

        // Setup layout
        // Image
        ImageView icon = (ImageView) view.findViewById(R.id.shareFileIcon);
        icon.setImageResource(MimetypeIconUtil.getFileTypeIconId(mFile.getMimetype(),
                mFile.getFileName()));
        if (mFile.isImage()) {
            String remoteId = String.valueOf(mFile.getRemoteId());
            Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(remoteId);
            if (thumbnail != null){
                icon.setImageBitmap(thumbnail);
            }
        }
        // Name
        TextView filename = (TextView) view.findViewById(R.id.shareFileName);
        filename.setText(mFile.getFileName());
        // Size
        TextView size = (TextView) view.findViewById(R.id.shareFileSize);
        if (mFile.isFolder()){
            size.setVisibility(View.GONE);
        } else {
            size.setText(DisplayUtils.bytesToHumanReadable(mFile.getFileLength()));
        }

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view)
                .setPositiveButton(R.string.common_ok, this)
                .setTitle(R.string.share_link_title);

        Dialog d = builder.create();
        return d;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
