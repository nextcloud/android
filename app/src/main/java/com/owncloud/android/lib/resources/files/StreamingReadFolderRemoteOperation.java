/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.lib.resources.files;

import android.net.Uri;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Remote operation that reads a folder from the server using streaming XML parser.
 * This implementation uses SAX parser to avoid loading the entire XML response into memory,
 * preventing OutOfMemoryError for large folders with many files.
 *
 * @see ReadFolderRemoteOperation
 */
public class StreamingReadFolderRemoteOperation extends RemoteOperation<List<Object>> {
    private static final String TAG = StreamingReadFolderRemoteOperation.class.getSimpleName();

    private final String remotePath;

    public StreamingReadFolderRemoteOperation(String remotePath) {
        this.remotePath = remotePath;
    }

    @Override
    protected RemoteOperationResult<List<Object>> run(OwnCloudClient client) {
        if (remotePath == null) {
            return new RemoteOperationResult(ResultCode.UNKNOWN_ERROR);
        }

        String davPath;
        String davBasePath;
        try {
            Uri davUri = client.getFilesDavUri();
            davBasePath = davUri.toString();

            // Use Uri.Builder to properly encode path segments with special characters (e.g., Cyrillic)
            // Uri.Builder.appendPath() automatically URL-encodes each segment
            Uri.Builder builder = davUri.buildUpon();

            // Normalize remotePath - ensure it starts with /
            String normalizedRemotePath = remotePath.startsWith("/") ? remotePath : "/" + remotePath;

            // Split remotePath into segments and append each one
            // This ensures proper URL encoding of each segment (including Cyrillic characters)
            String[] segments = normalizedRemotePath.split("/");
            for (String segment : segments) {
                if (!segment.isEmpty()) {
                    builder.appendPath(segment);
                }
            }

            // If path ends with /, append empty segment to preserve trailing slash
            if (normalizedRemotePath.endsWith("/") && !normalizedRemotePath.equals("/")) {
                builder.appendPath("");
            }

            Uri fullUri = builder.build();
            davPath = fullUri.toString();
        } catch (Exception e) {
            Log_OC.e(TAG, "Error getting DAV path", e);
            return new RemoteOperationResult(e);
        }

        StreamingPropFindMethod method = null;
        try {
            // Create PROPFIND request with depth 1 (folder + immediate children)
            // Use StreamingPropFindMethod to prevent automatic response body processing
            // Use ALL_PROP to get all available properties including OC/NC specific ones
            method = new StreamingPropFindMethod(davPath, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);

            int status = client.executeMethod(method);

            Log_OC.d(TAG, "PROPFIND request status: " + status + " for path: " + remotePath);

            if (status == HttpStatus.SC_MULTI_STATUS || status == HttpStatus.SC_OK) {
                InputStream inputStream = method.getResponseBodyAsStream();

                if (inputStream == null) {
                    Log_OC.e(TAG, "Response body stream is null");
                    return new RemoteOperationResult(ResultCode.UNKNOWN_ERROR);
                }

                // Parse XML using SAX parser
                PropFindSaxHandler handler = new PropFindSaxHandler(davBasePath);
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);

                try {
                    SAXParser parser = factory.newSAXParser();
                    parser.parse(inputStream, handler);

                    List<Object> filesList = handler.getFiles();
                    Log_OC.d(TAG, "Parsed " + filesList.size() + " files from folder: " + remotePath);

                    // Convert List to ArrayList as required by setData()
                    ArrayList<Object> files = new ArrayList<>(filesList);

                    RemoteOperationResult result = new RemoteOperationResult(true, method);
                    result.setData(files);
                    return result;
                } catch (SAXParseException e) {
                    Log_OC.e(TAG, "XML parsing error at line " + e.getLineNumber() +
                        ", column " + e.getColumnNumber() + " for path: " + remotePath, e);
                    return new RemoteOperationResult(ResultCode.UNKNOWN_ERROR);
                } catch (SAXException e) {
                    Log_OC.e(TAG, "SAX parsing error for path: " + remotePath, e);
                    return new RemoteOperationResult(ResultCode.UNKNOWN_ERROR);
                } catch (ParserConfigurationException e) {
                    Log_OC.e(TAG, "SAX parser configuration error", e);
                    return new RemoteOperationResult(ResultCode.UNKNOWN_ERROR);
                } catch (Exception e) {
                    Log_OC.e(TAG, "Error parsing XML response for path: " + remotePath, e);
                    return new RemoteOperationResult(e);
                } finally {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                        Log_OC.w(TAG, "Error closing input stream: " + e.getMessage());
                    }
                }
            } else {
                Log_OC.e(TAG, "PROPFIND request failed with status: " + status + " for path: " + remotePath);
                return new RemoteOperationResult(false, method);
            }
        } catch (OutOfMemoryError e) {
            Log_OC.e(TAG, "OutOfMemoryError while fetching folder " + remotePath, e);
            return new RemoteOperationResult(ResultCode.UNKNOWN_ERROR);
        } catch (Exception e) {
            Log_OC.e(TAG, "Exception while fetching folder " + remotePath, e);
            return new RemoteOperationResult(e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    /**
     * Custom PropFindMethod that doesn't automatically process the response body.
     * This allows us to get the raw InputStream for streaming XML parsing.
     */
    private static class StreamingPropFindMethod extends PropFindMethod {
        StreamingPropFindMethod(String uri, int propfindType, int depth) throws IOException {
            super(uri, propfindType, new DavPropertyNameSet(), depth);
        }

        @Override
        protected void processResponseBody(HttpState httpState, HttpConnection httpConnection) {
            // Do not process the response body here. 
            // We need the raw stream for SAX parsing to avoid loading entire XML into memory.
        }
    }
}

