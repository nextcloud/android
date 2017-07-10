package com.owncloud.android.ui;

import com.owncloud.android.lib.resources.files.SearchOperation;
import com.owncloud.android.ui.events.SearchEvent;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by JARP on 6/30/17.
 */

public class EventTest {


    @Test
    public void withSameEventReturnsTrue()
    {
        SearchEvent event = new SearchEvent("Any", SearchOperation.SearchType.CONTENT_TYPE_SEARCH, SearchEvent.UnsetType.NO_UNSET);
        SearchEvent expectedEvent = new SearchEvent("Any", SearchOperation.SearchType.CONTENT_TYPE_SEARCH, SearchEvent.UnsetType.NO_UNSET);
        Assert.assertTrue(event.equals(expectedEvent));
    }


    @Test
    public void differentEventReturnsFalse()
    {
        SearchEvent event = new SearchEvent("Any", SearchOperation.SearchType.FAVORITE_SEARCH, SearchEvent.UnsetType.NO_UNSET);
        SearchEvent expectedEvent = new SearchEvent("Any", SearchOperation.SearchType.CONTENT_TYPE_SEARCH, SearchEvent.UnsetType.NO_UNSET);
        Assert.assertNotEquals(event,expectedEvent);
    }


    @Test
    public void differentSearcEventReturnsFalse()
    {
        SearchEvent event = new SearchEvent("Any", SearchOperation.SearchType.FAVORITE_SEARCH, SearchEvent.UnsetType.UNSET_BOTTOM_NAV_BAR);
        SearchEvent expectedEvent = new SearchEvent("Any", SearchOperation.SearchType.FAVORITE_SEARCH, SearchEvent.UnsetType.NO_UNSET);
        Assert.assertNotEquals(event,expectedEvent);
    }

    @Test
    public void differentSearcWordReturnsFalse()
    {
        SearchEvent event = new SearchEvent("Any", SearchOperation.SearchType.FAVORITE_SEARCH, SearchEvent.UnsetType.NO_UNSET);
        SearchEvent expectedEvent = new SearchEvent("Any_1", SearchOperation.SearchType.FAVORITE_SEARCH, SearchEvent.UnsetType.NO_UNSET);
        Assert.assertNotEquals(event,expectedEvent);
    }
}
