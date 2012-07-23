package eu.alefzero.owncloud.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockDialogFragment;

import eu.alefzero.owncloud.R;

public class ConfirmationDialogFragment extends SherlockDialogFragment {

    public final static String ARG_CONF_TARGET = "target";
    
    ConfirmationDialogFragmentListener mListener;
    
    public static ConfirmationDialogFragment newInstance(String confirmationTarget) {
        ConfirmationDialogFragment frag = new ConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CONF_TARGET, confirmationTarget);
        frag.setArguments(args);
        return frag;
    }
    
    public void setOnConfirmationListener(ConfirmationDialogFragmentListener listener) {
        mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String confirmationTarget = getArguments().getString(ARG_CONF_TARGET);
        if (confirmationTarget == null)
            confirmationTarget = "";

        return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(String.format(getString(R.string.confirmation_alert), confirmationTarget))
                .setPositiveButton(R.string.common_ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mListener.onConfirmation(true, getTag()); 
                        }
                    }
                )
                .setNegativeButton(R.string.common_cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mListener.onConfirmation(false, getTag()); 
                        }
                    }
                )
                .create();
    }
    
    
    public interface ConfirmationDialogFragmentListener {
        public void onConfirmation(boolean confirmation, String callerTag);
    }
    
}

