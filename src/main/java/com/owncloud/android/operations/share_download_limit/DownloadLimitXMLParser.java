package com.owncloud.android.operations.share_download_limit;

import android.util.Xml;

import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.shares.ShareUtils;
import com.owncloud.android.lib.resources.shares.ShareXMLParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * class to parse the Download Limit api XML response This class code referenced from java in NC library
 */
public class DownloadLimitXMLParser {
    private static final String TAG = DownloadLimitXMLParser.class.getSimpleName();

    // No namespaces
    private static final String ns = null;

    // NODES for XML Parser
    private static final String NODE_OCS = "ocs";

    private static final String NODE_META = "meta";
    private static final String NODE_STATUS = "status";
    private static final String NODE_STATUS_CODE = "statuscode";
    private static final String NODE_MESSAGE = "message";

    private static final String NODE_DATA = "data";
    private static final String NODE_LIMIT = "limit";
    private static final String NODE_COUNT = "count";

    private static final int SUCCESS = 100;
    private static final int OK = 200;
    private static final int ERROR_WRONG_PARAMETER = 400;
    private static final int ERROR_FORBIDDEN = 403;
    private static final int ERROR_NOT_FOUND = 404;

    private String mStatus;
    private int mStatusCode;
    private String mMessage = "";

    // Getters and Setters
    public String getStatus() {
        return mStatus;
    }

    public void setStatus(String status) {
        this.mStatus = status;
    }

    public int getStatusCode() {
        return mStatusCode;
    }

    public void setStatusCode(int statusCode) {
        this.mStatusCode = statusCode;
    }

    public String getMessage() {
        return mMessage;
    }

    public boolean isSuccess() {
        return mStatusCode == SUCCESS || mStatusCode == OK;
    }

    public boolean isForbidden() {
        return mStatusCode == ERROR_FORBIDDEN;
    }

    public boolean isNotFound() {
        return mStatusCode == ERROR_NOT_FOUND;
    }

    public boolean isWrongParameter() {
        return mStatusCode == ERROR_WRONG_PARAMETER;
    }

    public void setMessage(String message) {
        this.mMessage = message;
    }

    /**
     * method to parse the Download limit response
     * @param isGet check if parsing has to do for GET api or not
     *              because the parsing will depend on that
     *              if API is GET then response will have <data> tag else it wont have
     * @param serverResponse
     * @return
     */
    public RemoteOperationResult<DownloadLimitResponse> parse(boolean isGet, String serverResponse) {
        if (serverResponse == null || serverResponse.length() == 0) {
            return new RemoteOperationResult<>(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE);
        }

        RemoteOperationResult<DownloadLimitResponse> result;
        try {
            // Parse xml response and obtain the list of downloadLimitResponse
            InputStream is = new ByteArrayInputStream(serverResponse.getBytes());

            DownloadLimitResponse downloadLimitResponse = parseXMLResponse(is);

            if (isSuccess()) {
                if (downloadLimitResponse != null && isGet) {
                    result = new RemoteOperationResult<>(RemoteOperationResult.ResultCode.OK);
                    result.setResultData(downloadLimitResponse);
                } else if (!isGet) {
                    result = new RemoteOperationResult<>(RemoteOperationResult.ResultCode.OK);
                } else {
                    result = new RemoteOperationResult<>(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE);
                    Log_OC.e(TAG, "Successful status with no share in the response");
                }
            } else if (isWrongParameter()) {
                result = new RemoteOperationResult<>(RemoteOperationResult.ResultCode.SHARE_WRONG_PARAMETER);
                result.setMessage(getMessage());
            } else if (isNotFound()) {
                result = new RemoteOperationResult<>(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND);
                result.setMessage(getMessage());
            } else if (isForbidden()) {
                result = new RemoteOperationResult<>(RemoteOperationResult.ResultCode.SHARE_FORBIDDEN);
                result.setMessage(getMessage());
            } else {
                result = new RemoteOperationResult<>(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE);
                result.setMessage(getMessage());
            }

        } catch (XmlPullParserException e) {
            Log_OC.e(TAG, "Error parsing response from server ", e);
            result = new RemoteOperationResult<>(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE);

        } catch (IOException e) {
            Log_OC.e(TAG, "Error reading response from server ", e);
            result = new RemoteOperationResult<>(RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE);
        }

        return result;
    }

    /**
     * Parse is as response of Share API
     *
     * @param is InputStream to parse
     * @return List of ShareRemoteFiles
     * @throws XmlPullParserException
     * @throws IOException
     */
    private DownloadLimitResponse parseXMLResponse(InputStream is) throws XmlPullParserException, IOException {
        try {
            // XMLPullParser
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);

            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(is, null);
            parser.nextTag();
            return readOCS(parser);

        } finally {
            is.close();
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
    private DownloadLimitResponse readOCS(XmlPullParser parser) throws XmlPullParserException,
        IOException {
        DownloadLimitResponse downloadLimitResponse = new DownloadLimitResponse();
        parser.require(XmlPullParser.START_TAG, ns, NODE_OCS);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // read NODE_META and NODE_DATA
            if (NODE_META.equalsIgnoreCase(name)) {
                readMeta(parser);
            } else if (NODE_DATA.equalsIgnoreCase(name)) {
                downloadLimitResponse = readData(parser);
            } else {
                skip(parser);
            }

        }
        return downloadLimitResponse;


    }

    /**
     * Parse Meta node
     *
     * @param parser
     * @throws XmlPullParserException
     * @throws IOException
     */
    private void readMeta(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, NODE_META);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();

            if (NODE_STATUS.equalsIgnoreCase(name)) {
                setStatus(readNode(parser, NODE_STATUS));

            } else if (NODE_STATUS_CODE.equalsIgnoreCase(name)) {
                setStatusCode(Integer.parseInt(readNode(parser, NODE_STATUS_CODE)));

            } else if (NODE_MESSAGE.equalsIgnoreCase(name)) {
                setMessage(readNode(parser, NODE_MESSAGE));

            } else {
                skip(parser);
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
    private DownloadLimitResponse readData(XmlPullParser parser) throws XmlPullParserException,
        IOException {
        DownloadLimitResponse downloadLimitResponse = new DownloadLimitResponse();

        parser.require(XmlPullParser.START_TAG, ns, NODE_DATA);
        //Log_OC.d(TAG, "---- NODE DATA ---");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (NODE_LIMIT.equalsIgnoreCase(name)) {
                downloadLimitResponse.setLimit(Integer.parseInt(readNode(parser, NODE_LIMIT)));
            } else if (NODE_COUNT.equalsIgnoreCase(name)) {
                downloadLimitResponse.setCount(Integer.parseInt(readNode(parser, NODE_COUNT)));
            } else {
                skip(parser);
            }
        }

        return downloadLimitResponse;
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
    private String readNode(XmlPullParser parser, String node) throws XmlPullParserException,
        IOException {
        parser.require(XmlPullParser.START_TAG, ns, node);
        String value = readText(parser);
        //Log_OC.d(TAG, "node= " + node + ", value= " + value);
        parser.require(XmlPullParser.END_TAG, ns, node);
        return value;
    }


    /**
     * Read the text from a node
     *
     * @param parser
     * @return Text of the node
     * @throws IOException
     * @throws XmlPullParserException
     */
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    /**
     * Skip tags in parser procedure
     *
     * @param parser
     * @throws XmlPullParserException
     * @throws IOException
     */
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
