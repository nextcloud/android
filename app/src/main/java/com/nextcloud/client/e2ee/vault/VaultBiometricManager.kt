/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.owncloud.android.R
import javax.inject.Inject

class VaultBiometricManager @Inject constructor(private val context: Context) {
    fun canAuthenticate(): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(
        activity: FragmentActivity,
        onAuthenticationSucceeded: Runnable,
        onAuthenticationRejected: Runnable
    ) {
        if (!canAuthenticate()) {
            onAuthenticationRejected.run()
            return
        }

        val biometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onAuthenticationRejected.run()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onAuthenticationSucceeded.run()
                }
            }
        )

        biometricPrompt.authenticate(createPromptInfo())
    }

    private fun createPromptInfo(): BiometricPrompt.PromptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(context.getString(R.string.e2ee_vault_biometric_prompt_title))
        .setSubtitle(context.getString(R.string.e2ee_vault_biometric_prompt_subtitle))
        .setAllowedAuthenticators(AUTHENTICATORS)
        .build()

    private companion object {
        private const val AUTHENTICATORS = BIOMETRIC_STRONG or DEVICE_CREDENTIAL
    }
}
