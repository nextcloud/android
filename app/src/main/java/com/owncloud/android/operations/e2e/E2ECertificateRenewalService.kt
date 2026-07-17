/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.e2e

import android.accounts.AccountManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.e2ee.CsrHelper
import com.owncloud.android.lib.resources.users.DeletePublicKeyRemoteOperation
import com.owncloud.android.lib.resources.users.SendCSRRemoteOperation
import com.owncloud.android.utils.EncryptionUtils
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.PKCS8EncodedKeySpec

/**
 * Renews the end-to-end encryption certificate while preserving the existing key pair.
 *
 */
class E2ECertificateRenewalService(
    private val clientFactory: ClientFactory,
    private val arbitraryDataProvider: ArbitraryDataProvider
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun getCertificateValidity(user: User): E2ECertificateValidity? {
        val certificate = arbitraryDataProvider.getValue(user.accountName, EncryptionUtils.PUBLIC_KEY)
        if (certificate.isEmpty()) {
            return null
        }

        return runCatching {
            val x509 = EncryptionUtils.convertCertFromString(certificate)
            E2ECertificateValidity(x509.notBefore, x509.notAfter)
        }.getOrNull()
    }

    fun showRenewCertificateDialog(context: Context, user: User, onResult: (E2ECertificateRenewalResult) -> Unit) {
        MaterialAlertDialogBuilder(context, R.style.FallbackTheming_Dialog)
            .setTitle(R.string.prefs_renew_e2e_certificate)
            .setMessage(R.string.renew_e2e_certificate_dialog_message)
            .setCancelable(true)
            .setNegativeButton(R.string.common_cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.common_ok) { dialog, _ ->
                dialog.dismiss()
                renewCertificate(context.applicationContext, user, onResult)
            }
            .show()
    }

    private fun renewCertificate(context: Context, user: User, onResult: (E2ECertificateRenewalResult) -> Unit) {
        Thread {
            val result = runCatching { renew(context, user) }
                .getOrElse { e ->
                    Log.e(TAG, "Cannot renew E2E certificate", e)
                    E2ECertificateRenewalResult.Failure(R.string.renew_e2e_certificate_error)
                }

            mainHandler.post { onResult(result) }
        }.start()
    }

    @Suppress("ReturnCount")
    private fun renew(context: Context, user: User): E2ECertificateRenewalResult {
        val keyPair = reconstructKeyPair(user)
            ?: return E2ECertificateRenewalResult.Failure(R.string.renew_e2e_certificate_private_key_missing)

        val userId = AccountManager
            .get(context)
            .getUserData(user.toPlatformAccount(), AccountUtils.Constants.KEY_USER_ID)
        val csr = CsrHelper().generateCsrPemEncodedString(keyPair, userId)

        val client = clientFactory.createNextcloudClient(user)

        if (!DeletePublicKeyRemoteOperation().execute(client).isSuccess) {
            return E2ECertificateRenewalResult.Failure(R.string.renew_e2e_certificate_error)
        }

        val sendCsrResult = SendCSRRemoteOperation(csr).execute(client)
        val renewedCertificate = sendCsrResult.resultData
        if (!sendCsrResult.isSuccess || renewedCertificate.isNullOrEmpty()) {
            return E2ECertificateRenewalResult.Failure(R.string.renew_e2e_certificate_error)
        }

        if (!EncryptionUtils.isMatchingKeys(keyPair, renewedCertificate)) {
            EncryptionUtils.reportE2eError(arbitraryDataProvider, user)
            return E2ECertificateRenewalResult.Failure(R.string.renew_e2e_certificate_error)
        }

        arbitraryDataProvider.storeOrUpdateKeyValue(user.accountName, EncryptionUtils.PUBLIC_KEY, renewedCertificate)
        Log_OC.i(TAG, "E2E certificate renewed successfully for " + user.accountName)

        return E2ECertificateRenewalResult.Success
    }

    private fun reconstructKeyPair(user: User): KeyPair? {
        val privateKeyString = arbitraryDataProvider.getValue(user.accountName, EncryptionUtils.PRIVATE_KEY)
        val certificate = arbitraryDataProvider.getValue(user.accountName, EncryptionUtils.PUBLIC_KEY)
        if (privateKeyString.isEmpty() || certificate.isEmpty()) {
            return null
        }

        return runCatching {
            val privateKeyBytes = EncryptionUtils.decodeStringToBase64Bytes(privateKeyString)
            val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
            val privateKey = KeyFactory.getInstance(EncryptionUtils.RSA).generatePrivate(keySpec)
            val publicKey = EncryptionUtils.convertPublicKeyFromString(certificate)
            KeyPair(publicKey, privateKey)
        }.getOrNull()
    }

    companion object {
        private val TAG = E2ECertificateRenewalService::class.java.simpleName
    }
}
