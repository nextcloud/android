/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.network.ManualClientCertificateStore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;

/** Optional mTLS certificate import for an account that is already configured. */
public class ManualClientCertificateActivity extends AppCompatActivity {
    private static final int REQUEST_CERTIFICATE = 8472;
    private static final int MAX_IMPORT_BYTES = 2 * 1024 * 1024;

    private String host;
    private int port;
    private TextView status;
    private Button removeButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User user = UserAccountManagerImpl.fromContext(this).getUser();
        URI server = user != null && user.getServer() != null ? user.getServer().getUri() : null;
        if (server == null || !"https".equalsIgnoreCase(server.getScheme()) ||
                server.getHost() == null) {
            Toast.makeText(this, R.string.mtls_invalid_server, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        host = server.getHost();
        port = server.getPort() == -1 ? 443 : server.getPort();

        // The app uses edge-to-edge rendering. Place the toolbar below the status bar
        // and keep the form scrollable on phones with smaller screens or larger font sizes.
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setMinimumHeight((int) (56 * getResources().getDisplayMetrics().density));
        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(layout, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        TextView description = new TextView(this);
        description.setText(getString(R.string.mtls_description, host, port));
        layout.addView(description);

        status = new TextView(this);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = padding;
        layout.addView(status, statusParams);

        Button importButton = new Button(this);
        importButton.setText(R.string.mtls_import);
        importButton.setOnClickListener(view -> selectCertificate());
        layout.addView(importButton);

        removeButton = new Button(this);
        removeButton.setText(R.string.mtls_remove);
        removeButton.setOnClickListener(view -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.mtls_remove)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.mtls_remove, (dialog, which) -> removeCertificate())
                .show());
        layout.addView(removeButton);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        setContentView(root);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.mtls_title);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        ViewCompat.requestApplyInsets(root);
        updateStatus();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void updateStatus() {
        boolean configured = ManualClientCertificateStore.isConfigured(this, host, port);
        status.setText(configured ? R.string.mtls_status_imported : R.string.mtls_status_keychain);
        removeButton.setEnabled(configured);
    }

    private void selectCertificate() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CERTIFICATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CERTIFICATE || resultCode != Activity.RESULT_OK ||
                data == null || data.getData() == null) {
            return;
        }
        Uri selected = data.getData();
        EditText passwordInput = new EditText(this);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint(R.string.mtls_password);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.mtls_password)
                .setView(passwordInput)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.mtls_import, (dialog, which) -> {
                    char[] password = passwordInput.getText().toString().toCharArray();
                    passwordInput.getText().clear();
                    new Thread(() -> importCertificate(selected, password)).start();
                })
                .show();
    }

    private void importCertificate(Uri selected, char[] password) {
        byte[] contents = null;
        boolean success = false;
        try (InputStream input = getContentResolver().openInputStream(selected)) {
            if (input == null) {
                throw new IllegalArgumentException("Certificate file could not be opened");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) != -1) {
                if (output.size() + length > MAX_IMPORT_BYTES) {
                    throw new IllegalArgumentException("Certificate file is too large");
                }
                output.write(buffer, 0, length);
            }
            contents = output.toByteArray();
            ManualClientCertificateStore.importCertificate(getApplicationContext(), host, port,
                    contents, password);
            success = true;
        } catch (Exception ignored) {
            // Report a generic message; never expose certificate or filesystem details in the UI.
        } finally {
            Arrays.fill(password, '\0');
            if (contents != null) {
                Arrays.fill(contents, (byte) 0);
            }
        }
        boolean imported = success;
        runOnUiThread(() -> {
            Toast.makeText(this, imported ? R.string.mtls_import_success : R.string.mtls_import_failed,
                    Toast.LENGTH_LONG).show();
            updateStatus();
        });
    }

    private void removeCertificate() {
        new Thread(() -> {
            try {
                ManualClientCertificateStore.remove(getApplicationContext(), host, port);
                runOnUiThread(() -> {
                    updateStatus();
                    Toast.makeText(this, R.string.mtls_remove_success, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception ignored) {
                runOnUiThread(() -> Toast.makeText(this, R.string.mtls_remove_failed,
                        Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
