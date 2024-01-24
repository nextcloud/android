/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */
package com.owncloud.android.lib.common.network

import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.nextcloud.extensions.fromDavProperty
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.model.FileLockType
import com.owncloud.android.lib.resources.files.model.FileLockType.Companion.fromValue
import com.owncloud.android.lib.resources.files.model.GeoLocation
import com.owncloud.android.lib.resources.files.model.ImageDimension
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.ShareeUser
import org.apache.jackrabbit.webdav.MultiStatusResponse
import org.apache.jackrabbit.webdav.property.DavProperty
import org.apache.jackrabbit.webdav.property.DavPropertyName
import org.apache.jackrabbit.webdav.property.DavPropertySet
import org.apache.jackrabbit.webdav.xml.Namespace
import org.w3c.dom.Element
import java.lang.ClassCastException
import java.math.BigDecimal

@Suppress("Detekt.TooGenericExceptionCaught") // legacy code
class WebdavEntry constructor(ms: MultiStatusResponse, splitElement: String) {
    var name: String? = null
        private set
    var path: String? = null
    var uri: String? = null
        private set
    var contentType: String? = null
        private set
    var eTag: String? = null
    var permissions: String? = null
        private set
    var remoteId: String? = null
        private set
    var localId: Long = 0
        private set
    var trashbinOriginalLocation: String? = null
    var trashbinFilename: String? = null
    var trashbinDeletionTimestamp: Long = 0
    var isFavorite = false
    var isEncrypted = false
    var mountType: MountType? = null
    var contentLength: Long = 0
        private set
    var createTimestamp: Long = 0
        private set
    var modifiedTimestamp: Long = 0
        private set
    var uploadTimestamp: Long = 0
    var size: Long = 0
        private set
    var quotaUsedBytes: BigDecimal? = null
        private set
    var quotaAvailableBytes: BigDecimal? = null
        private set
    var ownerId: String? = null
    var ownerDisplayName: String? = null
    var unreadCommentsCount = 0
    var isHasPreview = false
    var note = ""
    var sharees = arrayOfNulls<ShareeUser>(0)
    var richWorkspace: String? = null
    var isLocked = false
        private set
    var lockOwnerType: FileLockType? = null
        private set
    var lockOwnerId: String? = null
        private set
    var lockOwnerDisplayName: String? = null
        private set
    var lockTimestamp: Long = 0
        private set
    var lockOwnerEditor: String? = null
        private set
    var lockTimeout: Long = 0
        private set
    var lockToken: String? = null
        private set
    var tags = arrayOfNulls<String>(0)
    var imageDimension: ImageDimension? = null
    var geoLocation: GeoLocation? = null
    var hidden = false
        private set
    var livePhoto: String? = null
        private set

    private val gson = Gson()

    enum class MountType {
        INTERNAL,
        EXTERNAL,
        GROUP
    }

