/**
 *   Nextcloud Android client application
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
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.content.Context.INPUT_METHOD_SERVICE;



class PassFieldLinearLayout extends LinearLayout implements View.OnFocusChangeListener  {

    public PassFieldLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize() {
        setFocusableInTouchMode(true);
        setFocusable(true);
        setOnFocusChangeListener(this);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (hasFocus) {
            imm.showSoftInput(v, 0);
        } else {
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    private class PassThroughInputConnection extends BaseInputConnection {

        PassThroughInputConnection(View targetView, boolean fullEditor) {
            super(targetView, fullEditor);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
            sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            return true;
        }
    }
    
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        PassThroughInputConnection ic = new PassThroughInputConnection(this, false);
        outAttrs.inputType = InputType.TYPE_CLASS_NUMBER;
        outAttrs.imeOptions = EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_FULLSCREEN;
        return ic;
    }
}

class SoftKeyboardUtil {
    private static final String TAG = SoftKeyboardUtil.class.getSimpleName();

    private AppCompatActivity mActivity;
    private boolean mIsSoftKeyboardOpened;
    private SoftKeyboardListener mSoftKeyboardListener;
    private int mHideCount;
    private View mView;

    interface SoftKeyboardListener {
        void onClose();
    }

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

    public SoftKeyboardUtil(AppCompatActivity activity, View view, SoftKeyboardListener softKeyboardListener) {
        mActivity = activity;
        mView = view;
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
        View focusedView = mView;
        if (focusedView == null) {
            focusedView = mActivity.getCurrentFocus();
        }
        if (focusedView != null) {
            InputMethodManager imm = (InputMethodManager)mActivity.getSystemService(INPUT_METHOD_SERVICE);
            String s = focusedView.getClass().getName();
            if (s.indexOf("EditText") != -1) {
                imm.showSoftInput(focusedView, 0);
            } else {
                imm.toggleSoftInput(0, 0);
            }
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
            InputMethodManager imm = (InputMethodManager)mActivity.getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        } else {
            Log_OC.i(TAG, "focusedView = null in hide()");
        }
    }

    public void appClose() {
        mHideCount = 0;
        mIsSoftKeyboardOpened = false;
    }
}

public class PassCodeActivity extends AppCompatActivity implements SoftKeyboardUtil.SoftKeyboardListener {

    private static final String TAG = PassCodeActivity.class.getSimpleName();

    private static final String SCREEN_NAME = "Passcode lock";

    public final static String ACTION_REQUEST_WITH_RESULT = "ACTION_REQUEST_WITH_RESULT";
    public final static String ACTION_CHECK_WITH_RESULT = "ACTION_CHECK_WITH_RESULT";
    public final static String ACTION_CHECK = "ACTION_CHECK";

    // NOTE: PREFERENCE_SET_PASSCODE must have the same value as preferences.xml-->android:key for passcode preference
    public final static String PREFERENCE_SET_PASSCODE = "set_pincode";

    public final static String PREFERENCE_PASSCODE_D = "PrefPinCode";
    public final static String PREFERENCE_PASSCODE_D1 = "PrefPinCode1";
    public final static String PREFERENCE_PASSCODE_D2 = "PrefPinCode2";
    public final static String PREFERENCE_PASSCODE_D3 = "PrefPinCode3";
    public final static String PREFERENCE_PASSCODE_D4 = "PrefPinCode4";

    public final static String KEY_PASSCODE = "KEY_PASSCODE";
    public final static String KEY_CHECK_RESULT = "KEY_CHECK_RESULT";

    private static final String AUTO_PREF__SOFT_KEYBOARD_MODE = "prefs_soft_keyboard_mode";

    // Preference
    private static final boolean INIT_SOFT_KEYBOARD_MODE = true;  // true=soft keyboard / false=buttons
    private static final boolean ENABLE_GO_HOME = false;
    private static final boolean ENABLE_SWITCH_SOFT_KEYBOARD = true;
    private static final int GUARD_TIME = 5*1000;    // (ms)
    private static final boolean ENABLE_SUFFLE_BUTTONS = false;

    private static final String KEY_CONFIRMING_PASSCODE = "CONFIRMING_PASSCODE";

    private String mConfirmingPassCode;
    private boolean mConfirmingPassCodeFlag = false;

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
    private static final int passFields[] = {
            R.id.passField1,
            R.id.passField2,
            R.id.passField3,
            R.id.passField4,
    };
    private Integer[] mButtonsIDListShuffle = new Integer[12];
    private AppCompatButton[] mButtonsList = new AppCompatButton[12];
    enum ButtonVisibility {
        Startup,
        Visible,
        Invisible
    }
    private boolean mSoftKeyboardMode;          // true=soft(virtual) keyboard / false=buttons
    private ButtonVisibility mButtonVisibility;
    private boolean mShowButtonsWhenSoftKeyboardClose = true;
    private SoftKeyboardUtil mSoftKeyboard;
    private SharedPreferences mPref;
    private String mPassCodeStr;
    private Snackbar mSnackbar;
    private Handler mHandler = new Handler();
    private TextView mPassCodeHdr;


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

        mPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mPassCodeHdr = (TextView)findViewById(R.id.header);
        setupButtons();
        mSoftKeyboardMode = mPref.getBoolean(AUTO_PREF__SOFT_KEYBOARD_MODE, INIT_SOFT_KEYBOARD_MODE);
        PassFieldLinearLayout ll = (PassFieldLinearLayout)findViewById(R.id.PasscodeLinearLayout);
        ll.initialize();
        mSoftKeyboard = new SoftKeyboardUtil(this, ll, this);

        int explanationId = 0;
        if (ACTION_CHECK.equals(getIntent().getAction())) {
            /// this is a pass code request; the user has to input the right value
            setCancelButtonEnabled(false);      // no option to cancel

        } else if (ACTION_REQUEST_WITH_RESULT.equals(getIntent().getAction())) {
            if (savedInstanceState != null) {
                mConfirmingPassCodeFlag = savedInstanceState.getBoolean(PassCodeActivity.KEY_CONFIRMING_PASSCODE);
                mConfirmingPassCode = savedInstanceState.getString(PassCodeActivity.KEY_PASSCODE);
            }
            if (!mConfirmingPassCodeFlag) {
                /// pass code preference has just been activated in Preferences;
                // will receive and confirm pass code value
                explanationId = R.string.pass_code_configure_your_pass_code_explanation;

            } else {
                //the app was in the passcodeconfirmation
                requestPassCodeConfirmation();
            }
            setCancelButtonEnabled(true);

        } else if (ACTION_CHECK_WITH_RESULT.equals(getIntent().getAction())) {
            /// pass code preference has just been disabled in Preferences;
            // will confirm user knows pass code, then remove it
            explanationId = R.string.pass_code_remove_your_pass_code;
            setCancelButtonEnabled(true);

        } else {
            throw new IllegalArgumentException("A valid ACTION is needed in the Intent passed to " + TAG);
        }

        mButtonVisibility = ButtonVisibility.Visible;
        if (explanationId != 0) {
            mButtonVisibility = ButtonVisibility.Startup;
            setButtonsVisibility(false);
            View view = findViewById(android.R.id.content);
            mSnackbar = Snackbar.make(view, explanationId, Snackbar.LENGTH_LONG);
            mSnackbar
                .setAction(R.string.common_ok, new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // nothing to do
                    }
                })
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        setupKeyboard();
                    }
                })
                .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        clearPassCodeEditText();
        if (mSnackbar == null || !mSnackbar.isShown()) {
            mButtonVisibility = ButtonVisibility.Startup;
            setupKeyboard();
            if (mSoftKeyboardMode) {
                mSoftKeyboard.initVisible();
            } else {
                mSoftKeyboard.initHidden();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSoftKeyboard.appClose();
        if (mSnackbar != null && mSnackbar.isShown()) {
            mSnackbar.setCallback(null);
        }
        mHandler.removeCallbacksAndMessages(null);
        SharedPreferences.Editor editor = mPref.edit();
        editor.putBoolean(AUTO_PREF__SOFT_KEYBOARD_MODE, mSoftKeyboardMode);
        editor.apply();
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
            mButtonsList[i] = (AppCompatButton)findViewById(mButtonsIDList[i]);
            mButtonsList[i].setAllCaps(false);
        }
    }

    private void buttonAnimation(View view, boolean visible, int duration) {
        ObjectAnimator objectAnimator;
        if (visible) {
            objectAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        } else {
            objectAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        }
        objectAnimator.setDuration(duration);
        objectAnimator.start();
    }

    private void shuffleButtonsIDList() {
        if (ENABLE_SUFFLE_BUTTONS) {
            List<Integer> list = Arrays.asList(mButtonsIDListShuffle);
            Collections.shuffle(list);
            mButtonsIDListShuffle = list.toArray(new Integer[list.size()]);
        }
    }

    private void showButtons() {
        int duration = mButtonVisibility == ButtonVisibility.Startup ? 0 : 500;
        shuffleButtonsIDList();
        for (int i = 0; i < mButtonsList.length; i++) {
            AppCompatButton b = mButtonsList[i];
            buttonAnimation(b, true, duration);
            b.setClickable(true);
            int j = ENABLE_SUFFLE_BUTTONS ? mButtonsIDListShuffle[i] : i;
            b.setText(mButtonsMainStr[j]);
            b.setOnClickListener(new ButtonClicked(j));
            boolean switch_soft_keyboard = (j == 11 && ENABLE_SWITCH_SOFT_KEYBOARD);
            if (switch_soft_keyboard) {
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtils.setCurrentScreenName(this, SCREEN_NAME, TAG);
    }

    private void hideButtons() {
        int duration = mButtonVisibility == ButtonVisibility.Startup ? 0 : 500;

        for (AppCompatButton b: mButtonsList) {
            buttonAnimation(b, false, duration);
            b.setClickable(false);
            b.setOnClickListener(null);
            b.setLongClickable(false);
            b.setOnLongClickListener(null);
        }
    }
    
    private void setButtonsVisibility(boolean visible) {
        if (visible && mButtonVisibility != ButtonVisibility.Visible) {
            showButtons();
            mButtonVisibility = ButtonVisibility.Visible;
        }
        if (!visible && mButtonVisibility != ButtonVisibility.Invisible) {
            hideButtons();
            mButtonVisibility = ButtonVisibility.Invisible;
        }
    }

    /**
     * Enables or disables the cancel button to allow the user interrupt the ACTION
     * requested to the activity.
     *
     * @param enabled 'True' makes the cancel button available, 'false' hides it.
     */
    protected void setCancelButtonEnabled(boolean enabled) {
        AppCompatButton cancel = (AppCompatButton)findViewById(R.id.cancel);
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
        View view = findViewById(android.R.id.content);
        mSnackbar = Snackbar.make(view, errorMessage, Snackbar.LENGTH_INDEFINITE);
        mSnackbar.show();
    }

    private void eraseErrorMessage() {
        mSnackbar.dismiss();
        mSnackbar = null;
    }

    private void startGuard() {
        if (!mSoftKeyboardMode) {
            setButtonsVisibility(false);
        } else {
            hideSoftKeyboard();
        }
        Animation animation = AnimationUtils.loadAnimation(PassCodeActivity.this, R.anim.shake);
        PassFieldLinearLayout ll = (PassFieldLinearLayout)findViewById(R.id.PasscodeLinearLayout);
        ll.startAnimation(animation);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                endGuard();
            }
        }, GUARD_TIME);
    }

    private void endGuard() {
        eraseErrorMessage();
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
    }

    /**
     * Compares pass code entered by the user with the value currently saved in the app.
     *
     * @return 'True' if entered pass code equals to the saved one.
     */
    protected boolean checkPassCode(String passCode) {
        String savedPassCode = "";
        savedPassCode += mPref.getString(PREFERENCE_PASSCODE_D1, null);
        savedPassCode += mPref.getString(PREFERENCE_PASSCODE_D2, null);
        savedPassCode += mPref.getString(PREFERENCE_PASSCODE_D3, null);
        savedPassCode += mPref.getString(PREFERENCE_PASSCODE_D4, null);

        return passCode.equals(savedPassCode);
    }

    /**
     * Sets the input fields to empty strings and puts the focus on the first one.
     */
    protected void clearPassCodeEditText() {
        mPassCodeStr = "";
        fillPassFields();
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
        } else if (KeyEvent.KEYCODE_0 <= keyCode &&  keyCode <= KeyEvent.KEYCODE_9) {
            mPassCodeStr += (char)('0' + (keyCode - KeyEvent.KEYCODE_0));
            fillPassFields();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            int len = mPassCodeStr.length();
            if (len >= 1) {
                mPassCodeStr = mPassCodeStr.substring(0, len - 1);
                fillPassFields();
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_CLEAR) {
            clearPassCodeEditText();
            fillPassFields();
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

    private void setBackground15(View v, Drawable drawable) {
        v.setBackgroundDrawable(drawable);
    }

    @TargetApi(android.os.Build.VERSION_CODES.JELLY_BEAN)
    private void setBackground16(View v, Drawable drawable) {
        v.setBackground(drawable);
    }

    private void setBackground(View v, Drawable drawable) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            setBackground15(v, drawable);
        } else {
            setBackground16(v, drawable);
        }
    }
    
    private void fillPassFields() {
        int len = mPassCodeStr.length();

        Resources res = getResources();
        for (int i = 0 ; i < passFields.length; i++) {
            int id;
            if (i < len) {
                id = R.drawable.passcode_circular_fill;
            } else {
                id = R.drawable.passcode_circular_border;
            }
            Drawable drawable = ResourcesCompat.getDrawable(res, id, null);
            View v = findViewById(passFields[i]);
            setBackground(v, drawable);
        }

        if (len == passFields.length) {
            processFullPassCode(mPassCodeStr);
        }
    }

    private class ButtonClicked implements OnClickListener {

        private int mIndex;

        ButtonClicked(int index) {
            mIndex = index;
        }

        public void onClick(View v) {
            int key;
            if (mIndex <= 9) {
                // 0,1,2,...,8,9
                key = KeyEvent.KEYCODE_0 + mIndex;
            } else if (mIndex == 10) {
                // clear
                key = KeyEvent.KEYCODE_CLEAR;
            } else {
                // delete
                key = KeyEvent.KEYCODE_DEL;
            }
            PassCodeActivity.this.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, key));
            PassCodeActivity.this.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, key));
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
