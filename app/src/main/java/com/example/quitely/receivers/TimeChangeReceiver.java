package com.example.quitely.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.quitely.managers.ScheduleManager;

public class TimeChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "TimeChangeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        Log.d(TAG, "Time or timezone changed: " + intent.getAction());
        ScheduleManager.getInstance(context).reRegisterAllEnabledSchedules();
    }
}
