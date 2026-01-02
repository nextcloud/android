/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.lib.resources.files;

import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.utils.MimeType;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SAX Handler for parsing WebDAV PROPFIND responses.
 * Handles streaming XML parsing to avoid OutOfMemoryError for large folders.
 */
public class PropFindSaxHandler extends DefaultHandler {
    private static final String TAG = PropFindSaxHandler.class.getSimpleName();

    // XML Namespaces
    private static final String NS_DAV = "DAV:";
    private static final String NS_OC = "http://owncloud.org/ns";
    private static final String NS_NC = "http://nextcloud.org/ns";

    // Element names
    private static final String ELEMENT_RESPONSE = "response";
    private static final String ELEMENT_HREF = "href";
    private static final String ELEMENT_PROP = "prop";
    private static final String ELEMENT_PROPSTAT = "propstat";
    private static final String ELEMENT_GETLASTMODIFIED = "getlastmodified";
    private static final String ELEMENT_GETETAG = "getetag";
    private static final String ELEMENT_RESOURCETYPE = "resourcetype";
    private static final String ELEMENT_COLLECTION = "collection";
    private static final String ELEMENT_GETCONTENTLENGTH = "getcontentlength";
    private static final String ELEMENT_GETCONTENTTYPE = "getcontenttype";
    private static final String ELEMENT_ID = "id";
    private static final String ELEMENT_FILEID = "fileid";
    private static final String ELEMENT_PERMISSIONS = "permissions";
    private static final String ELEMENT_SIZE = "size";
    private static final String ELEMENT_FAVORITE = "favorite";
    private static final String ELEMENT_OWNER_ID = "owner-id";
    private static final String ELEMENT_OWNER_DISPLAY_NAME = "owner-display-name";
    private static final String ELEMENT_HAS_PREVIEW = "has-preview";
    private static final String ELEMENT_MOUNT_TYPE = "mount-type";
    private static final String ELEMENT_IS_ENCRYPTED = "is-encrypted";
    private static final String ELEMENT_NOTE = "note";
    private static final String ELEMENT_LOCK = "lock";
    private static final String ELEMENT_RICH_WORKSPACE = "rich-workspace";
    private static final String ELEMENT_COMMENTS_UNREAD = "comments-unread";
    private static final String ELEMENT_STATUS = "status";

    // String constants for logging
    private static final String LOG_EQUALS_QUOTE = " = '";
    
    // Initial capacity optimized for typical XML element sizes (paths, dates, IDs)
    // StringBuilder is cleared after each XML element, preventing memory leaks
    private static final int INITIAL_STRING_BUILDER_CAPACITY = 512;

    private final List<Object> files = new ArrayList<>();
    private RemoteFile currentFile;
    // StringBuilder is cleared after each XML element in endElement(), preventing memory accumulation
    private final StringBuilder currentText = new StringBuilder(INITIAL_STRING_BUILDER_CAPACITY);
    private boolean inResponse = false;
    private boolean inProp = false;
    private boolean inPropstat = false;
    private boolean inResourcetype = false;
    private boolean isCollection = false;
    private String currentHref;
    private String davBasePath;
    // Track if we've determined the resource type (file or folder)
    private boolean resourceTypeDetermined = false;
    // Store the determined resource type for this file (default to file)
    private boolean currentResourceIsCollection = false;
    // Track HTTP status in current propstat (only process properties if status is 200 OK)
    private boolean propstatStatusOk = false;

    // Lock information
    private boolean inLock = false;
    // StringBuilder is cleared after each lock element is processed, preventing memory accumulation
    private StringBuilder lockText = new StringBuilder(INITIAL_STRING_BUILDER_CAPACITY);

    public PropFindSaxHandler() {
        this(null);
    }

