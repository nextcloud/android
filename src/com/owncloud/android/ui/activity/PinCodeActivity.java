/* ownCloud Android client application
 *   Copyright (C) 2011 Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
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

import java.util.Arrays;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import com.owncloud.android.R;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class PinCodeActivity extends SherlockFragmentActivity {

  
    public final static String EXTRA_ACTIVITY = "com.owncloud.android.ui.activity.PinCodeActivity.ACTIVITY";
    public final static String EXTRA_NEW_STATE = "com.owncloud.android.ui.activity.PinCodeActivity.NEW_STATE";
    
    Button bCancel;
    TextView mPinHdr;
    TextView mPinHdrExplanation;
    EditText mText1;
    EditText mText2;
    EditText mText3;
    EditText mText4;
    
    String [] tempText ={"","","",""};
    
    String activity;
    
    boolean confirmingPinCode = false;
    boolean pinCodeChecked = false;
    boolean newPasswordEntered = false;
    boolean bChange = true; // to control that only one blocks jump
    int tCounter ; // Count the number of attempts an user could introduce the PIN code

    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pincodelock); 
        
        Intent intent = getIntent();
        activity = intent.getStringExtra(EXTRA_ACTIVITY);
     
        bCancel = (Button) findViewById(R.id.cancel);
        mPinHdr = (TextView) findViewById(R.id.pinHdr);
        mPinHdrExplanation = (TextView) findViewById(R.id.pinHdrExpl);
        mText1 = (EditText) findViewById(R.id.txt1);
        mText1.requestFocus();
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mText2 = (EditText) findViewById(R.id.txt2);
        mText3 = (EditText) findViewById(R.id.txt3);
        mText4 = (EditText) findViewById(R.id.txt4);
        
        
        
        SharedPreferences appPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        
 
        // Not PIN Code defined yet.
        // In a previous version settings is allow from start
        if ( (appPrefs.getString("PrefPinCode1", null) == null ) ){
            setChangePincodeView(true);
            pinCodeChecked = true; 
            newPasswordEntered = true;
            
        }else{ 
            
            if (appPrefs.getBoolean("set_pincode", false)){
               // pincode activated
               if (activity.equals("preferences")){
                // PIN has been activated yet
                 mPinHdr.setText(R.string.pincode_configure_your_pin);
                 mPinHdrExplanation.setVisibility(View.VISIBLE);
                 pinCodeChecked = true ; // No need to check it 
                 setChangePincodeView(true);
               }else{
                // PIN active
                 bCancel.setVisibility(View.INVISIBLE);
                 bCancel.setVisibility(View.GONE);
                 mPinHdr.setText(R.string.pincode_enter_pin_code);
                 mPinHdrExplanation.setVisibility(View.INVISIBLE);
                 setChangePincodeView(false);
              }
            
           }else {
            // pincode removal
              mPinHdr.setText(R.string.pincode_remove_your_pincode);
              mPinHdrExplanation.setVisibility(View.INVISIBLE);
              pinCodeChecked = false;
              setChangePincodeView(true); 
           }
           
        }
        setTextListeners();
        
        
    }
    

     
    protected void setInitVars(){
        confirmingPinCode = false;
        pinCodeChecked = false;
        newPasswordEntered = false;

    }
    
    protected void setInitView(){
        bCancel.setVisibility(View.INVISIBLE);
        bCancel.setVisibility(View.GONE);
        mPinHdr.setText(R.string.pincode_enter_pin_code);
        mPinHdrExplanation.setVisibility(View.INVISIBLE);
    }
    
   
    protected void setChangePincodeView(boolean state){
       
        if(state){
        bCancel.setVisibility(View.VISIBLE);
        bCancel.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
            
            SharedPreferences.Editor appPrefsE = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext()).edit();
            
            SharedPreferences appPrefs = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext());
            
            boolean state = appPrefs.getBoolean("set_pincode", false);
            appPrefsE.putBoolean("set_pincode",!state); 
            appPrefsE.commit();
            setInitVars();
            finish();
            }
        });
        }  
    
    }
    
    
    
    /*
     *  
     */
    protected void setTextListeners(){
    
        /*------------------------------------------------
         *  FIRST BOX
         -------------------------------------------------*/
        
        mText1.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    if (!confirmingPinCode){
                       tempText[0] = mText1.getText().toString();
                       
                    }
                    mText2.requestFocus();
                 }
            }
        });
        
        

        /*------------------------------------------------
         *  SECOND BOX 
         -------------------------------------------------*/
        mText2.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    if (!confirmingPinCode){
                        tempText[1] = mText2.getText().toString();
                    }
                    
                    mText3.requestFocus();
                }
            }
        });
 
        mText2.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL && bChange) {

                    mText1.setText("");
                    mText1.requestFocus();
                    if (!confirmingPinCode)
                       tempText[0] = "";
                    bChange= false;
                
                }else if(!bChange){
                    bChange=true;
                    
                }
                return false;
            }
        });        
 
        mText2.setOnFocusChangeListener(new OnFocusChangeListener() {
               
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mText2.setCursorVisible(true);
                if (mText1.getText().toString().equals("")){
                    mText2.setSelected(false);
                    mText2.setCursorVisible(false);
                    mText1.requestFocus(); 
                    mText1.setSelected(true);
                    mText1.setSelection(0);
                }
                
            }
        });
        
        
        /*------------------------------------------------
         *  THIRD BOX
         -------------------------------------------------*/
        mText3.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    if (!confirmingPinCode){
                        tempText[2] = mText3.getText().toString();
                    }
                    mText4.requestFocus();
                }
            }
        });
        
        mText3.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL && bChange) {
                    mText2.requestFocus();
                    if (!confirmingPinCode)
                        tempText[1] = "";
                    mText2.setText("");
                    bChange= false;
                    
                }else if(!bChange){
                    bChange=true;                        
                    
                }
                return false;
            }
        });
        
        mText3.setOnFocusChangeListener(new OnFocusChangeListener() {
            
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mText3.setCursorVisible(true);
                if (mText1.getText().toString().equals("")){
                    mText3.setSelected(false);
                    mText3.setCursorVisible(false);
                    mText1.requestFocus();
                    mText1.setSelected(true);
                    mText1.setSelection(0);
                }else if (mText2.getText().toString().equals("")){
                    mText3.setSelected(false);
                    mText3.setCursorVisible(false);
                    mText2.requestFocus();
                    mText2.setSelected(true);
                    mText2.setSelection(0);
                }
                
            }
        });
        
        /*------------------------------------------------
         *  FOURTH BOX
         -------------------------------------------------*/
        mText4.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    
                    if (!confirmingPinCode){
                       tempText[3] = mText4.getText().toString();
                    }
                    mText1.requestFocus();

                    if (!pinCodeChecked){
                        pinCodeChecked = checkPincode();
                    }
                    
                    if (pinCodeChecked && activity.equals("FileDisplayActivity")){
                        finish();
                    } else if (pinCodeChecked){
                        
                        Intent intent = getIntent();
                        String newState = intent.getStringExtra(EXTRA_NEW_STATE);
                        
                        if (newState.equals("false")){
                            SharedPreferences.Editor appPrefs = PreferenceManager
                                    .getDefaultSharedPreferences(getApplicationContext()).edit();
                            appPrefs.putBoolean("set_pincode",false);
                            appPrefs.commit();
                            
                            setInitVars();
                            pinCodeEnd(false);
                            
                        }else{
                        
                            if (!confirmingPinCode){
                                pinCodeChangeRequest();
                             
                            } else {
                                confirmPincode();
                            }
                        }
                   
                        
                    }    
                }
            }
        });

        
        
        mText4.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL && bChange) {
                    mText3.requestFocus();
                    if (!confirmingPinCode)
                        tempText[2]="";
                    mText3.setText("");
                    bChange= false;
                    
                }else if(!bChange){
                    bChange=true;    
                }
                return false;
            }
        });
        
       mText4.setOnFocusChangeListener(new OnFocusChangeListener() {
            
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mText4.setCursorVisible(true);
                
                if (mText1.getText().toString().equals("")){
                    mText4.setSelected(false);
                    mText4.setCursorVisible(false);
                    mText1.requestFocus();
                    mText1.setSelected(true);
                    mText1.setSelection(0);
                }else if (mText2.getText().toString().equals("")){
                    mText4.setSelected(false);
                    mText4.setCursorVisible(false);
                    mText2.requestFocus();
                    mText2.setSelected(true);
                    mText2.setSelection(0);
                }else if (mText3.getText().toString().equals("")){
                    mText4.setSelected(false);
                    mText4.setCursorVisible(false);
                    mText3.requestFocus();
                    mText3.setSelected(true);
                    mText3.setSelection(0);
                }
                
            }
        });
        
        
        
    } // end setTextListener
    
    
    protected void pinCodeChangeRequest(){
    
        clearBoxes(); 
        mPinHdr.setText(R.string.pincode_reenter_your_pincode); 
        mPinHdrExplanation.setVisibility(View.INVISIBLE);        
        confirmingPinCode =true;
        
    }
    
    
    protected boolean checkPincode(){
        
        
        SharedPreferences appPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        
       String pText1 = appPrefs.getString("PrefPinCode1", null);
        String pText2 = appPrefs.getString("PrefPinCode2", null);
        String pText3 = appPrefs.getString("PrefPinCode3", null);
        String pText4 = appPrefs.getString("PrefPinCode4", null);

        if ( tempText[0].equals(pText1) && 
             tempText[1].equals(pText2) &&
             tempText[2].equals(pText3) &&
             tempText[3].equals(pText4) ) {
            
            return true;
        
        
        }else {
            Arrays.fill(tempText, null);
            AlertDialog aDialog = new AlertDialog.Builder(this).create();
            CharSequence errorSeq = getString(R.string.common_error);
            aDialog.setTitle(errorSeq);
            CharSequence cseq = getString(R.string.pincode_wrong);
            aDialog.setMessage(cseq);
            CharSequence okSeq = getString(R.string.common_ok);
            aDialog.setButton(okSeq, new DialogInterface.OnClickListener(){

                @Override
                public void onClick(DialogInterface dialog, int which) {
                   return; 
                }
                
            });
            aDialog.show();
            clearBoxes(); 
            mPinHdr.setText(R.string.pincode_enter_pin_code);
            mPinHdrExplanation.setVisibility(View.INVISIBLE);
            newPasswordEntered = true;
            confirmingPinCode = false;
            
        }
     
        
        return false;
    }
    
    protected void confirmPincode(){
        
        confirmingPinCode = false;
        
        String rText1 = mText1.getText().toString();
        String rText2 = mText2.getText().toString();
        String rText3 = mText3.getText().toString();
        String rText4 = mText4.getText().toString();
        
        if ( tempText[0].equals(rText1) && 
             tempText[1].equals(rText2) &&
             tempText[2].equals(rText3) &&
             tempText[3].equals(rText4) ) {
                        
            savePincodeAndExit();
            
        } else {
            
            Arrays.fill(tempText, null);
            AlertDialog aDialog = new AlertDialog.Builder(this).create();
            CharSequence errorSeq = getString(R.string.common_error);
            aDialog.setTitle(errorSeq);
            CharSequence cseq = getString(R.string.pincode_mismatch);
            aDialog.setMessage(cseq);
            CharSequence okSeq = getString(R.string.common_ok);
            aDialog.setButton(okSeq, new DialogInterface.OnClickListener(){

                @Override
                public void onClick(DialogInterface dialog, int which) {
                   return; 
                }
                
            });
            aDialog.show();
            mPinHdr.setText(R.string.pincode_configure_your_pin);
            mPinHdrExplanation.setVisibility(View.VISIBLE);
            clearBoxes();
        }
    
    }
   
    
    protected void pinCodeEnd(boolean state){
        AlertDialog aDialog = new AlertDialog.Builder(this).create();
        
        if (state){
            CharSequence saveSeq = getString(R.string.common_save_exit);
            aDialog.setTitle(saveSeq);
            CharSequence cseq = getString(R.string.pincode_stored);
            aDialog.setMessage(cseq);
            
        }else{
            CharSequence saveSeq = getString(R.string.common_save_exit);
            aDialog.setTitle(saveSeq);
            CharSequence cseq = getString(R.string.pincode_removed);
            aDialog.setMessage(cseq);
            
        }
        CharSequence okSeq = getString(R.string.common_ok);
        aDialog.setButton(okSeq, new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                return; 
            }
            
        });
        aDialog.show(); 
    }
    
    protected void savePincodeAndExit(){
        SharedPreferences.Editor appPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext()).edit();
        
        appPrefs.putString("PrefPinCode1", tempText[0]);
        appPrefs.putString("PrefPinCode2",tempText[1]);
        appPrefs.putString("PrefPinCode3", tempText[2]);
        appPrefs.putString("PrefPinCode4", tempText[3]);
        appPrefs.putBoolean("set_pincode",true);
        appPrefs.commit();
        
        pinCodeEnd(true);
        
        
        
    }
    
    
    protected void clearBoxes(){
        
        mText1.setText("");
        mText2.setText("");
        mText3.setText("");
        mText4.setText("");
        mText1.requestFocus(); 
    }
    
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount()== 0){
            
            if (activity.equals("preferences")){
                SharedPreferences.Editor appPrefsE = PreferenceManager
            
                    .getDefaultSharedPreferences(getApplicationContext()).edit();
            
                SharedPreferences appPrefs = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext());
            
                boolean state = appPrefs.getBoolean("set_pincode", false);
                appPrefsE.putBoolean("set_pincode",!state); 
                appPrefsE.commit();
                setInitVars();
                finish();
            }
            return true; 
            
        }
        
        return super.onKeyDown(keyCode, event);
    }
    
   

    
            
}