    init {
        resetData()
        val ocNamespace = Namespace.getNamespace(NAMESPACE_OC)
        val ncNamespace = Namespace.getNamespace(NAMESPACE_NC)
        if (ms.status.isNotEmpty()) {
            uri = ms.href
            path =
                uri!!.split(splitElement.toRegex(), limit = 2).toTypedArray()[1].replace("//", "/")
            var status = ms.status[0].statusCode
            if (status == CODE_PROP_NOT_FOUND) {
                status = ms.status[1].statusCode
            }
            val propSet = ms.getProperties(status)
            var prop = propSet[DavPropertyName.DISPLAYNAME]
            if (prop != null) {
                name = prop.name.toString()
                name = name!!.substring(1, name!!.length - 1)
            } else {
                val tmp = path!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (tmp.isNotEmpty()) name = tmp[tmp.size - 1]
            }

            // use unknown mimetype as default behavior
            // {DAV:}getcontenttype
            contentType = "application/octet-stream"
            prop = propSet[DavPropertyName.GETCONTENTTYPE]
            if (prop != null) {
                val contentType = prop.value as String?
                // dvelasco: some builds of ownCloud server 4.0.x added a trailing ';'
                // to the MIME type ; if looks fixed, but let's be cautious
                if (contentType != null) {
                    if (contentType.contains(";")) {
                        this.contentType = contentType.substring(0, contentType.indexOf(";"))
                    } else {
                        this.contentType = contentType
                    }
                }
            }

            // check if it's a folder in the standard way: see RFC2518 12.2 . RFC4918 14.3
            // {DAV:}resourcetype
            prop = propSet[DavPropertyName.RESOURCETYPE]
            if (prop != null) {
                val value = prop.value
                if (value != null) {
                    contentType = "DIR" // a specific attribute would be better,
                    // but this is enough;
                    // unless while we have no reason to distinguish
                    // MIME types for folders
                }
            }

            // {DAV:}getcontentlength
            prop = propSet[DavPropertyName.GETCONTENTLENGTH]
            if (prop != null) {
                contentLength = (prop.value as String).toLong()
            }

            // {DAV:}getlastmodified
            prop = propSet[DavPropertyName.GETLASTMODIFIED]
            if (prop != null) {
                val d = WebdavUtils.parseResponseDate(prop.value as String)
                modifiedTimestamp = d?.time ?: 0
            }

            // {NS:} creation_time
            prop = propSet[EXTENDED_PROPERTY_CREATION_TIME, ncNamespace]
            if (prop != null) {
                createTimestamp =
                    try {
                        (prop.value as String).toLong()
                    } catch (e: NumberFormatException) {
                        0
                    }
            }

            // {NS:} upload_time
            prop = propSet[EXTENDED_PROPERTY_UPLOAD_TIME, ncNamespace]
            if (prop != null) {
                uploadTimestamp =
                    try {
                        (prop.value as String).toLong()
                    } catch (e: NumberFormatException) {
                        0
                    }
            }

            // {DAV:}getetag
            prop = propSet[DavPropertyName.GETETAG]
            if (prop != null) {
                eTag = prop.value as String
                eTag = WebdavUtils.parseEtag(eTag)
            }

            // {DAV:}quota-used-bytes
            prop = propSet[DavPropertyName.create(PROPERTY_QUOTA_USED_BYTES)]
            if (prop != null) {
                val quotaUsedBytesSt = prop.value as String
                try {
                    quotaUsedBytes = BigDecimal(quotaUsedBytesSt)
                } catch (e: NumberFormatException) {
                    Log_OC.w(TAG, "No value for QuotaUsedBytes - NumberFormatException")
                } catch (e: NullPointerException) {
                    Log_OC.w(TAG, "No value for QuotaUsedBytes - NullPointerException")
                }
                Log_OC.d(TAG, "QUOTA_USED_BYTES $quotaUsedBytesSt")
            }

            // {DAV:}quota-available-bytes
            prop = propSet[DavPropertyName.create(PROPERTY_QUOTA_AVAILABLE_BYTES)]
            if (prop != null) {
                val quotaAvailableBytesSt = prop.value as String
                try {
                    quotaAvailableBytes = BigDecimal(quotaAvailableBytesSt)
                } catch (e: NumberFormatException) {
                    Log_OC.w(TAG, "No value for QuotaAvailableBytes - NumberFormatException")
                } catch (e: NullPointerException) {
                    Log_OC.w(TAG, "No value for QuotaAvailableBytes")
                }
                Log_OC.d(TAG, "QUOTA_AVAILABLE_BYTES $quotaAvailableBytesSt")
            }

            // OC permissions property <oc:permissions>
            prop = propSet[EXTENDED_PROPERTY_NAME_PERMISSIONS, ocNamespace]
            if (prop != null && prop.value != null) {
                permissions = prop.value.toString()
            }

            // OC remote id property <oc:id>
            prop = propSet[EXTENDED_PROPERTY_NAME_REMOTE_ID, ocNamespace]
            if (prop != null) {
                remoteId = prop.value.toString()
            }

            // OC remote id property <oc:fileid>
            prop = propSet[EXTENDED_PROPERTY_NAME_LOCAL_ID, ocNamespace]
            if (prop != null) {
                localId = (prop.value as String).toLong()
            }

            // OC size property <oc:size>
            prop = propSet[EXTENDED_PROPERTY_NAME_SIZE, ocNamespace]
            if (prop != null) {
                size = (prop.value as String).toLong()
            }

            // OC favorite property <oc:favorite>
            prop = propSet[EXTENDED_PROPERTY_FAVORITE, ocNamespace]
            isFavorite =
                if (prop != null) {
                    val favoriteValue = prop.value as String
                    IS_ENCRYPTED == favoriteValue
                } else {
                    false
                }

            // NC encrypted property <nc:is-encrypted>
            prop = propSet[EXTENDED_PROPERTY_IS_ENCRYPTED, ncNamespace]
            isEncrypted =
                if (prop != null) {
                    val encryptedValue = prop.value as String
                    IS_ENCRYPTED == encryptedValue
                } else {
                    false
                }

            // NC mount-type property <nc:mount-type>
            prop = propSet[EXTENDED_PROPERTY_MOUNT_TYPE, ncNamespace]
            mountType =
                if (prop != null) {
                    when (prop.value) {
                        "external" -> {
                            MountType.EXTERNAL
                        }

                        "group" -> {
                            MountType.GROUP
                        }

                        else -> {
                            MountType.INTERNAL
                        }
                    }
                } else {
                    MountType.INTERNAL
                }

            // OC owner-id property <oc:owner-id>
            prop = propSet[EXTENDED_PROPERTY_OWNER_ID, ocNamespace]
            ownerId =
                if (prop != null) {
                    prop.value as String
                } else {
                    ""
                }

            // OC owner-display-name property <oc:owner-display-name>
            prop = propSet[EXTENDED_PROPERTY_OWNER_DISPLAY_NAME, ocNamespace]
            ownerDisplayName =
                if (prop != null) {
                    prop.value as String
                } else {
                    ""
                }

            // OC unread comments property <oc-comments-unread>
            prop = propSet[EXTENDED_PROPERTY_UNREAD_COMMENTS, ocNamespace]
            unreadCommentsCount =
                if (prop != null) {
                    Integer.valueOf(prop.value.toString())
                } else {
                    0
                }

            // NC has preview property <nc-has-preview>
            prop = propSet[EXTENDED_PROPERTY_HAS_PREVIEW, ncNamespace]
            isHasPreview =
                if (prop != null) {
                    java.lang.Boolean.valueOf(prop.value.toString())
                } else {
                    true
                }

            // NC trashbin-original-location <nc:trashbin-original-location>
            prop = propSet[TRASHBIN_ORIGINAL_LOCATION, ncNamespace]
            if (prop != null) {
                trashbinOriginalLocation = prop.value.toString()
            }

            // NC trashbin-filename <nc:trashbin-filename>
            prop = propSet[TRASHBIN_FILENAME, ncNamespace]
            if (prop != null) {
                trashbinFilename = prop.value.toString()
            }

            // NC trashbin-deletion-time <nc:trashbin-deletion-time>
            prop = propSet[TRASHBIN_DELETION_TIME, ncNamespace]
            if (prop != null) {
                trashbinDeletionTimestamp = (prop.value as String).toLong()
            }

            // NC note property <nc:note>
            prop = propSet[EXTENDED_PROPERTY_NOTE, ncNamespace]
            if (prop != null && prop.value != null) {
                note = prop.value.toString()
            }

            // NC rich-workspace property <nc:rich-workspace>
            // can be null if rich-workspace is disabled for this user
            prop = propSet[EXTENDED_PROPERTY_RICH_WORKSPACE, ncNamespace]
            richWorkspace =
                if (prop != null) {
                    if (prop.value != null) {
                        prop.value.toString()
                    } else {
                        ""
                    }
                } else {
                    null
                }

            // NC sharees property <nc-sharees>
            prop = propSet[EXTENDED_PROPERTY_SHAREES, ncNamespace]
            if (prop != null && prop.value != null) {
                if (prop.value is ArrayList<*>) {
                    val list = prop.value as ArrayList<*>
                    val tempList: MutableList<ShareeUser?> = ArrayList()
                    for (i in list.indices) {
                        val element = list[i] as Element
                        val user = createShareeUser(element)
                        if (user != null) {
                            tempList.add(user)
                        }
                    }
                    sharees = tempList.toTypedArray()
                } else {
                    // single item or empty
                    val element = prop.value as Element
                    val user = createShareeUser(element)
                    if (user != null) {
                        sharees = arrayOf(user)
                    }
                }
            }

            prop = propSet[EXTENDED_PROPERTY_SYSTEM_TAGS, ncNamespace]
            if (prop != null && prop.value != null) {
                if (prop.value is ArrayList<*>) {
                    val list = prop.value as ArrayList<*>
                    val tempList: MutableList<String> = ArrayList(list.size)
                    for (i in list.indices) {
                        val element = list[i] as Element
                        tempList.add(element.firstChild.textContent)
                    }
                    tags = tempList.toTypedArray()
                } else {
                    // single item or empty
                    val element = prop.value as Element
                    val value = element.firstChild.textContent

                    if (value != null) {
                        tags = arrayOf(value)
                    }
                }
            }

            // NC metadata size property <nc:file-metadata-size>
            prop = propSet[EXTENDED_PROPERTY_METADATA_PHOTOS_SIZE, ncNamespace]
            imageDimension =
                if (prop == null) {
                    prop = propSet[EXTENDED_PROPERTY_METADATA_SIZE, ncNamespace]
                    gson.fromDavProperty<ImageDimension>(prop)
                } else {
                    val xmlData = prop.value as ArrayList<*>
                    var width = 0f
                    var height = 0f
                    xmlData.forEach {
                        val element = it as Element
                        if (element.tagName == "width") {
                            width = element.firstChild.textContent.toFloat()
                        } else if (element.tagName == "height") {
                            height = element.firstChild.textContent.toFloat()
                        }
                    }

                    ImageDimension(width, height)
                }

            // NC metadata gps property <nc:file-metadata-gps>
            prop = propSet[EXTENDED_PROPERTY_METADATA_PHOTOS_GPS, ncNamespace]
            geoLocation =
                if (prop == null) {
                    prop = propSet[EXTENDED_PROPERTY_METADATA_GPS, ncNamespace]
                    gson.fromDavProperty<GeoLocation>(prop)
                } else {
                    try {
                        val xmlData = prop.value as ArrayList<*>
                        var latitude = 0.0
                        var longitude = 0.0
                        xmlData.forEach {
                            val element = it as Element
                            if (element.tagName == "latitude") {
                                latitude = element.firstChild.textContent.toDouble()
                            } else if (element.tagName == "longitude") {
                                longitude = element.firstChild.textContent.toDouble()
                            }
                        }

                        GeoLocation(latitude, longitude)
                    } catch (e: ClassCastException) {

                        prop = propSet[EXTENDED_PROPERTY_METADATA_GPS, ncNamespace]
                        gson.fromDavProperty<GeoLocation>(prop)
                    }
                }

            // NC metadata live photo property: <nc:metadata-files-live-photo/>
            prop = propSet[EXTENDED_PROPERTY_METADATA_LIVE_PHOTO, ncNamespace]
            if (prop != null && prop.value != null) {
                livePhoto = prop.value.toString()
            }

            // NC has hidden property <nc:hidden>
            prop = propSet[EXTENDED_PROPERTY_HIDDEN, ncNamespace]
            hidden =
                if (prop != null) {
                    java.lang.Boolean.valueOf(prop.value.toString())
                } else {
                    false
                }

            parseLockProperties(ncNamespace, propSet)
        } else {
            Log_OC.e("WebdavEntry", "General error, no status for webdav response")
        }
    }

