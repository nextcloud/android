/**
 *   Nextcloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2016 ownCloud Inc.
 *   Copyright (C) 2016 Nextcloud
 *   Copyright (C) 2016 Bartosz Przybylski
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.owncloud.android.R;

/**
 * Created by Bartosz Przybylski on 07.11.2015.
 */
public class LocalDirectorySelectorActivity extends UploadFilesActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mUploadBtn.setText(R.string.folder_picker_choose_button_text);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.upload_files_btn_cancel) {
			setResult(RESULT_CANCELED);
			finish();

		} else if (v.getId() == R.id.upload_files_btn_upload) {
			Intent resultIntent = new Intent();
			resultIntent.putExtra(EXTRA_CHOSEN_FILES, getInitialDirectory().getAbsolutePath());
			setResult(RESULT_OK, resultIntent);
			finish();
		}
	}
}
