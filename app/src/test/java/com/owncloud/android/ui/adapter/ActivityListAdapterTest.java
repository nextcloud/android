/*
 * Nextcloud Android client application
 *
 * @author Alex Plutta
 * Copyright (C) 2019 Alex Plutta
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
