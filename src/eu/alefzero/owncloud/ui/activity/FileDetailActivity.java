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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.ui.fragment.FileDetailFragment;

/**
 * This activity displays the details of a file like its name, its size and so
 * on.
 * 
 * @author Bartek Przybylski
 * 
 */
public class FileDetailActivity extends SherlockFragmentActivity {
    private FileDetailFragment mFileDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_activity_details);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        mFileDetail = new FileDetailFragment();
        ft.replace(R.id.fragment, mFileDetail, "FileDetails");
        ft.commit();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean returnValue = false;
        
        switch(item.getItemId()){
        case android.R.id.home:
            Intent intent = new Intent(this, FileDisplayActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(FileDetailFragment.EXTRA_FILE, mFileDetail.getDisplayedFile());
            startActivity(intent);
            finish();
            returnValue = true;
        }
        
        return returnValue;
    }



    @Override
    protected void onResume() {
        super.onResume();
        mFileDetail.updateFileDetails(getIntent());
    }
    
    

}