    public PropFindSaxHandler(String davBasePath) {
        this.davBasePath = davBasePath;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        clearStringBuilder(currentText);

        if (NS_DAV.equals(uri) && ELEMENT_RESPONSE.equals(localName)) {
            inResponse = true;
            currentFile = new RemoteFile("/"); // Temporary path, will be set from href
            currentHref = null;
            isCollection = false; // Reset for each response - assume file by default
            resourceTypeDetermined = false; // Will be set when we process resourcetype
            currentResourceIsCollection = false; // Default to file, will be set to true if <d:collection/> found
        } else if (NS_DAV.equals(uri) && ELEMENT_PROPSTAT.equals(localName)) {
            inPropstat = true;
            // Default to true - assume properties are valid unless status says otherwise
            // Status might come before or after prop, so we'll update this when we see status
            propstatStatusOk = true;
        } else if (NS_DAV.equals(uri) && ELEMENT_PROP.equals(localName)) {
            inProp = true;
        } else if (NS_DAV.equals(uri) && ELEMENT_RESOURCETYPE.equals(localName)) {
            inResourcetype = true;
            // Reset collection flag when starting resourcetype - assume it's a file
            isCollection = false;
        } else if (NS_DAV.equals(uri) && ELEMENT_COLLECTION.equals(localName)) {
            isCollection = true;
        } else if ((NS_OC.equals(uri) || NS_NC.equals(uri)) && ELEMENT_LOCK.equals(localName)) {
            inLock = true;
            clearStringBuilder(lockText);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (inLock) {
            lockText.append(ch, start, length);
        } else {
            currentText.append(ch, start, length);
        }
    }
    
    /**
     * Clear StringBuilder and trim capacity periodically to prevent memory leaks.
     * StringBuilder is cleared after each XML element, so it doesn't accumulate data
     * between elements. Periodic trimming prevents capacity from growing indefinitely.
     */
    private void clearStringBuilder(StringBuilder sb) {
        sb.setLength(0);
        // Trim capacity periodically to prevent memory leaks
        // This is safe because StringBuilder is cleared after each XML element
        if (sb.capacity() > INITIAL_STRING_BUILDER_CAPACITY * 4) {
            sb.trimToSize();
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        String text = currentText.toString().trim();

        if (NS_DAV.equals(uri) && ELEMENT_RESPONSE.equals(localName)) {
            if (currentFile != null && currentHref != null) {
                String remotePath = extractPathFromHref(currentHref);
                currentFile.setRemotePath(remotePath);

                // Ensure MimeType is properly set based on resource type determination
                String finalMimeType = currentFile.getMimeType();
                if (resourceTypeDetermined) {
                    // Resource type was determined from resourcetype element
                    if (currentResourceIsCollection) {
                        // Make sure folder has DIRECTORY mime type
                        if (!MimeType.DIRECTORY.equals(finalMimeType)) {
                            currentFile.setMimeType(MimeType.DIRECTORY);
                        }
                    } else {
                        // This is a file - ensure it has a proper mime type
                        if (finalMimeType == null || finalMimeType.isEmpty() || MimeType.DIRECTORY.equals(finalMimeType)) {
                            // No mime type set from getcontenttype, use default
                            currentFile.setMimeType(MimeType.FILE);
                        }
                    }
                } else {
                    // Resource type was not determined - this shouldn't happen with proper XML
                    Log_OC.w(TAG, "WARNING: Resource type was not determined for path: " + currentFile.getRemotePath() + " - assuming it's a file");
                    if (finalMimeType == null || finalMimeType.isEmpty()) {
                        currentFile.setMimeType(MimeType.FILE);
                    }
                }

                // Ensure remoteId is always set, even if server doesn't provide it
                String remoteId = currentFile.getRemoteId();
                if (remoteId == null || remoteId.isEmpty()) {
                    // Try to use fileid as fallback
                    Long localId = currentFile.getLocalId();
                    if (localId != null && localId > 0) {
                        remoteId = String.valueOf(localId);
                        currentFile.setRemoteId(remoteId);
                    } else {
                        // Try to create a stable remoteId from path
                        String path = currentFile.getRemotePath();
                        if (path != null && !path.isEmpty()) {
                            if ("/".equals(path)) {
                                // For root directory, use a fixed id
                                remoteId = "root";
                                currentFile.setRemoteId(remoteId);
                            } else {
                                // For files and folders, create a stable id based on path
                                // Use a prefix to ensure it's not treated as a number
                                String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
                                remoteId = "path_" + normalizedPath.replace("/", "_").replace(" ", "_").replace(".", "_").replaceAll("[^a-zA-Z0-9_]", "_");
                                currentFile.setRemoteId(remoteId);
                            }
                        } else {
                            // Last resort: use path hash with prefix
                            remoteId = "hash_" + Math.abs((path != null ? path : "").hashCode());
                            currentFile.setRemoteId(remoteId);
                            Log_OC.w(TAG, "WARNING: Using path hash as remoteId fallback: " + remoteId + " for path: " + (path != null ? path : "null"));
                        }
                    }
                }

                files.add(currentFile);
            }
            inResponse = false;
            currentFile = null;
            currentHref = null;
            isCollection = false;
            resourceTypeDetermined = false;
        } else if (NS_DAV.equals(uri) && ELEMENT_HREF.equals(localName)) {
            currentHref = text;
        } else if (NS_DAV.equals(uri) && ELEMENT_STATUS.equals(localName)) {
            // Check if status is 200 OK
            // Format: "HTTP/1.1 200 OK" or "HTTP/1.1 404 Not Found"
            // Note: Status might come before or after prop in the XML
            boolean statusIsOk = text != null && text.contains("200");
            if (statusIsOk) {
                propstatStatusOk = true;
            } else {
                propstatStatusOk = false;
                // If status came after prop, we've already processed some properties
                // We can't undo that, but at least we know this propstat had an error
            }
        } else if (NS_DAV.equals(uri) && ELEMENT_PROPSTAT.equals(localName)) {
            inPropstat = false;
            propstatStatusOk = false; // Reset for next propstat
        } else if (NS_DAV.equals(uri) && ELEMENT_PROP.equals(localName)) {
            inProp = false;
        } else if (NS_DAV.equals(uri) && ELEMENT_RESOURCETYPE.equals(localName)) {
            inResourcetype = false;
            if (currentFile != null) {
                currentResourceIsCollection = isCollection; // Store the determined type
                resourceTypeDetermined = true; // We've processed resourcetype, so type is determined
                if (isCollection) {
                    currentFile.setMimeType(MimeType.DIRECTORY);
                } else {
                    // This is a file - don't set MimeType here, wait for getcontenttype
                }
            }
            // Don't reset isCollection here - it's still needed for other elements like oc:size
        } else if ((NS_OC.equals(uri) || NS_NC.equals(uri)) && ELEMENT_LOCK.equals(localName)) {
            if (currentFile != null && lockText.length() > 0) {
                parseLockInfo(lockText.toString());
            }
            inLock = false;
            clearStringBuilder(lockText);
        } else if (inProp && currentFile != null && inPropstat) {
            if (!propstatStatusOk) {
                Log_OC.w(TAG, "Processing properties for propstat with non-OK status: " + uri + ":" + localName + LOG_EQUALS_QUOTE + text + "'");
            }
            // Parse DAV properties
            if (NS_DAV.equals(uri) && ELEMENT_GETLASTMODIFIED.equals(localName)) {
                currentFile.setModifiedTimestamp(parseLastModified(text));
            } else if (NS_DAV.equals(uri) && ELEMENT_GETETAG.equals(localName)) {
                currentFile.setEtag(parseEtag(text));
            } else if (NS_DAV.equals(uri) && ELEMENT_GETCONTENTLENGTH.equals(localName)) {
                try {
                    currentFile.setLength(Long.parseLong(text));
                } catch (NumberFormatException e) {
                    // Ignore invalid content length
                }
            } else if (NS_DAV.equals(uri) && ELEMENT_GETCONTENTTYPE.equals(localName) && currentFile != null) {
                // Handle getcontenttype - this can help determine file type
                // Special case: if getcontenttype is "httpd/unix-directory", this is definitely a folder
                if ("httpd/unix-directory".equals(text)) {
                        currentFile.setMimeType(MimeType.DIRECTORY);
                        currentResourceIsCollection = true;
                        resourceTypeDetermined = true;
                    } else if (currentResourceIsCollection) {
                        // Already determined this is a collection from resourcetype, keep DIRECTORY
                    } else {
                        // This appears to be a file
                        String currentMimeType = currentFile.getMimeType();
                        // Only set if MimeType is not already set to DIRECTORY or WEBDAV_FOLDER
                        // and text is not null/empty
                        if ((currentMimeType == null ||
                            (!MimeType.DIRECTORY.equals(currentMimeType) &&
                                !MimeType.WEBDAV_FOLDER.equals(currentMimeType))) &&
                            text != null && !text.isEmpty()) {
                            // This is a file, set its MimeType
                            currentFile.setMimeType(text);
                            if (!resourceTypeDetermined) {
                                resourceTypeDetermined = true; // We now know this is a file
                            }
                        }
                    }
            }
            // Parse ownCloud/Nextcloud properties
            // Handle various possible id element names - try all possible variations
            // Nextcloud 31 might use different element names
            if ((NS_OC.equals(uri) || NS_NC.equals(uri)) &&
                ("id".equals(localName) || "fileid".equals(localName) || "file-id".equals(localName) ||
                    "fileId".equals(localName) || "resource-id".equals(localName) || "file_id".equals(localName) ||
                    "nc:id".equals(localName) || "oc:id".equals(localName))) {
                if (text != null && !text.isEmpty() && !"null".equals(text)) {
                    // Only set if not already set or if this is a more specific id
                    String currentRemoteId = currentFile.getRemoteId();
                    if (currentRemoteId == null || currentRemoteId.isEmpty() || currentRemoteId.startsWith("/")) {
                        currentFile.setRemoteId(text);
                    }
                }
            } else if ((NS_OC.equals(uri) || NS_NC.equals(uri)) && "fileid".equals(localName)) {
                // Additional check for fileid element
                if (text != null && !text.isEmpty() && !"null".equals(text)) {
                    try {
                        long fileIdLong = Long.parseLong(text);
                        currentFile.setLocalId(fileIdLong);
                        // If remoteId was not set from other id elements, use fileid as fallback
                        String currentRemoteId = currentFile.getRemoteId();
                        if (currentRemoteId == null || currentRemoteId.isEmpty() || currentRemoteId.startsWith("/")) {
                            String fileIdStr = String.valueOf(fileIdLong);
                            currentFile.setRemoteId(fileIdStr);
                        }
                    } catch (NumberFormatException e) {
                        Log_OC.w(TAG, "Failed to parse fileid: '" + text + "'");
                    }
                }
            } else if ((NS_OC.equals(uri) || NS_NC.equals(uri)) && ELEMENT_FILEID.equals(localName)) {
                try {
                    long fileIdLong = Long.parseLong(text);
                    currentFile.setLocalId(fileIdLong);
                    // If remoteId was not set from other id elements, use fileid as fallback
                    String currentRemoteId = currentFile.getRemoteId();
                    if (currentRemoteId == null || currentRemoteId.isEmpty() || currentRemoteId.startsWith("/")) {
                        String fileIdStr = String.valueOf(fileIdLong);
                        currentFile.setRemoteId(fileIdStr);
                    }
                } catch (NumberFormatException e) {
                    Log_OC.w(TAG, "Failed to parse " + uri + ":fileid: '" + text + "'");
                }
            } else if ((NS_OC.equals(uri) || NS_NC.equals(uri)) && ELEMENT_PERMISSIONS.equals(localName)) {
                currentFile.setPermissions(text);
            } else if ((NS_OC.equals(uri) || NS_NC.equals(uri)) && ELEMENT_SIZE.equals(localName)) {
                try {
                    if (currentResourceIsCollection) {
                        currentFile.setSize(Long.parseLong(text));
                    }
                } catch (NumberFormatException e) {
                }
            } else if (NS_DAV.equals(uri) && "quota-used-bytes".equals(localName)) {
                try {
                    if (currentResourceIsCollection) {
                        long quotaSize = Long.parseLong(text);
                        currentFile.setSize(quotaSize);
                    }
                } catch (NumberFormatException e) {
                }
            } else if ((NS_OC.equals(uri) || NS_NC.equals(uri)) && ELEMENT_FAVORITE.equals(localName)) {
                currentFile.setFavorite("1".equals(text));
            } else if ((NS_OC.equals(uri) || NS_NC.equals(uri)) && ELEMENT_OWNER_ID.equals(localName)) {
                currentFile.setOwnerId(text);
            } else if ((NS_OC.equals(uri) || NS_NC.equals(uri)) && ELEMENT_OWNER_DISPLAY_NAME.equals(localName)) {
                currentFile.setOwnerDisplayName(text);
            } else if ((NS_OC.equals(uri) || NS_NC.equals(uri)) && ELEMENT_HAS_PREVIEW.equals(localName)) {
                boolean hasPreview = "true".equalsIgnoreCase(text);
                currentFile.setHasPreview(hasPreview);
            } else if (NS_DAV.equals(uri) && "getcontenttype".equals(localName)) {
                // Set preview availability based on MIME type for Nextcloud 31
                if (text != null && currentFile != null && !currentResourceIsCollection) {
                    boolean hasPreview = isPreviewableMimeType(text);
                    if (hasPreview) {
                        currentFile.setHasPreview(true);
                    }
                }
            } else if ((NS_OC.equals(uri) || NS_NC.equals(uri)) && ELEMENT_MOUNT_TYPE.equals(localName)) {
                // Mount type parsing - using reflection since WebdavEntry is from library
                try {
                    parseMountType(text);
                } catch (Exception e) {
                    Log_OC.w(TAG, "Could not parse mount type: " + text);
                }
            } else if ((NS_OC.equals(uri) || NS_NC.equals(uri)) && ELEMENT_IS_ENCRYPTED.equals(localName)) {
                currentFile.setEncrypted("true".equalsIgnoreCase(text));
            } else if ((NS_OC.equals(uri) || NS_NC.equals(uri)) && ELEMENT_NOTE.equals(localName)) {
                currentFile.setNote(text);
            } else if ((NS_OC.equals(uri) || NS_NC.equals(uri)) && ELEMENT_RICH_WORKSPACE.equals(localName)) {
                currentFile.setRichWorkspace(text);
            } else if ((NS_OC.equals(uri) || NS_NC.equals(uri)) && ELEMENT_COMMENTS_UNREAD.equals(localName)) {
                try {
                    currentFile.setUnreadCommentsCount(Integer.parseInt(text));
                } catch (NumberFormatException e) {
                    // Ignore invalid count
                }
            }
        }

        clearStringBuilder(currentText);
    }

    /**
     * Extract path from href.
     * Href format: /remote.php/dav/files/{user}/{path} or http://server/remote.php/dav/files/{user}/{path}
     * We need to extract {path} relative to DAV base path.
     */
    private String extractPathFromHref(String href) {
        if (href == null || href.isEmpty()) {
            return "/";
        }

        // Remove protocol and domain if present (http://server/path -> /path)
        String normalizedHref = href;
        try {
            java.net.URI uri = new java.net.URI(href);
            String path = uri.getPath();
            if (path != null) {
                normalizedHref = path;
            }
        } catch (Exception e) {
            // If URI parsing fails, use href as-is
        }

        // If we have davBasePath, use it to extract relative path
        if (davBasePath != null) {
            try {
                java.net.URI baseUri = new java.net.URI(davBasePath);
                String basePath = baseUri.getPath();
                if (basePath != null && normalizedHref.startsWith(basePath)) {
                    String path = normalizedHref.substring(basePath.length());
                    // Ensure path starts with /
                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }
                    return path.isEmpty() ? "/" : path;
                }
            } catch (Exception e) {
                // If URI parsing fails, try string matching
                if (normalizedHref.startsWith(davBasePath)) {
                    String path = normalizedHref.substring(davBasePath.length());
                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }
                    return path.isEmpty() ? "/" : path;
                }
            }
        }

        // Fallback: try to extract path after /dav/files/
        int davFilesIndex = normalizedHref.indexOf("/dav/files/");
        if (davFilesIndex >= 0) {
            String path = normalizedHref.substring(davFilesIndex + "/dav/files/".length());
            // Remove user name (everything before first /)
            int firstSlash = path.indexOf('/');
            if (firstSlash >= 0) {
                path = path.substring(firstSlash);
            } else {
                path = "/";
            }
            return path;
        }

        // If we can't extract, return normalized href as-is
        return normalizedHref.isEmpty() ? "/" : normalizedHref;
    }

