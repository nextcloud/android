/* ownCloud Android client application
 *   Copyright (C) 2011 Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
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
package eu.alefzero.owncloud.ui.activity;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import eu.alefzero.owncloud.R;

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

  
    public final static String EXTRA_ACTIVITY = "eu.alefzero.owncloud.ui.activity.PinCodeActivity.ACTIVITY";
    public final static String EXTRA_NEW_STATE = "eu.alefzero.owncloud.ui.activity.PinCodeActivity.NEW_STATE";
    
    Button bCancel;
    TextView mPinHdr;
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
    int tCounter ; // Count the number of attempts an user could introduce de PIN code

    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pincodelock); 
        
        Intent intent = getIntent();
        activity = intent.getStringExtra(EXTRA_ACTIVITY);
     
        bCancel = (Button) findViewById(R.id.cancel);
        mPinHdr = (TextView) findViewById(R.id.pinHdr);
        mText1 = (EditText) findViewById(R.id.txt1);
        mText1.requestFocus();
        mText2 = (EditText) findViewById(R.id.txt2);
        mText3 = (EditText) findViewById(R.id.txt3);
        mText4 = (EditText) findViewById(R.id.txt4);
        
        
        SharedPreferences appPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        
        // Not PIN Code defined yet
        if ( appPrefs.getString("PrefPinCode1", null) == null ){
            setChangePincodeView();
            pinCodeChecked = true; 
            newPasswordEntered = true;
            
        } else {
            setInitView(); 
        }
           
       
        setTextListeners();
        
        
    }
       
    
    protected void setInitView(){
        bCancel.setVisibility(View.INVISIBLE);
        bCancel.setVisibility(View.GONE);
        mPinHdr.setText("Please, Insert your PIN");
    }
    
    
    
    protected void setChangePincodeView(){
        mPinHdr.setText("Configure your PIN");
        bCancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });
    
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
                // TODO Auto-generated method stub
                if (s.length() > 0) {
                   if (!confirmingPinCode){
                      tempText[0] = mText1.getText().toString();
                      
                   }
                   mText2.requestFocus();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub

            }
        });
        
        

        /*------------------------------------------------
         *  SECOND BOX 
         -------------------------------------------------*/
        mText2.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                // TODO Auto-generated method stub
                if (s.length() > 0) {
                    if (!confirmingPinCode){
                        tempText[1] = mText2.getText().toString();
                    }
                    mText3.requestFocus();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub

            }
        });
 
        mText2.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // TODO Auto-generated method stub

                if (keyCode == KeyEvent.KEYCODE_DEL && bChange) {

                    mText1.setText("");
                    mText1.requestFocus();
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
                // TODO Auto-generated method stub
                if (mText1.getText().toString().equals("")){
                    mText1.requestFocus(); 
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
                // TODO Auto-generated method stub
                if (s.length() > 0) {
                    if (!confirmingPinCode){
                        tempText[2] = mText3.getText().toString();
                    }
                    mText4.requestFocus();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub

            }
        });
        
        mText3.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // TODO Auto-generated method stub

                if (keyCode == KeyEvent.KEYCODE_DEL && bChange) {
                    mText2.requestFocus();
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
                // TODO Auto-generated method stub
                if (mText1.getText().toString().equals("")){
                    mText1.requestFocus(); 
                }else if (mText2.getText().toString().equals("")){
                    mText2.requestFocus(); 
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
                           // TODO Alert Message que salte y vuelva a la pantalla anterior
                           
                           finish();
                           
                       }else{
                       
                           if (!confirmingPinCode && !newPasswordEntered){
                               pinCodeChangeRequest();
                           } else if (newPasswordEntered && !confirmingPinCode){
                               mPinHdr.setText("Confirm your PINCode, please");
                               confirmingPinCode = true;
                               clearBoxes();
                           } else {
                               confirmPincode();
                           }
                       }
                  
                       
                   }    
                                      
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub

            }
        });

        
        
        mText4.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // TODO Auto-generated method stub

                if (keyCode == KeyEvent.KEYCODE_DEL && bChange) {
                    mText3.requestFocus();
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
                // TODO Auto-generated method stub
                if (mText1.getText().toString().equals("")){
                    mText1.requestFocus(); 
                }else if (mText2.getText().toString().equals("")){
                    mText2.requestFocus(); 
                }else if (mText3.getText().toString().equals("")){
                    mText3.requestFocus(); 
                }
                
            }
        });
        
        
        
    } // end setTextListener
    
    
    protected void pinCodeChangeRequest(){
    
        AlertDialog.Builder aBuilder = new AlertDialog.Builder(this);
        aBuilder.setMessage("Do yo want to set a new PIN Code")
              .setCancelable(false)
              .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                 
                 @Override
                 public void onClick(DialogInterface dialog, int which) {
                     // TODO Auto-generated method stub
                     setChangePincodeView();
                     mPinHdr.setText("Please, insert your new PIN Code");
                     clearBoxes();
                     newPasswordEntered = true;
                 }
             })
             .setNegativeButton("No", new DialogInterface.OnClickListener() {
                 
                 @Override
                 public void onClick(DialogInterface dialog, int which) {
                     // TODO Auto-generated method stub
                     SharedPreferences.Editor appPrefs = PreferenceManager
                             .getDefaultSharedPreferences(getApplicationContext()).edit();
                     appPrefs.putBoolean("set_pincode",false);
                     appPrefs.commit();
                     finish();
                 }
             });
        
        AlertDialog alert =aBuilder.create();
      
        alert.show();
        
        
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
            AlertDialog aDialog = new AlertDialog.Builder(this).create();
            aDialog.setTitle("ERROR");
            aDialog.setMessage("Wrong PIN");
            aDialog.setButton("OK", new DialogInterface.OnClickListener(){

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub("");
                   return; 
                }
                
            });
            aDialog.show();
            clearBoxes(); 
            mPinHdr.setText("Configure your PIN");
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
            
            AlertDialog aDialog = new AlertDialog.Builder(this).create();
            aDialog.setTitle("ERROR");
            aDialog.setMessage("PIN Code Mismatch");
            aDialog.setButton("OK", new DialogInterface.OnClickListener(){

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub("");
                   return; 
                }
                
            });
            aDialog.show();
            mPinHdr.setText("Configure your PIN");
            clearBoxes();
        }
    
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
        
        finish();
    }
    
    
    protected void clearBoxes(){
        
        mText1.setText("");
        mText2.setText("");
        mText3.setText("");
        mText4.setText("");
        mText1.requestFocus(); 
    }
            
            
}
