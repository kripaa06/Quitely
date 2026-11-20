package com.example.quitely.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.quitely.database.AppDatabase;
import com.example.quitely.database.entities.SoundSchedule;
import com.example.quitely.managers.ScheduleManager;
import com.example.quitely.managers.SoundAutomationManager;

public class ScheduleReceiver extends BroadcastReceiver {

    private static final String TAG = "ScheduleReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        int scheduleId = intent.getIntExtra(ScheduleManager.EXTRA_SCHEDULE_ID, -1);
        if (scheduleId == -1) return;

        SoundAutomationManager soundManager = SoundAutomationManager.getInstance(context);

        if (ScheduleManager.ACTION_SCHEDULE_START.equals(action)) {
            Log.d(TAG, "Schedule START received for ID: " + scheduleId);
            soundManager.onScheduleStart(scheduleId);

            // Re-schedule alarms for this schedule for the next cycle
            AppDatabase.databaseWriteExecutor.execute(() -> {
                SoundSchedule s = AppDatabase.getInstance(context).soundScheduleDao().getScheduleById(scheduleId);
                if (s != null && s.isEnabled()) {
                    ScheduleManager.getInstance(context).scheduleAlarmsForSchedule(s);
                }
            });

        } else if (ScheduleManager.ACTION_SCHEDULE_END.equals(action)) {
            Log.d(TAG, "Schedule END received for ID: " + scheduleId);
            soundManager.onScheduleEnd(scheduleId);

            // Re-schedule alarms for this schedule for the next cycle
            AppDatabase.databaseWriteExecutor.execute(() -> {
                SoundSchedule s = AppDatabase.getInstance(context).soundScheduleDao().getScheduleById(scheduleId);
                if (s != null && s.isEnabled()) {
                    ScheduleManager.getInstance(context).scheduleAlarmsForSchedule(s);
                }
            });
        }
    }
}
