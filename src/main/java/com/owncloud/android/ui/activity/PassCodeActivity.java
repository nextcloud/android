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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
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

    public final static String KEY_PASSCODE  = "KEY_PASSCODE";
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
    private EditText[] mPassCodeEditTexts = new EditText[4];

    private String [] mPassCodeDigits = {"","","",""};
    private static final String KEY_PASSCODE_DIGITS = "PASSCODE_DIGITS";
    private boolean mConfirmingPassCode = false;
    private static final String KEY_CONFIRMING_PASSCODE = "CONFIRMING_PASSCODE";

    private boolean mBChange = true; // to control that only one blocks jump

    private int mButtonIDList[] = {
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
        
        mBCancel = (Button) findViewById(R.id.cancel);
        mPassCodeHdr = (TextView) findViewById(R.id.header);
        mPassCodeHdrExplanation = (TextView) findViewById(R.id.explanation);
        mPassCodeEditTexts[0] = (EditText) findViewById(R.id.txt0);
        mPassCodeEditTexts[0].setTextIsSelectable(false);
//        getWindow().setSoftInputMode(
//                android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mPassCodeEditTexts[1] = (EditText) findViewById(R.id.txt1);
        mPassCodeEditTexts[1].setTextIsSelectable(false);
        mPassCodeEditTexts[2] = (EditText) findViewById(R.id.txt2);
        mPassCodeEditTexts[2].setTextIsSelectable(false);
        mPassCodeEditTexts[3] = (EditText) findViewById(R.id.txt3);
        mPassCodeEditTexts[3].setTextIsSelectable(false);

        mPassCodeEditTexts[0].requestFocus();
        mEditPos = 0;

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
                mPassCodeDigits = savedInstanceState.getStringArray(PassCodeActivity.KEY_PASSCODE_DIGITS);
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

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setTextListeners();
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
    
         ///  First input field
        mPassCodeEditTexts[0].addTextChangedListener(new PassCodeDigitTextWatcher(0, false));
        mPassCodeEditTexts[0].setOnFocusChangeListener(new FocusChecker(0));


        /*------------------------------------------------
         *  SECOND BOX 
         -------------------------------------------------*/
        mPassCodeEditTexts[1].addTextChangedListener(new PassCodeDigitTextWatcher(1, false));

        mPassCodeEditTexts[1].setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL && mBChange) {  // TODO WIP: event should be
                // used to control what's exactly happening with DEL, not any custom field...
                    mPassCodeEditTexts[0].setText("");
                    mPassCodeEditTexts[0].requestFocus();
                    if (!mConfirmingPassCode) {
                        mPassCodeDigits[0] = "";
                    }
                    mBChange = false;

                } else if (!mBChange) {
                    mBChange = true;
                }
                return false;
            }
        });
        mPassCodeEditTexts[1].setOnFocusChangeListener(new FocusChecker(1));

        
        
        /*------------------------------------------------
         *  THIRD BOX
         -------------------------------------------------*/
        mPassCodeEditTexts[2].addTextChangedListener(new PassCodeDigitTextWatcher(2, false));

        mPassCodeEditTexts[2].setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL && mBChange) {
                    mPassCodeEditTexts[1].requestFocus();
                    if (!mConfirmingPassCode) {
                        mPassCodeDigits[1] = "";
                    }
                    mPassCodeEditTexts[1].setText("");
                    mBChange = false;

                } else if (!mBChange) {
                    mBChange = true;

                }
                return false;
            }
        });

        mPassCodeEditTexts[2].setOnFocusChangeListener(new FocusChecker(2));


        /*------------------------------------------------
         *  FOURTH BOX
         -------------------------------------------------*/
        mPassCodeEditTexts[3].addTextChangedListener(new PassCodeDigitTextWatcher(3, true));

        mPassCodeEditTexts[3].setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL && mBChange) {
                    mPassCodeEditTexts[2].requestFocus();
                    if (!mConfirmingPassCode) {
                        mPassCodeDigits[2] = "";
                    }
                    mPassCodeEditTexts[2].setText("");
                    mBChange = false;

                } else if (!mBChange) {
                    mBChange = true;
                }
                return false;
            }
        });

        mPassCodeEditTexts[3].setOnFocusChangeListener(new FocusChecker(3));
    } // end setTextListener


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
                showErrorAndRestart(R.string.pass_code_wrong, R.string.pass_code_enter_pass_code,
                        View.INVISIBLE);
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

    private void showErrorAndRestart(int errorMessage, int headerMessage,
                                     int explanationVisibility) {
        Arrays.fill(mPassCodeDigits, null);
        CharSequence errorSeq = getString(errorMessage);
        Toast.makeText(this, errorSeq, Toast.LENGTH_LONG).show();
        mPassCodeHdr.setText(headerMessage);                // TODO check if really needed
        mPassCodeHdrExplanation.setVisibility(explanationVisibility); // TODO check if really needed
        clearBoxes();
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

        String savedPassCodeDigits[] = new String[4];
        savedPassCodeDigits[0] = appPrefs.getString(PREFERENCE_PASSCODE_D1, null);
        savedPassCodeDigits[1] = appPrefs.getString(PREFERENCE_PASSCODE_D2, null);
        savedPassCodeDigits[2] = appPrefs.getString(PREFERENCE_PASSCODE_D3, null);
        savedPassCodeDigits[3] = appPrefs.getString(PREFERENCE_PASSCODE_D4, null);

        boolean result = true;
        for (int i = 0; i < mPassCodeDigits.length && result; i++) {
            result = (mPassCodeDigits[i] != null) &&
                    mPassCodeDigits[i].equals(savedPassCodeDigits[i]);
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
        mConfirmingPassCode = false;

        boolean result = true;
        for (int i = 0; i < mPassCodeEditTexts.length && result; i++) {
            result = ((mPassCodeEditTexts[i].getText().toString()).equals(mPassCodeDigits[i]));
        }
        return result;
    }

    /**
     * Sets the input fields to empty strings and puts the focus on the first one.
     */
    protected void clearBoxes(){
        for (int i=0; i < mPassCodeEditTexts.length; i++) {
            mPassCodeEditTexts[i].setText("");
        }
        mPassCodeEditTexts[0].requestFocus();
        mEditPos = 0;
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
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount()== 0){
            if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction()) ||
                    ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
                finish();
            }   // else, do nothing, but report that the key was consumed to stay alive
            else
            {
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
        resultIntent.putExtra(KEY_PASSCODE,
                mPassCodeDigits[0] + mPassCodeDigits[1] + mPassCodeDigits[2] + mPassCodeDigits[3]);

        setResult(RESULT_OK, resultIntent);

        finish();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PassCodeActivity.KEY_CONFIRMING_PASSCODE, mConfirmingPassCode);
        outState.putStringArray(PassCodeActivity.KEY_PASSCODE_DIGITS, mPassCodeDigits);
    }

    private class PassCodeDigitTextWatcher implements TextWatcher {

        private int mIndex = -1;
        private boolean mLastOne = false;

        /**
         * Constructor
         *
         * @param index         Position in the pass code of the input field that will be bound to
         *                      this watcher.
         * @param lastOne       'True' means that watcher corresponds to the last position of the
         *                      pass code.
         */
        public PassCodeDigitTextWatcher(int index, boolean lastOne) {
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
                if (!mConfirmingPassCode) {
                    mPassCodeDigits[mIndex] = mPassCodeEditTexts[mIndex].getText().toString();
                }
                mPassCodeEditTexts[next()].requestFocus();

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

	private class FocusChecker implements View.OnFocusChangeListener {

        private int mIndex;

        public FocusChecker(int index) {
            mIndex = index;
        }
            
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                int i;
                mEditPos = mIndex;
                for (i=0;i<mIndex;i++) {
                    if (mPassCodeEditTexts[i].getText().toString().equals("")) {
                        mPassCodeEditTexts[i].requestFocus();
                        break;
                    }
                }
                for(int j=i;j<4;j++) {
                    mPassCodeEditTexts[j].setText("");
                }
            }
        }
    }

    private class ButtonClicked implements OnClickListener {

        private int mIndex;

        public ButtonClicked(int index) {
            mIndex = index;
        }

		public void onClick(View v) {

			Button button = (Button) v;

            if (mIndex < 9) {
                String s;
                s = String.format("%d", mIndex);
                mPassCodeEditTexts[mEditPos].setText(s);
                mEditPos++;
            } else if (mIndex == 10) {
                // clear
                clearBoxes();
            } else {
                // delete
                if (mEditPos > 0) {
                    for (int i=mEditPos ; i < 4 ; i++) {
                        mPassCodeEditTexts[i].setText("");
                    }
                    mEditPos--;
                    mPassCodeEditTexts[mEditPos].requestFocus();
                }
            }

            {
                String s;
                s = String.format("key %d", mIndex);
                Toast toast = Toast.makeText(PassCodeActivity.this, s, Toast.LENGTH_SHORT);
                toast.show();
            }

        }
	}
}
