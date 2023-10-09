package com.nextcloud.client.account

import android.accounts.Account
import android.accounts.AccountManager
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.accounts.AccountUtils
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class UserAccountManagerImplTest : AbstractOnServerIT() {
    private var accountManager: AccountManager? = null
    @Before
    fun setUp() {
        accountManager = AccountManager.get(targetContext)
    }

    @Test
    fun updateOneAccount() {
        val appPreferences = AppPreferencesImpl.fromContext(targetContext)
        val sut = UserAccountManagerImpl(targetContext, accountManager)
        TestCase.assertEquals(1, sut.accounts.size)
        Assert.assertFalse(appPreferences.isUserIdMigrated)
        val account = sut.accounts[0]

        // for testing remove userId
        accountManager!!.setUserData(account, AccountUtils.Constants.KEY_USER_ID, null)
        TestCase.assertNull(accountManager!!.getUserData(account, AccountUtils.Constants.KEY_USER_ID))
        val success = sut.migrateUserId()
        TestCase.assertTrue(success)
        val arguments = InstrumentationRegistry.getArguments()
        val userId = arguments.getString("TEST_SERVER_USERNAME")

        // assume that userId == loginname (as we manually set it)
        TestCase.assertEquals(userId, accountManager!!.getUserData(account, AccountUtils.Constants.KEY_USER_ID))
    }

    @Test
    fun checkName() {
        val sut = UserAccountManagerImpl(targetContext, accountManager)
        val owner = Account("John@nextcloud.local", "nextcloud")
        val account1 = Account("John@nextcloud.local", "nextcloud")
        val account2 = Account("john@nextcloud.local", "nextcloud")
        val file1 = OCFile("/test1.pdf")
        file1.ownerId = "John"
        TestCase.assertTrue(sut.accountOwnsFile(file1, owner))
        TestCase.assertTrue(sut.accountOwnsFile(file1, account1))
        TestCase.assertTrue(sut.accountOwnsFile(file1, account2))
        file1.ownerId = "john"
        TestCase.assertTrue(sut.accountOwnsFile(file1, owner))
        TestCase.assertTrue(sut.accountOwnsFile(file1, account1))
        TestCase.assertTrue(sut.accountOwnsFile(file1, account2))
    }
}