    private fun parseLockProperties(
        ncNamespace: Namespace,
        propSet: DavPropertySet
    ) {
        // file locking
        var prop: DavProperty<*>? = propSet[EXTENDED_PROPERTY_LOCK, ncNamespace]
        isLocked =
            if (prop != null && prop.value != null) {
                "1" == prop.value as String
            } else {
                false
            }
        prop = propSet[EXTENDED_PROPERTY_LOCK_OWNER_TYPE, ncNamespace]
        lockOwnerType =
            if (prop != null && prop.value != null) {
                val value: Int = (prop.value as String).toInt()
                fromValue(value)
            } else {
                null
            }
        lockOwnerId = parseStringProp(propSet, EXTENDED_PROPERTY_LOCK_OWNER, ncNamespace)
        lockOwnerDisplayName =
            parseStringProp(propSet, EXTENDED_PROPERTY_LOCK_OWNER_DISPLAY_NAME, ncNamespace)
        lockOwnerEditor = parseStringProp(propSet, EXTENDED_PROPERTY_LOCK_OWNER_EDITOR, ncNamespace)
        lockTimestamp = parseLongProp(propSet, EXTENDED_PROPERTY_LOCK_TIME, ncNamespace)
        lockTimeout = parseLongProp(propSet, EXTENDED_PROPERTY_LOCK_TIMEOUT, ncNamespace)
        lockToken = parseStringProp(propSet, EXTENDED_PROPERTY_LOCK_TOKEN, ncNamespace)
    }

