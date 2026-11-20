package com.example.quitely.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.quitely.R;
import com.example.quitely.activities.ImportantContactsActivity;
import com.example.quitely.managers.SoundAutomationManager;
import com.example.quitely.utils.PermissionManager;
import com.example.quitely.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    private SwitchMaterial switchSettingsMaster;
    private SwitchMaterial switchNotifEntry;
    private SwitchMaterial switchNotifExit;
    private SwitchMaterial switchNotifSchedule;

    private TextView tvPermLocationStatus;
    private TextView tvPermBgLocationStatus;
    private TextView tvPermDndStatus;
    private TextView tvPermBatteryStatus;
    private TextView tvPermNotifStatus;
    private TextView tvPermAlarmStatus;

    private ImageView ivPermLocationIcon;
    private ImageView ivPermBgLocationIcon;
    private ImageView ivPermDndIcon;
    private ImageView ivPermBatteryIcon;
    private ImageView ivPermNotifIcon;
    private ImageView ivPermAlarmIcon;

    private MaterialButton btnFixLocation;
    private MaterialButton btnFixBgLocation;
    private MaterialButton btnFixDnd;
    private MaterialButton btnFixBattery;
    private MaterialButton btnFixNotif;
    private MaterialButton btnFixAlarm;

    private MaterialCardView cardLinkImportantContacts;
    private View rowPrivacyPolicy;
    private View rowAboutApp;

    private PreferenceManager prefManager;
    private SoundAutomationManager soundManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        prefManager = PreferenceManager.getInstance(requireContext());
        soundManager = SoundAutomationManager.getInstance(requireContext());

        initViews(view);
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        switchSettingsMaster = view.findViewById(R.id.switchSettingsMaster);
        switchNotifEntry = view.findViewById(R.id.switchNotifEntry);
        switchNotifExit = view.findViewById(R.id.switchNotifExit);
        switchNotifSchedule = view.findViewById(R.id.switchNotifSchedule);

        tvPermLocationStatus = view.findViewById(R.id.tvPermLocationStatus);
        tvPermBgLocationStatus = view.findViewById(R.id.tvPermBgLocationStatus);
        tvPermDndStatus = view.findViewById(R.id.tvPermDndStatus);
        tvPermBatteryStatus = view.findViewById(R.id.tvPermBatteryStatus);
        tvPermNotifStatus = view.findViewById(R.id.tvPermNotifStatus);
        tvPermAlarmStatus = view.findViewById(R.id.tvPermAlarmStatus);

        ivPermLocationIcon = view.findViewById(R.id.ivPermLocationIcon);
        ivPermBgLocationIcon = view.findViewById(R.id.ivPermBgLocationIcon);
        ivPermDndIcon = view.findViewById(R.id.ivPermDndIcon);
        ivPermBatteryIcon = view.findViewById(R.id.ivPermBatteryIcon);
        ivPermNotifIcon = view.findViewById(R.id.ivPermNotifIcon);
        ivPermAlarmIcon = view.findViewById(R.id.ivPermAlarmIcon);

        btnFixLocation = view.findViewById(R.id.btnFixLocation);
        btnFixBgLocation = view.findViewById(R.id.btnFixBgLocation);
        btnFixDnd = view.findViewById(R.id.btnFixDnd);
        btnFixBattery = view.findViewById(R.id.btnFixBattery);
        btnFixNotif = view.findViewById(R.id.btnFixNotif);
        btnFixAlarm = view.findViewById(R.id.btnFixAlarm);

        cardLinkImportantContacts = view.findViewById(R.id.cardLinkImportantContacts);
        rowPrivacyPolicy = view.findViewById(R.id.rowPrivacyPolicy);
        rowAboutApp = view.findViewById(R.id.rowAboutApp);
    }

    private void setupListeners() {
        switchSettingsMaster.setChecked(prefManager.isAutomationEnabled());
        switchSettingsMaster.setOnCheckedChangeListener((buttonView, isChecked) -> {
            soundManager.setMasterAutomationEnabled(isChecked);
            Toast.makeText(requireContext(), isChecked ? "Automation enabled" : "Automation paused", Toast.LENGTH_SHORT).show();
        });

        switchNotifEntry.setChecked(prefManager.isNotificationEntryEnabled());
        switchNotifEntry.setOnCheckedChangeListener((b, isChecked) -> prefManager.setNotificationEntryEnabled(isChecked));

        switchNotifExit.setChecked(prefManager.isNotificationExitEnabled());
        switchNotifExit.setOnCheckedChangeListener((b, isChecked) -> prefManager.setNotificationExitEnabled(isChecked));

        switchNotifSchedule.setChecked(prefManager.isNotificationScheduleEnabled());
        switchNotifSchedule.setOnCheckedChangeListener((b, isChecked) -> prefManager.setNotificationScheduleEnabled(isChecked));

        btnFixLocation.setOnClickListener(v -> PermissionManager.requestForegroundLocation(requireActivity()));
        btnFixBgLocation.setOnClickListener(v -> PermissionManager.openAppSettings(requireContext()));
        btnFixDnd.setOnClickListener(v -> PermissionManager.openNotificationPolicySettings(requireContext()));
        btnFixBattery.setOnClickListener(v -> PermissionManager.requestIgnoreBatteryOptimizations(requireContext()));
        btnFixNotif.setOnClickListener(v -> PermissionManager.requestNotificationPermission(requireActivity()));
        btnFixAlarm.setOnClickListener(v -> PermissionManager.openExactAlarmSettings(requireContext()));

        cardLinkImportantContacts.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ImportantContactsActivity.class);
            startActivity(intent);
        });

        rowPrivacyPolicy.setOnClickListener(v -> showPrivacyPolicyDialog());
        rowAboutApp.setOnClickListener(v -> showAboutDialog());
    }

    private void updatePermissionDiagnostics() {
        if (!isAdded()) return;

        // Foreground Location
        boolean hasLoc = PermissionManager.hasLocationPermission(requireContext());
        setPermItemState(hasLoc, tvPermLocationStatus, ivPermLocationIcon, btnFixLocation, "Granted", "Action Required");

        // Background Location
        boolean hasBgLoc = PermissionManager.hasBackgroundLocationPermission(requireContext());
        setPermItemState(hasBgLoc, tvPermBgLocationStatus, ivPermBgLocationIcon, btnFixBgLocation, "Granted (All the time)", "Requires App Settings");

        // DND Policy
        boolean hasDnd = PermissionManager.hasNotificationPolicyAccess(requireContext());
        setPermItemState(hasDnd, tvPermDndStatus, ivPermDndIcon, btnFixDnd, "Granted", "Access Required for Silent Mode");

        // Battery Optimization
        boolean isBatteryExempt = PermissionManager.isIgnoringBatteryOptimizations(requireContext());
        setPermItemState(isBatteryExempt, tvPermBatteryStatus, ivPermBatteryIcon, btnFixBattery, "Exempted (High reliability)", "Optimized (May delay alarms)");

        // Notifications (Android 13+)
        boolean hasNotif = PermissionManager.hasNotificationPermission(requireContext());
        setPermItemState(hasNotif, tvPermNotifStatus, ivPermNotifIcon, btnFixNotif, "Granted", "Action Required (Android 13+)");

        // Exact Alarms (Android 12+)
        boolean canExactAlarm = PermissionManager.canScheduleExactAlarms(requireContext());
        setPermItemState(canExactAlarm, tvPermAlarmStatus, ivPermAlarmIcon, btnFixAlarm, "Granted (Precise schedule firing)", "Permission Restricted");
    }

    private void setPermItemState(boolean isGranted, TextView tvStatus, ImageView ivIcon, MaterialButton btnFix, String grantedText, String deniedText) {
        if (isGranted) {
            tvStatus.setText(grantedText);
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.state_normal));
            ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_normal)));
            btnFix.setVisibility(View.GONE);
        } else {
            tvStatus.setText(deniedText);
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning));
            ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.warning)));
            btnFix.setVisibility(View.VISIBLE);
        }
    }

    private void showPrivacyPolicyDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Privacy Policy")
                .setMessage("Quitely – Smart Sound Automation respects your privacy.\n\n" +
                        "1. Local Processing: All geofence boundaries, schedule rules, and sound event logs are stored locally on your device in an encrypted SQLite/Room database.\n\n" +
                        "2. Zero Analytics & No Cloud Tracking: Quitely has zero advertising frameworks, third-party analytics trackers, or remote servers. Your location coordinates never leave your device.\n\n" +
                        "3. Operating System Standard APIs: Quitely utilizes Google Play Services Geofencing API strictly for low-power hardware geofence triggering.")
                .setPositiveButton("Understood", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("About Quitely")
                .setMessage("Quitely – Smart Sound Automation\n" +
                        "Version 1.0 (Build 1)\n\n" +
                        "Tagline: Silence when you need it. Sound when you don't.\n\n" +
                        "An intelligent sound automation utility designed for students, faculty, and professionals to eliminate phone distractions in classrooms, libraries, and quiet zones.\n\n" +
                        "Developed using Java, Android Geofencing API, and Material Design.")
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePermissionDiagnostics();
    }
}
