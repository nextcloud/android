package com.owncloud.android.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.owncloud.android.R;

public class ConflictsResolveDialog extends SherlockDialogFragment {

    public static enum Decision { 
        CANCEL,
        KEEP_BOTH,
        OVERWRITE
    }
    
    OnConflictDecisionMadeListener mListener;
    
    public static ConflictsResolveDialog newInstance(String path, OnConflictDecisionMadeListener listener) {
        ConflictsResolveDialog f = new ConflictsResolveDialog();
        Bundle args = new Bundle();
        args.putString("remotepath", path);
        f.setArguments(args);
        f.mListener = listener;
        return f;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String remotepath = getArguments().getString("remotepath");
        return new AlertDialog.Builder(getSherlockActivity())
                   .setIcon(R.drawable.icon)
                   .setTitle(R.string.conflict_title)
                   .setMessage(String.format(getString(R.string.conflict_message), remotepath))
                   .setPositiveButton(R.string.conflict_overwrite,
                       new DialogInterface.OnClickListener() {

                           @Override
                           public void onClick(DialogInterface dialog, int which) {
                               if (mListener != null)
                                   mListener.ConflictDecisionMade(Decision.OVERWRITE);
                           }
                       })
                   .setNeutralButton(R.string.conflict_keep_both,
                       new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mListener != null)
                                    mListener.ConflictDecisionMade(Decision.KEEP_BOTH);
                            }
                        })
                   .setNegativeButton(R.string.conflict_dont_upload,
                       new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialog, int which) {
                               if (mListener != null)
                                   mListener.ConflictDecisionMade(Decision.CANCEL);
                           }
                   })
                   .create();
    }
    
    public void showDialog(SherlockFragmentActivity activity) {
        Fragment prev = activity.getSupportFragmentManager().findFragmentByTag("dialog");
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        this.show(ft, "dialog");
    }

    public static void dismissDialog(SherlockFragmentActivity activity, String tag) {
        Fragment prev = activity.getSupportFragmentManager().findFragmentByTag(tag);
        if (prev != null) {
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            ft.remove(prev);
            ft.commit();
        }
    }
    
    public interface OnConflictDecisionMadeListener {
        public void ConflictDecisionMade(Decision decision);
    }
}
