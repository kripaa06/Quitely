package com.example.quitely.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.quitely.managers.SoundAutomationManager;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null");
            return;
        }

        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
            Log.e(TAG, "Geofencing error: " + errorMessage);
            return;
        }

        int transitionType = geofencingEvent.getGeofenceTransition();
        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

        if (triggeringGeofences == null || triggeringGeofences.isEmpty()) {
            Log.d(TAG, "No triggering geofences found in event");
            return;
        }

        SoundAutomationManager soundManager = SoundAutomationManager.getInstance(context);

        for (Geofence geofence : triggeringGeofences) {
            String requestId = geofence.getRequestId();
            try {
                int zoneId = Integer.parseInt(requestId);
                if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    Log.d(TAG, "Geofence ENTER triggered for zone ID: " + zoneId);
                    soundManager.onGeofenceEnter(zoneId);
                } else if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    Log.d(TAG, "Geofence EXIT triggered for zone ID: " + zoneId);
                    soundManager.onGeofenceExit(zoneId);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid requestId in geofence: " + requestId, e);
            }
        }
    }
}
