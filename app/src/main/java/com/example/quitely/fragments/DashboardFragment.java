package com.example.quitely.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.quitely.MainActivity;
import com.example.quitely.R;
import com.example.quitely.activities.AddEditZoneActivity;
import com.example.quitely.database.AppDatabase;
import com.example.quitely.database.entities.SilentZone;
import com.example.quitely.database.entities.SoundHistory;
import com.example.quitely.database.entities.SoundSchedule;
import com.example.quitely.managers.ScheduleManager;
import com.example.quitely.managers.SoundAutomationManager;
import com.example.quitely.utils.PermissionManager;
import com.example.quitely.utils.PreferenceManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private ImageView ivModeIndicatorIcon;
    private TextView tvCurrentSoundMode;
    private TextView tvAutomationStatus;
    private TextView tvActiveZoneStatus;
    private TextView tvActiveScheduleStatus;
    private TextView tvLastActivationTime;
    private TextView tvLastRestorationTime;
    private SwitchMaterial switchMasterAutomation;
    private TextView tvMasterToggleLabel;
    private MaterialCardView cardSoundIndicator;
    private LinearLayout layoutIndicatorBackground;

    // Concentric Hero Aura views
    private View viewAuraOuter;
    private View viewAuraMiddle;
    private LinearLayout layoutStatusPill;
    private View viewStatusDot;

    // Quick Mode Controls
    private MaterialCardView btnQuickNormal;
    private MaterialCardView btnQuickVibrate;
    private MaterialCardView btnQuickSilent;

    // Live Proximity & Schedule Cards
    private MaterialCardView cardProximityCheck;
    private TextView tvProximitySummary;
    private MaterialCardView cardNextSchedule;
    private TextView tvNextScheduleInfo;

    private MaterialButton btnDashboardAddZone;
    private MaterialButton btnDashboardViewZones;

    private LinearLayout layoutRecentHistoryList;
    private TextView tvNoRecentActivity;
    private TextView tvSeeAllHistory;

    private SoundAutomationManager soundManager;
    private PreferenceManager prefManager;
    private AppDatabase db;
    private FusedLocationProviderClient fusedLocationClient;

    private final BroadcastReceiver automationStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateDashboardState();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        soundManager = SoundAutomationManager.getInstance(requireContext());
        prefManager = PreferenceManager.getInstance(requireContext());
        db = AppDatabase.getInstance(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        initViews(view);
        setupListeners();
        observeDatabaseData();

        return view;
    }

    private void initViews(View view) {
        ivModeIndicatorIcon = view.findViewById(R.id.ivModeIndicatorIcon);
        tvCurrentSoundMode = view.findViewById(R.id.tvCurrentSoundMode);
        tvAutomationStatus = view.findViewById(R.id.tvAutomationStatus);
        tvActiveZoneStatus = view.findViewById(R.id.tvActiveZoneStatus);
        tvActiveScheduleStatus = view.findViewById(R.id.tvActiveScheduleStatus);
        tvLastActivationTime = view.findViewById(R.id.tvLastActivationTime);
        tvLastRestorationTime = view.findViewById(R.id.tvLastRestorationTime);
        switchMasterAutomation = view.findViewById(R.id.switchMasterAutomation);
        tvMasterToggleLabel = view.findViewById(R.id.tvMasterToggleLabel);
        cardSoundIndicator = view.findViewById(R.id.cardSoundIndicator);
        layoutIndicatorBackground = view.findViewById(R.id.layoutIndicatorBackground);

        viewAuraOuter = view.findViewById(R.id.viewAuraOuter);
        viewAuraMiddle = view.findViewById(R.id.viewAuraMiddle);
        layoutStatusPill = view.findViewById(R.id.layoutStatusPill);
        viewStatusDot = view.findViewById(R.id.viewStatusDot);

        btnQuickNormal = view.findViewById(R.id.btnQuickNormal);
        btnQuickVibrate = view.findViewById(R.id.btnQuickVibrate);
        btnQuickSilent = view.findViewById(R.id.btnQuickSilent);

        cardProximityCheck = view.findViewById(R.id.cardProximityCheck);
        tvProximitySummary = view.findViewById(R.id.tvProximitySummary);
        cardNextSchedule = view.findViewById(R.id.cardNextSchedule);
        tvNextScheduleInfo = view.findViewById(R.id.tvNextScheduleInfo);

        btnDashboardAddZone = view.findViewById(R.id.btnDashboardAddZone);
        btnDashboardViewZones = view.findViewById(R.id.btnDashboardViewZones);

        layoutRecentHistoryList = view.findViewById(R.id.layoutRecentHistoryList);
        tvNoRecentActivity = view.findViewById(R.id.tvNoRecentActivity);
        tvSeeAllHistory = view.findViewById(R.id.tvSeeAllHistory);
    }

    private void setupListeners() {
        switchMasterAutomation.setChecked(prefManager.isAutomationEnabled());
        switchMasterAutomation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            soundManager.setMasterAutomationEnabled(isChecked);
            tvMasterToggleLabel.setText(isChecked ? "Auto ON" : "Paused");
            updateDashboardState();
            Toast.makeText(requireContext(), isChecked ? "Sound automation active" : "Sound automation paused", Toast.LENGTH_SHORT).show();
        });

        btnQuickNormal.setOnClickListener(v -> {
            soundManager.applyManualSoundMode(AudioManager.RINGER_MODE_NORMAL, false, 0);
            updateDashboardState();
            Toast.makeText(requireContext(), "Normal Mode set", Toast.LENGTH_SHORT).show();
        });

        btnQuickVibrate.setOnClickListener(v -> {
            soundManager.applyManualSoundMode(AudioManager.RINGER_MODE_VIBRATE, false, 0);
            updateDashboardState();
            Toast.makeText(requireContext(), "Vibrate Mode set", Toast.LENGTH_SHORT).show();
        });

        btnQuickSilent.setOnClickListener(v -> {
            soundManager.applyManualSoundMode(AudioManager.RINGER_MODE_SILENT, false, 0);
            updateDashboardState();
            Toast.makeText(requireContext(), "Silent Mode set", Toast.LENGTH_SHORT).show();
        });

        // Long press for temporary override dialog
        View.OnLongClickListener overrideLongClick = v -> {
            showTemporaryOverrideDialog();
            return true;
        };
        btnQuickNormal.setOnLongClickListener(overrideLongClick);
        btnQuickVibrate.setOnLongClickListener(overrideLongClick);
        btnQuickSilent.setOnLongClickListener(overrideLongClick);

        cardProximityCheck.setOnClickListener(v -> runProximityCheck());

        cardNextSchedule.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(R.id.navigation_schedules);
            }
        });

        btnDashboardAddZone.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditZoneActivity.class);
            startActivity(intent);
        });

        btnDashboardViewZones.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(R.id.navigation_zones);
            }
        });

        tvSeeAllHistory.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(R.id.navigation_history);
            }
        });
    }

    private void showTemporaryOverrideDialog() {
        String[] options = {"1 Hour Quiet Mode", "2 Hours Quiet Mode", "3 Hours Quiet Mode", "Clear Override (Resume Automation)"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Temporary Sound Override")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            soundManager.applyManualSoundMode(AudioManager.RINGER_MODE_SILENT, true, 3600000L);
                            Toast.makeText(requireContext(), "Silent Mode set for 1 hour", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            soundManager.applyManualSoundMode(AudioManager.RINGER_MODE_SILENT, true, 7200000L);
                            Toast.makeText(requireContext(), "Silent Mode set for 2 hours", Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            soundManager.applyManualSoundMode(AudioManager.RINGER_MODE_SILENT, true, 10800000L);
                            Toast.makeText(requireContext(), "Silent Mode set for 3 hours", Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            soundManager.resumeAutomation();
                            Toast.makeText(requireContext(), "Manual override cleared. Automation resumed.", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    updateDashboardState();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @SuppressLint("MissingPermission")
    private void runProximityCheck() {
        if (!PermissionManager.hasLocationPermission(requireContext())) {
            PermissionManager.requestForegroundLocation(requireActivity());
            return;
        }

        tvProximitySummary.setText("Acquiring GPS fix...");
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (!isAdded()) return;
                    if (location == null) {
                        tvProximitySummary.setText("GPS location fix unavailable. Try outdoors or turn on high accuracy.");
                        return;
                    }

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        List<SilentZone> zones = db.silentZoneDao().getEnabledZones();
                        if (zones.isEmpty()) {
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() ->
                                        tvProximitySummary.setText("No enabled zones. Tap 'Add Zone' below to register one."));
                            }
                            return;
                        }

                        SilentZone closestZone = null;
                        float minDistance = Float.MAX_VALUE;
                        boolean insideAny = false;

                        for (SilentZone z : zones) {
                            float[] res = new float[1];
                            Location.distanceBetween(location.getLatitude(), location.getLongitude(), z.getLatitude(), z.getLongitude(), res);
                            float dist = res[0];
                            if (dist < minDistance) {
                                minDistance = dist;
                                closestZone = z;
                            }
                            if (dist <= z.getRadius()) {
                                insideAny = true;
                            }
                        }

                        soundManager.checkProximityToAllZones(location);

                        final SilentZone targetZone = closestZone;
                        final float dist = minDistance;
                        final boolean isInside = insideAny;

                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                if (isInside && targetZone != null) {
                                    tvProximitySummary.setText("Inside " + targetZone.getZoneName() + " (" + (int) dist + "m / " + (int) targetZone.getRadius() + "m) • Trigger active");
                                } else if (targetZone != null) {
                                    int outsideDiff = (int) (dist - targetZone.getRadius());
                                    tvProximitySummary.setText("Closest: " + targetZone.getZoneName() + " is " + (int) dist + "m away (" + outsideDiff + "m outside)");
                                }
                                updateDashboardState();
                            });
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        tvProximitySummary.setText("Location check error: " + e.getMessage());
                    }
                });
    }

    private void observeDatabaseData() {
        // Observe recent history
        db.soundHistoryDao().getRecentHistoryLive(3).observe(getViewLifecycleOwner(), this::populateRecentHistory);

        // Observe next schedule
        db.soundScheduleDao().getAllSchedulesLive().observe(getViewLifecycleOwner(), this::updateUpcomingSchedule);
    }

    private void updateUpcomingSchedule(List<SoundSchedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            tvNextScheduleInfo.setText("No upcoming schedules configured");
            return;
        }

        SoundSchedule nextSchedule = null;
        long earliestTime = Long.MAX_VALUE;

        for (SoundSchedule s : schedules) {
            if (s.isEnabled()) {
                long occurrence = ScheduleManager.calculateNextOccurrence(s.getStartHour(), s.getStartMinute(), s.getSelectedDays());
                if (occurrence < earliestTime) {
                    earliestTime = occurrence;
                    nextSchedule = s;
                }
            }
        }

        if (nextSchedule != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, hh:mm a", Locale.getDefault());
            String formattedTime = sdf.format(new Date(earliestTime));
            tvNextScheduleInfo.setText(nextSchedule.getScheduleName() + " (" + formattedTime + ")");
        } else {
            tvNextScheduleInfo.setText("All schedules currently disabled");
        }
    }

    private void populateRecentHistory(List<SoundHistory> historyList) {
        layoutRecentHistoryList.removeAllViews();
        if (historyList == null || historyList.isEmpty()) {
            tvNoRecentActivity.setVisibility(View.VISIBLE);
            layoutRecentHistoryList.setVisibility(View.GONE);
            return;
        }

        tvNoRecentActivity.setVisibility(View.GONE);
        layoutRecentHistoryList.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (SoundHistory item : historyList) {
            View itemView = inflater.inflate(R.layout.item_recent_history, layoutRecentHistoryList, false);
            ImageView ivIcon = itemView.findViewById(R.id.ivRecentIcon);
            TextView tvTitle = itemView.findViewById(R.id.tvRecentTitle);
            TextView tvSubtitle = itemView.findViewById(R.id.tvRecentSubtitle);
            TextView tvTime = itemView.findViewById(R.id.tvRecentTime);

            tvTitle.setText(item.getEventType());
            String sub = (item.getZoneOrScheduleName() != null && !item.getZoneOrScheduleName().isEmpty())
                    ? (item.getZoneOrScheduleName() + " • " + item.getPreviousMode() + " → " + item.getNewMode())
                    : (item.getPreviousMode() + " → " + item.getNewMode());
            tvSubtitle.setText(sub);
            tvTime.setText(item.getFormattedTime());

            if ("LOCATION".equalsIgnoreCase(item.getTriggerSource())) {
                ivIcon.setImageResource(R.drawable.ic_location_pin);
                ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
                ivIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_light)));
            } else if ("SCHEDULE".equalsIgnoreCase(item.getTriggerSource())) {
                ivIcon.setImageResource(R.drawable.ic_schedule);
                ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.secondary)));
                ivIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.secondary_light)));
            } else {
                ivIcon.setImageResource(R.drawable.ic_sound_normal);
                ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_vibrate)));
                ivIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_vibrate_light)));
            }

            layoutRecentHistoryList.addView(itemView);
        }
    }

    private void updateDashboardState() {
        if (!isAdded()) return;

        boolean isAutomationEnabled = prefManager.isAutomationEnabled();
        int ringerMode = soundManager.getCurrentRingerMode();
        float density = getResources().getDisplayMetrics().density;

        switchMasterAutomation.setChecked(isAutomationEnabled);
        tvMasterToggleLabel.setText(isAutomationEnabled ? "Auto ON" : "Paused");

        // Last activation and restoration
        long lastActivation = prefManager.getLastActivationTime();
        if (lastActivation > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a, MMM dd", Locale.getDefault());
            tvLastActivationTime.setText("Activated: " + sdf.format(new Date(lastActivation)));
        } else {
            tvLastActivationTime.setText("Activated: None");
        }

        long lastRestoration = prefManager.getLastRestorationTime();
        if (lastRestoration > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a, MMM dd", Locale.getDefault());
            tvLastRestorationTime.setText("Restored: " + sdf.format(new Date(lastRestoration)));
        } else {
            tvLastRestorationTime.setText("Restored: None");
        }

        // Active zone & schedule
        String activeZone = prefManager.getActiveZoneName();
        tvActiveZoneStatus.setText(activeZone.isEmpty() ? "None (Outside zones)" : ("Active: " + activeZone));

        String activeSchedule = prefManager.getActiveScheduleName();
        tvActiveScheduleStatus.setText(activeSchedule.isEmpty() ? "None active" : ("Active: " + activeSchedule));

        // Quick Controls Dock Highlight
        int strokeActive = (int) (2 * density);
        int strokeNormal = (int) (1 * density);

        // Reset all 3 cards first
        btnQuickNormal.setStrokeWidth(strokeNormal);
        btnQuickNormal.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.card_stroke));
        btnQuickNormal.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.card_background));

        btnQuickVibrate.setStrokeWidth(strokeNormal);
        btnQuickVibrate.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.card_stroke));
        btnQuickVibrate.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.card_background));

        btnQuickSilent.setStrokeWidth(strokeNormal);
        btnQuickSilent.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.card_stroke));
        btnQuickSilent.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.card_background));

        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            btnQuickNormal.setStrokeWidth(strokeActive);
            btnQuickNormal.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.state_normal));
            btnQuickNormal.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.state_normal_light));
        } else if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            btnQuickVibrate.setStrokeWidth(strokeActive);
            btnQuickVibrate.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.state_vibrate));
            btnQuickVibrate.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.state_vibrate_light));
        } else if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
            btnQuickSilent.setStrokeWidth(strokeActive);
            btnQuickSilent.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.state_silent));
            btnQuickSilent.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.state_silent_light));
        }

        // Visual indicator state and concentric aura
        if (!isAutomationEnabled) {
            tvCurrentSoundMode.setText(SoundAutomationManager.modeToString(ringerMode) + " Mode");
            tvAutomationStatus.setText("Automation Paused • Manual control active");
            tvAutomationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.state_disabled));
            ivModeIndicatorIcon.setImageResource(R.drawable.ic_sound_normal);
            ivModeIndicatorIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)));
            ivModeIndicatorIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_disabled)));
            cardSoundIndicator.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.state_disabled_border));

            if (viewAuraOuter != null) viewAuraOuter.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_disabled_light)));
            if (viewAuraMiddle != null) viewAuraMiddle.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_disabled_border)));
            if (layoutStatusPill != null) layoutStatusPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_disabled_light)));
            if (viewStatusDot != null) viewStatusDot.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_disabled)));
            return;
        }

        switch (ringerMode) {
            case AudioManager.RINGER_MODE_NORMAL:
                tvCurrentSoundMode.setText("Normal Mode");
                tvAutomationStatus.setText("Automation Active • Monitoring silent zones");
                tvAutomationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.state_normal));
                ivModeIndicatorIcon.setImageResource(R.drawable.ic_sound_normal);
                ivModeIndicatorIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)));
                ivModeIndicatorIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_normal)));
                cardSoundIndicator.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.state_normal_border));

                if (viewAuraOuter != null) viewAuraOuter.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_normal_light)));
                if (viewAuraMiddle != null) viewAuraMiddle.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.glow_normal)));
                if (layoutStatusPill != null) layoutStatusPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_normal_light)));
                if (viewStatusDot != null) viewStatusDot.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_normal)));
                break;

            case AudioManager.RINGER_MODE_VIBRATE:
                tvCurrentSoundMode.setText("Vibrate Mode");
                tvAutomationStatus.setText("Automation Active • Vibrate profile engaged");
                tvAutomationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.state_vibrate));
                ivModeIndicatorIcon.setImageResource(R.drawable.ic_sound_vibrate);
                ivModeIndicatorIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)));
                ivModeIndicatorIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_vibrate)));
                cardSoundIndicator.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.state_vibrate_border));

                if (viewAuraOuter != null) viewAuraOuter.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_vibrate_light)));
                if (viewAuraMiddle != null) viewAuraMiddle.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.glow_vibrate)));
                if (layoutStatusPill != null) layoutStatusPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_vibrate_light)));
                if (viewStatusDot != null) viewStatusDot.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_vibrate)));
                break;

            case AudioManager.RINGER_MODE_SILENT:
            default:
                tvCurrentSoundMode.setText("Silent Mode");
                tvAutomationStatus.setText("Automation Active • Total silence engaged");
                tvAutomationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.state_silent));
                ivModeIndicatorIcon.setImageResource(R.drawable.ic_sound_silent);
                ivModeIndicatorIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white)));
                ivModeIndicatorIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_silent)));
                cardSoundIndicator.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.state_silent_border));

                if (viewAuraOuter != null) viewAuraOuter.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_silent_light)));
                if (viewAuraMiddle != null) viewAuraMiddle.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.glow_silent)));
                if (layoutStatusPill != null) layoutStatusPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_silent_light)));
                if (viewStatusDot != null) viewStatusDot.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.state_silent)));
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        filter.addAction(SoundAutomationManager.ACTION_AUTOMATION_STATE_CHANGED);
        ContextCompat.registerReceiver(requireContext(), automationStateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        updateDashboardState();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            requireContext().unregisterReceiver(automationStateReceiver);
        } catch (IllegalArgumentException ignored) {}
    }
}