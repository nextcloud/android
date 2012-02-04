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
package eu.alefzero.owncloud.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.ui.activity.FileDisplayActivity;
import eu.alefzero.owncloud.ui.activity.Preferences;

/**
 * Used on the Landing page to display what Components of 
 * the ownCloud there are. Like Files, Music, Contacts, etc.
 * 
 * @author Lennart Rosam
 *
 */
public class LandingPageFragment extends Fragment implements OnItemClickListener {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.landing_page_fragment, container);
		return root;
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		GridView grid = (GridView) getView().findViewById(R.id.gridview);
		grid.setAdapter(new LandingScreenAdapter(getActivity()));
		grid.setOnItemClickListener(this);
	}
	
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		/*
		 * Start an activity based on the selection
		 * the user made
		 */
		Intent intent;
		intent = (Intent) parent.getAdapter().getItem(position);
		if(intent != null ){
			startActivity(intent);
		} else {
			Toast toast = Toast.makeText(getActivity(), "Not yet implemented!", Toast.LENGTH_SHORT);
			toast.show();
		} 
	}

	/**
	 * Used to populate the landing page grid.
	 * Defined this one right in here as private class
	 * as it is unlikely that this Adapter can be useful
	 * anywhere else.
	 *  
	 * @author Lennart Rosam
	 *
	 */
	private class LandingScreenAdapter extends BaseAdapter {

		private Context mContext;

		private final Integer[] mLandingScreenIcons = { R.drawable.home,
				R.drawable.music, R.drawable.contacts,
				android.R.drawable.ic_menu_today,
				android.R.drawable.ic_menu_agenda,
				android.R.drawable.ic_menu_preferences };

		private final Integer[] mLandingScreenTexts = { R.string.main_files,
				R.string.main_music, R.string.main_contacts,
				R.string.main_calendar, R.string.main_bookmarks,
				R.string.main_settings };

		public LandingScreenAdapter(Context context) {
			mContext = context;
		}

		public int getCount() {
			return mLandingScreenIcons.length;
		}

		/**
		 * Returns the Intent associated with this object
		 * or null if the functionality is not yet implemented
		 */
		public Object getItem(int position) {
			Intent intent = new Intent();
			switch (position) {
			case 0:
				intent.setClass(mContext, FileDisplayActivity.class);
				break;
			case 5:
				intent.setClass(mContext, Preferences.class);
				break;
			default:
				intent = null;
			}
			return intent;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater inflator = LayoutInflater.from(mContext);
				convertView = inflator
						.inflate(R.layout.landing_page_item, null);

				ImageView icon = (ImageView) convertView
						.findViewById(R.id.gridImage);
				TextView iconText = (TextView) convertView
						.findViewById(R.id.gridText);

				icon.setImageResource(mLandingScreenIcons[position]);
				iconText.setText(mLandingScreenTexts[position]);
			}
			return convertView;
		}
	}

	
	
}
