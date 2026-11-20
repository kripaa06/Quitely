package com.example.quitely.managers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.quitely.database.AppDatabase;
import com.example.quitely.database.entities.SoundSchedule;
import com.example.quitely.receivers.ScheduleReceiver;

import java.util.Calendar;
import java.util.List;

public class ScheduleManager {

    private static final String TAG = "ScheduleManager";
    public static final String ACTION_SCHEDULE_START = "com.example.quitely.ACTION_SCHEDULE_START";
    public static final String ACTION_SCHEDULE_END = "com.example.quitely.ACTION_SCHEDULE_END";
    public static final String EXTRA_SCHEDULE_ID = "extra_schedule_id";

    private static volatile ScheduleManager INSTANCE;
    private final Context context;
    private final AlarmManager alarmManager;

    private ScheduleManager(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
    }

    public static ScheduleManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ScheduleManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ScheduleManager(context);
                }
            }
        }
        return INSTANCE;
    }

    public void scheduleAlarmsForSchedule(SoundSchedule schedule) {
        if (!schedule.isEnabled() || alarmManager == null) {
            return;
        }

        long nextStartTime = calculateNextOccurrence(
                schedule.getStartHour(),
                schedule.getStartMinute(),
                schedule.getSelectedDays()
        );

        long nextEndTime;
        if (isCurrentlyActive(schedule)) {
            Log.d(TAG, "Current time is within active schedule window for: " + schedule.getScheduleName() + ". Activating immediately!");
            SoundAutomationManager.getInstance(context).onScheduleStart(schedule.getId());

            Calendar endCal = Calendar.getInstance();
            endCal.set(Calendar.HOUR_OF_DAY, schedule.getEndHour());
            endCal.set(Calendar.MINUTE, schedule.getEndMinute());
            endCal.set(Calendar.SECOND, 0);
            endCal.set(Calendar.MILLISECOND, 0);
            if (endCal.getTimeInMillis() <= System.currentTimeMillis()) {
                endCal.add(Calendar.DAY_OF_YEAR, 1);
            }
            nextEndTime = endCal.getTimeInMillis();
        } else {
            nextEndTime = calculateNextEndTime(
                    schedule.getStartHour(),
                    schedule.getStartMinute(),
                    schedule.getEndHour(),
                    schedule.getEndMinute(),
                    schedule.getSelectedDays(),
                    nextStartTime
            );
        }

        PendingIntent startPI = getStartPendingIntent(schedule.getId());
        PendingIntent endPI = getEndPendingIntent(schedule.getId());

        setExactAlarm(nextStartTime, startPI);
        setExactAlarm(nextEndTime, endPI);

        Log.d(TAG, "Scheduled alarms for: " + schedule.getScheduleName() +
                " -> Start: " + nextStartTime + ", End: " + nextEndTime);
    }

    public void cancelAlarmsForSchedule(int scheduleId) {
        if (alarmManager == null) return;
        PendingIntent startPI = getStartPendingIntent(scheduleId);
        PendingIntent endPI = getEndPendingIntent(scheduleId);
        alarmManager.cancel(startPI);
        alarmManager.cancel(endPI);

        // If this schedule was actively maintaining quiet mode, trigger onScheduleEnd to restore sound
        com.example.quitely.utils.PreferenceManager pref = com.example.quitely.utils.PreferenceManager.getInstance(context);
        if (pref.getActiveSchedules().contains(String.valueOf(scheduleId))) {
            SoundAutomationManager.getInstance(context).onScheduleEnd(scheduleId);
        }
    }

    public void reRegisterAllEnabledSchedules() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            List<SoundSchedule> schedules = db.soundScheduleDao().getEnabledSchedules();
            for (SoundSchedule schedule : schedules) {
                scheduleAlarmsForSchedule(schedule);
            }
        });
    }

    private void setExactAlarm(long triggerAtMillis, PendingIntent pendingIntent) {
        if (alarmManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    private PendingIntent getStartPendingIntent(int scheduleId) {
        Intent intent = new Intent(context, ScheduleReceiver.class);
        intent.setAction(ACTION_SCHEDULE_START);
        intent.putExtra(EXTRA_SCHEDULE_ID, scheduleId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, scheduleId * 2, intent, flags);
    }

    private PendingIntent getEndPendingIntent(int scheduleId) {
        Intent intent = new Intent(context, ScheduleReceiver.class);
        intent.setAction(ACTION_SCHEDULE_END);
        intent.putExtra(EXTRA_SCHEDULE_ID, scheduleId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, scheduleId * 2 + 1, intent, flags);
    }

    /**
     * Calculates the next calendar timestamp for a given hour:minute on one of the selectedDays.
     */
    public static long calculateNextOccurrence(int targetHour, int targetMinute, String selectedDays) {
        Calendar now = Calendar.getInstance();
        Calendar candidate = (Calendar) now.clone();
        candidate.set(Calendar.HOUR_OF_DAY, targetHour);
        candidate.set(Calendar.MINUTE, targetMinute);
        candidate.set(Calendar.SECOND, 0);
        candidate.set(Calendar.MILLISECOND, 0);

        // Check the next 14 days to find the earliest matching day
        for (int i = 0; i < 14; i++) {
            int dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK);
            if (isDaySelected(dayOfWeek, selectedDays)) {
                if (candidate.getTimeInMillis() > now.getTimeInMillis()) {
                    return candidate.getTimeInMillis();
                }
            }
            candidate.add(Calendar.DAY_OF_YEAR, 1);
        }

        return candidate.getTimeInMillis();
    }

    /**
     * Calculates the corresponding end time given start time, handling midnight crossings.
     */
    public static long calculateNextEndTime(int startHour, int startMinute, int endHour, int endMinute, String selectedDays, long startTime) {
        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(startTime);

        Calendar endCal = (Calendar) startCal.clone();
        endCal.set(Calendar.HOUR_OF_DAY, endHour);
        endCal.set(Calendar.MINUTE, endMinute);
        endCal.set(Calendar.SECOND, 0);
        endCal.set(Calendar.MILLISECOND, 0);

        // If end time is earlier in the day than start time, it crosses midnight into the next day
        if (endCal.getTimeInMillis() <= startCal.getTimeInMillis()) {
            endCal.add(Calendar.DAY_OF_YEAR, 1);
        }

        return endCal.getTimeInMillis();
    }

    public static boolean isDaySelected(int dayOfWeek, String selectedDays) {
        if (selectedDays == null || selectedDays.trim().isEmpty()) return true;
        String[] days = selectedDays.split(",");
        for (String d : days) {
            try {
                if (Integer.parseInt(d.trim()) == dayOfWeek) {
                    return true;
                }
            } catch (NumberFormatException ignored) {}
        }
        return false;
    }

    /**
     * Returns true if the device's current wall clock time is currently within an enabled schedule window.
     */
    public static boolean isCurrentlyActive(SoundSchedule schedule) {
        if (schedule == null || !schedule.isEnabled()) return false;
        Calendar now = Calendar.getInstance();
        int currentDay = now.get(Calendar.DAY_OF_WEEK);
        if (!isDaySelected(currentDay, schedule.getSelectedDays())) {
            return false;
        }

        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int startMinutes = schedule.getStartHour() * 60 + schedule.getStartMinute();
        int endMinutes = schedule.getEndHour() * 60 + schedule.getEndMinute();

        if (endMinutes > startMinutes) {
            return currentMinutes >= startMinutes && currentMinutes < endMinutes;
        } else if (endMinutes < startMinutes) {
            return currentMinutes >= startMinutes || currentMinutes < endMinutes;
        }
        return false;
    }
}
