/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
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
package com.owncloud.android.ui.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.owncloud.android.ui.activity.LandingActivity;
import com.owncloud.android.ui.adapter.LandingScreenAdapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.owncloud.android.R;

/**
 * Used on the Landing page to display what Components of the ownCloud there
 * are. Like Files, Music, Contacts, etc.
 * 
 * @author Lennart Rosam
 * 
 */
public class LandingPageFragment extends SherlockFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.landing_page_fragment, container);
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ListView landingScreenItems = (ListView) getView().findViewById(
                R.id.homeScreenList);
        landingScreenItems.setAdapter(new LandingScreenAdapter(getActivity()));
        landingScreenItems
                .setOnItemClickListener((LandingActivity) getActivity());
    }

}
