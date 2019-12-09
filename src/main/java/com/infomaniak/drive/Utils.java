package com.infomaniak.drive;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.owncloud.android.datamodel.OCFile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.owncloud.android.utils.MimeTypeUtil.getMimeTypeFromPath;

public class Utils {

    public static final String TEAM_SPACE_ETAG = "team_space";

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

    /**
     * Set the "team space" element in head of OCFiles ArrayList
     *
     * @param files The ArrayList of files
     * @return The new ArrayList with teamspace ahead
     */
    public static List<OCFile> setTeamSpaceFirst(List<OCFile> files) {
        List<OCFile> newList = new ArrayList<>();
        Iterator<OCFile> i = files.iterator();
        while (i.hasNext()) {
            OCFile currentFile = i.next();
            if (currentFile.getEtagOnServer().equals(TEAM_SPACE_ETAG)) {
                newList.add(0, currentFile);
                i.remove();
            }
        }

        newList.addAll(files);
        return newList;
    }
}