    private fun parseStringProp(
        propSet: DavPropertySet,
        propName: String,
        namespace: Namespace
    ): String? {
        val prop = propSet[propName, namespace]
        return if (prop != null && prop.value != null) {
            prop.value as String
        } else {
            null
        }
    }

    private fun parseLongProp(
        propSet: DavPropertySet,
        propName: String,
        namespace: Namespace
    ): Long {
        val stringValue = parseStringProp(propSet, propName, namespace)
        return stringValue?.toLong() ?: 0L
    }

    private fun createShareeUser(element: Element): ShareeUser? {
        val displayName = extractDisplayName(element)
        val userId = extractUserId(element)
        val shareType = extractShareType(element)
        val isSupportedShareType =
            ShareType.EMAIL == shareType ||
                ShareType.FEDERATED == shareType ||
                ShareType.GROUP == shareType ||
                ShareType.ROOM == shareType
        return if ((isSupportedShareType || displayName.isNotEmpty()) && userId.isNotEmpty()) {
            ShareeUser(userId, displayName, shareType)
        } else {
            null
        }
    }

    private fun extractDisplayName(element: Element): String {
        val displayName = element.getElementsByTagNameNS(NAMESPACE_NC, SHAREES_DISPLAY_NAME).item(0)
        return if (displayName != null && displayName.firstChild != null) {
            displayName.firstChild.nodeValue
        } else {
            ""
        }
    }

