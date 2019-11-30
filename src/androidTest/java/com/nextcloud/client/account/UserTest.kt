package com.nextcloud.client.account

import android.accounts.Account
import android.net.Uri
import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudBasicCredentials
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URI

@RunWith(AndroidJUnit4::class)
class UserTest {

    @Test
    fun anonymousUserImplementsParcelable() {
        // GIVEN
        //      anonymous user instance
        val original = AnonymousUser("test_account")

        // WHEN
        //      instance is serialized into Parcel
        //      instance is retrieved from Parcel
        val parcel = Parcel.obtain()
        parcel.setDataPosition(0)
        parcel.writeParcelable(original, 0)
        parcel.setDataPosition(0)
        val retrieved = parcel.readParcelable<User>(User::class.java.classLoader)

        // THEN
        //      retrieved instance in distinct
        //      instances are equal
        assertNotSame(original, retrieved)
        assertTrue(retrieved is AnonymousUser)
        assertEquals(original, retrieved)
    }

    @Test
    fun registeredUserImplementsParcelable() {
        // GIVEN
        //      registered user instance
        val uri = Uri.parse("https://nextcloud.localhost.localdomain")
        val credentials = OwnCloudBasicCredentials("user", "pass")
        val account = Account("test@nextcloud.localhost.localdomain", "test-type")
        val ownCloudAccount = OwnCloudAccount(uri, credentials)
        val server = Server(
            uri = URI(uri.toString()),
            version = OwnCloudVersion.nextcloud_17
        )
        val original = RegisteredUser(
            account = account,
            ownCloudAccount = ownCloudAccount,
            server = server
        )

        // WHEN
        //      instance is serialized into Parcel
        //      instance is retrieved from Parcel
        val parcel = Parcel.obtain()
        parcel.setDataPosition(0)
        parcel.writeParcelable(original, 0)
        parcel.setDataPosition(0)
        val retrieved = parcel.readParcelable<User>(User::class.java.classLoader)

        // THEN
        //      retrieved instance in distinct
        //      instances are equal
        assertNotSame(original, retrieved)
        assertTrue(retrieved is RegisteredUser)
        assertEquals(original, retrieved)
    }
}
