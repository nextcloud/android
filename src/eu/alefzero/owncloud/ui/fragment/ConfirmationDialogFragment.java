package eu.alefzero.owncloud.ui.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.SherlockDialogFragment;

import eu.alefzero.owncloud.R;

public class ConfirmationDialogFragment extends SherlockDialogFragment {

    public final static String ARG_CONF_RESOURCE_ID = "resource_id";
    public final static String ARG_CONF_ARGUMENTS = "string_array";
    
    ConfirmationDialogFragmentListener mListener;
    
    public static ConfirmationDialogFragment newInstance(int string_id, String[] arguments) {
        ConfirmationDialogFragment frag = new ConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CONF_RESOURCE_ID, string_id);
        args.putStringArray(ARG_CONF_ARGUMENTS, arguments);
        frag.setArguments(args);
        return frag;
    }
    
    public void setOnConfirmationListener(ConfirmationDialogFragmentListener listener) {
        mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Object[] confirmationTarget = getArguments().getStringArray(ARG_CONF_ARGUMENTS);
        int resourceId = getArguments().getInt(ARG_CONF_RESOURCE_ID, -1);
        if (confirmationTarget == null || resourceId == -1) {
            Log.wtf(getTag(), "Calling confirmation dialog without resource or arguments");
            return null;
        }

        return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(String.format(getString(resourceId), confirmationTarget))
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

