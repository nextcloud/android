/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Alex Plutta <alex.plutta@googlemail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import com.owncloud.android.lib.resources.activities.model.Activity;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

public final class ActivityListAdapterTest {


    @Mock
    private ActivityListAdapter activityListAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        MockitoAnnotations.initMocks(activityListAdapter);
        activityListAdapter.values = new ArrayList<>();
    }

    @Test
    public void isHeader__ObjectIsHeader_ReturnTrue() {
        Object header = "Hello";
        Object activity = Mockito.mock(Activity.class);

        Mockito.when(activityListAdapter.isHeader(0)).thenCallRealMethod();
        Mockito.when(activityListAdapter.getItemViewType(0)).thenCallRealMethod();

        activityListAdapter.values.add(header);
        activityListAdapter.values.add(activity);

        final boolean result = activityListAdapter.isHeader(0);
        Assert.assertTrue(result);
    }

    @Test
    public void isHeader__ObjectIsActivity_ReturnFalse() {
        Object header = "Hello";
        Object activity = Mockito.mock(Activity.class);

        Mockito.when(activityListAdapter.isHeader(1)).thenCallRealMethod();
        Mockito.when(activityListAdapter.getItemViewType(1)).thenCallRealMethod();

        activityListAdapter.values.add(header);
        activityListAdapter.values.add(activity);
        Assert.assertFalse(activityListAdapter.isHeader(1));
    }

    @Test
    public void getHeaderPositionForItem__AdapterIsEmpty_ReturnZero(){
        Mockito.when(activityListAdapter.isHeader(0)).thenCallRealMethod();
        Mockito.when(activityListAdapter.getItemViewType(0)).thenCallRealMethod();

        Assert.assertEquals(0,activityListAdapter.getHeaderPositionForItem(0));
    }

    @Test
    public void getHeaderPositionForItem__ItemIsHeader_ReturnCurrentItem() {
        Object header = "Hello";
        Object activity = Mockito.mock(Activity.class);

        Mockito.when(activityListAdapter.isHeader(0)).thenCallRealMethod();
        Mockito.when(activityListAdapter.getItemViewType(0)).thenCallRealMethod();
        Mockito.when(activityListAdapter.isHeader(1)).thenCallRealMethod();
        Mockito.when(activityListAdapter.getItemViewType(1)).thenCallRealMethod();
        Mockito.when(activityListAdapter.isHeader(2)).thenCallRealMethod();
        Mockito.when(activityListAdapter.getItemViewType(2)).thenCallRealMethod();
        Mockito.when(activityListAdapter.getHeaderPositionForItem(2)).thenCallRealMethod();
        Mockito.when(activityListAdapter.isHeader(3)).thenCallRealMethod();
        Mockito.when(activityListAdapter.getItemViewType(3)).thenCallRealMethod();
        Mockito.when(activityListAdapter.getHeaderPositionForItem(3)).thenCallRealMethod();


        activityListAdapter.values.add(header);
        activityListAdapter.values.add(activity);
        activityListAdapter.values.add(header);
        activityListAdapter.values.add(activity);


        Assert.assertEquals(2, activityListAdapter.getHeaderPositionForItem(2));

    }

    @Test
    public void getHeaderPositionForItem__ItemIsActivity_ReturnNextHeader() {
        Object header = "Hello";
        Object activity = Mockito.mock(Activity.class);

        Mockito.when(activityListAdapter.isHeader(0)).thenCallRealMethod();
        Mockito.when(activityListAdapter.getItemViewType(0)).thenCallRealMethod();
        Mockito.when(activityListAdapter.getHeaderPositionForItem(0)).thenCallRealMethod();
        Mockito.when(activityListAdapter.isHeader(1)).thenCallRealMethod();
        Mockito.when(activityListAdapter.getItemViewType(1)).thenCallRealMethod();
        Mockito.when(activityListAdapter.getHeaderPositionForItem(1)).thenCallRealMethod();
        Mockito.when(activityListAdapter.isHeader(2)).thenCallRealMethod();
        Mockito.when(activityListAdapter.getItemViewType(2)).thenCallRealMethod();
        Mockito.when(activityListAdapter.getHeaderPositionForItem(2)).thenCallRealMethod();
        Mockito.when(activityListAdapter.isHeader(3)).thenCallRealMethod();
        Mockito.when(activityListAdapter.getItemViewType(3)).thenCallRealMethod();
        Mockito.when(activityListAdapter.getHeaderPositionForItem(3)).thenCallRealMethod();

        activityListAdapter.values.add(header);
        activityListAdapter.values.add(activity);
        activityListAdapter.values.add(header);
        activityListAdapter.values.add(activity);

        Assert.assertEquals(2, activityListAdapter.getHeaderPositionForItem(2));
    }

}
