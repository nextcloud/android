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
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.AnalyticsUtils;

import java.util.Arrays;

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

    private Button mBCancel;
    private TextView mPassCodeHdr;
    private TextView mPassCodeHdrExplanation;
    private EditText mPassCodeEditText;

    private String mPassCode;
    private boolean mConfirmingPassCode = false;
    private static final String KEY_CONFIRMING_PASSCODE = "CONFIRMING_PASSCODE";

    private boolean mBChange = true; // to control that only one blocks jump

    private static final int mButtonIDList[] = {
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
    private int mEditPos;
    private boolean mSoftinputMode = true;
    private Animation mAnimation;

    /**
     * Initializes the activity.
     *
     * An intent with a valid ACTION is expected; if none is found, an
     * {@link IllegalArgumentException} will be thrown.
     *
     * @param savedInstanceState    Previously saved state - irrelevant in this case
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passcodelock);

        mAnimation = AnimationUtils.loadAnimation(this, R.anim.shake);
        mBCancel = (Button) findViewById(R.id.cancel);
        mPassCodeHdr = (TextView) findViewById(R.id.header);
        mPassCodeHdrExplanation = (TextView) findViewById(R.id.explanation);

        for(int i=0;i< mButtonIDList.length; i++) {
            Button b = (Button)findViewById(mButtonIDList[i]);
            b.setOnClickListener(new ButtonClicked(i));
        }
        
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
            if(mConfirmingPassCode){
                //the app was in the passcodeconfirmation
                requestPassCodeConfirmation();
            }else{
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

        setTextListeners();
        setListenerToRootView();

        if (false) {
            if (mSoftinputMode) {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            } else {
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            }
        }

        ((Button) findViewById(R.id.button_softkey)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSoftinputMode = mSoftinputMode == false ? true : false;
                if (mSoftinputMode) {
                    hideSoftKeyboard();
//                    InputMethodManager imm = (InputMethodManager)getSystemService(
//                        Context.INPUT_METHOD_SERVICE);
//                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    setSoftkeyVisibility(true);
                } else {
                    showSoftKeyboard();
//                    InputMethodManager imm = (InputMethodManager)getSystemService(
//                        Context.INPUT_METHOD_SERVICE);
//                    imm.showSoftInput(mPassCodeEditTexts[0], 0);
                    setSoftkeyVisibility(false);
                }
            }
        });
    }

    private void setSoftkeyVisibility(boolean visible) {
        if (visible) {
            for(int i=0;i< mButtonIDList.length; i++) {
                Button b = (Button)findViewById(mButtonIDList[i]);
                ObjectAnimator objectAnimator = ObjectAnimator.ofFloat( b, "alpha", 0f, 1f );
                objectAnimator.setDuration( 3000 );
                objectAnimator.start();
 //               objectAnimator.addListener();
//                b.setVisibility(View.VISIBLE);
                b.setClickable(true);
            }
        } else {
            for(int i=0;i< mButtonIDList.length; i++) {
                Button b = (Button)findViewById(mButtonIDList[i]);
                ObjectAnimator objectAnimator = ObjectAnimator.ofFloat( b, "alpha", 1f, 0f );
                objectAnimator.setDuration( 3000 );
                objectAnimator.start();
                //b.setVisibility(View.INVISIBLE);
                b.setClickable(false);
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
     * @param enabled       'True' makes the cancel button available, 'false' hides it.
     */
    protected void setCancelButtonEnabled(boolean enabled){
        if(enabled){
            mBCancel.setVisibility(View.VISIBLE);
            mBCancel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
//                    hideSoftKeyboard();
                    finish();
                }
            });
        } else {
            mBCancel.setVisibility(View.GONE);
            mBCancel.setVisibility(View.INVISIBLE);
            mBCancel.setOnClickListener(null);
        }
    }


    /**
     * Binds the appropiate listeners to the input boxes receiving each digit of the pass code.
     */
    protected void setTextListeners() {
        TextInputLayout til = (TextInputLayout) findViewById(R.id.passcode);
        EditText et = til.getEditText();
        et.setShowSoftInputOnFocus(false);      // TODO: API21  for disabling popup softkey when double clicked
        et.requestFocus();
        et.addTextChangedListener(new PassCodeDigitTextWatcher(false));
        mPassCodeEditText = et;
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
                /// pass code accepted in request, user is allowed to access the app
                hideSoftKeyboard();
                finish();

            }  else {
                final Handler handler = new Handler();
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        handler.post(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                mPassCodeEditText.startAnimation(mAnimation);

//                                clearPasscode();
                                showErrorAndRestart(R.string.pass_code_wrong, R.string.pass_code_enter_pass_code,
                                        View.INVISIBLE);

                            }
                        });

                    }
                }).start();
            }

        } else if (ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
            if (checkPassCode()) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(KEY_CHECK_RESULT, true);
                setResult(RESULT_OK, resultIntent);
                hideSoftKeyboard();
                finish();
            } else {
                showErrorAndRestart(R.string.pass_code_wrong, R.string.pass_code_enter_pass_code,
                        View.INVISIBLE);
            }

        } else if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction())) {
            /// enabling pass code
            if (!mConfirmingPassCode) {
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

    private void showErrorAndRestart(int errorMessage, int headerMessage,
                                     int explanationVisibility) {
        mPassCode = "";
        CharSequence errorSeq = getString(errorMessage);
        Toast.makeText(this, errorSeq, Toast.LENGTH_LONG).show();
        mPassCodeHdr.setText(headerMessage);                // TODO check if really needed
        mPassCodeHdrExplanation.setVisibility(explanationVisibility); // TODO check if really needed
        TextInputLayout til = (TextInputLayout) findViewById(R.id.passcode);
        til.setError(errorSeq);
//        clearBoxes();
    }


    /**
     * Ask to the user for retyping the pass code just entered before saving it as the current pass
     * code.
     */
    protected void requestPassCodeConfirmation(){
        clearBoxes();
        mPassCodeHdr.setText(R.string.pass_code_reenter_your_pass_code);
        mPassCodeHdrExplanation.setVisibility(View.INVISIBLE);
        mConfirmingPassCode = true;
    }

    /**
     * Compares pass code entered by the user with the value currently saved in the app.
     *
     * @return     'True' if entered pass code equals to the saved one.
     */
    protected boolean checkPassCode(){
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
     * @return     'True' if retyped pass code equals to the entered before.
     */
    protected boolean confirmPassCode(){
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
     * @param keyCode       Key code of the key that triggered the down event.
     * @param event         Event triggered.
     * @return              'True' when the key event was processed by this method.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction()) ||
                    ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
                finish();
            }   // else, do nothing, but report that the key was consumed to stay alive
            else {
//                goHome();		// TODO:
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

        private int mIndex;
        private boolean mLastOne = false;

        /**
         * Constructor
         *
         * @param index         Position in the pass code of the input field that will be bound to
         *                      this watcher.
         * @param lastOne       'True' means that watcher corresponds to the last position of the
         *                      pass code.
         */
        public PassCodeDigitTextWatcher(boolean lastOne) {
            mIndex = 0;
            mLastOne  = lastOne;
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
            String passcode = mPassCodeEditText.getText().toString();
            if (passcode.length() == 4) {
                mPassCode = passcode;
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
                if (false) {
                    String passCode = mPassCodeEditText.getText().toString();
                    passCode = passCode.substring(0, passCode.length() - 1);
                    mPassCodeEditText.setText(passCode);
                } else {
                    mPassCodeEditText.dispatchKeyEvent(
                        new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0));
                    mPassCodeEditText.dispatchKeyEvent(
                        new KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, 0));
                }
            }
        }
	}

    boolean isOpened = false;

    // http://www.it1me.com/it-answers?id=25216749&s=Template:TalbotCountyMD-NRHP-stub&ttl=SoftKeyboard+open+and+close+listener+in+an+activity+in+Android%3F
    // http://stackoverflow.com/questions/25216749/softkeyboard-open-and-close-listener-in-an-activity-in-android
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
//                    onHideKeyboard();
                    if (isOpened) {
//                        goHome();
                        mSoftinputMode = true;
                        setSoftkeyVisibility(true);
                    }
                    isOpened = false;
                    int a = 0;
                } else {
                    isOpened = true;
//                    onShowKeyboard(keyboardHeight);
                    int b = 0;
                }
            }
        });
    }
}
