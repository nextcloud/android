/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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

import android.content.Context;
import android.content.res.Resources;

import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class ShareeListAdapterTest {
    @Mock
    private Context context;

    @Test
    public void testSorting() {
        MockitoAnnotations.initMocks(this);
        Resources resources = Mockito.mock(Resources.class);
        Mockito.when(context.getResources()).thenReturn(resources);

        List<OCShare> expectedSortOrder = new ArrayList<>();
        expectedSortOrder.add(new OCShare("/1")
                                  .setShareType(ShareType.EMAIL)
                                  .setSharedDate(1004));
        expectedSortOrder.add(new OCShare("/2")
                                  .setShareType(ShareType.PUBLIC_LINK)
                                  .setSharedDate(1003));
        expectedSortOrder.add(new OCShare("/3")
                                  .setShareType(ShareType.PUBLIC_LINK)
                                  .setSharedDate(1001));
        expectedSortOrder.add(new OCShare("/4")
                                  .setShareType(ShareType.EMAIL)
                                  .setSharedDate(1000));
        expectedSortOrder.add(new OCShare("/5")
                                  .setShareType(ShareType.USER)
                                  .setSharedDate(80));
        expectedSortOrder.add(new OCShare("/6")
                                  .setShareType(ShareType.CIRCLE)
                                  .setSharedDate(20));

        List<OCShare> randomOrder = new ArrayList<>(expectedSortOrder);
        Collections.shuffle(randomOrder);

        ShareeListAdapter sut = new ShareeListAdapter(context, randomOrder, null, null);

        sut.sortShares();

        // compare
        boolean compare = true;
        for (int i = 0; i < expectedSortOrder.size() && compare; i++) {
            compare = expectedSortOrder.get(i) == sut.getShares().get(i);
        }

        if (!compare) {
            System.out.println("Expected:");

            for (OCShare item : expectedSortOrder) {
                System.out.println(item.getPath() + " " + item.getShareType() + " " + item.getSharedDate());
            }

            System.out.println();
            System.out.println("Actual:");
            for (OCShare item : sut.getShares()) {
                System.out.println(item.getPath() + " " + item.getShareType() + " " + item.getSharedDate());
            }
        }

        assertTrue(compare);
    }
}
