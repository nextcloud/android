/* ownCloud Android client application
 *   Copyright (C) 2012  Lennart Rosam
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
package eu.alefzero.owncloud.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import eu.alefzero.owncloud.R;

/**
 * This activity is used as a landing page when the user first opens this app.
 * 
 * @author Lennart Rosam
 */
public class LandingActivity extends FragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.landing_page);

	}

}
