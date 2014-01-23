/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/)
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

package com.owncloud.android.oc_framework.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;
import android.util.Xml;

import com.owncloud.android.oc_framework.operations.ShareRemoteFile;
import com.owncloud.android.oc_framework.operations.ShareType;

/**
 * Parser for Share API Response
 * @author masensio
 *
 */

public class ShareXMLParser {

	private static final String TAG = ShareXMLParser.class.getSimpleName();

	// No namespaces
	private static final String ns = null;

	// NODES for XML Parser
	private static final String NODE_OCS = "ocs";

	private static final String NODE_META = "meta";
	private static final String NODE_STATUS = "status";
	private static final String NODE_STATUS_CODE = "statuscode";
	//private static final String NODE_MESSAGE = "message";

	private static final String NODE_DATA = "data";
	private static final String NODE_ELEMENT = "element";
	private static final String NODE_ID = "id";
	private static final String NODE_ITEM_TYPE = "item_type";
	private static final String NODE_ITEM_SOURCE = "item_source";
	private static final String NODE_PARENT = "parent";
	private static final String NODE_SHARE_TYPE = "share_type";
	private static final String NODE_SHARE_WITH = "share_with";
	private static final String NODE_FILE_SOURCE = "file_source";
	private static final String NODE_PATH = "path";
	private static final String NODE_PERMISSIONS = "permissions";
	private static final String NODE_STIME = "stime";
	private static final String NODE_EXPIRATION = "expiration";
	private static final String NODE_TOKEN = "token";
	private static final String NODE_STORAGE = "storage";
	private static final String NODE_MAIL_SEND = "mail_send";
	private static final String NODE_SHARE_WITH_DISPLAY_NAME = "share_with_display_name";

	private static final String TYPE_FOLDER = "folder";

	private String mStatus;
	private int mStatusCode;

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

	// Constructor
	public ShareXMLParser() {

	}

