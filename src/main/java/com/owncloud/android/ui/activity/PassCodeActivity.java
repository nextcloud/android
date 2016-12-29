/**
 *   ownCloud Android client application
 *
 *   @author Bartek Przybylski
 *   @author masensio
 *   @author David A. Velasco
 *   Copyright (C) 2011 Bartek Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
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

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.utils.AnalyticsUtils;

public class PassCodeActivity extends AppCompatActivity {

    private static final String TAG = PassCodeActivity.class.getSimpleName();

    private static final String SCREEN_NAME = "Passcode lock";

    public final static String ACTION_REQUEST_WITH_RESULT = "ACTION_REQUEST_WITH_RESULT";
    public final static String ACTION_CHECK_WITH_RESULT = "ACTION_CHECK_WITH_RESULT";
    public final static String ACTION_CHECK = "ACTION_CHECK";

    public final static String KEY_PASSCODE = "KEY_PASSCODE";
    public final static String KEY_CHECK_RESULT = "KEY_CHECK_RESULT";

    // NOTE: PREFERENCE_SET_PASSCODE must have the same value as preferences.xml-->android:key for passcode preference
    public final static String PREFERENCE_SET_PASSCODE = "set_pincode";

    public final static String PREFERENCE_PASSCODE_D = "PrefPinCode";
    public final static String PREFERENCE_PASSCODE_D1 = "PrefPinCode1";
    public final static String PREFERENCE_PASSCODE_D2 = "PrefPinCode2";
    public final static String PREFERENCE_PASSCODE_D3 = "PrefPinCode3";
    public final static String PREFERENCE_PASSCODE_D4 = "PrefPinCode4";

    private boolean ENABLE_GO_HOME = true;
    private boolean DEFAULT_SOFT_KEYBOARD_MODE = false;  // true=soft keyboard / false=buttons
    private int GUARD_TIME = 3000;

    private Button mBCancel;
    private TextView mPassCodeHdr;
    private TextView mPassCodeHdrExplanation;
    private EditText mPassCodeEditText;

    private String mPassCode;
    private boolean mConfirmingPassCode = false;
    private static final String KEY_CONFIRMING_PASSCODE = "CONFIRMING_PASSCODE";

    private static final int mButtonsIDList[] = {
            R.id.button0,
            R.id.button1,
            R.id.button2,
            R.id.button3,
            R.id.button4,
            R.id.button5,
            R.id.button6,
            R.id.button7,
            R.id.button8,
            R.id.button9,
            R.id.clear,
            R.id.back,
    };
    private boolean mSoftKeyboardMode;
    private boolean mIsSoftKeyboardOpend;

    /**
     * Initializes the activity.
     * <p>
     * An intent with a valid ACTION is expected; if none is found, an
     * {@link IllegalArgumentException} will be thrown.
     *
     * @param savedInstanceState Previously saved state - irrelevant in this case
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passcodelock);

        mBCancel = (Button) findViewById(R.id.cancel);
        mPassCodeHdr = (TextView) findViewById(R.id.header);
        mPassCodeHdrExplanation = (TextView) findViewById(R.id.explanation);

        if (ACTION_CHECK.equals(getIntent().getAction())) {
            /// this is a pass code request; the user has to input the right value
            mPassCodeHdr.setText(R.string.pass_code_enter_pass_code);
            mPassCodeHdrExplanation.setVisibility(View.INVISIBLE);
            setCancelButtonEnabled(false);      // no option to cancel

        } else if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction())) {
            if (savedInstanceState != null) {
                mConfirmingPassCode = savedInstanceState.getBoolean(PassCodeActivity.KEY_CONFIRMING_PASSCODE);
                mPassCode = savedInstanceState.getString(PassCodeActivity.KEY_PASSCODE);
            }
            if (mConfirmingPassCode) {
                //the app was in the passcodeconfirmation
                requestPassCodeConfirmation();
            } else {
                /// pass code preference has just been activated in Preferences;
                // will receive and confirm pass code value
                mPassCodeHdr.setText(R.string.pass_code_configure_your_pass_code);
                //mPassCodeHdr.setText(R.string.pass_code_enter_pass_code);
                // TODO choose a header, check iOS
                mPassCodeHdrExplanation.setVisibility(View.VISIBLE);
                setCancelButtonEnabled(true);
            }

        } else if (ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
            /// pass code preference has just been disabled in Preferences;
            // will confirm user knows pass code, then remove it
            mPassCodeHdr.setText(R.string.pass_code_remove_your_pass_code);
            mPassCodeHdrExplanation.setVisibility(View.INVISIBLE);
            setCancelButtonEnabled(true);

        } else {
            throw new IllegalArgumentException("A valid ACTION is needed in the Intent passed to "
                    + TAG);
        }

        setupPassCodeEditText();
        mSoftKeyboardMode = DEFAULT_SOFT_KEYBOARD_MODE;
        setupKeyboard();
        setListenerToRootView();

        if (mSoftKeyboardMode) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        } else {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    private void setupKeyboard() {
        if (mSoftKeyboardMode) {
            showSoftKeyboard();
            setButtonsVisibility(false);
        } else {
            hideSoftKeyboard();
            setButtonsVisibility(true);
        }
    }

    private void setButtonsVisibility(boolean visible) {
        if (visible) {
            for (int i = 0; i < mButtonsIDList.length; i++) {
                Button b = (Button) findViewById(mButtonsIDList[i]);
                ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(b, "alpha", 0f, 1f);
                objectAnimator.setDuration(300);
                objectAnimator.start();
                b.setClickable(true);
                b.setOnClickListener(new ButtonClicked(i));
                if (i == 11) {
                    b.setLongClickable(true);
                    b.setOnLongClickListener(new OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            mSoftKeyboardMode = !mSoftKeyboardMode;
                            setupKeyboard();
                            return true;
                        }
                    });
                }
            }
        } else {
            for (int i = 0; i < mButtonsIDList.length; i++) {
                Button b = (Button) findViewById(mButtonsIDList[i]);
                ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(b, "alpha", 1f, 0f);
                objectAnimator.setDuration(300);
                objectAnimator.start();
                b.setClickable(false);
                b.setOnClickListener(null);
                b.setLongClickable(false);
                b.setOnLongClickListener(null);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtils.setCurrentScreenName(this, SCREEN_NAME, TAG);
    }

    /**
     * Enables or disables the cancel button to allow the user interrupt the ACTION
     * requested to the activity.
     *
     * @param enabled 'True' makes the cancel button available, 'false' hides it.
     */
    protected void setCancelButtonEnabled(boolean enabled) {
        if (enabled) {
            mBCancel.setVisibility(View.VISIBLE);
            mBCancel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideSoftKeyboard();
                    finish();
                }
            });
        } else {
            mBCancel.setVisibility(View.GONE);
            mBCancel.setVisibility(View.INVISIBLE);
            mBCancel.setOnClickListener(null);
        }
    }

    protected void setupPassCodeEditText() {
        TextInputLayout til = (TextInputLayout) findViewById(R.id.passcode);
        EditText et = til.getEditText();
        if (Build.VERSION.SDK_INT >= 21) {
            et.setShowSoftInputOnFocus(false);  // for disabling popup soft keyboard when double clicked
        }
        et.requestFocus();
        et.addTextChangedListener(new PassCodeDigitTextWatcher());
        mPassCodeEditText = et;
    }

    /**
     * Processes the pass code entered by the user just after the last digit was in.
     * <p>
     * Takes into account the action requested to the activity, the currently saved pass code and
     * the previously typed pass code, if any.
     */
    private void processFullPassCode() {
        if (ACTION_CHECK.equals(getIntent().getAction())) {
            if (checkPassCode()) {
                /// pass code accepted in request, user is allowed to access the app
                hideSoftKeyboard();
                finish();

            } else {
                passCodeWrong();
            }

        } else if (ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
            if (checkPassCode()) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(KEY_CHECK_RESULT, true);
                setResult(RESULT_OK, resultIntent);
                hideSoftKeyboard();
                finish();
            } else {
                mPassCodeHdr.setText(R.string.pass_code_enter_pass_code);
                showErrorAndRestart(R.string.pass_code_wrong);
            }

        } else if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction())) {
            /// enabling pass code
            if (!mConfirmingPassCode) {
                requestPassCodeConfirmation();

            } else if (confirmPassCode()) {
                /// confirmed: user typed the same pass code twice
                savePassCodeAndExit();

            } else {
                mPassCodeHdr.setText(R.string.pass_code_configure_your_pass_code);
                showErrorAndRestart(R.string.pass_code_mismatch);
                mPassCodeHdrExplanation.setVisibility(View.VISIBLE);

            }
        }
    }

    private void passCodeWrong() {
        if (!mSoftKeyboardMode) {
            setButtonsVisibility(false);
        }
        Animation animation = AnimationUtils.loadAnimation(PassCodeActivity.this, R.anim.shake);
        mPassCodeEditText.startAnimation(animation);
        mPassCodeHdr.setText(R.string.pass_code_enter_pass_code);
        showErrorAndRestart(R.string.pass_code_wrong);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                TextInputLayout til = (TextInputLayout) findViewById(R.id.passcode);
                til.setError(null);
                if (!mSoftKeyboardMode) {
                    setButtonsVisibility(true);
                }
                clearBoxes();
            }
        }, GUARD_TIME);
    }

    private void hideSoftKeyboard() {
        View focusedView = getCurrentFocus();
        if (focusedView != null) {
            InputMethodManager inputMethodManager =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(
                    focusedView.getWindowToken(), 0);
        }
    }

    private void showSoftKeyboard() {
        View focusedView = getCurrentFocus();
        if (focusedView != null) {
            InputMethodManager inputMethodManager =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(
                    focusedView, 0);
        }
    }

    private void showErrorAndRestart(int errorMessage) {
        mPassCode = "";
        CharSequence errorSeq = getString(errorMessage);
        TextInputLayout til = (TextInputLayout) findViewById(R.id.passcode);
        til.setError(errorSeq);
    }

    /**
     * Ask to the user for retyping the pass code just entered before saving it as the current pass
     * code.
     */
    protected void requestPassCodeConfirmation() {
        clearBoxes();
        mPassCodeHdr.setText(R.string.pass_code_reenter_your_pass_code);
        mPassCodeHdrExplanation.setVisibility(View.INVISIBLE);
        mConfirmingPassCode = true;
    }

    /**
     * Compares pass code entered by the user with the value currently saved in the app.
     *
     * @return 'True' if entered pass code equals to the saved one.
     */
    protected boolean checkPassCode() {
        SharedPreferences appPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        String savedPassCode = "";
        savedPassCode += appPrefs.getString(PREFERENCE_PASSCODE_D1, null);
        savedPassCode += appPrefs.getString(PREFERENCE_PASSCODE_D2, null);
        savedPassCode += appPrefs.getString(PREFERENCE_PASSCODE_D3, null);
        savedPassCode += appPrefs.getString(PREFERENCE_PASSCODE_D4, null);

        boolean result = mPassCode.equals(savedPassCode);
        return result;
    }

    /**
     * Compares pass code retyped by the user in the input fields with the value entered just
     * before.
     *
     * @return 'True' if retyped pass code equals to the entered before.
     */
    protected boolean confirmPassCode() {
        mConfirmingPassCode = false;

        boolean result = mPassCode.equals(mPassCodeEditText.getText().toString());
        return result;
    }

    /**
     * Sets the input fields to empty strings and puts the focus on the first one.
     */
    protected void clearBoxes() {
        mPassCodeEditText.setText("");
    }

    /**
     * Overrides click on the BACK arrow to correctly cancel ACTION_ENABLE or ACTION_DISABLE, while
     * preventing than ACTION_CHECK may be worked around.
     *
     * @param keyCode Key code of the key that triggered the down event.
     * @param event   Event triggered.
     * @return 'True' when the key event was processed by this method.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction()) ||
                    ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
                finish();
            }   // else, do nothing, but report that the key was consumed to stay alive
            else {
                if (ENABLE_GO_HOME) {
                    goHome();
                }
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void goHome() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }

    /**
     * Saves the pass code input by the user as the current pass code.
     */
    protected void savePassCodeAndExit() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(KEY_PASSCODE, mPassCode);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PassCodeActivity.KEY_CONFIRMING_PASSCODE, mConfirmingPassCode);
        outState.putString(PassCodeActivity.KEY_PASSCODE, mPassCode);
    }

    private class PassCodeDigitTextWatcher implements TextWatcher {
        @Override
        public void afterTextChanged(Editable s) {
            String passCode = mPassCodeEditText.getText().toString();
            if (passCode.length() == 4) {
                mPassCode = passCode;
                processFullPassCode();
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

    private class ButtonClicked implements OnClickListener {

        private int mIndex;

        public ButtonClicked(int index) {
            mIndex = index;
        }

        public void onClick(View v) {
            if (mIndex <= 9) {
                // 0,1,2,...,8,9
                int key = KeyEvent.KEYCODE_0 + mIndex;
                mPassCodeEditText.dispatchKeyEvent(
                        new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, key, 0));
                mPassCodeEditText.dispatchKeyEvent(
                        new KeyEvent(0, 0, KeyEvent.ACTION_UP, key, 0));
            } else if (mIndex == 10) {
                // clear
                clearBoxes();
            } else {
                // delete
                mPassCodeEditText.dispatchKeyEvent(
                        new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0));
                mPassCodeEditText.dispatchKeyEvent(
                        new KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, 0));
            }
        }
    }

    public void setListenerToRootView() {
        View view = findViewById(android.R.id.content).getRootView();
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                // navigation bar height
                int navigationBarHeight = 0;
                int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    navigationBarHeight = getResources().getDimensionPixelSize(resourceId);
                }

                // status bar height
                int statusBarHeight = 0;
                resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    statusBarHeight = getResources().getDimensionPixelSize(resourceId);
                }

                // display window size for the app layout
                Rect rect = new Rect();
                getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);

                View view = findViewById(android.R.id.content).getRootView();
                int h = view.getHeight();
                // screen height - (user app height + status + nav) ..... if non-zero, then there is a soft keyboard
                int keyboardHeight = h - (statusBarHeight + navigationBarHeight + rect.height());

                if (keyboardHeight <= 0) {
                    if (mIsSoftKeyboardOpend) {
                        mSoftKeyboardMode = false;
                        setButtonsVisibility(true);
                    }
                    mIsSoftKeyboardOpend = false;
                } else {
                    mIsSoftKeyboardOpend = true;
                }
            }
        });
    }
}
