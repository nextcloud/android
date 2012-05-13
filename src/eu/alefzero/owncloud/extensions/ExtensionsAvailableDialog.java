package eu.alefzero.owncloud.extensions;

import eu.alefzero.owncloud.R;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ExtensionsAvailableDialog extends DialogFragment implements OnClickListener {

  public ExtensionsAvailableDialog() { }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.extensions_available_dialog, container);
    Button btnYes = (Button) view.findViewById(R.id.buttonYes);
    Button btnNo = (Button) view.findViewById(R.id.buttonNo);
    btnYes.setOnClickListener(this);
    btnNo.setOnClickListener(this);
    getDialog().setTitle(R.string.extensions_avail_title);
    return view;
  }
  
  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.buttonYes:
        {
          Intent i = new Intent(getActivity(), ExtensionsListActivity.class);
          startActivity(i);
          getActivity().finish();
        }
        break;
      case R.id.buttonNo:
        getActivity().finish();
        break;
      default:
        Log.e("EAD", "Button with unknown id clicked " + v.getId());
    }
  }

}
