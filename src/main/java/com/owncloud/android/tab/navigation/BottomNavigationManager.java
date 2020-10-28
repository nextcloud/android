package com.owncloud.android.tab.navigation;

import android.view.MenuItem;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BottomNavigationManager implements BottomNavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnNavigationItemReselectedListener {

    private final BottomNavigationView mBottomNavigationView;
    private NavigationListener mNavigationListener;

    public BottomNavigationManager(@NonNull BottomNavigationView bottomNavigationView, int menuId) {
        mBottomNavigationView = bottomNavigationView;
        //清除图标的调色机制
        mBottomNavigationView.setItemIconTintList(null);
        mBottomNavigationView.setOnNavigationItemSelectedListener(this);
        mBottomNavigationView.setOnNavigationItemReselectedListener(this);
        mBottomNavigationView.inflateMenu(menuId);
        BottomNavigationViewHelper.setItemStyle((BottomNavigationMenuView) mBottomNavigationView.getChildAt(0));
    }

    /**
     * 选中tab 的id, 注意, 这里是代码设置切换tab, 所以不会触发item点击的回调
     */
    public void setSelectedById(@IdRes int id) {
        mBottomNavigationView.post(() -> {
            if (id == mBottomNavigationView.getSelectedItemId()) {
                return;
            }
            mBottomNavigationView.setSelectedItemId(id);
        });
    }

    public int getSelectedItemId() {
        return mBottomNavigationView.getSelectedItemId();
    }

    @Override
    public void onNavigationItemReselected(@NonNull MenuItem menuItem) {
        if (mNavigationListener != null) {
            mNavigationListener.onItemSelected(menuItem, true);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        return mNavigationListener == null || mNavigationListener.onItemSelected(menuItem, false);
    }

    public void setOnNavigationListener(NavigationListener navigationListener) {
        mNavigationListener = navigationListener;
    }

    public interface NavigationListener {

        boolean onItemSelected(MenuItem menuItem, boolean reselect);
    }
}
