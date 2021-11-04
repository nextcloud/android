/*
 *  Copyright (C) 2015  Jon Griffiths (jon_p_griffiths@yahoo.com)
 *  Copyright (C) 2013  Dominik Schürmann <dominik@dominikschuermann.de>
 *  Copyright (C) 2010-2011  Lukas Aichbauer
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package third_parties.sufficientlysecure;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.nextcloud.client.account.User;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.files.downloader.PostUploadAction;
import com.nextcloud.client.files.downloader.Request;
import com.nextcloud.client.files.downloader.TransferManagerConnection;
import com.nextcloud.client.files.downloader.UploadRequest;
import com.nextcloud.client.files.downloader.UploadTrigger;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.NameCollisionPolicy;
import com.owncloud.android.lib.common.utils.Log_OC;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactoryImpl;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.FbType;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.FreeBusy;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Transp;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.model.property.XProperty;
import net.fortuna.ical4j.util.CompatibilityHints;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@SuppressLint("NewApi")
public class SaveCalendar implements Injectable {
    private static final String TAG = "ICS_SaveCalendar";

    private final PropertyFactoryImpl mPropertyFactory = PropertyFactoryImpl.getInstance();
    private TimeZoneRegistry mTzRegistry;
    private final Set<TimeZone> mInsertedTimeZones = new HashSet<>();
    private final Set<String> mFailedOrganisers = new HashSet<>();
    boolean mAllCols;
    private final Context activity;
    private final AndroidCalendar selectedCal;
    private final AppPreferences preferences;
    private final User user;

    // UID generation
    long mUidMs = 0;
    String mUidTail = null;

    private static final List<String> STATUS_ENUM = Arrays.asList("TENTATIVE", "CONFIRMED", "CANCELLED");
    private static final List<String> CLASS_ENUM = Arrays.asList(null, "CONFIDENTIAL", "PRIVATE", "PUBLIC");
    private static final List<String> AVAIL_ENUM = Arrays.asList(null, "FREE", "BUSY-TENTATIVE");

    private static final String[] EVENT_COLS = new String[]{
        Events._ID, Events.ORIGINAL_ID, Events.UID_2445, Events.TITLE, Events.DESCRIPTION,
        Events.ORGANIZER, Events.EVENT_LOCATION, Events.STATUS, Events.ALL_DAY, Events.RDATE,
        Events.RRULE, Events.DTSTART, Events.EVENT_TIMEZONE, Events.DURATION, Events.DTEND,
        Events.EVENT_END_TIMEZONE, Events.ACCESS_LEVEL, Events.AVAILABILITY, Events.EXDATE,
        Events.EXRULE, Events.CUSTOM_APP_PACKAGE, Events.CUSTOM_APP_URI, Events.HAS_ALARM
    };

    private static final String[] REMINDER_COLS = new String[]{
        Reminders.MINUTES, Reminders.METHOD
    };

    public SaveCalendar(Context activity, AndroidCalendar calendar, AppPreferences preferences, User user) {
        this.activity = activity; // TODO rename
        this.selectedCal = calendar;
        this.preferences = preferences;
        this.user = user;
    }

    public void start() throws Exception {
        mInsertedTimeZones.clear();
        mFailedOrganisers.clear();
        mAllCols = false;

        String file = selectedCal.mDisplayName + "_" +
            DateFormat.format("yyyy-MM-dd_HH-mm-ss", java.util.Calendar.getInstance()).toString() +
            ".ics";

        File fileName = new File(activity.getCacheDir(), file);

        Log_OC.i(TAG, "Save id " + selectedCal.mIdStr + " to file " + fileName.getAbsolutePath());

        String name = activity.getPackageName();
        String ver;
        try {
            ver = activity.getPackageManager().getPackageInfo(name, 0).versionName;
        } catch (NameNotFoundException e) {
            ver = "Unknown Build";
        }

        String prodId = "-//" + selectedCal.mOwner + "//iCal Import/Export " + ver + "//EN";
        Calendar cal = new Calendar();
        cal.getProperties().add(new ProdId(prodId));
        cal.getProperties().add(Version.VERSION_2_0);
        cal.getProperties().add(Method.PUBLISH);
        cal.getProperties().add(CalScale.GREGORIAN);

        if (selectedCal.mTimezone != null) {
            // We don't write any events with floating times, but export this
            // anyway so the default timezone for new events is correct when
            // the file is imported into a system that supports it.
            cal.getProperties().add(new XProperty("X-WR-TIMEZONE", selectedCal.mTimezone));
        }

        // query events
        ContentResolver resolver = activity.getContentResolver();
        int numberOfCreatedUids = 0;
        if (Events.UID_2445 != null) {
            numberOfCreatedUids = ensureUids(activity, resolver, selectedCal);
        }
        boolean relaxed = true; // settings.getIcal4jValidationRelaxed(); // TODO is this option needed? default true
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_VALIDATION, relaxed);
        List<VEvent> events = getEvents(resolver, selectedCal, cal);

        for (VEvent v : events) {
            cal.getComponents().add(v);
        }

        new CalendarOutputter().output(cal, new FileOutputStream(fileName));

        Resources res = activity.getResources();
        String msg = res.getQuantityString(R.plurals.wrote_n_events_to, events.size(), events.size(), file);
        if (numberOfCreatedUids > 0) {
            msg += "\n" + res.getQuantityString(R.plurals.created_n_uids_to, numberOfCreatedUids, numberOfCreatedUids);
        }

        // TODO replace DisplayUtils.showSnackMessage(activity, msg);

        upload(fileName);
    }

    private int ensureUids(Context activity, ContentResolver resolver, AndroidCalendar cal) {
        String[] cols = new String[]{Events._ID};
        String[] args = new String[]{cal.mIdStr};
        Map<Long, String> newUids = new HashMap<>();
        Cursor cur = resolver.query(Events.CONTENT_URI, cols,
                                    Events.CALENDAR_ID + " = ? AND " + Events.UID_2445 + " IS NULL", args, null);
        while (cur.moveToNext()) {
            Long id = getLong(cur, Events._ID);
            String uid = generateUid();
            newUids.put(id, uid);
        }
        for (Long id : newUids.keySet()) {
            String uid = newUids.get(id);
            Uri updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id);
            ContentValues c = new ContentValues();
            c.put(Events.UID_2445, uid);
            resolver.update(updateUri, c, null, null);
            Log_OC.i(TAG, "Generated UID " + uid + " for event " + id);
        }
        return newUids.size();
    }

    private List<VEvent> getEvents(ContentResolver resolver, AndroidCalendar cal_src, Calendar cal_dst) {
        String where = Events.CALENDAR_ID + "=?";
        String[] args = new String[]{cal_src.mIdStr};
        String sortBy = Events.CALENDAR_ID + " ASC";
        Cursor cur;
        try {
            cur = resolver.query(Events.CONTENT_URI, mAllCols ? null : EVENT_COLS,
                                 where, args, sortBy);
        } catch (Exception except) {
            Log_OC.w(TAG, "Calendar provider is missing columns, continuing anyway");
            int n = 0;
            for (n = 0; n < EVENT_COLS.length; ++n) {
                if (EVENT_COLS[n] == null) {
                    Log_OC.e(TAG, "Invalid EVENT_COLS index " + Integer.toString(n));
                }
            }
            cur = resolver.query(Events.CONTENT_URI, null, where, args, sortBy);
        }

        DtStamp timestamp = new DtStamp(); // Same timestamp for all events

        // Collect up events and add them after any timezones
        List<VEvent> events = new ArrayList<>();
        while (cur.moveToNext()) {
            VEvent e = convertFromDb(cur, cal_dst, timestamp);
            if (e != null) {
                events.add(e);
                Log_OC.d(TAG, "Adding event: " + e.toString());
            }
        }
        cur.close();
        return events;
    }

    private String calculateFileName(final String displayName) {
        // Replace all non-alnum chars with '_'
        String stripped = displayName.replaceAll("[^a-zA-Z0-9_-]", "_");
        // Replace repeated '_' with a single '_'
        return stripped.replaceAll("(_)\\1{1,}", "$1");
    }

    private void getFileImpl(final String previousFile, final String suggestedFile,
                             final String[] result) {

        final EditText input = new EditText(activity);
        input.setHint(R.string.destination_filename);
        input.setText(previousFile);
        input.selectAll();

        final int ok = android.R.string.ok;
        final int cancel = android.R.string.cancel;
        final int suggest = R.string.suggest;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        AlertDialog dlg = builder.setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.enter_destination_filename)
            .setView(input)
            .setPositiveButton(ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface iface, int id) {
                    result[0] = input.getText().toString();
                }
            })
            .setNeutralButton(suggest, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface iface, int id) {
                }
            })
            .setNegativeButton(cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface iface, int id) {
                    result[0] = "";
                }
            })
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface iface) {
                    result[0] = "";
                }
            })
            .create();
        int state = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE;
        dlg.getWindow().setSoftInputMode(state);
        dlg.show();
        // Overriding 'Suggest' here prevents it from closing the dialog
        dlg.getButton(DialogInterface.BUTTON_NEUTRAL)
            .setOnClickListener(new View.OnClickListener() {
                public void onClick(View onClick) {
                    input.setText(suggestedFile);
                    input.setSelection(input.getText().length());
                }
            });
    }

    private VEvent convertFromDb(Cursor cur, Calendar cal, DtStamp timestamp) {
        Log_OC.d(TAG, "cursor: " + DatabaseUtils.dumpCurrentRowToString(cur));

        if (hasStringValue(cur, Events.ORIGINAL_ID)) {
            // FIXME: Support these edited instances
            Log_OC.w(TAG, "Ignoring edited instance of a recurring event");
            return null;
        }

        PropertyList l = new PropertyList();
        l.add(timestamp);
        copyProperty(l, Property.UID, cur, Events.UID_2445);

        String summary = copyProperty(l, Property.SUMMARY, cur, Events.TITLE);
        String description = copyProperty(l, Property.DESCRIPTION, cur, Events.DESCRIPTION);

        String organizer = getString(cur, Events.ORGANIZER);
        if (!TextUtils.isEmpty(organizer)) {
            // The check for mailto: here handles early versions of this code which
            // incorrectly left it in the organizer column.
            if (!organizer.startsWith("mailto:")) {
                organizer = "mailto:" + organizer;
            }
            try {
                l.add(new Organizer(organizer));
            } catch (URISyntaxException ignored) {
                if (!mFailedOrganisers.contains(organizer)) {
                    Log_OC.e(TAG, "Failed to create mailTo for organizer " + organizer);
                    mFailedOrganisers.add(organizer);
                }
            }
        }

        copyProperty(l, Property.LOCATION, cur, Events.EVENT_LOCATION);
        copyEnumProperty(l, Property.STATUS, cur, Events.STATUS, STATUS_ENUM);

        boolean allDay = TextUtils.equals(getString(cur, Events.ALL_DAY), "1");
        boolean isTransparent;
        DtEnd dtEnd = null;

        if (allDay) {
            // All day event
            isTransparent = true;
            Date start = getDateTime(cur, Events.DTSTART, null, null);
            Date end = getDateTime(cur, Events.DTEND, null, null);
            l.add(new DtStart(new Date(start)));

            if (end != null) {
                dtEnd = new DtEnd(new Date(end));
            } else {
                dtEnd = new DtEnd(utcDateFromMs(start.getTime() + DateUtils.DAY_IN_MILLIS));
            }

            l.add(dtEnd);
        } else {
            // Regular or zero-time event. Start date must be a date-time
            Date startDate = getDateTime(cur, Events.DTSTART, Events.EVENT_TIMEZONE, cal);
            l.add(new DtStart(startDate));

            // Use duration if we have one, otherwise end date
            if (hasStringValue(cur, Events.DURATION)) {
                isTransparent = getString(cur, Events.DURATION).equals("PT0S");
                if (!isTransparent) {
                    copyProperty(l, Property.DURATION, cur, Events.DURATION);
                }
            } else {
                String endTz = Events.EVENT_END_TIMEZONE;
                if (endTz == null) {
                    endTz = Events.EVENT_TIMEZONE;
                }
                Date end = getDateTime(cur, Events.DTEND, endTz, cal);
                dtEnd = new DtEnd(end);
                isTransparent = startDate.getTime() == end.getTime();
                if (!isTransparent) {
                    l.add(dtEnd);
                }
            }
        }

        copyEnumProperty(l, Property.CLASS, cur, Events.ACCESS_LEVEL, CLASS_ENUM);

        int availability = getInt(cur, Events.AVAILABILITY);
        if (availability > Events.AVAILABILITY_TENTATIVE) {
            availability = -1;     // Unknown/Invalid
        }

        if (isTransparent) {
            // This event is ordinarily transparent. If availability shows that its
            // not free, then mark it opaque.
            if (availability >= 0 && availability != Events.AVAILABILITY_FREE) {
                l.add(Transp.OPAQUE);
            }

        } else if (availability > Events.AVAILABILITY_BUSY) {
            // This event is ordinarily busy but differs, so output a FREEBUSY
            // period covering the time of the event
            FreeBusy fb = new FreeBusy();
            fb.getParameters().add(new FbType(AVAIL_ENUM.get(availability)));
            DateTime start = new DateTime(((DtStart) l.getProperty(Property.DTSTART)).getDate());

            if (dtEnd != null) {
                fb.getPeriods().add(new Period(start, new DateTime(dtEnd.getDate())));
            } else {
                Duration d = (Duration) l.getProperty(Property.DURATION);
                fb.getPeriods().add(new Period(start, d.getDuration()));
            }
            l.add(fb);
        }

        copyProperty(l, Property.RRULE, cur, Events.RRULE);
        copyProperty(l, Property.RDATE, cur, Events.RDATE);
        copyProperty(l, Property.EXRULE, cur, Events.EXRULE);
        copyProperty(l, Property.EXDATE, cur, Events.EXDATE);
        if (TextUtils.isEmpty(getString(cur, Events.CUSTOM_APP_PACKAGE))) {
            // Only copy URL if there is no app i.e. we probably imported it.
            copyProperty(l, Property.URL, cur, Events.CUSTOM_APP_URI);
        }

        VEvent e = new VEvent(l);

        if (getInt(cur, Events.HAS_ALARM) == 1) {
            // Add alarms

            String s = summary == null ? (description == null ? "" : description) : summary;
            Description desc = new Description(s);

            ContentResolver resolver = activity.getContentResolver();
            long eventId = getLong(cur, Events._ID);
            Cursor alarmCur;
            alarmCur = Reminders.query(resolver, eventId, mAllCols ? null : REMINDER_COLS);
            while (alarmCur.moveToNext()) {
                int mins = getInt(alarmCur, Reminders.MINUTES);
                if (mins == -1) {
                    mins = 60;     // FIXME: Get the real default
                }

                // FIXME: We should support other types if possible
                int method = getInt(alarmCur, Reminders.METHOD);
                if (method == Reminders.METHOD_DEFAULT || method == Reminders.METHOD_ALERT) {
                    VAlarm alarm = new VAlarm(new Dur(0, 0, -mins, 0));
                    alarm.getProperties().add(Action.DISPLAY);
                    alarm.getProperties().add(desc);
                    e.getAlarms().add(alarm);
                }
            }
            alarmCur.close();
        }

        return e;
    }

    private int getColumnIndex(Cursor cur, String dbName) {
        return dbName == null ? -1 : cur.getColumnIndex(dbName);
    }

    private String getString(Cursor cur, String dbName) {
        int i = getColumnIndex(cur, dbName);
        return i == -1 ? null : cur.getString(i);
    }

    private long getLong(Cursor cur, String dbName) {
        int i = getColumnIndex(cur, dbName);
        return i == -1 ? -1 : cur.getLong(i);
    }

    private int getInt(Cursor cur, String dbName) {
        int i = getColumnIndex(cur, dbName);
        return i == -1 ? -1 : cur.getInt(i);
    }

    private boolean hasStringValue(Cursor cur, String dbName) {
        int i = getColumnIndex(cur, dbName);
        return i != -1 && !TextUtils.isEmpty(cur.getString(i));
    }

    private Date utcDateFromMs(long ms) {
        // This date will be UTC provided the default false value of the iCal4j property
        // "net.fortuna.ical4j.timezone.date.floating" has not been changed.
        return new Date(ms);
    }

    private boolean isUtcTimeZone(final String tz) {
        if (TextUtils.isEmpty(tz)) {
            return true;
        }
        final String utz = tz.toUpperCase(Locale.US);
        return utz.equals("UTC") || utz.equals("UTC-0") || utz.equals("UTC+0") || utz.endsWith("/UTC");
    }

    private Date getDateTime(Cursor cur, String dbName, String dbTzName, Calendar cal) {
        int i = getColumnIndex(cur, dbName);
        if (i == -1 || cur.isNull(i)) {
            Log_OC.e(TAG, "No valid " + dbName + " column found, index: " + Integer.toString(i));
            return null;
        }

        if (cal == null) {
            return utcDateFromMs(cur.getLong(i));     // Ignore timezone for date-only dates
        } else if (dbTzName == null) {
            Log_OC.e(TAG, "No valid tz " + dbName + " column given");
        }

        String tz = getString(cur, dbTzName);
        final boolean isUtc = isUtcTimeZone(tz);

        DateTime dt = new DateTime(isUtc);
        if (dt.isUtc() != isUtc) {
            throw new RuntimeException("UTC mismatch after construction");
        }
        dt.setTime(cur.getLong(i));
        if (dt.isUtc() != isUtc) {
            throw new RuntimeException("UTC mismatch after setTime");
        }

        if (!isUtc) {
            if (mTzRegistry == null) {
                mTzRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();
                if (mTzRegistry == null) {
                    throw new RuntimeException("Failed to create TZ registry");
                }
            }
            TimeZone t = mTzRegistry.getTimeZone(tz);
            if (t == null) {
                Log_OC.e(TAG, "Unknown TZ " + tz + ", assuming UTC");
            } else {
                dt.setTimeZone(t);
                if (!mInsertedTimeZones.contains(t)) {
                    cal.getComponents().add(t.getVTimeZone());
                    mInsertedTimeZones.add(t);
                }
            }
        }
        return dt;
    }

    private String copyProperty(PropertyList l, String evName, Cursor cur, String dbName) {
        // None of the exceptions caught below should be able to be thrown AFAICS.
        try {
            String value = getString(cur, dbName);
            if (value != null) {
                Property p = mPropertyFactory.createProperty(evName);
                p.setValue(value);
                l.add(p);
                return value;
            }
        } catch (IOException | URISyntaxException | ParseException ignored) {
        }
        return null;
    }

    private void copyEnumProperty(PropertyList l, String evName, Cursor cur, String dbName,
                                  List<String> vals) {
        // None of the exceptions caught below should be able to be thrown AFAICS.
        try {
            int i = getColumnIndex(cur, dbName);
            if (i != -1 && !cur.isNull(i)) {
                int value = (int) cur.getLong(i);
                if (value >= 0 && value < vals.size() && vals.get(value) != null) {
                    Property p = mPropertyFactory.createProperty(evName);
                    p.setValue(vals.get(value));
                    l.add(p);
                }
            }
        } catch (IOException | URISyntaxException | ParseException ignored) {
        }
    }

    // TODO move this to some common place
    private String generateUid() {
        // Generated UIDs take the form <ms>-<uuid>@nextcloud.com.
        if (mUidTail == null) {
            String uidPid = preferences.getUidPid();
            if (uidPid.length() == 0) {
                uidPid = UUID.randomUUID().toString().replace("-", "");
                preferences.setUidPid(uidPid);
            }
            mUidTail = uidPid + "@nextcloud.com";
        }

        mUidMs = Math.max(mUidMs, System.currentTimeMillis());
        String uid = mUidMs + mUidTail;
        mUidMs++;

        return uid;
    }

    private void upload(File file) {
        String backupFolder = activity.getResources().getString(R.string.calendar_backup_folder)
            + OCFile.PATH_SEPARATOR;

        Request request = new UploadRequest.Builder(user, file.getAbsolutePath(), backupFolder + file.getName())
            .setFileSize(file.length())
            .setNameConflicPolicy(NameCollisionPolicy.RENAME)
            .setCreateRemoteFolder(true)
            .setTrigger(UploadTrigger.USER)
            .setPostAction(PostUploadAction.MOVE_TO_APP)
            .setRequireWifi(false)
            .setRequireCharging(false)
            .build();

        TransferManagerConnection connection = new TransferManagerConnection(activity, user);
        connection.enqueue(request);
    }
}
