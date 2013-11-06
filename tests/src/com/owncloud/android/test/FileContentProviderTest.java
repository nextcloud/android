package com.owncloud.android.test;

import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.providers.FileContentProvider;
import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.CUPCAKE)
public class FileContentProviderTest extends ProviderTestCase2<FileContentProvider> {

	private static final String TAG = FileContentProvider.class.getName();
	
	private static MockContentResolver resolve;
	
	public FileContentProviderTest(Class<FileContentProvider> providerClass,
			String providerAuthority) {
		super(providerClass, providerAuthority);
		// TODO Auto-generated constructor stub
	}
	
	public FileContentProviderTest() {
		super(FileContentProvider.class, "com.owncloud.android.providers.FileContentProvider");	
	}
	
	@Override
	public void setUp() {
		Log.i(TAG, "Entered setup");
		try {
			super.setUp();
			resolve = this.getMockContentResolver();
		} catch (Exception e) {
			
		}
	}
	
	public void testGetTypeFile() {
		Uri testuri = Uri.parse("content://org.owncloud/file/");
		assertEquals(ProviderTableMeta.CONTENT_TYPE_ITEM, resolve.getType(testuri));

		testuri = Uri.parse("content://org.owncloud/file/123");
		assertEquals(ProviderTableMeta.CONTENT_TYPE_ITEM, resolve.getType(testuri));
	}
	
	public void testGetTypeRoot() {
		Uri testuri = Uri.parse("content://org.owncloud/");
		assertEquals(ProviderTableMeta.CONTENT_TYPE, resolve.getType(testuri));
	}

}
