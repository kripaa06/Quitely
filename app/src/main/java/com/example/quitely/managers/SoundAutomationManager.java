package com.example.quitely.managers;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import com.example.quitely.database.AppDatabase;
import com.example.quitely.database.entities.SilentZone;
import com.example.quitely.database.entities.SoundHistory;
import com.example.quitely.database.entities.SoundSchedule;
import com.example.quitely.notifications.NotificationHelper;
import com.example.quitely.utils.PermissionManager;
import com.example.quitely.utils.PreferenceManager;

import java.util.List;
import java.util.Set;

/**
 * SoundAutomationManager is the central authority controlling all sound-mode changes
 * across location-based geofences, time-based schedules, and manual controls.
 */
public class SoundAutomationManager {

    private static final String TAG = "SoundAutomationManager";
    public static final String ACTION_AUTOMATION_STATE_CHANGED = "com.example.quitely.ACTION_AUTOMATION_STATE_CHANGED";
    private static volatile SoundAutomationManager INSTANCE;

    private final Context context;
    private final AudioManager audioManager;
    private final PreferenceManager prefManager;
    private final AppDatabase db;

    private SoundAutomationManager(Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        this.prefManager = PreferenceManager.getInstance(this.context);
        this.db = AppDatabase.getInstance(this.context);
    }

