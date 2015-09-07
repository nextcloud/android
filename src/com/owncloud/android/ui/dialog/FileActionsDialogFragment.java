package com.owncloud.android.ui.dialog;

import android.support.v4.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.ui.dialog.parcel.MenuItemParcelable;
import com.owncloud.android.ui.dialog.parcel.MenuParcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for choosing a file action.
 */
public class FileActionsDialogFragment extends DialogFragment implements
        OnItemClickListener {
    private static final String ARG_ITEM_LIST = "ITEM_LIST";
    private static final String ARG_FILE_POSITION = "FILE_POSITION";
    private static final String ARG_FILE_NAME = "FILE_NAME";
    public static final String FTAG_FILE_ACTIONS = "FILE_ACTIONS_FRAGMENT";

    private List<MenuItemParcelable> mMenuItems;

    private int mFilePosition;

    private ListView mListView;

    /**
     * Listener interface for the file action fragment.
     */
    public interface FileActionsDialogFragmentListener {
        public boolean onFileActionChosen(int menuId, int filePosition);
    }

    /**
     * Public factory method to create new FileActionsDialogFragment instances.
     *
     * @param menu menu to be display.
     * @return Dialog ready to show.
     */
    public static FileActionsDialogFragment newInstance(Menu menu, int filePosition, String fileName) {
        FileActionsDialogFragment fragment = new FileActionsDialogFragment();
        Bundle args = new Bundle();

        MenuParcelable menuParcelable = new MenuParcelable();
        menuParcelable.setMenuItems(calculateMenuParcel(menu));

        args.putParcelable(ARG_ITEM_LIST, menuParcelable);
        args.putInt(ARG_FILE_POSITION, filePosition);
        args.putCharSequence(ARG_FILE_NAME, fileName);

        fragment.setArguments(args);
        return fragment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.ownCloud_Dialog);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.file_actions, null, false);
        mListView = (ListView) view.findViewById(R.id.file_actions_list);

        CharSequence title =  getArguments().getCharSequence(ARG_FILE_NAME);
        if(title != null && title.length() > 0) {
            TextView header = (TextView)view.findViewById(R.id.file_actions_header);
            header.setText(title);
            header.setVisibility(View.VISIBLE);
            view.findViewById(R.id.file_actions_header_divider).setVisibility(View.VISIBLE);
        } else {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        return view;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mFilePosition = getArguments().getInt(ARG_FILE_POSITION);

        MenuParcelable menu = getArguments().getParcelable(ARG_ITEM_LIST);
        mMenuItems = menu.getMenuItems();
        List<String> stringList = new ArrayList<String>();
        for (int i = 0; i < mMenuItems.size(); i++) {
            stringList.add(mMenuItems.get(i).getMenuText());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                R.layout.simple_dialog_list_item, stringList);

        mListView.setAdapter(adapter);

        mListView.setOnItemClickListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        dismiss();
        ((FileActionsDialogFragmentListener) getTargetFragment())
                .onFileActionChosen(mMenuItems.get(position).getMenuItemId(), mFilePosition);
    }

    /**
     * calculates a parcelable list of MenuItemParcelable based on the given menu and the visibility of the menu items.
     *
     * @param menu the menu to be displayed
     * @return a filtered List of parcelables based on the menu
     */
    private static List<MenuItemParcelable> calculateMenuParcel(Menu menu) {
        int index = 0;
        boolean hasNext = true;
        List<MenuItemParcelable> itemList = new ArrayList<MenuItemParcelable>();
        MenuParcelable menuParcelable = new MenuParcelable();
        try {
            while (hasNext) {
                MenuItem item = menu.getItem(index);
                if (item.isVisible()) {
                    itemList.add(new MenuItemParcelable(item));
                }
                index++;
            }
        } catch (IndexOutOfBoundsException iobe) {
            // reach the end of the item list
        }

        return itemList;
    }
}
