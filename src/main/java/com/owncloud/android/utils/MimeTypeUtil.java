/*
 * ownCloud Android client application
 * <p>
 * Copyright (C) 2016 ownCloud Inc.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.nextcloud.client.account.User;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.network.WebdavEntry;
import com.owncloud.android.lib.resources.files.model.ServerFileInterface;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeDrawableUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * <p>Helper class for detecting the right icon for a file or folder,
 * based on its mime type and file extension.</p>
 *
 * This class maintains all the necessary mappings fot these detections.<br/>
 * In order to add further mappings, there are up to three look up maps that need further values:
 * <ol>
 *     <li>
 *         {@link MimeTypeUtil#FILE_EXTENSION_TO_MIMETYPE_MAPPING}<br/>
 *         to add a new file extension to mime type mapping
 *     </li>
 *     <li>
 *         {@link MimeTypeUtil#MIMETYPE_TO_ICON_MAPPING}<br/>
 *         to add a new mapping of a mime type to an icon mapping
 *     </li>
 *     <li>
 *         {@link MimeTypeUtil#MAIN_MIMETYPE_TO_ICON_MAPPING}<br/>
 *         to add a new mapping for the main part of a mime type.
 *         This is a list of fallback mappings in case there is no mapping for the complete mime type
 *     </li>
 * </ol>
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class MimeTypeUtil {
    /** Mapping: icon for mime type */
    private static final Map<String, Integer> MIMETYPE_TO_ICON_MAPPING = new HashMap<>();
    /** Mapping: icon for main mime type (first part of a mime type declaration). */
    private static final Map<String, Integer> MAIN_MIMETYPE_TO_ICON_MAPPING = new HashMap<>();
    /** Mapping: mime type for file extension. */
    private static final Map<String, List<String>> FILE_EXTENSION_TO_MIMETYPE_MAPPING = new HashMap<>();
    public static final String MIMETYPE_TEXT_MARKDOWN = "text/markdown";

    static {
        populateFileExtensionMimeTypeMapping();
        populateMimeTypeIconMapping();
        populateMainMimeTypeMapping();
    }

    private MimeTypeUtil() {
        // utility class -> private constructor
    }

    /**
     * Returns the Drawable of an image to use as icon associated to a known MIME type.
     *
     * @param mimetype MIME type string; if NULL, the method tries to guess it from the extension in filename
     * @param filename Name, with extension.
     * @return Drawable of an image resource.
     */
    public static Drawable getFileTypeIcon(String mimetype, String filename, Context context) {
        return getFileTypeIcon(mimetype, filename, null, context);
    }

    /**
     * Returns the Drawable of an image to use as icon associated to a known MIME type.
     *
     * @param mimetype MIME type string; if NULL, the method tries to guess it from the extension in filename
     * @param filename Name, with extension.
     * @param user user which color should be used
     * @return Drawable of an image resource.
     */
    @Nullable
    public static Drawable getFileTypeIcon(String mimetype, String filename, @Nullable User user, Context context) {
        if (context != null) {
            int iconId = MimeTypeUtil.getFileTypeIconId(mimetype, filename);
            Drawable icon = ContextCompat.getDrawable(context, iconId);

            if (R.drawable.file_zip == iconId) {
                ThemeDrawableUtils.tintDrawable(icon, ThemeColorUtils.primaryColor(user, true, context));
            }

            return icon;
        } else {
            return null;
        }
    }

    /**
     * Returns the resource identifier of an image to use as icon associated to a known MIME type.
     *
     * @param mimetype MIME type string; if NULL, the method tries to guess it from the extension in filename
     * @param filename Name, with extension.
     * @return Identifier of an image resource.
     */
    public static int getFileTypeIconId(String mimetype, String filename) {
        List<String> possibleMimeTypes;
        if (mimetype == null) {
            possibleMimeTypes = determineMimeTypesByFilename(filename);
        } else {
            possibleMimeTypes = Collections.singletonList(mimetype);
        }

        return determineIconIdByMimeTypeList(possibleMimeTypes);
    }

    /**
     * Returns the resource identifier of an image to use as icon associated to a type of folder.
     *
     * @param isSharedViaUsers flag if the folder is shared via the users system
     * @param isSharedViaLink  flag if the folder is publicly shared via link
     * @return Identifier of an image resource.
     */
    public static Drawable getFolderTypeIcon(boolean isSharedViaUsers, boolean isSharedViaLink, boolean isEncrypted,
                                             WebdavEntry.MountType mountType, Context context) {
        return getFolderTypeIcon(isSharedViaUsers, isSharedViaLink, isEncrypted, null, mountType, context);
    }

    /**
     * Returns the resource identifier of an image to use as icon associated to a type of folder.
     *
     * @param isSharedViaUsers flag if the folder is shared via the users system
     * @param isSharedViaLink flag if the folder is publicly shared via link
     * @param isEncrypted flag if the folder is encrypted
     * @param user user which color should be used
     * @return Identifier of an image resource.
     */
    public static Drawable getFolderTypeIcon(boolean isSharedViaUsers,
                                             boolean isSharedViaLink,
                                             boolean isEncrypted,
                                             @Nullable User user,
                                             WebdavEntry.MountType mountType,
                                             Context context) {
        int drawableId;

        if (isSharedViaLink) {
            drawableId = R.drawable.folder_shared_link;
        } else if (isSharedViaUsers) {
            drawableId = R.drawable.folder_shared_users;
        } else if (isEncrypted) {
            drawableId = R.drawable.folder_encrypted;
        } else if (WebdavEntry.MountType.EXTERNAL == mountType) {
            drawableId = R.drawable.folder_external;
        } else if (WebdavEntry.MountType.GROUP == mountType) {
            drawableId = R.drawable.folder_group;
        } else {
            drawableId = R.drawable.folder;
        }

        int color = ThemeColorUtils.primaryColor(user != null ? user.toPlatformAccount() : null,
                                            true,
                                            context);
        return ThemeDrawableUtils.tintDrawable(drawableId, color);
    }

    public static Drawable getDefaultFolderIcon(Context context) {
        return getFolderTypeIcon(false, false, false, WebdavEntry.MountType.INTERNAL, context);
    }


    /**
     * Returns a single MIME type of all the possible, by inspection of the file extension, and taking
     * into account the MIME types known by ownCloud first.
     *
     * @param filename      Name of file
     * @return A single MIME type, "application/octet-stream" for unknown file extensions.
     */
    public static String getBestMimeTypeByFilename(String filename) {
        return getMimeTypeFromCandidates(determineMimeTypesByFilename(filename));
    }

    private static String getMimeTypeFromCandidates(List<String> candidates) {
        if (candidates == null || candidates.size() < 1) {
            return "application/octet-stream";
        }
        return candidates.get(0);
    }

    public static boolean isMedia(String mimeType) {
        return isImage(mimeType) || isVideo(mimeType) || isAudio(mimeType);
    }

    public static boolean isImageOrVideo(String mimeType) {
        return isImage(mimeType) || isVideo(mimeType);
    }

    public static boolean isImageOrVideo(File file) {
        return isImage(file) || isVideo(file);
    }

    public static boolean isImageOrVideo(ServerFileInterface file) {
        return isImage(file) || isVideo(file);
    }

    /**
     * @return 'True' if the mime type defines image
     */
    public static boolean isImage(String mimeType) {
        return mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("image/") &&
                !mimeType.toLowerCase(Locale.ROOT).contains("djvu");
    }

    /**
     * @return 'True' the mime type defines video
     */
    public static boolean isVideo(String mimeType) {
        return mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("video/");
    }

    /**
     * @return 'True' the mime type defines audio
     */
    public static boolean isAudio(String mimeType) {
        return mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("audio/");
    }

    /**
     * @return 'True' if mime type defines text
     */
    private static boolean isText(String mimeType) {
        return mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("text/");
    }

    /**
     * @return 'True' if mime type defines vcard
     */
    public static boolean isVCard(String mimeType) {
        return "text/vcard".equalsIgnoreCase(mimeType);
    }

    /**
     * Checks if file passed is a video.
     *
     * @param file the file to be checked
     * @return 'True' the mime type defines video
     */
    public static boolean isVideo(File file) {
        return isVideo(extractMimeType(file));
    }

    /**
     * Checks if file passed is an image.
     *
     * @param file the file to be checked
     * @return 'True' the mime type defines video
     */
    public static boolean isImage(File file) {
        return isImage(extractMimeType(file));
    }

    public static boolean isSVG(OCFile file) {
        return "image/svg+xml".equalsIgnoreCase(file.getMimeType());
    }

    /**
     * @param file the file to be analyzed
     * @return 'True' if the file contains audio
     */
    public static boolean isAudio(OCFile file) {
        return MimeTypeUtil.isAudio(file.getMimeType());
    }

    /**
     * @param file the file to be analyzed
     * @return 'True' if the file contains video
     */
    public static boolean isVideo(ServerFileInterface file) {
        return MimeTypeUtil.isVideo(file.getMimeType());
    }

    /**
     * @param file the file to be analyzed
     * @return 'True' if the file contains an image
     */
    public static boolean isImage(ServerFileInterface file) {
        return MimeTypeUtil.isImage(file.getMimeType())
            || MimeTypeUtil.isImage(getMimeTypeFromPath(file.getRemotePath()));
    }

    /**
     * @param file the file to be analyzed
     * @return 'True' if the file is simple text (e.g. not application-dependent, like .doc or .docx)
     */
    public static boolean isText(OCFile file) {
        return MimeTypeUtil.isText(file.getMimeType())
            || MimeTypeUtil.isText(getMimeTypeFromPath(file.getRemotePath()));
    }

    /**
     * @param file the file to be analyzed
     * @return 'True' if the file is a vcard
     */
    public static boolean isVCard(OCFile file) {
        return isVCard(file.getMimeType()) || isVCard(getMimeTypeFromPath(file.getRemotePath()));
    }

    public static boolean isFolder(String mimeType) {
        return MimeType.DIRECTORY.equalsIgnoreCase(mimeType);
    }

    /**
     * Extracts the mime type for the given file.
     *
     * @param file the file to be analyzed
     * @return the file's mime type
     */
    private static String extractMimeType(File file) {
        Uri selectedUri = Uri.fromFile(file);
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString().toLowerCase(Locale.ROOT));
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
    }

    /**
     * determines the icon based on the mime type.
     *
     * @param mimetypes the mimetypes
     * @return the icon id, R.drawable.file if the mime type could not be matched at all or was {@code null}
     */
    private static int determineIconIdByMimeTypeList(List<String> mimetypes) {
        // no mime type leads to file
        if (mimetypes == null || mimetypes.size() < 1) {
            return R.drawable.file;
        } else {

            // search for full mime type mapping
            for (String mimetype : mimetypes) {
                Integer iconId = MIMETYPE_TO_ICON_MAPPING.get(mimetype);

                if (iconId != null) {
                    return iconId;
                }
            }

            // fallback to main mime type part mapping
            for (String mimetype : mimetypes) {
                String mainMimetypePart = mimetype.split("/")[0];

                Integer iconId = MAIN_MIMETYPE_TO_ICON_MAPPING.get(mainMimetypePart);
                if (iconId != null) {
                    return iconId;
                }
            }
        }

        // no match found at all, falling back to file
        return R.drawable.file;
    }

    /**
     * determines the list of possible mime types for the given file, based on its extension.
     *
     * @param filename the file name
     * @return list of possible mime types (ordered), empty list in case no mime types found
     */
    private static List<String> determineMimeTypesByFilename(String filename) {
        return determineMimeTypesByExtension(getExtension(filename));
    }

    /**
     * determines the list of possible mime types for the given file, based on its extension.
     *
     * @param extension the file extension
     * @return list of possible mime types (ordered), empty list in case no mime types found
     */
    private static List<String> determineMimeTypesByExtension(String extension) {
        // try detecting the mimetype based on the web app logic equivalent
        List<String> mimeTypeList = FILE_EXTENSION_TO_MIMETYPE_MAPPING.get(extension);
        if (mimeTypeList != null && mimeTypeList.size() > 0) {
            return mimeTypeList;
        } else {
            // try detecting the mime type via android itself
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mimeType != null) {
                return Collections.singletonList(mimeType);
            } else {
                return new ArrayList<>();
            }
        }
    }

    public static String getMimeTypeFromPath(String path) {
        String extension = "";
        int pos = path.lastIndexOf('.');
        if (pos >= 0) {
            extension = path.substring(pos + 1).toLowerCase(Locale.ROOT);
        }

        // Ask OS for mimetype
        String result = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        // fallback to Nc mimetype mapping
        if (result == null) {
            result = getMimeTypeFromCandidates(determineMimeTypesByExtension(extension));
        }

        return (result != null) ? result : "";
    }

    /**
     * provides the file extension of a given filename.
     *
     * @param filename the filename
     * @return the file extension
     */
    private static String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * populates the mapping list:  full mime type --> icon.
     */
    private static void populateMimeTypeIconMapping() {
        MIMETYPE_TO_ICON_MAPPING.put("application/coreldraw", R.drawable.file_image);
        MIMETYPE_TO_ICON_MAPPING.put("application/epub+zip", R.drawable.file_text);
        MIMETYPE_TO_ICON_MAPPING.put("application/font-sfnt", R.drawable.file_image);
        MIMETYPE_TO_ICON_MAPPING.put("application/font-woff", R.drawable.file_image);
        MIMETYPE_TO_ICON_MAPPING.put("application/gpx+xml", R.drawable.file_location);
        MIMETYPE_TO_ICON_MAPPING.put("application/illustrator", R.drawable.file_image);
        MIMETYPE_TO_ICON_MAPPING.put("application/internet-shortcut", R.drawable.file_link);
        MIMETYPE_TO_ICON_MAPPING.put("application/java", R.drawable.file_application);
        MIMETYPE_TO_ICON_MAPPING.put("application/javascript", R.drawable.file_code);
        MIMETYPE_TO_ICON_MAPPING.put("application/json", R.drawable.file_code);
        MIMETYPE_TO_ICON_MAPPING.put("application/msaccess", R.drawable.file);
        MIMETYPE_TO_ICON_MAPPING.put("application/msexcel", R.drawable.file_xls);
        MIMETYPE_TO_ICON_MAPPING.put("application/msonenote", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/mspowerpoint", R.drawable.file_ppt);
        MIMETYPE_TO_ICON_MAPPING.put("application/msword", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/octet-stream", R.drawable.file);
        MIMETYPE_TO_ICON_MAPPING.put("application/postscript", R.drawable.file_image);
        MIMETYPE_TO_ICON_MAPPING.put("application/pdf", R.drawable.file_pdf);
        MIMETYPE_TO_ICON_MAPPING.put("application/rss+xml", R.drawable.file_code);
        MIMETYPE_TO_ICON_MAPPING.put("application/rtf", R.drawable.file);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.android.package-archive", R.drawable.file_zip);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.garmin.tcx+xml", R.drawable.file_location);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.google-earth.kml+xml", R.drawable.file_location);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.google-earth.kmz", R.drawable.file_location);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.lotus-wordpro", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-excel", R.drawable.file_xls);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-excel.addin.macroEnabled.12", R.drawable.file_xls);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-excel.sheet.binary.macroEnabled.12", R.drawable.file_xls);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-excel.sheet.macroEnabled.12", R.drawable.file_xls);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-excel.template.macroEnabled.12", R.drawable.file_xls);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-fontobject", R.drawable.file_image);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-powerpoint", R.drawable.file_ppt);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-powerpoint.addin.macroEnabled.12", R.drawable.file_ppt);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-powerpoint.presentation.macroEnabled.12", R.drawable.file_ppt);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-powerpoint.slideshow.macroEnabled.12", R.drawable.file_ppt);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-powerpoint.template.macroEnabled.12", R.drawable.file_ppt);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-visio.drawing.macroEnabled.12", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-visio.drawing", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-visio.stencil.macroEnabled.12", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-visio.stencil", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-visio.template.macroEnabled.12", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-visio.template", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-word.document.macroEnabled.12", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.ms-word.template.macroEnabled.12", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.oasis.opendocument.presentation", R.drawable.file_ppt);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.oasis.opendocument.presentation-template", R.drawable.file_ppt);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.oasis.opendocument.spreadsheet", R.drawable.file_xls);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.oasis.opendocument.spreadsheet-template", R.drawable.file_xls);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.oasis.opendocument.text", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.oasis.opendocument.text-master", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.oasis.opendocument.text-template", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.oasis.opendocument.text-web", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", R.drawable.file_ppt);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.openxmlformats-officedocument.presentationml.slideshow", R.drawable.file_ppt);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.openxmlformats-officedocument.presentationml.template", R.drawable.file_ppt);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", R.drawable.file_xls);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.openxmlformats-officedocument.spreadsheetml.template", R.drawable.file_xls);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.openxmlformats-officedocument.wordprocessingml.template", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.visio", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/vnd.wordperfect", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-7z-compressed", R.drawable.file_zip);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-bin", R.drawable.file_application);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-bzip2", R.drawable.file_zip);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-cbr", R.drawable.file_text);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-compressed", R.drawable.file_zip);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-dcraw", R.drawable.file_image);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-deb", R.drawable.file_zip);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-fictionbook+xml", R.drawable.file_text);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-font", R.drawable.file_image);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-gimp", R.drawable.file_image);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-gzip", R.drawable.file_zip);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-love-game", R.drawable.file);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-mobipocket-ebook", R.drawable.file_text);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-ms-dos-executable", R.drawable.file_application);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-msdos-program", R.drawable.file_application);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-msi", R.drawable.file_application);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-iwork-numbers-sffnumbers", R.drawable.file_xls);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-iwork-keynote-sffkey", R.drawable.file_ppt);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-iwork-pages-sffpages", R.drawable.file_doc);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-perl", R.drawable.file_code);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-photoshop", R.drawable.file_image);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-php", R.drawable.file_code);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-rar-compressed", R.drawable.file_zip);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-shockwave-flash", R.drawable.file_application);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-tar", R.drawable.file_zip);
        MIMETYPE_TO_ICON_MAPPING.put("application/x-tex", R.drawable.file_text);
        MIMETYPE_TO_ICON_MAPPING.put("application/xml", R.drawable.file_text);
        MIMETYPE_TO_ICON_MAPPING.put("application/yaml", R.drawable.file_code);
        MIMETYPE_TO_ICON_MAPPING.put("application/zip", R.drawable.file_zip);
        MIMETYPE_TO_ICON_MAPPING.put("database", R.drawable.file);
        MIMETYPE_TO_ICON_MAPPING.put("httpd/unix-directory", R.drawable.folder);
        MIMETYPE_TO_ICON_MAPPING.put("image/svg+xml", R.drawable.file_image);
        MIMETYPE_TO_ICON_MAPPING.put("image/vector", R.drawable.file_image);
        MIMETYPE_TO_ICON_MAPPING.put("text/calendar", R.drawable.file_calendar);
        MIMETYPE_TO_ICON_MAPPING.put("text/css", R.drawable.file_code);
        MIMETYPE_TO_ICON_MAPPING.put("text/csv", R.drawable.file_xls);
        MIMETYPE_TO_ICON_MAPPING.put("text/html", R.drawable.file_code);
        MIMETYPE_TO_ICON_MAPPING.put("text/vcard", R.drawable.file_vcard);
        MIMETYPE_TO_ICON_MAPPING.put("text/x-c", R.drawable.file_code);
        MIMETYPE_TO_ICON_MAPPING.put("text/x-c++src", R.drawable.file_code);
        MIMETYPE_TO_ICON_MAPPING.put("text/x-h", R.drawable.file_code);
        MIMETYPE_TO_ICON_MAPPING.put("text/x-java-source", R.drawable.file_code);
        MIMETYPE_TO_ICON_MAPPING.put("text/x-ldif", R.drawable.file_code);
        MIMETYPE_TO_ICON_MAPPING.put("text/x-python", R.drawable.file_code);
        MIMETYPE_TO_ICON_MAPPING.put("text/x-shellscript", R.drawable.file_code);
        MIMETYPE_TO_ICON_MAPPING.put("web", R.drawable.file_code);
        MIMETYPE_TO_ICON_MAPPING.put(MimeType.DIRECTORY, R.drawable.folder);
    }

    /**
     * populates the mapping list: main mime type --> icon.
     */
    private static void populateMainMimeTypeMapping() {
        MAIN_MIMETYPE_TO_ICON_MAPPING.put("audio", R.drawable.file_sound);
        MAIN_MIMETYPE_TO_ICON_MAPPING.put("database", R.drawable.file);
        MAIN_MIMETYPE_TO_ICON_MAPPING.put("httpd", R.drawable.file_zip);
        MAIN_MIMETYPE_TO_ICON_MAPPING.put("image", R.drawable.file_image);
        MAIN_MIMETYPE_TO_ICON_MAPPING.put("text", R.drawable.file_text);
        MAIN_MIMETYPE_TO_ICON_MAPPING.put("video", R.drawable.file_movie);
        MAIN_MIMETYPE_TO_ICON_MAPPING.put("web", R.drawable.file_code);
    }

    /**
     * populates the mapping list: file extension --> mime type.
     */
    private static void populateFileExtensionMimeTypeMapping() {
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("3gp", Collections.singletonList("video/3gpp"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("7z", Collections.singletonList("application/x-7z-compressed"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("accdb", Collections.singletonList("application/msaccess"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("ai", Collections.singletonList("application/illustrator"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("apk", Collections.singletonList("application/vnd.android.package-archive"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("arw", Collections.singletonList("image/x-dcraw"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("avi", Collections.singletonList("video/x-msvideo"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("bash", Collections.singletonList("text/x-shellscript"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("bat", Collections.singletonList("application/x-msdos-program"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("blend", Collections.singletonList("application/x-blender"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("bin", Collections.singletonList("application/x-bin"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("bmp", Collections.singletonList("image/bmp"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("bpg", Collections.singletonList("image/bpg"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("bz2", Collections.singletonList("application/x-bzip2"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("cb7", Collections.singletonList("application/x-cbr"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("cba", Collections.singletonList("application/x-cbr"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("cbr", Collections.singletonList("application/x-cbr"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("cbt", Collections.singletonList("application/x-cbr"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("cbtc", Collections.singletonList("application/x-cbr"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("cbz", Collections.singletonList("application/x-cbr"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("cc", Collections.singletonList("text/x-c"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("cdr", Collections.singletonList("application/coreldraw"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("class", Collections.singletonList("application/java"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("cnf", Collections.singletonList("text/plain"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("conf", Collections.singletonList("text/plain"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("cpp", Collections.singletonList("text/x-c++src"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("cr2", Collections.singletonList("image/x-dcraw"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("css", Collections.singletonList("text/css"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("csv", Collections.singletonList("text/csv"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("cvbdl", Collections.singletonList("application/x-cbr"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("c", Collections.singletonList("text/x-c"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("c++", Collections.singletonList("text/x-c++src"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("dcr", Collections.singletonList("image/x-dcraw"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("deb", Collections.singletonList("application/x-deb"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("dng", Collections.singletonList("image/x-dcraw"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("doc", Collections.singletonList("application/msword"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("docm", Collections.singletonList("application/vnd.ms-word.document.macroEnabled.12"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("docx", Collections.singletonList("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("dot", Collections.singletonList("application/msword"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("dotx", Collections.singletonList("application/vnd.openxmlformats-officedocument.wordprocessingml.template"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("dv", Collections.singletonList("video/dv"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("eot", Collections.singletonList("application/vnd.ms-fontobject"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("epub", Collections.singletonList("application/epub+zip"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("eps", Collections.singletonList("application/postscript"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("erf", Collections.singletonList("image/x-dcraw"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("exe", Collections.singletonList("application/x-ms-dos-executable"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("fb2", Arrays.asList("application/x-fictionbook+xml", "text/plain"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("flac", Collections.singletonList("audio/flac"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("flv", Collections.singletonList("video/x-flv"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("gif", Collections.singletonList("image/gif"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("gpx", Collections.singletonList("application/gpx+xml"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("gz", Collections.singletonList("application/gzip"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("gzip", Collections.singletonList("application/gzip"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("h", Collections.singletonList("text/x-h"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("heic", Collections.singletonList("image/heic"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("heif", Collections.singletonList("image/heif"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("hh", Collections.singletonList("text/x-h"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("hpp", Collections.singletonList("text/x-h"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("htaccess", Collections.singletonList("text/plain"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("html", Arrays.asList("text/html", "text/plain"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("htm", Arrays.asList("text/html", "text/plain"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("ical", Collections.singletonList("text/calendar"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("ics", Collections.singletonList("text/calendar"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("iiq", Collections.singletonList("image/x-dcraw"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("impress", Collections.singletonList("text/impress"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("java", Collections.singletonList("text/x-java-source"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("jp2", Collections.singletonList("image/jp2"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("jpeg", Collections.singletonList("image/jpeg"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("jpg", Collections.singletonList("image/jpeg"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("jps", Collections.singletonList("image/jpeg"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("js", Arrays.asList("application/javascript", "text/plain"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("json", Arrays.asList("application/json", "text/plain"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("k25", Collections.singletonList("image/x-dcraw"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("kdc", Collections.singletonList("image/x-dcraw"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("key", Collections.singletonList("application/x-iwork-keynote-sffkey"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("keynote", Collections.singletonList("application/x-iwork-keynote-sffkey"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("kml", Collections.singletonList("application/vnd.google-earth.kml+xml"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("kmz", Collections.singletonList("application/vnd.google-earth.kmz"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("kra", Collections.singletonList("application/x-krita"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("ldif", Collections.singletonList("text/x-ldif"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("love", Collections.singletonList("application/x-love-game"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("lwp", Collections.singletonList("application/vnd.lotus-wordpro"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("m", Arrays.asList("text/x-matlab", "text/plain"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("m2t", Collections.singletonList("video/mp2t"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("m3u", Collections.singletonList("audio/mpegurl"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("m3u8", Collections.singletonList("audio/mpegurl"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("m4a", Collections.singletonList("audio/mp4"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("m4b", Collections.singletonList("audio/m4b"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("m4v", Collections.singletonList("video/mp4"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("markdown", Collections.singletonList(MIMETYPE_TEXT_MARKDOWN));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("mdown", Collections.singletonList(MIMETYPE_TEXT_MARKDOWN));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("md", Collections.singletonList(MIMETYPE_TEXT_MARKDOWN));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("mdb", Collections.singletonList("application/msaccess"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("mdwn", Collections.singletonList(MIMETYPE_TEXT_MARKDOWN));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("mkd", Collections.singletonList(MIMETYPE_TEXT_MARKDOWN));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("mef", Collections.singletonList("image/x-dcraw"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("mkv", Collections.singletonList("video/x-matroska"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("mobi", Collections.singletonList("application/x-mobipocket-ebook"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("mov", Collections.singletonList("video/quicktime"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("mp3", Collections.singletonList("audio/mpeg"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("mp4", Collections.singletonList("video/mp4"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("mpeg", Collections.singletonList("video/mpeg"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("mpg", Collections.singletonList("video/mpeg"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("mpo", Collections.singletonList("image/jpeg"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("msi", Collections.singletonList("application/x-msi"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("mts", Collections.singletonList("video/MP2T"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("mt2s", Collections.singletonList("video/MP2T"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("nef", Collections.singletonList("image/x-dcraw"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("numbers", Collections.singletonList("application/x-iwork-numbers-sffnumbers"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("odf", Collections.singletonList("application/vnd.oasis.opendocument.formula"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("odg", Collections.singletonList("application/vnd.oasis.opendocument.graphics"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("odp", Collections.singletonList("application/vnd.oasis.opendocument.presentation"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("ods", Collections.singletonList("application/vnd.oasis.opendocument.spreadsheet"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("odt", Collections.singletonList("application/vnd.oasis.opendocument.text"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("oga", Collections.singletonList("audio/ogg"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("ogg", Collections.singletonList("audio/ogg"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("ogv", Collections.singletonList("video/ogg"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("one", Collections.singletonList("application/msonenote"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("opus", Collections.singletonList("audio/ogg"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("orf", Collections.singletonList("image/x-dcraw"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("otf", Collections.singletonList("application/font-sfnt"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("pages", Collections.singletonList("application/x-iwork-pages-sffpages"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("pdf", Collections.singletonList("application/pdf"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("pfb", Collections.singletonList("application/x-font"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("pef", Collections.singletonList("image/x-dcraw"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("php", Collections.singletonList("application/x-php"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("pl", Collections.singletonList("application/x-perl"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("pls", Collections.singletonList("audio/x-scpls"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("png", Collections.singletonList("image/png"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("pot", Collections.singletonList("application/vnd.ms-powerpoint"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("potm", Collections.singletonList("application/vnd.ms-powerpoint.template.macroEnabled.12"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("potx", Collections.singletonList("application/vnd.openxmlformats-officedocument.presentationml.template"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("ppa", Collections.singletonList("application/vnd.ms-powerpoint"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("ppam", Collections.singletonList("application/vnd.ms-powerpoint.addin.macroEnabled.12"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("pps", Collections.singletonList("application/vnd.ms-powerpoint"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("ppsm", Collections.singletonList("application/vnd.ms-powerpoint.slideshow.macroEnabled.12"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("ppsx", Collections.singletonList("application/vnd.openxmlformats-officedocument.presentationml.slideshow"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("ppt", Collections.singletonList("application/vnd.ms-powerpoint"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("pptm", Collections.singletonList("application/vnd.ms-powerpoint.presentation.macroEnabled.12"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("pptx", Collections.singletonList("application/vnd.openxmlformats-officedocument.presentationml.presentation"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("ps", Collections.singletonList("application/postscript"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("psd", Collections.singletonList("application/x-photoshop"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("py", Collections.singletonList("text/x-python"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("raf", Collections.singletonList("image/x-dcraw"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("rar", Collections.singletonList("application/x-rar-compressed"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("reveal", Collections.singletonList("text/reveal"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("rss", Collections.singletonList("application/rss+xml"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("rtf", Collections.singletonList("application/rtf"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("rw2", Collections.singletonList("image/x-dcraw"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("schema", Collections.singletonList("text/plain"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("sgf", Collections.singletonList("application/sgf"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("sh-lib", Collections.singletonList("text/x-shellscript"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("sh", Collections.singletonList("text/x-shellscript"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("srf", Collections.singletonList("image/x-dcraw"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("sr2", Collections.singletonList("image/x-dcraw"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("svg", Arrays.asList("image/svg+xml", "text/plain"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("swf", Arrays.asList("application/x-shockwave-flash", "application/octet-stream"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("tar", Collections.singletonList("application/x-tar"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("tar.bz2", Collections.singletonList("application/x-bzip2"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("tar.gz", Collections.singletonList("application/x-compressed"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("tbz2", Collections.singletonList("application/x-bzip2"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("tcx", Collections.singletonList("application/vnd.garmin.tcx+xml"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("tex", Collections.singletonList("application/x-tex"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("tgz", Collections.singletonList("application/x-compressed"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("tiff", Collections.singletonList("image/tiff"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("tif", Collections.singletonList("image/tiff"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("ttf", Collections.singletonList("application/font-sfnt"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("txt", Collections.singletonList("text/plain"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("vcard", Collections.singletonList("text/vcard"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("vcf", Collections.singletonList("text/vcard"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("vob", Collections.singletonList("video/dvd"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("vsd", Collections.singletonList("application/vnd.visio"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("vsdm", Collections.singletonList("application/vnd.ms-visio.drawing.macroEnabled.12"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("vsdx", Collections.singletonList("application/vnd.ms-visio.drawing"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("vssm", Collections.singletonList("application/vnd.ms-visio.stencil.macroEnabled.12"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("vssx", Collections.singletonList("application/vnd.ms-visio.stencil"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("vstm", Collections.singletonList("application/vnd.ms-visio.template.macroEnabled.12"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("vstx", Collections.singletonList("application/vnd.ms-visio.template"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("wav", Collections.singletonList("audio/wav"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("webm", Collections.singletonList("video/webm"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("woff", Collections.singletonList("application/font-woff"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("wpd", Collections.singletonList("application/vnd.wordperfect"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("wmv", Collections.singletonList("video/x-ms-wmv"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("xcf", Collections.singletonList("application/x-gimp"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("xla", Collections.singletonList("application/vnd.ms-excel"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("xlam", Collections.singletonList("application/vnd.ms-excel.addin.macroEnabled.12"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("xls", Collections.singletonList("application/vnd.ms-excel"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("xlsb", Collections.singletonList("application/vnd.ms-excel.sheet.binary.macroEnabled.12"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("xlsm", Collections.singletonList("application/vnd.ms-excel.sheet.macroEnabled.12"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("xlsx", Collections.singletonList("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("xlt", Collections.singletonList("application/vnd.ms-excel"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("xltm", Collections.singletonList("application/vnd.ms-excel.template.macroEnabled.12"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("xltx", Collections.singletonList("application/vnd.openxmlformats-officedocument.spreadsheetml.template"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("xml", Arrays.asList("application/xml", "text/plain"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("xrf", Collections.singletonList("image/x-dcraw"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("yaml", Arrays.asList("application/yaml", "text/plain"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("yml", Arrays.asList("application/yaml", "text/plain"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("zip", Collections.singletonList("application/zip"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("url", Collections.singletonList("application/internet-shortcut"));
        FILE_EXTENSION_TO_MIMETYPE_MAPPING.put("webloc", Collections.singletonList("application/internet-shortcut"));
    }
}
