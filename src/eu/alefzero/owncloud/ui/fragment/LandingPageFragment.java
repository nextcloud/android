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
