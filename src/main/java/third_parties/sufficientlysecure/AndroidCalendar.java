/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2021 Tobias Kaminsky
 * Copyright (C) 2021 Nextcloud GmbH
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
package third_parties.sufficientlysecure;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;

import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.ArrayList;
import java.util.List;


public class AndroidCalendar {
    private static final String TAG = "ICS_AndroidCalendar";

    public long mId;
    public String mIdStr;
    public String mName;
    public String mDisplayName;
    public String mAccountName;
    public String mAccountType;
    public String mOwner;
    public boolean mIsActive;
    public String mTimezone;
    public int mNumEntries;

    private static final String[] CAL_COLS = new String[]{
        Calendars._ID,
        Calendars.DELETED,
        Calendars.NAME,
        Calendars.CALENDAR_DISPLAY_NAME,
        Calendars.ACCOUNT_NAME,
        Calendars.ACCOUNT_TYPE,
        Calendars.OWNER_ACCOUNT,
        Calendars.VISIBLE,
        Calendars.CALENDAR_TIME_ZONE};

    private static final String[] CAL_ID_COLS = new String[]{Events._ID};
    private static final String CAL_ID_WHERE = Events.CALENDAR_ID + "=?";

    // Load all available calendars.
    // If an empty list is returned the caller probably needs to enable calendar
    // read permissions in App Ops/XPrivacy etc.
    public static List<AndroidCalendar> loadAll(ContentResolver resolver) {

        if (missing(resolver, Calendars.CONTENT_URI) ||
            missing(resolver, Events.CONTENT_URI)) {
            return new ArrayList<>();
        }

        Cursor cur;
        try {
            cur = resolver.query(Calendars.CONTENT_URI, CAL_COLS, null, null, null);
        } catch (Exception except) {
            Log_OC.w(TAG, "Calendar provider is missing columns, continuing anyway");
            cur = resolver.query(Calendars.CONTENT_URI, null, null, null, null);
        }
        List<AndroidCalendar> calendars = new ArrayList<>(cur.getCount());

        while (cur.moveToNext()) {
            if (getLong(cur, Calendars.DELETED) != 0) {
                continue;
            }

            AndroidCalendar calendar = new AndroidCalendar();
            calendar.mId = getLong(cur, Calendars._ID);
            if (calendar.mId == -1) {
                continue;
            }
            calendar.mIdStr = getString(cur, Calendars._ID);
            calendar.mName = getString(cur, Calendars.NAME);
            calendar.mDisplayName = getString(cur, Calendars.CALENDAR_DISPLAY_NAME);
            calendar.mAccountName = getString(cur, Calendars.ACCOUNT_NAME);
            calendar.mAccountType = getString(cur, Calendars.ACCOUNT_TYPE);
            calendar.mOwner = getString(cur, Calendars.OWNER_ACCOUNT);
            calendar.mIsActive = getLong(cur, Calendars.VISIBLE) == 1;
            calendar.mTimezone = getString(cur, Calendars.CALENDAR_TIME_ZONE);

            final String[] args = new String[]{calendar.mIdStr};
            Cursor eventsCur = resolver.query(Events.CONTENT_URI, CAL_ID_COLS, CAL_ID_WHERE, args, null);
            calendar.mNumEntries = eventsCur.getCount();
            eventsCur.close();
            calendars.add(calendar);
        }
        cur.close();

        return calendars;
    }

    private static int getColumnIndex(Cursor cur, String dbName) {
        return dbName == null ? -1 : cur.getColumnIndex(dbName);
    }

    private static long getLong(Cursor cur, String dbName) {
        int i = getColumnIndex(cur, dbName);
        return i == -1 ? -1 : cur.getLong(i);
    }

    private static String getString(Cursor cur, String dbName) {
        int i = getColumnIndex(cur, dbName);
        return i == -1 ? null : cur.getString(i);
    }

    private static boolean missing(ContentResolver resolver, Uri uri) {
        // Determine if a provider is missing
        ContentProviderClient provider = resolver.acquireContentProviderClient(uri);
        if (provider != null) {
            provider.release();
        }
        return provider == null;
    }

    @Override
    public String toString() {
        return mDisplayName + " (" + mIdStr + ")";
    }

    private boolean differ(final String lhs, final String rhs) {
        if (lhs == null) {
            return rhs != null;
        }
        return rhs == null || !lhs.equals(rhs);
    }

    public boolean differsFrom(AndroidCalendar other) {
        return mId != other.mId ||
            mIsActive != other.mIsActive ||
            mNumEntries != other.mNumEntries ||
            differ(mName, other.mName) ||
            differ(mDisplayName, other.mDisplayName) ||
            differ(mAccountName, other.mAccountName) ||
            differ(mAccountType, other.mAccountType) ||
            differ(mOwner, other.mOwner) ||
            differ(mTimezone, other.mTimezone);
    }
}
