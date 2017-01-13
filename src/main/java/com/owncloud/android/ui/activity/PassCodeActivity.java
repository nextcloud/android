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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewAnimationUtils;
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
import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.content.Context.INPUT_METHOD_SERVICE;


class SoftKeyboardUtil {
    public interface SoftKeyboardListener {
        void onClose();
    }
    private static final String TAG = SoftKeyboardUtil.class.getSimpleName();

    private AppCompatActivity mActivity;
    private boolean mIsSoftKeyboardOpened;
    private SoftKeyboardListener mSoftKeyboardListener;
    private int mHideCount;

    private void setListenerToRootView() {
        View view = mActivity.findViewById(android.R.id.content).getRootView();
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // navigation bar height
                int navigationBarHeight = 0;
                int resourceId = mActivity.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    navigationBarHeight = mActivity.getResources().getDimensionPixelSize(resourceId);
                }

                // status bar height
                int statusBarHeight = 0;
                resourceId = mActivity.getResources().getIdentifier("status_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    statusBarHeight = mActivity.getResources().getDimensionPixelSize(resourceId);
                }

                // display window size for the app layout
                Rect rect = new Rect();
                mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
                int appHeight = rect.height();

                // screen height
                View view = mActivity.findViewById(android.R.id.content).getRootView();
                int screenHeight = view.getHeight();

                // soft keyboard height
                int softKeyboardHeight = screenHeight - (statusBarHeight + navigationBarHeight + appHeight);

                if (softKeyboardHeight <= 0) {
                    if (mIsSoftKeyboardOpened) {
                        // soft keyboard was closed
                        if (mHideCount == 0) {
                            // back key was pressed
                            mSoftKeyboardListener.onClose();
                        }
                        mHideCount = 0;
                    }
                    mIsSoftKeyboardOpened = false;
                } else {
                    mIsSoftKeyboardOpened = true;
                }
            }
        });
    }

    public SoftKeyboardUtil(AppCompatActivity activity, SoftKeyboardListener softKeyboardListener) {
        mActivity = activity;
        mSoftKeyboardListener = softKeyboardListener;
        if (softKeyboardListener != null) {
            setListenerToRootView();
        }
        mHideCount = 0;
    }

    public void initHidden() {
        mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public void initVisible() {
        mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    public void show() {
        mHideCount = 0;
        View focusedView = mActivity.getCurrentFocus();
        if (focusedView != null) {
            InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(focusedView, 0);
        } else {
            Log_OC.i(TAG, "focusedView = null in show()");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    show();
                }
            }, 10);
        }
    }
    public void hide() {
        mHideCount++;
        View focusedView = mActivity.getCurrentFocus();
        if (focusedView != null) {
            InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        } else {
            Log_OC.i(TAG, "focusedView = null in hide()");
        }
    }
}

public class PassCodeActivity extends AppCompatActivity implements SoftKeyboardUtil.SoftKeyboardListener {

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

    private static final boolean ENABLE_GO_HOME = false;
    private static final boolean INIT_SOFT_KEYBOARD_MODE = false;  // true=soft keyboard / false=buttons
    private static final boolean ENABLE_SWITCH_SOFT_KEYBOARD = true;
    private static final int GUARD_TIME = 3000;    // (ms)
    private static final boolean ENABLE_SUFFLE_BUTTONS = false;

    private TextView mPassCodeHdr;
    private TextView mPassCodeHdrExplanation;
    private EditText mPassCodeEditText;

    private String mConfirmingPassCode;
    private boolean mConfirmingPassCodeFlag = false;
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
    private static final String mButtonsMainStr[] = {
            "0",
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8",
            "9",
            "Clear",
            "Back"
    };
    private static String mButtonsSubStr[] = {
            "",
            "",
            "abc",
            "def",
            "ghi",
            "jkl",
            "mno",
            "pqrs",
            "tuv",
            "wxyz",
            "",
            "softkey..."
    };
    private Integer[] mButtonsIDListShuffle = new Integer[12];
    private AppCompatButton[] mButtonsList = new AppCompatButton[12];
    private int mButtonVisibilityPrev = 0;       // 0=unknown/1=visible/2=invisible
    private boolean mSoftKeyboardMode;
    private boolean mShowButtonsWhenSoftKeyboardClose = true;
    private SoftKeyboardUtil mSoftKeyboard;
    
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

