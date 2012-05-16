package eu.alefzero.owncloud.extensions;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class ExtensionsAvailableActivity extends SherlockFragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager fm = getSupportFragmentManager();
        ExtensionsAvailableDialog ead = new ExtensionsAvailableDialog();
        ead.show(fm, "extensions_available_dialog");
    }
}
