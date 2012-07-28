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
    
    public final static String ARG_POSITIVE_BTN_RES = "positive_btn_res";
    public final static String ARG_NEUTRAL_BTN_RES = "neutral_btn_res";
    public final static String ARG_NEGATIVE_BTN_RES = "negative_btn_res";
    
    private ConfirmationDialogFragmentListener mListener;
    
    public static ConfirmationDialogFragment newInstance(int string_id, String[] arguments, int posBtn, int neuBtn, int negBtn) {
        ConfirmationDialogFragment frag = new ConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CONF_RESOURCE_ID, string_id);
        args.putStringArray(ARG_CONF_ARGUMENTS, arguments);
        args.putInt(ARG_POSITIVE_BTN_RES, posBtn);
        args.putInt(ARG_NEUTRAL_BTN_RES, neuBtn);
        args.putInt(ARG_NEGATIVE_BTN_RES, negBtn);
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
        int posBtn = getArguments().getInt(ARG_POSITIVE_BTN_RES, -1);
        int neuBtn = getArguments().getInt(ARG_NEUTRAL_BTN_RES, -1);
        int negBtn = getArguments().getInt(ARG_NEGATIVE_BTN_RES, -1);
        
        if (confirmationTarget == null || resourceId == -1) {
            Log.wtf(getTag(), "Calling confirmation dialog without resource or arguments");
            return null;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setMessage(String.format(getString(resourceId), confirmationTarget))
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setTitle(android.R.string.dialog_alert_title);
        
        if (posBtn != -1)
            builder.setPositiveButton(posBtn,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mListener.onConfirmation(getTag()); 
                        }
                    });
        if (neuBtn != -1)
            builder.setNeutralButton(neuBtn,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mListener.onNeutral(getTag()); 
                        }
                    });
        if (negBtn != -1)
            builder.setNegativeButton(negBtn,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mListener.onCancel(getTag());
                        }
                    });
      return builder.create();
    }
    
    
    public interface ConfirmationDialogFragmentListener {
        public void onConfirmation(String callerTag);
        public void onNeutral(String callerTag);
        public void onCancel(String callerTag);
    }
    
}

