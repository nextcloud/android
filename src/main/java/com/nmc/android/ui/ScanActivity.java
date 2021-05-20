package com.nmc.android.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;

import com.nmc.android.interfaces.OnDocScanListener;
import com.nmc.android.interfaces.OnFragmentChangeListener;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ActivityScanBinding;
import com.owncloud.android.ui.activity.ToolbarActivity;
import com.owncloud.android.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import io.scanbot.sdk.ScanbotSDK;

public class ScanActivity extends ToolbarActivity implements OnFragmentChangeListener, OnDocScanListener {

    protected static final String FRAGMENT_SCAN_TAG = "SCAN_FRAGMENT_TAG";
    protected static final String FRAGMENT_EDIT_SCAN_TAG = "EDIT_SCAN_FRAGMENT_TAG";
    protected static final String FRAGMENT_CROP_SCAN_TAG = "CROP_SCAN_FRAGMENT_TAG";
    protected static final String FRAGMENT_SAVE_SCAN_TAG = "SAVE_SCAN_FRAGMENT_TAG";

    protected static final String TAG = "ScanActivity";

    private ActivityScanBinding binding;
    private ScanbotSDK scanbotSDK;

    public static final List<Bitmap> scannedImages = new ArrayList<>();

    public static void openScanActivity(Context context, int requestCode) {
        Intent intent = new Intent(context, ScanActivity.class);
        ((AppCompatActivity)context).startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate and set the layout view
        binding = ActivityScanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        scannedImages.clear();
        initScanbotSDK();
        setupToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        createScanFragment(savedInstanceState);
    }

    private void createScanFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            ScanDocumentFragment scanDocumentFragment = ScanDocumentFragment.newInstance(TAG);
            onReplaceFragment(scanDocumentFragment, FRAGMENT_SCAN_TAG, false);
        } else {
            getSupportFragmentManager().findFragmentByTag(FRAGMENT_SCAN_TAG);
        }
    }

    @Override
    public void onReplaceFragment(Fragment fragment, String tag, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.scan_frame_container, fragment, tag);
        if (addToBackStack) {
            transaction.addToBackStack(tag);
        }
        transaction.commit();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressHandle();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        onBackPressHandle();
    }

    private void onBackPressHandle() {
        Fragment editScanFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_EDIT_SCAN_TAG);
        Fragment cropScanFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_CROP_SCAN_TAG);
        Fragment saveScanFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_SAVE_SCAN_TAG);
        if (cropScanFragment != null || saveScanFragment != null) {
            int index = 0;
            if (cropScanFragment instanceof CropScannedDocumentFragment){
                index = ((CropScannedDocumentFragment)cropScanFragment).getScannedDocIndex();
            }
            onReplaceFragment(EditScannedDocumentFragment.newInstance(index), FRAGMENT_EDIT_SCAN_TAG, false);
        } else if (editScanFragment != null) {
            createScanFragment(null);
        } else {
            super.onBackPressed();
        }
    }

    private void initScanbotSDK() {
        scanbotSDK = new ScanbotSDK(this);
    }

    public ScanbotSDK getScanbotSDK() {
        return scanbotSDK;
    }

    @Override
    public void addScannedDoc(Bitmap file) {
        if (file != null) {
            scannedImages.add(file);
        }
    }

    @Override
    public List<Bitmap> getScannedDocs() {
        return scannedImages;
    }

    @Override
    public boolean removedScannedDoc(Bitmap file) {
        if (scannedImages.size() > 0 && file != null) {
            return scannedImages.remove(file);
        }
        return false;
    }

    @Override
    public Bitmap replaceScannedDoc(int index, Bitmap newFile) {
        if (scannedImages.size() > 0 && newFile != null && index >= 0 && scannedImages.size() - 1 >= index) {
            return scannedImages.set(index, newFile);
        }
        return null;
    }
}
