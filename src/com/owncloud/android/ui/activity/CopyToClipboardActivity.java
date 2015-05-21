/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
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

package com.owncloud.android.ui.activity;

import com.owncloud.android.R;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.widget.Toast;

/**
 * Activity copying the text of the received Intent into the system clibpoard.
 */
@SuppressWarnings("deprecation")
public class CopyToClipboardActivity extends Activity {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // get the clipboard system service
        ClipboardManager clipboardManager = (ClipboardManager) this.getSystemService(CLIPBOARD_SERVICE);
        
        // get the text to copy into the clipboard 
        Intent intent = getIntent();
        CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        
        // and put the text the clipboard
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            // API level >= 11 -> modern Clipboard
            ClipData clip = ClipData.newPlainText("ownCloud was here", text);
            ((android.content.ClipboardManager)clipboardManager).setPrimaryClip(clip);
            
        } else {
            // API level >= 11 -> legacy Clipboard
            clipboardManager.setText(text);    
        }
        
        // alert the user that the text is in the clipboard and we're done
        Toast.makeText(this, R.string.clipboard_text_copied, Toast.LENGTH_SHORT).show();
        
        finish();
    }    

}
