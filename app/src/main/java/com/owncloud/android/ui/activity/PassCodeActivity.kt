/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2011 Bartek Przybylski
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.authentication.PassCodeManager
import com.owncloud.android.databinding.PasscodelockBinding
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.components.PassCodeEditText
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.util.Arrays
import javax.inject.Inject

@Suppress("TooManyFunctions", "MagicNumber")
class PassCodeActivity : AppCompatActivity(), Injectable {

    companion object {
        private val TAG = PassCodeActivity::class.java.simpleName

        private const val KEY_PASSCODE_DIGITS = "PASSCODE_DIGITS"
        private const val KEY_CONFIRMING_PASSCODE = "CONFIRMING_PASSCODE"
        const val ACTION_REQUEST_WITH_RESULT = "ACTION_REQUEST_WITH_RESULT"
        const val ACTION_CHECK_WITH_RESULT = "ACTION_CHECK_WITH_RESULT"
        const val ACTION_CHECK = "ACTION_CHECK"
        const val KEY_PASSCODE = "KEY_PASSCODE"
        const val KEY_CHECK_RESULT = "KEY_CHECK_RESULT"
        const val PREFERENCE_PASSCODE_D = "PrefPinCode"
        const val PREFERENCE_PASSCODE_D1 = "PrefPinCode1"
        const val PREFERENCE_PASSCODE_D2 = "PrefPinCode2"
        const val PREFERENCE_PASSCODE_D3 = "PrefPinCode3"
        const val PREFERENCE_PASSCODE_D4 = "PrefPinCode4"
    }

    @JvmField
    @Inject
    var preferences: AppPreferences? = null

    @JvmField
    @Inject
    var passCodeManager: PassCodeManager? = null

    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    @get:VisibleForTesting
    lateinit var binding: PasscodelockBinding
        private set

