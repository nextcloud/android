/*
 *   ownCloud Android client application
 *
 *   Copyright (C) 2015 ownCloud Inc.
 *   Copyright (C) Chris Narkiewicz <hello@ezaquarii.com>
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
 */

package com.owncloud.android.ui.activity;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.nextcloud.client.di.ViewModelFactory;
import com.nextcloud.client.logger.ui.LogsAdapter;
import com.nextcloud.client.logger.ui.LogsViewModel;
import com.owncloud.android.R;
import com.owncloud.android.utils.ThemeUtils;

import javax.inject.Inject;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;


public class LogsActivity extends ToolbarActivity {

    private Unbinder unbinder;

    @BindView(R.id.deleteLogHistoryButton)
    Button deleteHistoryButton;

    @BindView(R.id.sendLogHistoryButton)
    Button sendHistoryButton;

    @BindView(R.id.logsList)
    RecyclerView logListView;

    @Inject ViewModelFactory viewModelFactory;
    private LogsViewModel vm;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logs_activity);
        unbinder = ButterKnife.bind(this);
        final LogsAdapter logsAdapter = new LogsAdapter(this);
        logListView.setLayoutManager(new LinearLayoutManager(this));
        logListView.setAdapter(logsAdapter);

        vm = new ViewModelProvider(this, viewModelFactory).get(LogsViewModel.class);
        vm.getEntries().observe(this, logsAdapter::setEntries);
        vm.load();

        setupToolbar();

        setTitle(getText(R.string.actionbar_logger));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        sendHistoryButton.getBackground().setColorFilter(ThemeUtils.primaryColor(this), PorterDuff.Mode.SRC_ATOP);
        deleteHistoryButton.setTextColor(ThemeUtils.primaryColor(this, true));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                retval = super.onOptionsItemSelected(item);
                break;
        }
        return retval;
    }

    @OnClick(R.id.deleteLogHistoryButton)
    void deleteLogs() {
        vm.deleteAll();
        finish();
    }

    @OnClick(R.id.sendLogHistoryButton)
    void sendLogs() {
        vm.send();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }
}
