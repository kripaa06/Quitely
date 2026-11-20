package com.example.quitely.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.quitely.managers.GeofenceManager;
import com.example.quitely.managers.ScheduleManager;
import com.example.quitely.notifications.NotificationHelper;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        Log.d(TAG, "Device booted or package replaced: " + action);

        NotificationHelper.createNotificationChannels(context);

        // Re-register all geofences and alarm schedules
        GeofenceManager.getInstance(context).reRegisterAllEnabledGeofences();
        ScheduleManager.getInstance(context).reRegisterAllEnabledSchedules();
    }
}
