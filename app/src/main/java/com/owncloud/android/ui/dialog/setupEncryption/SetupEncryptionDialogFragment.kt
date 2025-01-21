/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog.setupEncryption

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
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
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
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
    private var task: DownloadKeysAsyncTask? = null
    private var keyResult: String? = null
    private var keyWords: ArrayList<String>? = null

    private lateinit var binding: SetupEncryptionDialogBinding

    override fun onStart() {
        super.onStart()

        setupAlertDialog()
        executeTask()
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

    private fun executeTask() {
        task = DownloadKeysAsyncTask(requireContext())
        task?.execute()
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

    @Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown")
    private fun decryptPrivateKey(dialog: DialogInterface) {
        Log_OC.d(TAG, "Decrypt private key")
        binding.encryptionStatus.setText(R.string.end_to_end_encryption_decrypting)

        try {
            val privateKey = task?.get()
            val mnemonicUnchanged = binding.encryptionPasswordInput.text.toString().trim()
            val mnemonic =
                binding.encryptionPasswordInput.text.toString().replace("\\s".toRegex(), "")
                    .lowercase()
            val decryptedPrivateKey = EncryptionUtils.decryptPrivateKey(
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

        val newKeysTask = GenerateNewKeysAsyncTask(requireContext())
        newKeysTask.execute()
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

    @SuppressLint("StaticFieldLeak")
    inner class DownloadKeysAsyncTask(context: Context) : AsyncTask<Void?, Void?, String?>() {
        private val mWeakContext: WeakReference<Context> = WeakReference(context)

        @Suppress("ReturnCount", "LongMethod")
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Void?): String? {
            // fetch private/public key
            // if available
            //  - store public key
            //  - decrypt private key, store unencrypted private key in database

            val context = mWeakContext.get() ?: return null
            val certificateOperation = GetPublicKeyRemoteOperation()
            val serverPublicKeyOperation = GetServerPublicKeyRemoteOperation()
            val user = user ?: return null

            val privateKeyOperation = GetPrivateKeyRemoteOperation()
            val privateKeyResult = privateKeyOperation.executeNextcloudClient(user, context)
            val certificateResult = certificateOperation.executeNextcloudClient(user, context)
            val serverPublicKeyResult = serverPublicKeyOperation.executeNextcloudClient(user, context)

            var encryptedPrivateKey: com.owncloud.android.lib.ocs.responses.PrivateKey? = null
            if (privateKeyResult.isSuccess) {
                encryptedPrivateKey = privateKeyResult.resultData
            }

            if (!certificateResult.isSuccess || !serverPublicKeyResult.isSuccess) {
                Log_OC.d(TAG, "certificate or server public key not fetched")
                return null
            }

            val serverKey = serverPublicKeyResult.resultData
            val certificateAsString = certificateResult.resultData
            val isCertificateValid = certificateValidator?.validate(serverKey, certificateAsString)

            if (isCertificateValid == false) {
                Log_OC.d(TAG, "Could not save certificate, certificate is not valid")
                return null
            }

            if (arbitraryDataProvider == null) {
                return null
            }

            arbitraryDataProvider?.storeOrUpdateKeyValue(
                user.accountName,
                EncryptionUtils.PUBLIC_KEY,
                certificateAsString
            )

            if (privateKeyResult.isSuccess) {
                Log_OC.d(TAG, "private key successful downloaded for " + user.accountName)
                keyResult = KEY_EXISTING_USED
                return encryptedPrivateKey?.getKey()
            }

            return null
        }

        @Deprecated("Deprecated in Java")
        override fun onPreExecute() {
            super.onPreExecute()

            binding.encryptionStatus.setText(R.string.end_to_end_encryption_retrieving_keys)
            positiveButton?.visibility = View.INVISIBLE
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(privateKey: String?) {
            super.onPostExecute(privateKey)

            val context = mWeakContext.get()
            if (context == null) {
                Log_OC.e(TAG, "Context lost after fetching private keys.")
                return
            }

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
                binding.encryptionStatus.setText(R.string.end_to_end_encryption_enter_password)
                binding.encryptionPasswordInputContainer.visibility = View.VISIBLE
                positiveButton?.visibility = View.VISIBLE
            } else {
                Log_OC.e(TAG, "Got empty private key string")
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class GenerateNewKeysAsyncTask(context: Context) : AsyncTask<Void?, Void?, String>() {
        private val mWeakContext: WeakReference<Context> = WeakReference(context)

        @Deprecated("Deprecated in Java")
        override fun onPreExecute() {
            super.onPreExecute()
            binding.encryptionStatus.setText(R.string.end_to_end_encryption_generating_keys)
        }

        @Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown", "ReturnCount", "LongMethod")
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg voids: Void?): String {
            //  - create CSR, push to server, store returned public key in database
            //  - encrypt private key, push key to server, store unencrypted private key in database
            try {
                val context = mWeakContext.get()
                val publicKeyString: String

                if (context == null) {
                    keyResult = KEY_FAILED
                    return ""
                }

                // Create public/private key pair
                val keyPair = EncryptionUtils.generateKeyPair()

                // create CSR
                val accountManager = AccountManager.get(context)
                val user = user ?: return ""

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
                    return ""
                }

                val privateKey = keyPair.private
                val privateKeyString = EncryptionUtils.encodeBytesToBase64String(privateKey.encoded)
                val privatePemKeyString = EncryptionUtils.privateKeyToPEM(privateKey)
                val encryptedPrivateKey = EncryptionUtils.encryptPrivateKey(
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

                    return storePrivateKeyResult.resultData
                } else {
                    val deletePublicKeyOperation = DeletePublicKeyRemoteOperation()
                    deletePublicKeyOperation.executeNextcloudClient(user, context)
                }
            } catch (e: Exception) {
                Log_OC.e(TAG, e.message)
            }
            keyResult = KEY_FAILED
            return ""
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(s: String) {
            super.onPostExecute(s)
            val context = mWeakContext.get()
            if (context == null) {
                Log_OC.e(TAG, "Context lost after generating new private keys.")
                return
            }
            if (s.isEmpty()) {
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