	/**
	 * Parse is as response of Share API
	 * @param is
	 * @return List of ShareRemoteFiles
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	public ArrayList<ShareRemoteFile> parseXMLResponse(InputStream is) throws XmlPullParserException, IOException {

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
	 * @param parser
	 * @return List of ShareRemoteFiles
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private ArrayList<ShareRemoteFile> readOCS (XmlPullParser parser) throws XmlPullParserException, IOException {
		ArrayList<ShareRemoteFile> sharedFiles = new ArrayList<ShareRemoteFile>();
		parser.require(XmlPullParser.START_TAG,  ns , NODE_OCS);
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// read NODE_META and NODE_DATA
			if (name.equalsIgnoreCase(NODE_META)) {
				readMeta(parser);
			} else if (name.equalsIgnoreCase(NODE_DATA)) {
				sharedFiles = readData(parser);
			} else {
				skip(parser);
			}

		}
		return sharedFiles;


	}

	/**
	 * Parse Meta node
	 * @param parser
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private void readMeta(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, NODE_META);
		Log.d(TAG, "---- NODE META ---");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();

			if  (name.equalsIgnoreCase(NODE_STATUS)) {
				setStatus(readNode(parser, NODE_STATUS));

			} else if (name.equalsIgnoreCase(NODE_STATUS_CODE)) {
				setStatusCode(Integer.parseInt(readNode(parser, NODE_STATUS_CODE)));

			} else {
				skip(parser);
			}

		}
	}

	/**
	 * Parse Data node
	 * @param parser
	 * @return
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private ArrayList<ShareRemoteFile> readData(XmlPullParser parser) throws XmlPullParserException, IOException {
		ArrayList<ShareRemoteFile> sharedFiles = new ArrayList<ShareRemoteFile>();

		parser.require(XmlPullParser.START_TAG, ns, NODE_DATA);		
		Log.d(TAG, "---- NODE DATA ---");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equalsIgnoreCase(NODE_ELEMENT)) {
				sharedFiles.add(readElement(parser));
			} else {
				skip(parser);
			} 
		}

		return sharedFiles;

	}

	/** 
	 * Parse Element node
	 * @param parser
	 * @return
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private ShareRemoteFile readElement(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, NODE_ELEMENT);
		
		ShareRemoteFile sharedFile = new ShareRemoteFile();
		
		Log.d(TAG, "---- NODE ELEMENT ---");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
			
			String name = parser.getName();

			if (name.equalsIgnoreCase(NODE_ID)) {
				sharedFile.setIdRemoteShared(Integer.parseInt(readNode(parser, NODE_ID)));

			} else if (name.equalsIgnoreCase(NODE_ITEM_TYPE)) {
				sharedFile.setIsDirectory(readNode(parser, NODE_ITEM_TYPE).equalsIgnoreCase(TYPE_FOLDER));

			} else if (name.equalsIgnoreCase(NODE_ITEM_SOURCE)) {
				sharedFile.setItemSource(Long.parseLong(readNode(parser, NODE_ITEM_SOURCE)));

			} else if (name.equalsIgnoreCase(NODE_PARENT)) {
				readNode(parser, NODE_PARENT);

			} else if (name.equalsIgnoreCase(NODE_SHARE_TYPE)) {
				int value = Integer.parseInt(readNode(parser, NODE_SHARE_TYPE));
				sharedFile.setShareType(ShareType.fromValue(value));

			} else if (name.equalsIgnoreCase(NODE_SHARE_WITH)) {
				sharedFile.setShareWith(readNode(parser, NODE_SHARE_WITH));

			} else if (name.equalsIgnoreCase(NODE_FILE_SOURCE)) {
				sharedFile.setFileSource(Long.parseLong(readNode(parser, NODE_FILE_SOURCE)));

			} else if (name.equalsIgnoreCase(NODE_PATH)) {
				sharedFile.setPath(readNode(parser, NODE_PATH));

			} else if (name.equalsIgnoreCase(NODE_PERMISSIONS)) {
				sharedFile.setPermissions(Integer.parseInt(readNode(parser, NODE_PERMISSIONS)));

			} else if (name.equalsIgnoreCase(NODE_STIME)) {
				sharedFile.setSharedDate(Long.parseLong(readNode(parser, NODE_STIME)));

			} else if (name.equalsIgnoreCase(NODE_EXPIRATION)) {
				String value = readNode(parser, NODE_EXPIRATION);
				if (!value.isEmpty()) {
					sharedFile.setExpirationDate(Long.parseLong(readNode(parser, NODE_EXPIRATION))); // check if expiration is in long format or date format
				}

			} else if (name.equalsIgnoreCase(NODE_TOKEN)) {
				sharedFile.setToken(readNode(parser, NODE_TOKEN));

			} else if (name.equalsIgnoreCase(NODE_STORAGE)) {
				readNode(parser, NODE_STORAGE);
			} else if (name.equalsIgnoreCase(NODE_MAIL_SEND)) {
				readNode(parser, NODE_MAIL_SEND);

			} else if (name.equalsIgnoreCase(NODE_SHARE_WITH_DISPLAY_NAME)) {
				sharedFile.setSharedWithDisplayName(readNode(parser, NODE_SHARE_WITH_DISPLAY_NAME));

			} else {
				skip(parser);
			} 
		}		

		return sharedFile;
	}

	/**
	 * Parse a node, to obtain its text. Needs readText method
	 * @param parser
	 * @param node
	 * @return Text of the node
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private String readNode (XmlPullParser parser, String node) throws XmlPullParserException, IOException{
		parser.require(XmlPullParser.START_TAG, ns, node);
		String value = readText(parser);
		Log.d(TAG, "node= " + node + ", value= " + value);
		parser.require(XmlPullParser.END_TAG, ns, node);
		return value;
	}
	
	/**
	 * Read the text from a node
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