    public static SoundAutomationManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SoundAutomationManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SoundAutomationManager(context);
                }
            }
        }
        return INSTANCE;
    }

    private void notifyStateChanged() {
        Intent intent = new Intent(ACTION_AUTOMATION_STATE_CHANGED);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    /**
     * CASE 1 & CASE 7: User enters a registered silent zone.
     */
    public void onGeofenceEnter(int zoneId) {
        if (!prefManager.isAutomationEnabled()) {
            Log.d(TAG, "Automation is disabled globally. Ignoring geofence enter for zone " + zoneId);
            return;
        }

        if (prefManager.isManualOverrideActive()) {
            Log.d(TAG, "Manual override is currently active. Suspending automatic trigger.");
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            SilentZone zone = db.silentZoneDao().getZoneById(zoneId);
            if (zone == null || !zone.isEnabled()) {
                Log.d(TAG, "Zone not found or disabled: " + zoneId);
                return;
            }

            Set<String> activeZones = prefManager.getActiveZones();
            Set<String> activeSchedules = prefManager.getActiveSchedules();

            // Save original sound mode if this is the first active automation trigger
            if (activeZones.isEmpty() && activeSchedules.isEmpty() && !prefManager.isAutomationActive()) {
                int currentMode = getCurrentRingerMode();
                prefManager.setOriginalRingerMode(currentMode);
                prefManager.setAutomationActive(true);
                Log.d(TAG, "Saved original sound mode: " + modeToString(currentMode));
            }

            // Add to active zones
            prefManager.addActiveZone(zoneId);
            prefManager.setActiveZoneName(zone.getZoneName());
            prefManager.setLastActivationTime(System.currentTimeMillis());

            int targetRingerMode = mapSoundModeToRingerMode(zone.getSoundMode());
            int prevMode = getCurrentRingerMode();

            // Apply sound mode
            boolean applied = applyRingerModeInternal(targetRingerMode);

            // Record history
            String newModeStr = applied ? modeToString(targetRingerMode) : modeToString(prevMode);
            SoundHistory history = new SoundHistory(
                    "Classroom Entered",
                    "LOCATION",
                    zone.getZoneName(),
                    modeToString(prevMode),
                    newModeStr,
                    System.currentTimeMillis()
            );
            db.soundHistoryDao().insert(history);

            // Show Notification if preferred
            if (zone.isEntryNotification() && prefManager.isNotificationEntryEnabled()) {
                NotificationHelper.showZoneEnteredNotification(context, zone.getZoneName(), newModeStr);
            }

            notifyStateChanged();
        });
    }

    /**
     * CASE 2 & CASE 7: User leaves a registered silent zone.
     */
    public void onGeofenceExit(int zoneId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            SilentZone zone = db.silentZoneDao().getZoneById(zoneId);
            String zoneName = (zone != null) ? zone.getZoneName() : ("Zone #" + zoneId);

            // Remove from active zones
            prefManager.removeActiveZone(zoneId);

            Set<String> remainingZones = prefManager.getActiveZones();
            Set<String> remainingSchedules = prefManager.getActiveSchedules();

            int prevMode = getCurrentRingerMode();

            // Check if another silent zone is still active
            if (!remainingZones.isEmpty()) {
                Log.d(TAG, "Still inside another silent zone. Evaluating remaining zones.");
                // Update active zone name to another active zone
                try {
                    String nextZoneId = remainingZones.iterator().next();
                    SilentZone nextZone = db.silentZoneDao().getZoneById(Integer.parseInt(nextZoneId));
                    if (nextZone != null) {
                        prefManager.setActiveZoneName(nextZone.getZoneName());
                    }
                } catch (Exception ignored) {}

                int strictestMode = evaluateStrictestMode(remainingZones, remainingSchedules);
                applyRingerModeInternal(strictestMode);
                notifyStateChanged();
                return;
            }

            // Check if an active schedule requires silence
            if (!remainingSchedules.isEmpty()) {
                Log.d(TAG, "An automated schedule is still active. Maintaining quiet mode.");
                prefManager.setActiveZoneName("");
                int strictestMode = evaluateStrictestMode(remainingZones, remainingSchedules);
                applyRingerModeInternal(strictestMode);
                notifyStateChanged();
                return;
            }

            // No active rules require silence -> Perform safe restoration
            if (prefManager.isAutomationActive() && (zone == null || zone.isExitRestoration())) {
                restoreSoundModeInternal("LOCATION", zoneName, prevMode);
            } else {
                prefManager.setActiveZoneName("");
                notifyStateChanged();
            }
        });
    }

    /**
     * Evaluates current GPS location against all enabled zones.
     */
    public void checkProximityToAllZones(Location currentLocation) {
        if (currentLocation == null || !prefManager.isAutomationEnabled()) return;
        if (prefManager.isManualOverrideActive()) return;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<SilentZone> zones = db.silentZoneDao().getEnabledZones();
            Set<String> currentlyActive = prefManager.getActiveZones();

            for (SilentZone zone : zones) {
                float[] results = new float[1];
                Location.distanceBetween(
                        currentLocation.getLatitude(), currentLocation.getLongitude(),
                        zone.getLatitude(), zone.getLongitude(),
                        results
                );
                float distance = results[0];
                boolean isInside = distance <= zone.getRadius();
                boolean wasInside = currentlyActive.contains(String.valueOf(zone.getId()));

                if (isInside && !wasInside) {
                    Log.d(TAG, "Proximity check: Entering zone " + zone.getZoneName() + " (dist=" + distance + "m)");
                    onGeofenceEnter(zone.getId());
                } else if (!isInside && wasInside) {
                    Log.d(TAG, "Proximity check: Exiting zone " + zone.getZoneName() + " (dist=" + distance + "m)");
                    onGeofenceExit(zone.getId());
                }
            }
        });
    }

    /**
     * Simulates entering or exiting a specific zone for testing and instant verification.
     */
    public void simulateZoneTransition(int zoneId, boolean enter) {
        if (enter) {
            onGeofenceEnter(zoneId);
        } else {
            onGeofenceExit(zoneId);
        }
    }

    /**
     * CASE 3: Schedule starts.
     */
    public void onScheduleStart(int scheduleId) {
        if (!prefManager.isAutomationEnabled()) {
            Log.d(TAG, "Automation is disabled globally. Ignoring schedule start for ID " + scheduleId);
            return;
        }

        if (prefManager.isManualOverrideActive()) {
            Log.d(TAG, "Manual override active. Skipping schedule trigger.");
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            SoundSchedule schedule = db.soundScheduleDao().getScheduleById(scheduleId);
            if (schedule == null || !schedule.isEnabled()) {
                return;
            }

            Set<String> activeZones = prefManager.getActiveZones();
            Set<String> activeSchedules = prefManager.getActiveSchedules();

            if (activeZones.isEmpty() && activeSchedules.isEmpty() && !prefManager.isAutomationActive()) {
                int currentMode = getCurrentRingerMode();
                prefManager.setOriginalRingerMode(currentMode);
                prefManager.setAutomationActive(true);
            }

            prefManager.addActiveSchedule(scheduleId);
            prefManager.setActiveScheduleName(schedule.getScheduleName());
            prefManager.setLastActivationTime(System.currentTimeMillis());

            int targetRingerMode = mapSoundModeToRingerMode(schedule.getSoundMode());
            int prevMode = getCurrentRingerMode();

            boolean applied = applyRingerModeInternal(targetRingerMode);
            String newModeStr = applied ? modeToString(targetRingerMode) : modeToString(prevMode);

            SoundHistory history = new SoundHistory(
                    "Schedule Activated",
                    "SCHEDULE",
                    schedule.getScheduleName(),
                    modeToString(prevMode),
                    newModeStr,
                    System.currentTimeMillis()
            );
            db.soundHistoryDao().insert(history);

            if (prefManager.isNotificationScheduleEnabled()) {
                NotificationHelper.showScheduleStartedNotification(context, schedule.getScheduleName(), newModeStr);
            }

            notifyStateChanged();
        });
    }

    /**
     * CASE 4: Schedule ends.
     */
    public void onScheduleEnd(int scheduleId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            SoundSchedule schedule = db.soundScheduleDao().getScheduleById(scheduleId);
            String scheduleName = (schedule != null) ? schedule.getScheduleName() : ("Schedule #" + scheduleId);

            prefManager.removeActiveSchedule(scheduleId);

            Set<String> remainingZones = prefManager.getActiveZones();
            Set<String> remainingSchedules = prefManager.getActiveSchedules();

            int prevMode = getCurrentRingerMode();

            if (!remainingZones.isEmpty() || !remainingSchedules.isEmpty()) {
                Log.d(TAG, "Other zones or schedules still active. Maintaining quiet profile.");
                int strictestMode = evaluateStrictestMode(remainingZones, remainingSchedules);
                applyRingerModeInternal(strictestMode);
                notifyStateChanged();
                return;
            }

            if (prefManager.isAutomationActive()) {
                restoreSoundModeInternal("SCHEDULE", scheduleName, prevMode);
            } else {
                prefManager.setActiveScheduleName("");
                notifyStateChanged();
            }
        });
    }

    /**
     * CASE 5: User manually changes sound mode.
     */
    public void applyManualSoundMode(int ringerMode, boolean enableTemporaryOverride, long overrideDurationMs) {
        int prevMode = getCurrentRingerMode();

        if (enableTemporaryOverride) {
            prefManager.setManualOverride(true, overrideDurationMs);
        } else {
            prefManager.setManualOverride(false, 0);
        }

        applyRingerModeInternal(ringerMode);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            SoundHistory history = new SoundHistory(
                    "Manual Sound Change",
                    "MANUAL",
                    "Dashboard Control",
                    modeToString(prevMode),
                    modeToString(ringerMode),
                    System.currentTimeMillis()
            );
            db.soundHistoryDao().insert(history);
            notifyStateChanged();
        });
    }

    /**
     * CASE 6: User toggles master automation ON/OFF.
     */
    public void setMasterAutomationEnabled(boolean enabled) {
        prefManager.setAutomationEnabled(enabled);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            int currentMode = getCurrentRingerMode();
            if (!enabled) {
                prefManager.setAutomationActive(false);
                prefManager.setActiveZones(java.util.Collections.emptySet());
                prefManager.setActiveSchedules(java.util.Collections.emptySet());
                prefManager.setActiveZoneName("");
                prefManager.setActiveScheduleName("");
                prefManager.setManualOverride(false, 0);

                SoundHistory history = new SoundHistory(
                        "Automation Disabled",
                        "MANUAL",
                        "Settings / Dashboard",
                        modeToString(currentMode),
                        modeToString(currentMode),
                        System.currentTimeMillis()
                );
                db.soundHistoryDao().insert(history);
            } else {
                SoundHistory history = new SoundHistory(
                        "Automation Enabled",
                        "MANUAL",
                        "Settings / Dashboard",
                        modeToString(currentMode),
                        modeToString(currentMode),
                        System.currentTimeMillis()
                );
                db.soundHistoryDao().insert(history);
            }
            notifyStateChanged();
        });
    }

    /**
     * Resumes automation immediately, clearing any manual overrides.
     */
    public void resumeAutomation() {
        prefManager.setManualOverride(false, 0);
        prefManager.setAutomationEnabled(true);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Set<String> remainingZones = prefManager.getActiveZones();
            Set<String> remainingSchedules = prefManager.getActiveSchedules();
            if (!remainingZones.isEmpty() || !remainingSchedules.isEmpty()) {
                int strictest = evaluateStrictestMode(remainingZones, remainingSchedules);
                applyRingerModeInternal(strictest);
            }
            notifyStateChanged();
        });
    }

    /**
     * Smart Sound Restoration logic.
     */
    private void restoreSoundModeInternal(String triggerSource, String triggerName, int prevMode) {
        int restoredMode = prefManager.getOriginalRingerMode();
        prefManager.setAutomationActive(false);
        prefManager.setLastRestorationTime(System.currentTimeMillis());
        prefManager.setActiveZoneName("");
        prefManager.setActiveScheduleName("");

        applyRingerModeInternal(restoredMode);

        String restoredModeStr = modeToString(restoredMode);
        SoundHistory history = new SoundHistory(
                "Sound Mode Restored",
                triggerSource,
                triggerName,
                modeToString(prevMode),
                restoredModeStr,
                System.currentTimeMillis()
        );
        db.soundHistoryDao().insert(history);

        if (triggerSource.equals("LOCATION") && prefManager.isNotificationExitEnabled()) {
            NotificationHelper.showZoneExitedNotification(context, triggerName, restoredModeStr);
        } else if (triggerSource.equals("SCHEDULE") && prefManager.isNotificationScheduleEnabled()) {
            NotificationHelper.showScheduleEndedNotification(context, triggerName, restoredModeStr);
        }

        notifyStateChanged();
    }

    /**
     * Safe internal ringer mode applier. Handles DND permissions and catches SecurityExceptions.
     */
    private boolean applyRingerModeInternal(int targetMode) {
        if (audioManager == null) return false;

        try {
            // Check DND access if switching to or from Silent Mode
            if (targetMode == AudioManager.RINGER_MODE_SILENT) {
                if (!PermissionManager.hasNotificationPolicyAccess(context)) {
                    Log.w(TAG, "Notification Policy Access not granted. Falling back to VIBRATE mode.");
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    NotificationHelper.showPermissionRequiredNotification(
                            context,
                            "Do Not Disturb Access Needed",
                            "Quitely needs DND access to enable full Silent Mode. Vibrate mode was applied instead."
                    );
                    return true;
                }
            }

            audioManager.setRingerMode(targetMode);

            // If restored to Normal, ensure ringer stream volume is audible
            if (targetMode == AudioManager.RINGER_MODE_NORMAL) {
                int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                if (currentVol == 0) {
                    int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, Math.max(1, maxVol / 2), 0);
                }
            }

            return true;
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException while changing ringer mode: " + se.getMessage());
            try {
                audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            } catch (Exception ignored) {}
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error changing ringer mode: " + e.getMessage());
            return false;
        }
    }

    private int evaluateStrictestMode(Set<String> zoneIds, Set<String> scheduleIds) {
        int strictest = AudioManager.RINGER_MODE_NORMAL;

        for (String zId : zoneIds) {
            try {
                SilentZone z = db.silentZoneDao().getZoneById(Integer.parseInt(zId));
                if (z != null && z.isEnabled()) {
                    int ringer = mapSoundModeToRingerMode(z.getSoundMode());
                    strictest = pickStricter(strictest, ringer);
                }
            } catch (Exception ignored) {}
        }

        for (String sId : scheduleIds) {
            try {
                SoundSchedule s = db.soundScheduleDao().getScheduleById(Integer.parseInt(sId));
                if (s != null && s.isEnabled()) {
                    int ringer = mapSoundModeToRingerMode(s.getSoundMode());
                    strictest = pickStricter(strictest, ringer);
                }
            } catch (Exception ignored) {}
        }

        return strictest;
    }

    private int pickStricter(int modeA, int modeB) {
        if (modeA == AudioManager.RINGER_MODE_SILENT || modeB == AudioManager.RINGER_MODE_SILENT) {
            return AudioManager.RINGER_MODE_SILENT;
        }
        if (modeA == AudioManager.RINGER_MODE_VIBRATE || modeB == AudioManager.RINGER_MODE_VIBRATE) {
            return AudioManager.RINGER_MODE_VIBRATE;
        }
        return AudioManager.RINGER_MODE_NORMAL;
    }

    public int getCurrentRingerMode() {
        if (audioManager != null) {
            return audioManager.getRingerMode();
        }
        return AudioManager.RINGER_MODE_NORMAL;
    }

    public static int mapSoundModeToRingerMode(int entitySoundMode) {
        switch (entitySoundMode) {
            case 0:
                return AudioManager.RINGER_MODE_SILENT;
            case 1:
                return AudioManager.RINGER_MODE_VIBRATE;
            case 2:
            default:
                return AudioManager.RINGER_MODE_NORMAL;
        }
    }

    public static String modeToString(int ringerMode) {
        switch (ringerMode) {
            case AudioManager.RINGER_MODE_SILENT:
                return "Silent";
            case AudioManager.RINGER_MODE_VIBRATE:
                return "Vibrate";
            case AudioManager.RINGER_MODE_NORMAL:
            default:
                return "Normal";
        }
    }
}
