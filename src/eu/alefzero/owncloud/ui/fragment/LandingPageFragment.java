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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.Toast;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.ui.adapter.LandingScreenAdapter;

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
	
	@Override
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

	
	
}
