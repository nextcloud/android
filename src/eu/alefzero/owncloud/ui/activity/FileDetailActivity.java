/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
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
package eu.alefzero.owncloud.ui.activity;

import android.accounts.Account;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.datamodel.OCFile;
import eu.alefzero.owncloud.files.services.FileDownloader;
import eu.alefzero.owncloud.ui.fragment.FileDetailFragment;

/**
 * This activity displays the details of a file like its name, its size and so
 * on.
 * 
 * @author Bartek Przybylski
 * 
 */
public class FileDetailActivity extends SherlockFragmentActivity {
    
    private boolean mConfigurationChangedToLandscape = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check if configuration changed to large-land ; for a tablet being changed from portrait to landscape when in FileDetailActivity 
        Configuration conf = getResources().getConfiguration();
        mConfigurationChangedToLandscape = (conf.orientation == Configuration.ORIENTATION_LANDSCAPE && 
                                                (conf.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
                                           );

        if (!mConfigurationChangedToLandscape) {
            setContentView(R.layout.file_activity_details);
        
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
        
            OCFile file = getIntent().getParcelableExtra(FileDetailFragment.EXTRA_FILE);
            Account account = getIntent().getParcelableExtra(FileDownloader.EXTRA_ACCOUNT);
            FileDetailFragment mFileDetail = new FileDetailFragment(file, account);
        
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment, mFileDetail, FileDetailFragment.FTAG);
            ft.commit();
            
        }  else {
            backToDisplayActivity();   // the 'back' won't be effective until this.onStart() and this.onResume() are completed;
        }
        
        
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean returnValue = false;
        
        switch(item.getItemId()){
        case android.R.id.home:
            backToDisplayActivity();
            returnValue = true;
        }
        
        return returnValue;
    }



    @Override
    protected void onResume() {
        
        super.onResume();
        if (!mConfigurationChangedToLandscape) { 
            FileDetailFragment fragment = (FileDetailFragment) getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
            fragment.updateFileDetails();
        }
    }
    

    private void backToDisplayActivity() {
        Intent intent = new Intent(this, FileDisplayActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(FileDetailFragment.EXTRA_FILE, getIntent().getParcelableExtra(FileDetailFragment.EXTRA_FILE));
        startActivity(intent);
        finish();
    }

}