    private fun extractUserId(element: Element): String {
        val userId = element.getElementsByTagNameNS(NAMESPACE_NC, SHAREES_ID).item(0)
        return if (userId != null && userId.firstChild != null) {
            userId.firstChild.nodeValue
        } else {
            ""
        }
    }

    private fun extractShareType(element: Element): ShareType {
        val shareType = element.getElementsByTagNameNS(NAMESPACE_NC, SHAREES_SHARE_TYPE).item(0)
        if (shareType != null && shareType.firstChild != null) {
            val value = shareType.firstChild.nodeValue.toInt()
            return ShareType.fromValue(value)
        }
        return ShareType.NO_SHARED
    }

    fun decodedPath(): String {
        return Uri.decode(path)
    }

    val isDirectory: Boolean
        get() = "DIR" == contentType

    private fun resetData() {
        permissions = null
        contentType = permissions
        uri = contentType
        name = uri
        remoteId = null
        localId = -1
        modifiedTimestamp = 0
        createTimestamp = modifiedTimestamp
        contentLength = createTimestamp
        size = 0
        quotaUsedBytes = null
        quotaAvailableBytes = null
        isFavorite = false
        isHasPreview = false
    }

    companion object {
        private val TAG = WebdavEntry::class.java.simpleName
        const val NAMESPACE_OC = "http://owncloud.org/ns"
        const val NAMESPACE_NC = "http://nextcloud.org/ns"
        const val EXTENDED_PROPERTY_NAME_PERMISSIONS = "permissions"
        const val EXTENDED_PROPERTY_NAME_LOCAL_ID = "fileid"
        const val EXTENDED_PROPERTY_NAME_REMOTE_ID = "id"
        const val EXTENDED_PROPERTY_NAME_SIZE = "size"
        const val EXTENDED_PROPERTY_FAVORITE = "favorite"
        const val EXTENDED_PROPERTY_IS_ENCRYPTED = "is-encrypted"
        const val EXTENDED_PROPERTY_MOUNT_TYPE = "mount-type"
        const val EXTENDED_PROPERTY_OWNER_ID = "owner-id"
        const val EXTENDED_PROPERTY_OWNER_DISPLAY_NAME = "owner-display-name"
        const val EXTENDED_PROPERTY_UNREAD_COMMENTS = "comments-unread"
        const val EXTENDED_PROPERTY_HAS_PREVIEW = "has-preview"
        const val EXTENDED_PROPERTY_NOTE = "note"
        const val EXTENDED_PROPERTY_SHAREES = "sharees"
        const val EXTENDED_PROPERTY_RICH_WORKSPACE = "rich-workspace"
        const val EXTENDED_PROPERTY_CREATION_TIME = "creation_time"
        const val EXTENDED_PROPERTY_UPLOAD_TIME = "upload_time"
        const val EXTENDED_PROPERTY_LOCK = "lock"
        const val EXTENDED_PROPERTY_LOCK_OWNER_TYPE = "lock-owner-type"
        const val EXTENDED_PROPERTY_LOCK_OWNER = "lock-owner"
        const val EXTENDED_PROPERTY_LOCK_OWNER_DISPLAY_NAME = "lock-owner-displayname"
        const val EXTENDED_PROPERTY_LOCK_OWNER_EDITOR = "lock-owner-editor"
        const val EXTENDED_PROPERTY_LOCK_TIME = "lock-time"
        const val EXTENDED_PROPERTY_LOCK_TIMEOUT = "lock-timeout"
        const val EXTENDED_PROPERTY_LOCK_TOKEN = "lock-token"
        const val EXTENDED_PROPERTY_SYSTEM_TAGS = "system-tags"

        // v27
        const val EXTENDED_PROPERTY_METADATA_SIZE = "file-metadata-size"
        const val EXTENDED_PROPERTY_METADATA_GPS = "file-metadata-gps"

        const val EXTENDED_PROPERTY_HIDDEN = "hidden"
        const val EXTENDED_PROPERTY_METADATA_LIVE_PHOTO = "metadata-files-live-photo"

        const val EXTENDED_PROPERTY_METADATA_PHOTOS_SIZE = "metadata-photos-size"
        const val EXTENDED_PROPERTY_METADATA_PHOTOS_GPS = "metadata-photos-gps"
        const val TRASHBIN_FILENAME = "trashbin-filename"
        const val TRASHBIN_ORIGINAL_LOCATION = "trashbin-original-location"
        const val TRASHBIN_DELETION_TIME = "trashbin-deletion-time"
        const val SHAREES_DISPLAY_NAME = "display-name"
        const val SHAREES_ID = "id"
        const val SHAREES_SHARE_TYPE = "type"
        const val PROPERTY_QUOTA_USED_BYTES = "quota-used-bytes"
        const val PROPERTY_QUOTA_AVAILABLE_BYTES = "quota-available-bytes"
        private const val IS_ENCRYPTED = "1"
        private const val CODE_PROP_NOT_FOUND = 404
    }
}