    /**
     * Parse RFC 1123 date format (e.g., "Mon, 01 Jan 2024 12:00:00 GMT")
     */
    private long parseLastModified(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return 0;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
            return format.parse(dateStr).getTime();
        } catch (ParseException e) {
            Log_OC.w(TAG, "Failed to parse date: " + dateStr + ", error: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Parse ETag, removing surrounding quotes if present
     */
    private String parseEtag(String etag) {
        if (etag == null || etag.isEmpty()) {
            return "";
        }
        // ETag format: "abc123" -> abc123
        if (etag.startsWith("\"") && etag.endsWith("\"") && etag.length() > 1) {
            return etag.substring(1, etag.length() - 1);
        }
        return etag;
    }

    /**
     * Parse mount type string and set it on RemoteFile using reflection.
     * This is needed because WebdavEntry.MountType is from the library.
     */
    @SuppressWarnings("unchecked")
    private void parseMountType(String mountType) {
        if (mountType == null || mountType.isEmpty() || currentFile == null) {
            return;
        }
        try {
            // Use reflection to access WebdavEntry.MountType enum from library
            Class<?> webdavEntryClass = Class.forName("com.owncloud.android.lib.resources.files.model.WebdavEntry");
            Class<?> mountTypeEnum = Class.forName("com.owncloud.android.lib.resources.files.model.WebdavEntry$MountType");
            Object[] enumValues = mountTypeEnum.getEnumConstants();

            String mountTypeUpper = mountType.toUpperCase(Locale.US);
            Object mountTypeValue = null;

            for (Object enumValue : enumValues) {
                if (enumValue.toString().equals(mountTypeUpper)) {
                    mountTypeValue = enumValue;
                    break;
                }
            }

            if (mountTypeValue == null) {
                // Try INTERNAL as default
                for (Object enumValue : enumValues) {
                    if (enumValue.toString().equals("INTERNAL")) {
                        mountTypeValue = enumValue;
                        break;
                    }
                }
            }

            if (mountTypeValue != null) {
                java.lang.reflect.Method setMountTypeMethod = currentFile.getClass().getMethod("setMountType", mountTypeEnum);
                setMountTypeMethod.invoke(currentFile, mountTypeValue);
            }
        } catch (Exception e) {
            // If reflection fails, mount type will remain unset (default value)
            Log_OC.w(TAG, "Could not set mount type: " + mountType);
        }
    }

    /**
     * Parse lock information from XML
     */
    private void parseLockInfo(String lockXml) {
        // Basic lock parsing - set locked flag if lock element exists
        // Use more efficient blank check instead of trim().isEmpty()
        if (currentFile != null && lockXml != null && !isBlank(lockXml)) {
            currentFile.setLocked(true);
            // TODO: Parse detailed lock information if needed
        }
    }
    
    /**
     * Efficiently check if a string is blank (null, empty, or only whitespace)
     */
    private boolean isBlank(String str) {
        if (str == null || str.isEmpty()) {
            return true;
        }
        int length = str.length();
        for (int i = 0; i < length; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if MIME type supports preview generation in Nextcloud
     */
    private boolean isPreviewableMimeType(String mimeType) {
        if (mimeType == null) return false;

        // Images
        if (mimeType.startsWith("image/")) return true;

        // Videos
        if (mimeType.startsWith("video/")) return true;

        // PDFs
        if ("application/pdf".equals(mimeType)) return true;

        // Office documents
        if (mimeType.startsWith("application/vnd.openxmlformats-officedocument.") ||
            mimeType.startsWith("application/msword") ||
            mimeType.startsWith("application/vnd.ms-") ||
            mimeType.startsWith("application/vnd.oasis.opendocument.")) return true;

        return false;
    }

    public List<Object> getFiles() {
        return files;
    }
}