    private val passCodeEditTexts = arrayOfNulls<PassCodeEditText>(4)
    private var passCodeDigits: Array<String?>? = arrayOf("", "", "", "")
    private var confirmingPassCode = false
    private var changed = true // to control that only one blocks jump

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PasscodelockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyTint()
        setupPasscodeEditTexts()
        setSoftInputMode()
        setupUI(savedInstanceState)
        setTextListeners()
    }

    private fun applyTint() {
        viewThemeUtils?.platform?.colorViewBackground(binding.cardViewContent, ColorRole.SURFACE_VARIANT)
        viewThemeUtils?.material?.colorMaterialButtonPrimaryBorderless(binding.cancel)
    }

    private fun setupPasscodeEditTexts() {
        passCodeEditTexts[0] = binding.txt0
        passCodeEditTexts[1] = binding.txt1
        passCodeEditTexts[2] = binding.txt2
        passCodeEditTexts[3] = binding.txt3

        passCodeEditTexts.forEach {
            it?.let { viewThemeUtils?.platform?.colorEditText(it) }
        }

        passCodeEditTexts[0]?.requestFocus()
    }

    private fun setSoftInputMode() {
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    private fun setupUI(savedInstanceState: Bundle?) {
        if (ACTION_CHECK == intent.action) {
            // / this is a pass code request; the user has to input the right value
            binding.header.setText(R.string.pass_code_enter_pass_code)
            binding.explanation.visibility = View.INVISIBLE
            setCancelButtonEnabled(false) // no option to cancel
            showDelay()
        } else if (ACTION_REQUEST_WITH_RESULT == intent.action) {
            if (savedInstanceState != null) {
                confirmingPassCode = savedInstanceState.getBoolean(KEY_CONFIRMING_PASSCODE)
                passCodeDigits = savedInstanceState.getStringArray(KEY_PASSCODE_DIGITS)
            }
            if (confirmingPassCode) {
                // the app was in the passcode confirmation
                requestPassCodeConfirmation()
            } else {
                // pass code preference has just been activated in SettingsActivity;
                // will receive and confirm pass code value
                binding.header.setText(R.string.pass_code_configure_your_pass_code)
                binding.explanation.visibility = View.VISIBLE
            }
            setCancelButtonEnabled(true)
        } else if (ACTION_CHECK_WITH_RESULT == intent.action) {
            // pass code preference has just been disabled in SettingsActivity;
            // will confirm user knows pass code, then remove it
            binding.header.setText(R.string.pass_code_remove_your_pass_code)
            binding.explanation.visibility = View.INVISIBLE
            setCancelButtonEnabled(true)
        } else {
            throw IllegalArgumentException("A valid ACTION is needed in the Intent passed to $TAG")
        }
    }

    private fun setCancelButtonEnabled(enabled: Boolean) {
        binding.cancel.visibility = if (enabled) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
        binding.cancel.setOnClickListener {
            if (enabled) {
                finish()
            }
        }
    }

    private fun setTextListeners() {
        for (i in passCodeEditTexts.indices) {
            val editText = passCodeEditTexts[i]
            val isLast = (i == 3)

            editText?.addTextChangedListener(PassCodeDigitTextWatcher(i, isLast))

            if (i > 0) {
                setOnKeyListener(i)
            }

            editText?.onFocusChangeListener = View.OnFocusChangeListener { _: View?, _: Boolean ->
                onPassCodeEditTextFocusChange(i)
            }
        }
    }

    private fun onPassCodeEditTextFocusChange(passCodeIndex: Int) {
        for (i in 0 until passCodeIndex) {
            if (TextUtils.isEmpty(passCodeEditTexts[i]?.text)) {
                passCodeEditTexts[i]?.requestFocus()
                break
            }
        }
    }

    private fun setOnKeyListener(passCodeIndex: Int) {
        passCodeEditTexts[passCodeIndex]?.setOnKeyListener { _: View?, keyCode: Int, _: KeyEvent? ->
            if (keyCode == KeyEvent.KEYCODE_DEL && changed) {
                passCodeEditTexts[passCodeIndex - 1]?.requestFocus()

                if (!confirmingPassCode) {
                    passCodeDigits?.set(passCodeIndex - 1, "")
                }

                passCodeEditTexts[passCodeIndex - 1]?.setText(R.string.empty)

                changed = false
            } else if (!changed) {
                changed = true
            }
            false
        }
    }

    /**
     * Processes the pass code entered by the user just after the last digit was in.
     *
     *
     * Takes into account the action requested to the activity, the currently saved pass code and the previously typed
     * pass code, if any.
     */
    private fun processFullPassCode() {
        if (ACTION_CHECK == intent.action) {
            if (checkPassCode()) {
                preferences?.resetPinWrongAttempts()

                // / pass code accepted in request, user is allowed to access the app
                passCodeManager?.updateLockTimestamp()
                hideSoftKeyboard()
                finish()
            } else {
                preferences?.increasePinWrongAttempts()
                showErrorAndRestart(R.string.pass_code_wrong, R.string.pass_code_enter_pass_code, View.INVISIBLE)
            }
        } else if (ACTION_CHECK_WITH_RESULT == intent.action) {
            if (checkPassCode()) {
                passCodeManager?.updateLockTimestamp()

                val resultIntent = Intent()
                resultIntent.putExtra(KEY_CHECK_RESULT, true)
                setResult(RESULT_OK, resultIntent)
                hideSoftKeyboard()
                finish()
            } else {
                showErrorAndRestart(R.string.pass_code_wrong, R.string.pass_code_enter_pass_code, View.INVISIBLE)
            }
        } else if (ACTION_REQUEST_WITH_RESULT == intent.action) {
            // / enabling pass code
            if (!confirmingPassCode) {
                requestPassCodeConfirmation()
            } else if (confirmPassCode()) {
                // / confirmed: user typed the same pass code twice
                savePassCodeAndExit()
            } else {
                showErrorAndRestart(
                    R.string.pass_code_mismatch,
                    R.string.pass_code_configure_your_pass_code,
                    View.VISIBLE
                )
            }
        }
    }

    private fun hideSoftKeyboard() {
        currentFocus?.let {
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(
                it.windowToken,
                0
            )
        }
    }

    private fun showErrorAndRestart(errorMessage: Int, headerMessage: Int, explanationVisibility: Int) {
        passCodeDigits?.let { Arrays.fill(it, null) }

        Snackbar.make(findViewById(android.R.id.content), getString(errorMessage), Snackbar.LENGTH_LONG).show()
        binding.header.setText(headerMessage) // TODO check if really needed
        binding.explanation.visibility = explanationVisibility // TODO check if really needed
        clearBoxes()
        showDelay()
    }

    /**
     * Ask to the user for retyping the pass code just entered before saving it as the current pass code.
     */
    private fun requestPassCodeConfirmation() {
        clearBoxes()
        binding.header.setText(R.string.pass_code_reenter_your_pass_code)
        binding.explanation.visibility = View.INVISIBLE
        confirmingPassCode = true
    }

    private fun checkPassCode(): Boolean {
        val savedPassCodeDigits = preferences?.passCode
        return passCodeDigits?.zip(savedPassCodeDigits.orEmpty()) { input, saved ->
            input != null && input == saved
        }?.all { it } ?: false
    }

    private fun confirmPassCode(): Boolean {
        return passCodeEditTexts.indices.all { i ->
            passCodeEditTexts[i]?.text.toString() == passCodeDigits!![i]
        }
    }

    private fun clearBoxes() {
        passCodeEditTexts.forEach { it?.text?.clear() }
        passCodeEditTexts.firstOrNull()?.requestFocus()
    }

    /**
     * Overrides click on the BACK arrow to correctly cancel ACTION_ENABLE or ACTION_DISABLE, while preventing than
     * ACTION_CHECK may be worked around.
     *
     * @param keyCode Key code of the key that triggered the down event.
     * @param event   Event triggered.
     * @return 'True' when the key event was processed by this method.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0) {
            if (ACTION_CHECK == intent.action) {
                moveTaskToBack(true)
                finishAndRemoveTask()
            } else if (ACTION_REQUEST_WITH_RESULT == intent.action || ACTION_CHECK_WITH_RESULT == intent.action) {
                finish()
            } // else, do nothing, but report that the key was consumed to stay alive
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun savePassCodeAndExit() {
        val resultIntent = Intent()
        resultIntent.putExtra(
            KEY_PASSCODE,
            passCodeDigits!![0] + passCodeDigits!![1] + passCodeDigits!![2] + passCodeDigits!![3]
        )
        setResult(RESULT_OK, resultIntent)
        passCodeManager?.updateLockTimestamp()
        finish()
    }

    private fun showDelay() {
        val delay = preferences?.pinBruteForceDelay() ?: 0

        if (delay <= 0) {
            return
        }

        binding.explanation.setText(R.string.brute_force_delay)
        binding.explanation.visibility = View.VISIBLE
        binding.txt0.isEnabled = false
        binding.txt1.isEnabled = false
        binding.txt2.isEnabled = false
        binding.txt3.isEnabled = false

        Thread(object : Runnable {
            override fun run() {
                try {
                    Thread.sleep(delay * 1000L)

                    runOnUiThread {
                        binding.explanation.visibility = View.INVISIBLE
                        binding.txt0.isEnabled = true
                        binding.txt1.isEnabled = true
                        binding.txt2.isEnabled = true
                        binding.txt3.isEnabled = true
                    }
                } catch (e: InterruptedException) {
                    Log_OC.e(this, "Could not delay password input prompt")
                }
            }
        }).start()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_CONFIRMING_PASSCODE, confirmingPassCode)
        outState.putStringArray(KEY_PASSCODE_DIGITS, passCodeDigits)
    }

    private inner class PassCodeDigitTextWatcher(index: Int, lastOne: Boolean) : TextWatcher {
        private var mIndex = -1
        private val mLastOne: Boolean

        init {
            mIndex = index
            mLastOne = lastOne

            require(mIndex >= 0) {
                "Invalid index in " + PassCodeDigitTextWatcher::class.java.simpleName +
                    " constructor"
            }
        }

        private operator fun next(): Int {
            return if (mLastOne) 0 else mIndex + 1
        }

        /**
         * Performs several actions when the user types a digit in an input field: - saves the input digit to the state
         * of the activity; this will allow retyping the pass code to confirm it. - moves the focus automatically to the
         * next field - for the last field, triggers the processing of the full pass code
         *
         * @param s Changed text
         */
        override fun afterTextChanged(s: Editable) {
            if (s.isNotEmpty()) {
                if (!confirmingPassCode) {
                    val passCodeText = passCodeEditTexts[mIndex]?.text

                    if (passCodeText != null) {
                        passCodeDigits!![mIndex] = passCodeText.toString()
                    }
                }

                if (mLastOne) {
                    processFullPassCode()
                } else {
                    passCodeEditTexts[next()]?.requestFocus()
                }
            } else {
                Log_OC.d(TAG, "Text box $mIndex was cleaned")
            }
        }

        @Suppress("EmptyFunctionBlock")
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }

        @Suppress("EmptyFunctionBlock")
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        }
    }
}
