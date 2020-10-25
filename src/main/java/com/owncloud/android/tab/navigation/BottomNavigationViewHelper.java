package com.owncloud.android.tab.navigation;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.owncloud.android.R;
import com.owncloud.android.utils.DpUtil;

public class BottomNavigationViewHelper {

    private BottomNavigationViewHelper() {
        //...
    }

    @SuppressLint("RestrictedApi")
    static void setItemStyle(BottomNavigationMenuView menuView) {
        for (int i = 0; i < menuView.getChildCount(); i++) {
            BottomNavigationItemView item = (BottomNavigationItemView) menuView.getChildAt(i);
            item.setChecked(item.getItemData().isChecked());
            TextView smallLabel = item.findViewById(R.id.smallLabel);
            TextView largeLabel = item.findViewById(R.id.largeLabel);
            smallLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
            largeLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
            item.setTextAppearanceActive(-1);
            item.setTextAppearanceInactive(-1);

            ImageView iv = item.findViewById(R.id.icon);
            ViewGroup.LayoutParams layoutParams = iv.getLayoutParams();
            layoutParams.width = DpUtil.INSTANCE.dp2px(24f);
            layoutParams.height = DpUtil.INSTANCE.dp2px(24f);
            iv.setLayoutParams(layoutParams);
            item.setPadding(0, DpUtil.INSTANCE.dp2px(2F), 0, 0);
        }
        Resources resource = menuView.getContext().getResources();
        ColorStateList csl = resource.getColorStateList(R.color.main_tab_color);
        menuView.setItemTextColor(csl);
        menuView.setIconTintList(csl);
        menuView.updateMenuView();
    }
}
