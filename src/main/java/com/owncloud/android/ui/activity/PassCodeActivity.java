/*
 *   ownCloud Android client application
 *
 *   @author Bartek Przybylski
 *   @author masensio
 *   @author David A. Velasco
 *   Copyright (C) 2011 Bartek Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
 *   Copyright (C) 2020 Kwon Yuna <yunaghgh@naver.com>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.owncloud.android.R;
import com.owncloud.android.databinding.PasscodelockBinding;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.theme.ThemeButtonUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeTextInputUtils;

import java.util.Arrays;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

public class PassCodeActivity extends AppCompatActivity implements Injectable {

    private static final String TAG = PassCodeActivity.class.getSimpleName();
    private static final String KEY_PASSCODE_DIGITS = "PASSCODE_DIGITS";
    private static final String KEY_CONFIRMING_PASSCODE = "CONFIRMING_PASSCODE";

    public final static String ACTION_REQUEST_WITH_RESULT = "ACTION_REQUEST_WITH_RESULT";
    public final static String ACTION_CHECK_WITH_RESULT = "ACTION_CHECK_WITH_RESULT";
    public final static String ACTION_CHECK = "ACTION_CHECK";
    public final static String KEY_PASSCODE  = "KEY_PASSCODE";
    public final static String KEY_CHECK_RESULT = "KEY_CHECK_RESULT";

    public final static String PREFERENCE_PASSCODE_D = "PrefPinCode";
    public final static String PREFERENCE_PASSCODE_D1 = "PrefPinCode1";
    public final static String PREFERENCE_PASSCODE_D2 = "PrefPinCode2";
    public final static String PREFERENCE_PASSCODE_D3 = "PrefPinCode3";
    public final static String PREFERENCE_PASSCODE_D4 = "PrefPinCode4";

    @Inject AppPreferences preferences;
    private PasscodelockBinding binding;
    private final EditText[] passCodeEditTexts = new EditText[4];
    private String [] passCodeDigits = {"","","",""};
    private boolean confirmingPassCode;
    private boolean changed = true; // to control that only one blocks jump

    /**
     * Initializes the activity.
     *
     * An intent with a valid ACTION is expected; if none is found, an
     * {@link IllegalArgumentException} will be thrown.
     *
     * @param savedInstanceState    Previously saved state - irrelevant in this case
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PasscodelockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int elementColor = ThemeColorUtils.primaryColor(this, true);

        ThemeButtonUtils.themeBorderlessButton(ThemeColorUtils.primaryColor(this, true), binding.cancel);

        passCodeEditTexts[0] = binding.txt0;
        ThemeTextInputUtils.colorEditText(passCodeEditTexts[0], elementColor);
        ThemeTextInputUtils.themeEditText(this, passCodeEditTexts[0], false);
        passCodeEditTexts[0].requestFocus();

        passCodeEditTexts[1] = binding.txt1;
        ThemeTextInputUtils.colorEditText(passCodeEditTexts[1], elementColor);
        ThemeTextInputUtils.themeEditText(this, passCodeEditTexts[1], false);

        passCodeEditTexts[2] = binding.txt2;
        ThemeTextInputUtils.colorEditText(passCodeEditTexts[2], elementColor);
        ThemeTextInputUtils.themeEditText(this, passCodeEditTexts[2], false);

        passCodeEditTexts[3] = binding.txt3;
        ThemeTextInputUtils.colorEditText(passCodeEditTexts[3], elementColor);
        ThemeTextInputUtils.themeEditText(this, passCodeEditTexts[3], false);

        Window window = getWindow();
        if (window != null) {
            window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        if (ACTION_CHECK.equals(getIntent().getAction())) {
            /// this is a pass code request; the user has to input the right value
            binding.header.setText(R.string.pass_code_enter_pass_code);
            binding.explanation.setVisibility(View.INVISIBLE);
            setCancelButtonEnabled(false);      // no option to cancel

            showDelay();

        } else if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction())) {
            if (savedInstanceState != null) {
                confirmingPassCode = savedInstanceState.getBoolean(PassCodeActivity.KEY_CONFIRMING_PASSCODE);
                passCodeDigits = savedInstanceState.getStringArray(PassCodeActivity.KEY_PASSCODE_DIGITS);
            }
            if(confirmingPassCode){
                // the app was in the passcodeconfirmation
                requestPassCodeConfirmation();
            }else{
                // pass code preference has just been activated in SettingsActivity;
                // will receive and confirm pass code value
                binding.header.setText(R.string.pass_code_configure_your_pass_code);

                binding.explanation.setVisibility(View.VISIBLE);
                setCancelButtonEnabled(true);
            }

        } else if (ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
            // pass code preference has just been disabled in SettingsActivity;
            // will confirm user knows pass code, then remove it
            binding.header.setText(R.string.pass_code_remove_your_pass_code);
            binding.explanation.setVisibility(View.INVISIBLE);
            setCancelButtonEnabled(true);

        } else {
            throw new IllegalArgumentException("A valid ACTION is needed in the Intent passed to " + TAG);
        }

        setTextListeners();
    }

    /**
     * Enables or disables the cancel button to allow the user interrupt the ACTION
     * requested to the activity.
     *
     * @param enabled       'True' makes the cancel button available, 'false' hides it.
     */
    protected void setCancelButtonEnabled(boolean enabled){
        if(enabled){
            binding.cancel.setVisibility(View.VISIBLE);
            binding.cancel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        } else {
            binding.cancel.setVisibility(View.INVISIBLE);
            binding.cancel.setOnClickListener(null);
        }
    }

    @VisibleForTesting
    public PasscodelockBinding getBinding() {
        return binding;
    }

    /**
     * Binds the appropriate listeners to the input boxes receiving each digit of the pass code.
     */
    protected void setTextListeners() {
        passCodeEditTexts[0].addTextChangedListener(new PassCodeDigitTextWatcher(0, false));
        passCodeEditTexts[1].addTextChangedListener(new PassCodeDigitTextWatcher(1, false));
        passCodeEditTexts[2].addTextChangedListener(new PassCodeDigitTextWatcher(2, false));
        passCodeEditTexts[3].addTextChangedListener(new PassCodeDigitTextWatcher(3, true));

        setOnKeyListener(1);
        setOnKeyListener(2);
        setOnKeyListener(3);

        passCodeEditTexts[1].setOnFocusChangeListener((v, hasFocus) -> onPassCodeEditTextFocusChange(1));

        passCodeEditTexts[2].setOnFocusChangeListener((v, hasFocus) -> onPassCodeEditTextFocusChange(2));

        passCodeEditTexts[3].setOnFocusChangeListener((v, hasFocus) -> onPassCodeEditTextFocusChange(3));
    }

    private void onPassCodeEditTextFocusChange(final int passCodeIndex) {
        for (int i = 0; i < passCodeIndex; i++) {
            if (TextUtils.isEmpty(passCodeEditTexts[i].getText())) {
                passCodeEditTexts[i].requestFocus();
                break;
            }
        }
    }

    private void setOnKeyListener(final int passCodeIndex) {
        passCodeEditTexts[passCodeIndex].setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && changed) {
                passCodeEditTexts[passCodeIndex - 1].requestFocus();
                if (!confirmingPassCode) {
                    passCodeDigits[passCodeIndex - 1] = "";
                }
                passCodeEditTexts[passCodeIndex - 1].setText("");
                changed = false;

            } else if (!changed) {
                changed = true;
            }
            return false;
        });
    }

    /**
     * Processes the pass code entered by the user just after the last digit was in.
     *
     * Takes into account the action requested to the activity, the currently saved pass code and
     * the previously typed pass code, if any.
     */
    private void processFullPassCode() {
        if (ACTION_CHECK.equals(getIntent().getAction())) {
            if (checkPassCode()) {
                preferences.resetPinWrongAttempts();

                /// pass code accepted in request, user is allowed to access the app
                AppPreferencesImpl.fromContext(this).setLockTimestamp(SystemClock.elapsedRealtime());
                hideSoftKeyboard();
                finish();

            }  else {
                preferences.increasePinWrongAttempts();

                showErrorAndRestart(R.string.pass_code_wrong, R.string.pass_code_enter_pass_code, View.INVISIBLE);
            }

        } else if (ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
            if (checkPassCode()) {
                preferences.setLockTimestamp(SystemClock.elapsedRealtime());
                Intent resultIntent = new Intent();
                resultIntent.putExtra(KEY_CHECK_RESULT, true);
                setResult(RESULT_OK, resultIntent);
                hideSoftKeyboard();
                finish();
            } else {
                showErrorAndRestart(R.string.pass_code_wrong, R.string.pass_code_enter_pass_code, View.INVISIBLE);
            }

        } else if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction())) {
            /// enabling pass code
            if (!confirmingPassCode) {
                requestPassCodeConfirmation();

            } else if (confirmPassCode()) {
                /// confirmed: user typed the same pass code twice
                savePassCodeAndExit();

            } else {
                showErrorAndRestart(
                        R.string.pass_code_mismatch, R.string.pass_code_configure_your_pass_code, View.VISIBLE
                );
            }
        }
    }

    private void hideSoftKeyboard() {
        View focusedView = getCurrentFocus();
        if (focusedView != null) {
            InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(
                focusedView.getWindowToken(),
                0
            );
        }
    }

    private void showErrorAndRestart(int errorMessage, int headerMessage,
                                     int explanationVisibility) {
        Arrays.fill(passCodeDigits, null);
        Snackbar.make(findViewById(android.R.id.content), getString(errorMessage), Snackbar.LENGTH_LONG).show();
        binding.header.setText(headerMessage);                          // TODO check if really needed
        binding.explanation.setVisibility(explanationVisibility); // TODO check if really needed
        clearBoxes();

        showDelay();
    }


    /**
     * Ask to the user for retyping the pass code just entered before saving it as the current pass
     * code.
     */
    protected void requestPassCodeConfirmation(){
        clearBoxes();
        binding.header.setText(R.string.pass_code_reenter_your_pass_code);
        binding.explanation.setVisibility(View.INVISIBLE);
        confirmingPassCode = true;
    }

    /**
     * Compares pass code entered by the user with the value currently saved in the app.
     *
     * @return     'True' if entered pass code equals to the saved one.
     */
    protected boolean checkPassCode() {


        String[] savedPassCodeDigits = preferences.getPassCode();

        boolean result = true;
        for (int i = 0; i < passCodeDigits.length && result; i++) {
            result = passCodeDigits[i] != null && passCodeDigits[i].equals(savedPassCodeDigits[i]);
        }
        return result;
    }

    /**
     * Compares pass code retyped by the user in the input fields with the value entered just
     * before.
     *
     * @return     'True' if retyped pass code equals to the entered before.
     */
    protected boolean confirmPassCode(){
        confirmingPassCode = false;

        boolean result = true;
        for (int i = 0; i < passCodeEditTexts.length && result; i++) {
            result = passCodeEditTexts[i].getText().toString().equals(passCodeDigits[i]);
        }
        return result;
    }

    /**
     * Sets the input fields to empty strings and puts the focus on the first one.
     */
    protected void clearBoxes(){
        for (EditText mPassCodeEditText : passCodeEditTexts) {
            mPassCodeEditText.setText("");
        }
        passCodeEditTexts[0].requestFocus();
    }

    /**
     * Overrides click on the BACK arrow to correctly cancel ACTION_ENABLE or ACTION_DISABLE, while
     * preventing than ACTION_CHECK may be worked around.
     *
     * @param keyCode       Key code of the key that triggered the down event.
     * @param event         Event triggered.
     * @return              'True' when the key event was processed by this method.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (ACTION_CHECK.equals(getIntent().getAction())) {
                moveTaskToBack(true);
                finishAndRemoveTask();
            } else if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction()) ||
                ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
                finish();
            }// else, do nothing, but report that the key was consumed to stay alive
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Saves the pass code input by the user as the current pass code.
     */
    protected void savePassCodeAndExit() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(KEY_PASSCODE,
                              passCodeDigits[0] + passCodeDigits[1] + passCodeDigits[2] + passCodeDigits[3]);

        setResult(RESULT_OK, resultIntent);

        finish();
    }

    private void showDelay() {
        int delay = preferences.pinBruteForceDelay();

        if (delay > 0) {
            binding.explanation.setText(R.string.brute_force_delay);
            binding.explanation.setVisibility(View.VISIBLE);
            binding.txt0.setEnabled(false);
            binding.txt1.setEnabled(false);
            binding.txt2.setEnabled(false);
            binding.txt3.setEnabled(false);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(delay * 1000);

                        runOnUiThread(() -> {
                            binding.explanation.setVisibility(View.INVISIBLE);
                            binding.txt0.setEnabled(true);
                            binding.txt1.setEnabled(true);
                            binding.txt2.setEnabled(true);
                            binding.txt3.setEnabled(true);
                        });
                    } catch (InterruptedException e) {
                        Log_OC.e(this, "Could not delay password input prompt");
                    }
                }
            }).start();
        }
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PassCodeActivity.KEY_CONFIRMING_PASSCODE, confirmingPassCode);
        outState.putStringArray(PassCodeActivity.KEY_PASSCODE_DIGITS, passCodeDigits);
    }

    private class PassCodeDigitTextWatcher implements TextWatcher {

        private int mIndex = -1;
        private boolean mLastOne;

        /**
         * Constructor
         *
         * @param index         Position in the pass code of the input field that will be bound to
         *                      this watcher.
         * @param lastOne       'True' means that watcher corresponds to the last position of the
         *                      pass code.
         */
        PassCodeDigitTextWatcher(int index, boolean lastOne) {
            mIndex = index;
            mLastOne  = lastOne;
            if (mIndex < 0) {
                throw new IllegalArgumentException(
                        "Invalid index in " + PassCodeDigitTextWatcher.class.getSimpleName() +
                                " constructor"
                );
            }
        }

        private int next() {
            return mLastOne ? 0 : mIndex + 1;
        }

        /**
         * Performs several actions when the user types a digit in an input field:
         *  - saves the input digit to the state of the activity; this will allow retyping the
         *    pass code to confirm it.
         *  - moves the focus automatically to the next field
         *  - for the last field, triggers the processing of the full pass code
         *
         * @param s     Changed text
         */
        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() > 0) {
                if (!confirmingPassCode) {
                    passCodeDigits[mIndex] = passCodeEditTexts[mIndex].getText().toString();
                }
                passCodeEditTexts[next()].requestFocus();

                if (mLastOne) {
                    processFullPassCode();
                }

            } else {
                Log_OC.d(TAG, "Text box " + mIndex + " was cleaned");
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // nothing to do
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // nothing to do
        }
    }
}
