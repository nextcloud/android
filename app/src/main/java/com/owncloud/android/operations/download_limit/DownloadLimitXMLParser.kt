/*
 *
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2023 TSI-mc
 * Copyright (C) 2023 Nextcloud GmbH
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

package com.owncloud.android.operations.download_limit

import android.util.Xml
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

/**
 * class to parse the Download Limit api XML response This class code referenced from java in NC library
 */
class DownloadLimitXMLParser {
    // Getters and Setters
    var status: String? = null
    var statusCode = 0
    var message = ""
    val isSuccess: Boolean
        get() = statusCode == SUCCESS || statusCode == OK
    private val isForbidden: Boolean
        get() = statusCode == ERROR_FORBIDDEN
    private val isNotFound: Boolean
        get() = statusCode == ERROR_NOT_FOUND
    private val isWrongParameter: Boolean
        get() = statusCode == ERROR_WRONG_PARAMETER

    /**
     * method to parse the Download limit response
     * @param isGet check if parsing has to do for GET api or not
     * because the parsing will depend on that
     * if API is GET then response will have <data> tag else it wont have
     * @param serverResponse
     * @return
    </data> */
    fun parse(isGet: Boolean, serverResponse: String?): RemoteOperationResult<DownloadLimitResponse> {
        if (serverResponse == null || serverResponse.isEmpty()) {
            return RemoteOperationResult(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE)
        }
        var result: RemoteOperationResult<DownloadLimitResponse>
        try {
            // Parse xml response and obtain the list of downloadLimitResponse
            val inputStream: InputStream = ByteArrayInputStream(serverResponse.toByteArray())
            val downloadLimitResponse = parseXMLResponse(inputStream)
            if (isSuccess) {
                if (isGet) {
                    result = RemoteOperationResult(RemoteOperationResult.ResultCode.OK)
                    result.setResultData(downloadLimitResponse)
                } else {
                    result = RemoteOperationResult(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE)
                    Log_OC.e(TAG, "Successful status with no share in the response")
                }
            } else if (isWrongParameter) {
                result = RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_WRONG_PARAMETER)
                result.setMessage(message)
            } else if (isNotFound) {
                result = RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND)
                result.setMessage(message)
            } else if (isForbidden) {
                result = RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_FORBIDDEN)
                result.setMessage(message)
            } else {
                result = RemoteOperationResult(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE)
                result.setMessage(message)
            }
        } catch (e: XmlPullParserException) {
            Log_OC.e(TAG, "Error parsing response from server ", e)
            result = RemoteOperationResult(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE)
        } catch (e: IOException) {
            Log_OC.e(TAG, "Error reading response from server ", e)
            result = RemoteOperationResult(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE)
        }
        return result
    }

    /**
     * Parse is as response of Share API
     *
     * @param `is` InputStream to parse
     * @return List of ShareRemoteFiles
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseXMLResponse(inputStream: InputStream): DownloadLimitResponse {
        return inputStream.use {
            // XMLPullParser
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(it, null)
            parser.nextTag()
            readOCS(parser)
        }
    }

    /**
     * Parse OCS node
     *
     * @param parser
     * @return List of ShareRemoteFiles
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readOCS(parser: XmlPullParser): DownloadLimitResponse {
        var downloadLimitResponse = DownloadLimitResponse()
        parser.require(XmlPullParser.START_TAG, ns, NODE_OCS)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            // read NODE_META and NODE_DATA
            if (NODE_META.equals(name, ignoreCase = true)) {
                readMeta(parser)
            } else if (NODE_DATA.equals(name, ignoreCase = true)) {
                downloadLimitResponse = readData(parser)
            } else {
                skip(parser)
            }
        }
        return downloadLimitResponse
    }

    /**
     * Parse Meta node
     *
     * @param parser
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readMeta(parser: XmlPullParser) {
        parser.require(XmlPullParser.START_TAG, ns, NODE_META)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (NODE_STATUS.equals(name, ignoreCase = true)) {
                status = readNode(parser, NODE_STATUS)
            } else if (NODE_STATUS_CODE.equals(name, ignoreCase = true)) {
                statusCode = readNode(parser, NODE_STATUS_CODE).toInt()
            } else if (NODE_MESSAGE.equals(name, ignoreCase = true)) {
                message = readNode(parser, NODE_MESSAGE)
            } else {
                skip(parser)
            }
        }
    }

    /**
     * Parse Data node
     *
     * @param parser
     * @return
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readData(parser: XmlPullParser): DownloadLimitResponse {
        val downloadLimitResponse = DownloadLimitResponse()
        parser.require(XmlPullParser.START_TAG, ns, NODE_DATA)
        //Log_OC.d(TAG, "---- NODE DATA ---");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (NODE_LIMIT.equals(name, ignoreCase = true)) {
                val downloadLimit = readNode(parser, NODE_LIMIT)
                downloadLimitResponse.limit = if (downloadLimit.isNotEmpty()) downloadLimit.toLong() else 0L
            } else if (NODE_COUNT.equals(name, ignoreCase = true)) {
                val downloadCount = readNode(parser, NODE_COUNT)
                downloadLimitResponse.count = if (downloadCount.isNotEmpty()) downloadCount.toLong() else 0L
            } else {
                skip(parser)
            }
        }
        return downloadLimitResponse
    }

    /**
     * Parse a node, to obtain its text. Needs readText method
     *
     * @param parser
     * @param node
     * @return Text of the node
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readNode(parser: XmlPullParser, node: String): String {
        parser.require(XmlPullParser.START_TAG, ns, node)
        val value = readText(parser)
        //Log_OC.d(TAG, "node= " + node + ", value= " + value);
        parser.require(XmlPullParser.END_TAG, ns, node)
        return value
    }

    /**
     * Read the text from a node
     *
     * @param parser
     * @return Text of the node
     * @throws IOException
     * @throws XmlPullParserException
     */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    /**
     * Skip tags in parser procedure
     *
     * @param parser
     * @throws XmlPullParserException
     * @throws IOException
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        check(parser.eventType == XmlPullParser.START_TAG)
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    companion object {
        private val TAG = DownloadLimitXMLParser::class.java.simpleName

        // No namespaces
        private val ns: String? = null

        // NODES for XML Parser
        private const val NODE_OCS = "ocs"
        private const val NODE_META = "meta"
        private const val NODE_STATUS = "status"
        private const val NODE_STATUS_CODE = "statuscode"
        private const val NODE_MESSAGE = "message"
        private const val NODE_DATA = "data"
        private const val NODE_LIMIT = "limit"
        private const val NODE_COUNT = "count"
        private const val SUCCESS = 100
        private const val OK = 200
        private const val ERROR_WRONG_PARAMETER = 400
        private const val ERROR_FORBIDDEN = 403
        private const val ERROR_NOT_FOUND = 404
    }
}