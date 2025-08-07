/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog.setupEncryption

import android.accounts.AccountManager
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.SetupEncryptionDialogBinding
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.e2ee.CsrHelper
import com.owncloud.android.lib.resources.users.DeletePublicKeyRemoteOperation
import com.owncloud.android.lib.resources.users.GetPrivateKeyRemoteOperation
import com.owncloud.android.lib.resources.users.GetPublicKeyRemoteOperation
import com.owncloud.android.lib.resources.users.GetServerPublicKeyRemoteOperation
import com.owncloud.android.lib.resources.users.SendCSRRemoteOperation
import com.owncloud.android.lib.resources.users.StorePrivateKeyRemoteOperation
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.crypto.CryptoHelper
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.Arrays
import javax.inject.Inject

/*
 *  Dialog to setup encryption
 */
class SetupEncryptionDialogFragment :
    DialogFragment(),
    Injectable {

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @JvmField
    @Inject
    var clientFactory: ClientFactory? = null

    @JvmField
    @Inject
    var certificateValidator: CertificateValidator? = null

    private var user: User? = null
    private var arbitraryDataProvider: ArbitraryDataProvider? = null
    private var positiveButton: MaterialButton? = null
    private var negativeButton: MaterialButton? = null
    private var keyResult: String? = null
    private var keyWords: ArrayList<String>? = null
    private var downloadKeyResult: DownloadKeyResult? = null

    private lateinit var binding: SetupEncryptionDialogBinding

    override fun onStart() {
        super.onStart()

        setupAlertDialog()
        lifecycleScope.launch {
            downloadKeys()
        }
    }

    private fun setupAlertDialog() {
        val alertDialog = dialog as AlertDialog?

        if (alertDialog != null) {
            positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE) as? MaterialButton?
            positiveButton?.let {
                viewThemeUtils.material.colorMaterialButtonPrimaryTonal(it)
            }

            negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE) as? MaterialButton?
            negativeButton?.let {
                viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(it)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        checkNotNull(arguments) { "Arguments may not be null" }

        user = requireArguments().getParcelableArgument(ARG_USER, User::class.java)

        if (savedInstanceState != null) {
            keyWords = savedInstanceState.getStringArrayList(EncryptionUtils.MNEMONIC)
        }

        arbitraryDataProvider = ArbitraryDataProviderImpl(context)

        // Inflate the layout for the dialog
        val inflater = requireActivity().layoutInflater
        binding = SetupEncryptionDialogBinding.inflate(inflater, null, false)

        // Setup layout
        viewThemeUtils.material.colorTextInputLayout(binding.encryptionPasswordInputContainer)

        val builder = buildMaterialAlertDialog(binding.root)
        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(requireContext(), builder)
        return builder.create().apply {
            setCanceledOnTouchOutside(false)
            setOnShowListener { dialog1: DialogInterface ->
                val button = (dialog1 as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                button.setOnClickListener { positiveButtonOnClick(this) }
            }
        }
    }

    private fun buildMaterialAlertDialog(v: View): MaterialAlertDialogBuilder =
        MaterialAlertDialogBuilder(requireContext())
            .setView(v)
            .setPositiveButton(R.string.common_ok, null)
            .setNegativeButton(R.string.common_cancel) { dialog: DialogInterface, _: Int -> dialog.cancel() }
            .setTitle(R.string.end_to_end_encryption_title)

    private fun positiveButtonOnClick(dialog: DialogInterface) {
        when (keyResult) {
            KEY_CREATED -> {
                Log_OC.d(TAG, "New keys generated and stored.")
                dialog.dismiss()
                notifyResult()
            }

            KEY_EXISTING_USED -> {
                decryptPrivateKey(dialog)
            }

            KEY_GENERATE -> {
                generateKey()
            }

            else -> dialog.dismiss()
        }
    }

    @Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown", "ReturnCount", "LongMethod")
    private fun decryptPrivateKey(dialog: DialogInterface) {
        Log_OC.d(TAG, "Decrypt private key")
        binding.encryptionStatus.setText(R.string.end_to_end_encryption_decrypting)

        try {
            if (downloadKeyResult !is DownloadKeyResult.Success) {
                Log_OC.d(TAG, "DownloadKeyResult is not success")
                return
            }

            val privateKey = (downloadKeyResult as DownloadKeyResult.Success).privateKey
            if (privateKey.isNullOrEmpty()) {
                Log_OC.e(TAG, "privateKey is null or empty")
                return
            }
            val mnemonicUnchanged = binding.encryptionPasswordInput.text.toString().trim()
            val mnemonic =
                binding.encryptionPasswordInput.text.toString().replace("\\s".toRegex(), "")
                    .lowercase()
            val decryptedPrivateKey = CryptoHelper.decryptPrivateKey(
                privateKey,
                mnemonic
            )
            val accountName = user?.accountName ?: return

            arbitraryDataProvider?.storeOrUpdateKeyValue(
                accountName,
                EncryptionUtils.PRIVATE_KEY,
                decryptedPrivateKey
            )
            dialog.dismiss()

            Log_OC.d(TAG, "Private key successfully decrypted and stored")

            arbitraryDataProvider?.storeOrUpdateKeyValue(
                accountName,
                EncryptionUtils.MNEMONIC,
                mnemonicUnchanged
            )

            // check if private key and public key match
            val publicKey = arbitraryDataProvider?.getValue(
                accountName,
                EncryptionUtils.PUBLIC_KEY
            )

            val firstKey = EncryptionUtils.generateKey()
            val base64encodedKey = EncryptionUtils.encodeBytesToBase64String(firstKey)
            val encryptedString = EncryptionUtils.encryptStringAsymmetric(
                base64encodedKey,
                publicKey
            )
            val decryptedString = EncryptionUtils.decryptStringAsymmetric(
                encryptedString,
                decryptedPrivateKey
            )
            val secondKey = EncryptionUtils.decodeStringToBase64Bytes(decryptedString)

            if (!Arrays.equals(firstKey, secondKey)) {
                EncryptionUtils.reportE2eError(arbitraryDataProvider, user)
                throw Exception("Keys do not match")
            }

            notifyResult()
        } catch (e: Exception) {
            binding.encryptionStatus.setText(R.string.end_to_end_encryption_wrong_password)
            Log_OC.d(TAG, "Error while decrypting private key: " + e.message)
        }
    }

    private fun generateKey() {
        binding.encryptionPassphrase.visibility = View.GONE
        positiveButton?.visibility = View.GONE
        negativeButton?.visibility = View.GONE

        dialog?.setTitle(R.string.end_to_end_encryption_storing_keys)

        lifecycleScope.launch {
            generateNewKeys()
        }
    }

    private fun notifyResult() {
        val targetFragment = targetFragment
        targetFragment?.onActivityResult(
            targetRequestCode,
            SETUP_ENCRYPTION_RESULT_CODE,
            resultIntent
        )
        parentFragmentManager.setFragmentResult(RESULT_REQUEST_KEY, resultBundle)
    }

    private val resultIntent: Intent
        get() {
            return Intent().apply {
                putExtra(SUCCESS, true)
                putExtra(ARG_POSITION, requireArguments().getInt(ARG_POSITION))
            }
        }
    private val resultBundle: Bundle
        get() {
            return Bundle().apply {
                putBoolean(SUCCESS, true)
                putInt(ARG_POSITION, requireArguments().getInt(ARG_POSITION))
            }
        }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)

        val bundle = Bundle().apply {
            putBoolean(RESULT_KEY_CANCELLED, true)
        }

        parentFragmentManager.setFragmentResult(RESULT_REQUEST_KEY, bundle)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList(EncryptionUtils.MNEMONIC, keyWords)
        super.onSaveInstanceState(outState)
    }

    sealed class DownloadKeyResult(open val descriptionId: Int? = null) {
        data class CertificateVerificationFailed(
            override val descriptionId: Int = R.string.end_to_end_encryption_certificate_verification_failed
        ) : DownloadKeyResult(descriptionId)

        data class ServerPublicKeyUnavailable(
            override val descriptionId: Int = R.string.end_to_end_encryption_server_public_key_unavailable
        ) : DownloadKeyResult(descriptionId)

        data class ServerPrivateKeyUnavailable(
            override val descriptionId: Int = R.string.end_to_end_encryption_server_private_key_unavailable
        ) : DownloadKeyResult(descriptionId)

        data class CertificateUnavailable(
            override val descriptionId: Int = R.string.end_to_end_encryption_certificate_unavailable
        ) : DownloadKeyResult(descriptionId)

        data class UnexpectedError(
            override val descriptionId: Int = R.string.end_to_end_encryption_unexpected_error_occurred
        ) : DownloadKeyResult(descriptionId)

        data class Success(val privateKey: String?) : DownloadKeyResult()
    }

    private suspend fun downloadKeys() {
        binding.encryptionStatus.setText(R.string.end_to_end_encryption_retrieving_keys)
        positiveButton?.visibility = View.INVISIBLE

        downloadKeyResult = withContext(Dispatchers.IO) {
            val weakContext = WeakReference(context).get() ?: return@withContext DownloadKeyResult.UnexpectedError()
            val user = user ?: return@withContext DownloadKeyResult.UnexpectedError()
            val dataProvider = arbitraryDataProvider ?: return@withContext DownloadKeyResult.UnexpectedError()

            val certificateOperation = GetPublicKeyRemoteOperation()
            val certificateResult = certificateOperation.executeNextcloudClient(user, weakContext)
            val savedPrivateKey = dataProvider.getValue(user.accountName, EncryptionUtils.PRIVATE_KEY)
            if (!certificateResult.isSuccess) {
                // The certificate might not be available on the server yet.
                // Therefore, the user needs to generate a new passphrase first.
                return@withContext if (savedPrivateKey.isEmpty()) {
                    DownloadKeyResult.Success(null)
                } else {
                    DownloadKeyResult.CertificateUnavailable()
                }
            }

            val serverPublicKeyOperation = GetServerPublicKeyRemoteOperation()
            val serverPublicKeyResult = serverPublicKeyOperation.executeNextcloudClient(user, weakContext)
            if (!serverPublicKeyResult.isSuccess) {
                return@withContext DownloadKeyResult.ServerPublicKeyUnavailable()
            }

            val serverKey = serverPublicKeyResult.resultData
            val certificateAsString = certificateResult.resultData
            val isCertificateValid = certificateValidator?.validate(serverKey, certificateAsString)

            if (isCertificateValid == false) {
                return@withContext DownloadKeyResult.CertificateVerificationFailed()
            }

            dataProvider.storeOrUpdateKeyValue(
                user.accountName,
                EncryptionUtils.PUBLIC_KEY,
                certificateAsString
            )

            val privateKeyOperation = GetPrivateKeyRemoteOperation()
            val privateKeyResult = privateKeyOperation.executeNextcloudClient(user, weakContext)
            return@withContext if (privateKeyResult.isSuccess) {
                Log_OC.d(TAG, "private key successful downloaded for " + user.accountName)
                keyResult = KEY_EXISTING_USED
                val privateKey = privateKeyResult.resultData?.getKey()
                DownloadKeyResult.Success(privateKey)
            } else {
                DownloadKeyResult.ServerPrivateKeyUnavailable()
            }
        }

        downloadKeyResult?.let { result ->
            if (result is DownloadKeyResult.Success) {
                handlePrivateKey(result.privateKey)
            } else {
                val descriptionId = result.descriptionId ?: return
                val description = getString(descriptionId)
                dismiss()
                DisplayUtils.showSnackMessage(requireActivity(), description)
            }
        }
    }

    private fun handlePrivateKey(privateKey: String?) {
        if (privateKey == null) {
            // first show info
            try {
                if (keyWords == null || keyWords!!.isEmpty()) {
                    keyWords = EncryptionUtils.getRandomWords(NUMBER_OF_WORDS, context)
                }
                showMnemonicInfo()
            } catch (e: IOException) {
                binding.encryptionStatus.setText(R.string.common_error)
            }
        } else if (privateKey.isNotEmpty()) {
            binding.encryptionStatus.setText(R.string.end_to_end_encryption_enter_passphrase_to_access_files)
            binding.encryptionPasswordInputContainer.visibility = View.VISIBLE
            positiveButton?.visibility = View.VISIBLE
        } else {
            Log_OC.e(TAG, "Got empty private key string")
        }
    }

    @Suppress("LongMethod", "TooGenericExceptionCaught", "TooGenericExceptionThrown")
    private suspend fun generateNewKeys() {
        binding.encryptionStatus.setText(R.string.end_to_end_encryption_generating_keys)
        val context = context ?: return
        val privateKey: String = withContext(Dispatchers.IO) {
            //  - create CSR, push to server, store returned public key in database
            //  - encrypt private key, push key to server, store unencrypted private key in database
            try {
                val publicKeyString: String

                // Create public/private key pair
                val keyPair = EncryptionUtils.generateKeyPair()

                // create CSR
                val accountManager = AccountManager.get(context)
                val user = user ?: return@withContext ""

                val userId = accountManager.getUserData(user.toPlatformAccount(), AccountUtils.Constants.KEY_USER_ID)
                val urlEncoded = CsrHelper().generateCsrPemEncodedString(keyPair, userId)
                val operation = SendCSRRemoteOperation(urlEncoded)
                val result = operation.executeNextcloudClient(user, context)

                if (result.isSuccess) {
                    publicKeyString = result.resultData
                    if (!EncryptionUtils.isMatchingKeys(keyPair, publicKeyString)) {
                        EncryptionUtils.reportE2eError(arbitraryDataProvider, user)
                        throw RuntimeException("Wrong CSR returned")
                    }
                    Log_OC.d(TAG, "public key success")
                } else {
                    keyResult = KEY_FAILED
                    return@withContext ""
                }

                val privateKey = keyPair.private
                val privateKeyString = EncryptionUtils.encodeBytesToBase64String(privateKey.encoded)
                val privatePemKeyString = EncryptionUtils.privateKeyToPEM(privateKey)
                val encryptedPrivateKey = CryptoHelper.encryptPrivateKey(
                    privatePemKeyString,
                    generateMnemonicString(false)
                )

                // upload encryptedPrivateKey
                val storePrivateKeyOperation = StorePrivateKeyRemoteOperation(encryptedPrivateKey)
                val storePrivateKeyResult = storePrivateKeyOperation.executeNextcloudClient(user, context)
                if (storePrivateKeyResult.isSuccess) {
                    Log_OC.d(TAG, "private key success")
                    arbitraryDataProvider?.storeOrUpdateKeyValue(
                        user.accountName,
                        EncryptionUtils.PRIVATE_KEY,
                        privateKeyString
                    )
                    arbitraryDataProvider?.storeOrUpdateKeyValue(
                        user.accountName,
                        EncryptionUtils.PUBLIC_KEY,
                        publicKeyString
                    )
                    arbitraryDataProvider?.storeOrUpdateKeyValue(
                        user.accountName,
                        EncryptionUtils.MNEMONIC,
                        generateMnemonicString(true)
                    )
                    keyResult = KEY_CREATED

                    return@withContext storePrivateKeyResult.resultData
                } else {
                    val deletePublicKeyOperation = DeletePublicKeyRemoteOperation()
                    deletePublicKeyOperation.executeNextcloudClient(user, context)
                }
            } catch (e: Exception) {
                Log_OC.e(TAG, e.message)
            }
            keyResult = KEY_FAILED
            return@withContext ""
        }

        if (privateKey.isEmpty()) {
            errorSavingKeys()
        } else {
            if (dialog == null) {
                Log_OC.e(TAG, "Dialog is null cannot proceed further.")
                return
            }
            requireDialog().dismiss()
            notifyResult()
        }
    }

    private fun generateMnemonicString(withWhitespace: Boolean): String {
        val stringBuilder = StringBuilder()

        keyWords?.let {
            for (string in it) {
                stringBuilder.append(string)
                if (withWhitespace) {
                    stringBuilder.append(' ')
                }
            }
        }

        return stringBuilder.toString()
    }

    @VisibleForTesting
    fun showMnemonicInfo() {
        if (dialog == null) {
            Log_OC.e(TAG, "Dialog is null cannot proceed further.")
            return
        }
        requireDialog().setTitle(R.string.end_to_end_encryption_passphrase_title)
        binding.encryptionStatus.setText(R.string.end_to_end_encryption_keywords_description)
        viewThemeUtils.material.colorTextInputLayout(binding.encryptionPasswordInputContainer)
        binding.encryptionPassphrase.text = generateMnemonicString(true)
        binding.encryptionPassphrase.visibility = View.VISIBLE

        positiveButton?.setText(R.string.end_to_end_encryption_confirm_button)
        positiveButton?.visibility = View.VISIBLE
        negativeButton?.visibility = View.VISIBLE

        positiveButton?.let { positiveButton ->
            negativeButton?.let { negativeButton ->
                viewThemeUtils.platform.colorTextButtons(positiveButton, negativeButton)
            }
        }

        keyResult = KEY_GENERATE
    }

    @VisibleForTesting
    fun errorSavingKeys() {
        if (dialog == null) {
            Log_OC.e(TAG, "Dialog is null cannot proceed further.")
            return
        }

        keyResult = KEY_FAILED
        requireDialog().setTitle(R.string.common_error)
        binding.encryptionStatus.setText(R.string.end_to_end_encryption_unsuccessful)
        binding.encryptionPassphrase.visibility = View.GONE

        positiveButton?.setText(R.string.end_to_end_encryption_dialog_close)
        positiveButton?.visibility = View.VISIBLE
        positiveButton?.let {
            viewThemeUtils.platform.colorTextButtons(it)
        }
    }

    @VisibleForTesting
    fun setMnemonic(keyWords: ArrayList<String>?) {
        this.keyWords = keyWords
    }

    companion object {
        const val SUCCESS = "SUCCESS"
        const val SETUP_ENCRYPTION_RESULT_CODE = 101
        const val SETUP_ENCRYPTION_REQUEST_CODE = 100
        const val SETUP_ENCRYPTION_DIALOG_TAG = "SETUP_ENCRYPTION_DIALOG_TAG"
        const val ARG_POSITION = "ARG_POSITION"
        const val RESULT_REQUEST_KEY = "RESULT_REQUEST"
        const val RESULT_KEY_CANCELLED = "IS_CANCELLED"
        private const val NUMBER_OF_WORDS = 12
        private const val ARG_USER = "ARG_USER"
        private val TAG = SetupEncryptionDialogFragment::class.java.simpleName
        private const val KEY_CREATED = "KEY_CREATED"
        private const val KEY_EXISTING_USED = "KEY_EXISTING_USED"
        private const val KEY_FAILED = "KEY_FAILED"
        private const val KEY_GENERATE = "KEY_GENERATE"

        /**
         * Public factory method to create new SetupEncryptionDialogFragment instance
         *
         * @return Dialog ready to show.
         */
        @JvmStatic
        fun newInstance(user: User?, position: Int): SetupEncryptionDialogFragment {
            val bundle = Bundle().apply {
                putParcelable(ARG_USER, user)
                putInt(ARG_POSITION, position)
            }

            return SetupEncryptionDialogFragment().apply {
                arguments = bundle
            }
        }
    }
}