        mPassCodeHdr = (TextView) findViewById(R.id.header);
        mPassCodeHdrExplanation = (TextView) findViewById(R.id.explanation);
        setupButtons();

        if (ACTION_CHECK.equals(getIntent().getAction())) {
            /// this is a pass code request; the user has to input the right value
            mPassCodeHdr.setText(R.string.pass_code_enter_pass_code);
            mPassCodeHdrExplanation.setVisibility(View.INVISIBLE);
            setCancelButtonEnabled(false);      // no option to cancel

        } else if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction())) {
            if (savedInstanceState != null) {
                mConfirmingPassCodeFlag = savedInstanceState.getBoolean(PassCodeActivity.KEY_CONFIRMING_PASSCODE);
                mConfirmingPassCode = savedInstanceState.getString(PassCodeActivity.KEY_PASSCODE);
            }
            if (!mConfirmingPassCodeFlag) {
                /// pass code preference has just been activated in Preferences;
                // will receive and confirm pass code value
                mPassCodeHdr.setText(R.string.pass_code_configure_your_pass_code);
                mPassCodeHdrExplanation.setVisibility(View.VISIBLE);

                View view = findViewById(android.R.id.content);
                Snackbar
                        .make(view, getString(R.string.pass_code_configure_your_pass_code_explanation),
                                Snackbar.LENGTH_LONG)
                        .setAction("Cancel", new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra(KEY_CHECK_RESULT, true);
                                setResult(RESULT_CANCELED, resultIntent);
                                hideSoftKeyboard();
                                finish();
                            }
                        })
                        .show();
            } else {
                //the app was in the passcodeconfirmation
                requestPassCodeConfirmation();
            }
            setCancelButtonEnabled(true);

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

        if (!ENABLE_SWITCH_SOFT_KEYBOARD) {
            mButtonsSubStr[11] = "";
        }
        setupPassCodeEditText();
        mSoftKeyboardMode = INIT_SOFT_KEYBOARD_MODE;
        mSoftKeyboard = new SoftKeyboardUtil(this, this);
        setupKeyboard();
        if (mSoftKeyboardMode) {
            mSoftKeyboard.initVisible();
        } else {
            mSoftKeyboard.initHidden();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        clearPassCodeEditText();
    }

    private void setupKeyboard() {
        mShowButtonsWhenSoftKeyboardClose = true;
        if (mSoftKeyboardMode) {
            showSoftKeyboard();
            setButtonsVisibility(false);
        } else {
            hideSoftKeyboard();
            setButtonsVisibility(true);
        }
    }

    private void setupButtons() {
        for (int i = 0; i < mButtonsIDList.length; i++) {
            mButtonsIDListShuffle[i] = i;
        }
        for (int i = 0; i < mButtonsIDList.length; i++) {
            mButtonsList[i] = (AppCompatButton) findViewById(mButtonsIDList[i]);
            mButtonsList[i].setAllCaps(false);
        }
    }

    private void animeCircle(View myView, boolean visible) {
        if (visible) {
            int cx = (myView.getLeft() + myView.getRight()) / 2;
            int cy = (myView.getTop() + myView.getBottom()) / 2;
            int finalRadius = Math.max(myView.getWidth(), myView.getHeight());
            Animator anim =
                ViewAnimationUtils.createCircularReveal(myView, cx, cy, 0, finalRadius);
            anim.start();
        } else {
            int cx = (myView.getLeft() + myView.getRight()) / 2;
            int cy = (myView.getTop() + myView.getBottom()) / 2;
            int initialRadius = myView.getWidth();
            Animator anim =
                ViewAnimationUtils.createCircularReveal(myView, cx, cy, initialRadius, 0);
            anim.start();
        }
    }

    private void animeAlpha(View view, boolean visible) {
        if (visible) {
            ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
            objectAnimator.setDuration(500);
            objectAnimator.start();
        } else {
            ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
            objectAnimator.setDuration(500);
            objectAnimator.start();
        }
    }

    private void buttonAnime(Button b, boolean visible) {
        animeAlpha(b, visible);
//        animeCircle(b, visible);
    }
    
    private void setButtonsVisibility(boolean visible) {
        if (visible && mButtonVisibilityPrev != 1) {
            mButtonVisibilityPrev = 1;
            if (ENABLE_SUFFLE_BUTTONS) {
                List<Integer> list = Arrays.asList(mButtonsIDListShuffle);
                Collections.shuffle(list);
                mButtonsIDListShuffle = (Integer[]) list.toArray(new Integer[list.size()]);
            }
            for (int i = 0; i < mButtonsList.length; i++) {
                Button b = mButtonsList[i];
                int j = ENABLE_SUFFLE_BUTTONS ? mButtonsIDListShuffle[i] : i;
                buttonAnime(b, visible);
                b.setClickable(true);
                String s = String.format("<big>%s</big><br/><font color=\"grey\"><small>%s</small></font>",
                                         mButtonsMainStr[j], mButtonsSubStr[j]);
                b.setText(Html.fromHtml(s));
                b.setOnClickListener(new ButtonClicked(mPassCodeEditText, j));
                if (j == 11 && ENABLE_SWITCH_SOFT_KEYBOARD) {
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
        } else if (!visible && mButtonVisibilityPrev != 2) {
            mButtonVisibilityPrev = 2;
            for (int i = 0; i < mButtonsList.length; i++) {
                Button b = mButtonsList[i];
                buttonAnime(b, visible);
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
        Button cancel = (Button) findViewById(R.id.cancel);
        if (enabled) {
            cancel.setVisibility(View.VISIBLE);
            cancel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        } else {
            cancel.setVisibility(View.GONE);
            cancel.setOnClickListener(null);
        }
    }

    protected void setupPassCodeEditText() {
        TextInputLayout til = (TextInputLayout) findViewById(R.id.passcode);
        EditText et = til.getEditText();
        et.setTextIsSelectable(false);     // TODO:no effect for double tap?
        //et.setContextClickable(false);
        if (Build.VERSION.SDK_INT >= 21) {
            et.setShowSoftInputOnFocus(false);  // for disabling popup soft keyboard when double clicked
        }
        et.requestFocus();
        et.addTextChangedListener(new TextWatcher(){
            @Override
            public void afterTextChanged(Editable s) {
                String passCode = mPassCodeEditText.getText().toString();
                if (passCode.length() == 4) {
                    processFullPassCode(passCode);
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
        });
        mPassCodeEditText = et;
    }

    /**
     * Processes the pass code entered by the user just after the last digit was in.
     * <p>
     * Takes into account the action requested to the activity, the currently saved pass code and
     * the previously typed pass code, if any.
     */
    private void processFullPassCode(String passCode) {
        if (ACTION_CHECK.equals(getIntent().getAction())) {
            if (checkPassCode(passCode)) {
                /// pass code accepted in request, user is allowed to access the app
                finish();

            } else {
                mPassCodeHdr.setText(R.string.pass_code_enter_pass_code);
                showErrorMessage(R.string.pass_code_wrong);
                startGuard();
            }

        } else if (ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
            if (checkPassCode(passCode)) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(KEY_CHECK_RESULT, true);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                mPassCodeHdr.setText(R.string.pass_code_enter_pass_code);
                showErrorMessage(R.string.pass_code_wrong);
                startGuard();
            }

        } else if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction())) {
            /// enabling pass code
            if (!mConfirmingPassCodeFlag) {
                requestPassCodeConfirmation();
                mConfirmingPassCodeFlag = true;
                mConfirmingPassCode = passCode;

            } else if (mConfirmingPassCode.equals(passCode)) {
                /// confirmed: user typed the same pass code twice
                Intent resultIntent = new Intent();
                resultIntent.putExtra(KEY_PASSCODE, mConfirmingPassCode);
                setResult(RESULT_OK, resultIntent);
                finish();

            } else {
                mPassCodeHdr.setText(R.string.pass_code_configure_your_pass_code);
                showErrorMessage(R.string.pass_code_mismatch);
                startGuard();
            }
        }
    }

    private void showErrorMessage(int errorMessage) {
        CharSequence errorSeq = getString(errorMessage);
        TextInputLayout til = (TextInputLayout) findViewById(R.id.passcode);
        til.setError(errorSeq);
    }

    private void eraseErrorMessage() {
        TextInputLayout til = (TextInputLayout) findViewById(R.id.passcode);
        til.setError(null);
    }

    private void startGuard() {
        if (!mSoftKeyboardMode) {
            setButtonsVisibility(false);
        } else {
            hideSoftKeyboard();
        }
//        mPassCodeEditText.setFocusable(false);
        mPassCodeEditText.setClickable(false);
        Animation animation = AnimationUtils.loadAnimation(PassCodeActivity.this, R.anim.shake);
        mPassCodeEditText.startAnimation(animation);
        TextInputLayout til = (TextInputLayout) findViewById(R.id.passcode);
        til.setPasswordVisibilityToggleEnabled(false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                endGuard();
            }
        }, GUARD_TIME);
    }

    private void endGuard() {
//        mPassCodeEditText.setFocusable(true);     // TODO:
        mPassCodeEditText.setClickable(true);
        mPassCodeEditText.requestFocus();
        eraseErrorMessage();
        TextInputLayout til = (TextInputLayout) findViewById(R.id.passcode);
        til.setPasswordVisibilityToggleEnabled(true);
        if (ACTION_CHECK.equals(getIntent().getAction())) {
            setupKeyboard();
            clearPassCodeEditText();
        } else if (ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(KEY_CHECK_RESULT, true);
            setResult(RESULT_CANCELED, resultIntent);
            finish();
        } else if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction())) {
            setupKeyboard();
            clearPassCodeEditText();
            mPassCodeHdrExplanation.setVisibility(View.VISIBLE);
            mConfirmingPassCodeFlag = false;
        }
    }
            
    private void hideSoftKeyboard() {
        mShowButtonsWhenSoftKeyboardClose = false;
        mSoftKeyboard.hide();
    }

    private void showSoftKeyboard() {
        mSoftKeyboard.show();
    }

    /**
     * Ask to the user for retyping the pass code just entered before saving it as the current pass
     * code.
     */
    protected void requestPassCodeConfirmation() {
        clearPassCodeEditText();
        mPassCodeHdr.setText(R.string.pass_code_reenter_your_pass_code);
        mPassCodeHdrExplanation.setVisibility(View.INVISIBLE);
    }

    /**
     * Compares pass code entered by the user with the value currently saved in the app.
     *
     * @return 'True' if entered pass code equals to the saved one.
     */
    protected boolean checkPassCode(String passCode) {
        SharedPreferences appPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        String savedPassCode = "";
        savedPassCode += appPrefs.getString(PREFERENCE_PASSCODE_D1, null);
        savedPassCode += appPrefs.getString(PREFERENCE_PASSCODE_D2, null);
        savedPassCode += appPrefs.getString(PREFERENCE_PASSCODE_D3, null);
        savedPassCode += appPrefs.getString(PREFERENCE_PASSCODE_D4, null);

        return passCode.equals(savedPassCode);
    }

    /**
     * Sets the input fields to empty strings and puts the focus on the first one.
     */
    protected void clearPassCodeEditText() {
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
            } else {
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PassCodeActivity.KEY_CONFIRMING_PASSCODE, mConfirmingPassCodeFlag);
        outState.putString(PassCodeActivity.KEY_PASSCODE, mConfirmingPassCode);
    }

    private class ButtonClicked implements OnClickListener {

        private int mIndex;
        private EditText mEditText;

        public ButtonClicked(EditText editText, int index) {
            mEditText = editText;
            mIndex = index;
        }

        public void onClick(View v) {
            if (mIndex <= 9) {
                // 0,1,2,...,8,9
                int key = KeyEvent.KEYCODE_0 + mIndex;
                mEditText.dispatchKeyEvent(
                        new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, key, 0));
                mEditText.dispatchKeyEvent(
                        new KeyEvent(0, 0, KeyEvent.ACTION_UP, key, 0));
            } else if (mIndex == 10) {
                // clear
                mEditText.setText("");
            } else {
                // delete
                mEditText.dispatchKeyEvent(
                        new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0));
                mEditText.dispatchKeyEvent(
                        new KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, 0));
            }
        }
    }

    // when softKeyboard close
    @Override
    public void onClose()
    {
        if (ENABLE_SWITCH_SOFT_KEYBOARD) {
            if (mShowButtonsWhenSoftKeyboardClose) {
                mSoftKeyboardMode = false;
                setButtonsVisibility(true);
            }
        } else {
            if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction()) ||
                    ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
                // same as cancel button
                finish();
            } else {
                if (ENABLE_GO_HOME) {
                    goHome();
                } else {
                    showErrorMessage(R.string.pass_code_enter_pass_code);
                    startGuard();
                }
            }
        }
    }
}
