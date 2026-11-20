package com.example.quitely.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.quitely.R;
import com.example.quitely.utils.PermissionManager;
import com.google.android.material.button.MaterialButton;

public class ImportantContactsActivity extends AppCompatActivity {

    private ImageView ivDndStatusIcon;
    private TextView tvDndStatusBadge;
    private MaterialButton btnGrantDnd;
    private MaterialButton btnOpenDndSettings;
    private MaterialButton btnOpenContacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_important_contacts);

        initViews();
        setupToolbar();
        setupListeners();
    }

    private void initViews() {
        ivDndStatusIcon = findViewById(R.id.ivDndStatusIcon);
        tvDndStatusBadge = findViewById(R.id.tvDndStatusBadge);
        btnGrantDnd = findViewById(R.id.btnGrantDnd);
        btnOpenDndSettings = findViewById(R.id.btnOpenDndSettings);
        btnOpenContacts = findViewById(R.id.btnOpenContacts);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarContacts);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnGrantDnd.setOnClickListener(v -> PermissionManager.openNotificationPolicySettings(this));

        btnOpenDndSettings.setOnClickListener(v -> {
            try {
                // Try opening Zen Mode / DND priority settings directly
                Intent intent = new Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS);
                startActivity(intent);
            } catch (Exception e) {
                // Fallback to notification policy access settings
                try {
                    PermissionManager.openNotificationPolicySettings(this);
                } catch (Exception ex) {
                    Toast.makeText(this, "Please open Settings > Sound > Do Not Disturb", Toast.LENGTH_LONG).show();
                }
            }
        });

        btnOpenContacts.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Could not open Contacts app", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPermissionState() {
        boolean hasAccess = PermissionManager.hasNotificationPolicyAccess(this);
        if (hasAccess) {
            tvDndStatusBadge.setText("Permission Granted • Ready for DND");
            tvDndStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.state_normal));
            ivDndStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.state_normal)));
            ivDndStatusIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.state_normal_light)));
            btnGrantDnd.setVisibility(View.GONE);
        } else {
            tvDndStatusBadge.setText("Permission Required • Silent Mode Restricted");
            tvDndStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.warning));
            ivDndStatusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.warning)));
            ivDndStatusIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.warning_light)));
            btnGrantDnd.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionState();
    }
}
