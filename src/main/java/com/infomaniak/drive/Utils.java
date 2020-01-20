package com.infomaniak.drive;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.utils.DisplayUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.owncloud.android.utils.MimeTypeUtil.getMimeTypeFromPath;

public class Utils {

    private static final String COMMON_DOCUMENTS_NAME = "Common documents"; // Change if needed
    private static final String SHARED_NAME = "Shared"; // Change if needed
    private static final String IK_TAG = "Infomaniak Error";

    /**
     * If open in browser return true
     *
     * @param activity for intent
     * @param file
     * @return if open
     */
    public static boolean openOnlyOffice(Activity activity, OCFile file) {
        String type;
        if (isDoc(file)) {
            type = "text";
        } else if (isSpreadsheet(file)) {
            type = "spreadsheet";
        } else if (isPresentation(file)) {
            type = "presentation";
        } else {
            return false;
        }

        Pattern pattern = Pattern.compile(".+?@(\\d+)\\.connect\\.drive\\.infomaniak\\.com.*");
        Matcher matcher = pattern.matcher(file.getStoragePath());
        String driveID;
        if (matcher.find() && matcher.groupCount() == 1) {
            driveID = matcher.group(1);
        } else {
            return false;
        }

        String fileID = file.getRemoteId().replaceAll("^0*", "");

        String url = "https://drive.infomaniak.com/app/drive/" + driveID + "/preview/" + type + "/" + fileID;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        activity.startActivity(intent);
        return true;
    }

    /**
     * @param file the file to be analyzed
     * @return 'True' if the file is doc file application-dependent, like .doc or .docx
     */
    public static boolean isDoc(OCFile file) {
        return isDoc(file.getMimeType()) || isDoc(getMimeTypeFromPath(file.getRemotePath()));
    }

    /**
     * @param file the file to be analyzed
     * @return 'True' if the file is a vcard
     */
    public static boolean isSpreadsheet(OCFile file) {
        return isSpreadsheet(file.getMimeType()) || isSpreadsheet(getMimeTypeFromPath(file.getRemotePath()));
    }

    /**
     * @param file the file to be analyzed
     * @return 'True' if the file is a vcard
     */
    public static boolean isPresentation(OCFile file) {
        return isPresentation(file.getMimeType()) || isPresentation(getMimeTypeFromPath(file.getRemotePath()));
    }

    /**
     * @return 'True' if mime type defines doc
     */
    private static boolean isDoc(String mimeType) {
        return mimeType != null && (mimeType.toLowerCase(Locale.ROOT).startsWith("application/msword") ||
            mimeType.toLowerCase(Locale.ROOT).startsWith("application/vnd.ms-word") ||
            mimeType.toLowerCase(Locale.ROOT).startsWith("application/vnd.oasis.opendocument.text") ||
            mimeType.toLowerCase(Locale.ROOT).startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml"));
    }

    /**
     * @return 'True' if mime type defines spreadsheet
     */
    private static boolean isSpreadsheet(String mimeType) {
        return mimeType != null && (mimeType.toLowerCase(Locale.ROOT).startsWith("application/vnd.ms-excel") ||
            mimeType.toLowerCase(Locale.ROOT).startsWith("application/msexcel") ||
            mimeType.toLowerCase(Locale.ROOT).startsWith("application/x-msexcel") ||
            mimeType.toLowerCase(Locale.ROOT).startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml") ||
            mimeType.toLowerCase(Locale.ROOT).startsWith("application/vnd.oasis.opendocument.spreadsheet"));
    }

    /**
     * @return 'True' if mime type defines presentation
     */
    private static boolean isPresentation(String mimeType) {
        return mimeType != null && (mimeType.toLowerCase(Locale.ROOT).startsWith("application/powerpoint") ||
            mimeType.toLowerCase(Locale.ROOT).startsWith("application/mspowerpoint") ||
            mimeType.toLowerCase(Locale.ROOT).startsWith("application/vnd.ms-powerpoint") ||
            mimeType.toLowerCase(Locale.ROOT).startsWith("application/x-mspowerpoint") ||
            mimeType.toLowerCase(Locale.ROOT).startsWith("application/vnd.openxmlformats-officedocument.presentationml") ||
            mimeType.toLowerCase(Locale.ROOT).startsWith("application/vnd.oasis.opendocument.presentation"));
    }

    public static int sortInfomaniakFolder(OCFile file1, OCFile file2) {
        if (file1.getFileName().equals(COMMON_DOCUMENTS_NAME)) {
            return -1;
        } else if (file2.getFileName().equals(COMMON_DOCUMENTS_NAME)) {
            return 1;
        } else if (file1.getFileName().equals(SHARED_NAME)) {
            return -1;
        } else if (file2.getFileName().equals(SHARED_NAME)) {
            return 1;
        } else {
            return 0;
        }
    }

    public static String getUserEmail(String name) {
        String[] splitEmail = name.split("@");
        return splitEmail[0] + "@" + splitEmail[1];
    }

    public static void launchKSync(Activity activity) {
        Intent davDroidLoginIntent = activity.getPackageManager().getLaunchIntentForPackage("com.infomaniak.sync");
        if (davDroidLoginIntent != null) {
            activity.startActivity(davDroidLoginIntent);
        } else {
            // DAVdroid not installed
            Intent installIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.infomaniak.sync"));

            // launch market(s)
            if (installIntent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(installIntent);
            } else {
                // no f-droid market app or Play store installed --> launch browser for f-droid url
                Intent downloadIntent = new Intent(Intent.ACTION_VIEW,
                                                   Uri.parse("https://f-droid.org/repository/browse/?fdid=com.infomaniak.sync"));
                DisplayUtils.startIntentIfAppAvailable(downloadIntent, activity, R.string.no_browser_available);

                DisplayUtils.showSnackMessage(activity, R.string.prefs_calendar_contacts_no_store_error);
            }
        }
    }
}
