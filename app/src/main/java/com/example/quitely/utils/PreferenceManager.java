package com.example.quitely.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages local user preferences, automation master state, and temporary restoration tracking.
 */
public class PreferenceManager {

    private static final String PREF_NAME = "quitely_preferences";
    private static volatile PreferenceManager INSTANCE;
    private final SharedPreferences prefs;

    // Keys
    private static final String KEY_AUTOMATION_ENABLED = "key_automation_enabled";
    private static final String KEY_ORIGINAL_RINGER_MODE = "key_original_ringer_mode";
    private static final String KEY_IS_AUTOMATION_ACTIVE = "key_is_automation_active";
    private static final String KEY_ACTIVE_ZONES = "key_active_zones";
    private static final String KEY_ACTIVE_SCHEDULES = "key_active_schedules";
    private static final String KEY_MANUAL_OVERRIDE_ACTIVE = "key_manual_override_active";
    private static final String KEY_MANUAL_OVERRIDE_EXPIRY = "key_manual_override_expiry";
    private static final String KEY_DEFAULT_RESTORATION_MODE = "key_default_restoration_mode"; // 2 for Normal
    private static final String KEY_NOTIF_ENTRY = "key_notif_entry";
    private static final String KEY_NOTIF_EXIT = "key_notif_exit";
    private static final String KEY_NOTIF_SCHEDULE = "key_notif_schedule";
    private static final String KEY_ONBOARDING_COMPLETED = "key_onboarding_completed";
    private static final String KEY_LAST_ACTIVATION_TIME = "key_last_activation_time";
    private static final String KEY_LAST_RESTORATION_TIME = "key_last_restoration_time";
    private static final String KEY_CURRENT_ACTIVE_ZONE_NAME = "key_current_active_zone_name";
    private static final String KEY_CURRENT_ACTIVE_SCHEDULE_NAME = "key_current_active_schedule_name";

    private PreferenceManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static PreferenceManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (PreferenceManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PreferenceManager(context);
                }
            }
        }
        return INSTANCE;
    }

    public boolean isAutomationEnabled() {
        return prefs.getBoolean(KEY_AUTOMATION_ENABLED, true);
    }

    public void setAutomationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTOMATION_ENABLED, enabled).apply();
    }

    public int getOriginalRingerMode() {
        return prefs.getInt(KEY_ORIGINAL_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL);
    }

    public void setOriginalRingerMode(int mode) {
        prefs.edit().putInt(KEY_ORIGINAL_RINGER_MODE, mode).apply();
    }

    public boolean isAutomationActive() {
        return prefs.getBoolean(KEY_IS_AUTOMATION_ACTIVE, false);
    }

    public void setAutomationActive(boolean active) {
        prefs.edit().putBoolean(KEY_IS_AUTOMATION_ACTIVE, active).apply();
    }

    public Set<String> getActiveZones() {
        return new HashSet<>(prefs.getStringSet(KEY_ACTIVE_ZONES, new HashSet<>()));
    }

    public void setActiveZones(Set<String> zones) {
        prefs.edit().putStringSet(KEY_ACTIVE_ZONES, new HashSet<>(zones)).apply();
    }

    public void addActiveZone(int zoneId) {
        Set<String> zones = getActiveZones();
        zones.add(String.valueOf(zoneId));
        setActiveZones(zones);
    }

    public void removeActiveZone(int zoneId) {
        Set<String> zones = getActiveZones();
        zones.remove(String.valueOf(zoneId));
        setActiveZones(zones);
    }

    public Set<String> getActiveSchedules() {
        return new HashSet<>(prefs.getStringSet(KEY_ACTIVE_SCHEDULES, new HashSet<>()));
    }

    public void setActiveSchedules(Set<String> schedules) {
        prefs.edit().putStringSet(KEY_ACTIVE_SCHEDULES, new HashSet<>(schedules)).apply();
    }

    public void addActiveSchedule(int scheduleId) {
        Set<String> schedules = getActiveSchedules();
        schedules.add(String.valueOf(scheduleId));
        setActiveSchedules(schedules);
    }

    public void removeActiveSchedule(int scheduleId) {
        Set<String> schedules = getActiveSchedules();
        schedules.remove(String.valueOf(scheduleId));
        setActiveSchedules(schedules);
    }

    public boolean isManualOverrideActive() {
        if (!prefs.getBoolean(KEY_MANUAL_OVERRIDE_ACTIVE, false)) {
            return false;
        }
        long expiry = prefs.getLong(KEY_MANUAL_OVERRIDE_EXPIRY, 0);
        if (expiry > 0 && System.currentTimeMillis() > expiry) {
            setManualOverride(false, 0);
            return false;
        }
        return true;
    }

    public void setManualOverride(boolean active, long durationMillis) {
        long expiry = (active && durationMillis > 0) ? (System.currentTimeMillis() + durationMillis) : 0;
        prefs.edit()
                .putBoolean(KEY_MANUAL_OVERRIDE_ACTIVE, active)
                .putLong(KEY_MANUAL_OVERRIDE_EXPIRY, expiry)
                .apply();
    }

    public int getDefaultRestorationMode() {
        return prefs.getInt(KEY_DEFAULT_RESTORATION_MODE, AudioManager.RINGER_MODE_NORMAL);
    }

    public void setDefaultRestorationMode(int mode) {
        prefs.edit().putInt(KEY_DEFAULT_RESTORATION_MODE, mode).apply();
    }

    public boolean isNotificationEntryEnabled() {
        return prefs.getBoolean(KEY_NOTIF_ENTRY, true);
    }

    public void setNotificationEntryEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_ENTRY, enabled).apply();
    }

    public boolean isNotificationExitEnabled() {
        return prefs.getBoolean(KEY_NOTIF_EXIT, true);
    }

    public void setNotificationExitEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_EXIT, enabled).apply();
    }

    public boolean isNotificationScheduleEnabled() {
        return prefs.getBoolean(KEY_NOTIF_SCHEDULE, true);
    }

    public void setNotificationScheduleEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_SCHEDULE, enabled).apply();
    }

    public boolean isOnboardingCompleted() {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public void setOnboardingCompleted(boolean completed) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply();
    }

    public long getLastActivationTime() {
        return prefs.getLong(KEY_LAST_ACTIVATION_TIME, 0);
    }

    public void setLastActivationTime(long time) {
        prefs.edit().putLong(KEY_LAST_ACTIVATION_TIME, time).apply();
    }

    public long getLastRestorationTime() {
        return prefs.getLong(KEY_LAST_RESTORATION_TIME, 0);
    }

    public void setLastRestorationTime(long time) {
        prefs.edit().putLong(KEY_LAST_RESTORATION_TIME, time).apply();
    }

    public String getActiveZoneName() {
        return prefs.getString(KEY_CURRENT_ACTIVE_ZONE_NAME, "");
    }

    public void setActiveZoneName(String name) {
        prefs.edit().putString(KEY_CURRENT_ACTIVE_ZONE_NAME, name).apply();
    }

    public String getActiveScheduleName() {
        return prefs.getString(KEY_CURRENT_ACTIVE_SCHEDULE_NAME, "");
    }

    public void setActiveScheduleName(String name) {
        prefs.edit().putString(KEY_CURRENT_ACTIVE_SCHEDULE_NAME, name).apply();
    }
}
