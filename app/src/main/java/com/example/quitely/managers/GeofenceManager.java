package com.example.quitely.managers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.quitely.database.AppDatabase;
import com.example.quitely.database.entities.SilentZone;
import com.example.quitely.receivers.GeofenceBroadcastReceiver;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.Collections;
import java.util.List;

public class GeofenceManager {

    private static final String TAG = "GeofenceManager";
    private static volatile GeofenceManager INSTANCE;

    private final Context context;
    private final GeofencingClient geofencingClient;
    private final FusedLocationProviderClient fusedLocationClient;
    private PendingIntent geofencePendingIntent;
    private LocationCallback locationCallback;
    private boolean isMonitoringProximity = false;

    private GeofenceManager(Context context) {
        this.context = context.getApplicationContext();
        this.geofencingClient = LocationServices.getGeofencingClient(this.context);
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context);
    }

    public static GeofenceManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (GeofenceManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GeofenceManager(context);
                }
            }
        }
        return INSTANCE;
    }

    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        geofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);
        return geofencePendingIntent;
    }

    public void addGeofence(SilentZone zone, OnGeofenceActionListener listener) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot add geofence: ACCESS_FINE_LOCATION not granted");
            if (listener != null) listener.onError("Location permission not granted");
            return;
        }

        float radius = Math.max(zone.getRadius(), 30.0f);

        Geofence geofence = new Geofence.Builder()
                .setRequestId(String.valueOf(zone.getId()))
                .setCircularRegion(zone.getLatitude(), zone.getLongitude(), radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        try {
            geofencingClient.addGeofences(request, getGeofencePendingIntent())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Successfully registered geofence with OS for zone: " + zone.getZoneName());
                        if (listener != null) listener.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to register geofence with OS for zone: " + zone.getZoneName(), e);
                        if (listener != null) listener.onError(e.getMessage());
                    });
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException registering geofence: " + se.getMessage());
            if (listener != null) listener.onError(se.getMessage());
        }

        // IMMEDIATELY evaluate if the user is already inside this zone right now!
        checkProximityImmediately(zone);
    }

    public void removeGeofence(int zoneId, OnGeofenceActionListener listener) {
        try {
            geofencingClient.removeGeofences(Collections.singletonList(String.valueOf(zoneId)))
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Removed geofence for zoneId: " + zoneId);
                        if (listener != null) listener.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to remove geofence for zoneId: " + zoneId, e);
                        if (listener != null) listener.onError(e.getMessage());
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error removing geofence: " + e.getMessage());
        }
    }

    public void reRegisterAllEnabledGeofences() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            List<SilentZone> enabledZones = db.silentZoneDao().getEnabledZones();
            for (SilentZone zone : enabledZones) {
                addGeofence(zone, null);
            }
        });
    }

    /**
     * Checks if current GPS coordinates are inside a specific zone right now and triggers automation.
     */
    @SuppressLint("MissingPermission")
    public void checkProximityImmediately(SilentZone zone) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        evaluateZoneProximity(zone, location);
                    } else {
                        // Fallback to last known location
                        fusedLocationClient.getLastLocation().addOnSuccessListener(lastLoc -> {
                            if (lastLoc != null) {
                                evaluateZoneProximity(zone, lastLoc);
                            }
                        });
                    }
                });
    }

    private void evaluateZoneProximity(SilentZone zone, Location location) {
        if (zone == null || !zone.isEnabled() || location == null) return;

        float[] results = new float[1];
        Location.distanceBetween(
                location.getLatitude(), location.getLongitude(),
                zone.getLatitude(), zone.getLongitude(),
                results
        );
        float distanceMeters = results[0];
        Log.d(TAG, "Zone: " + zone.getZoneName() + ", Distance: " + distanceMeters + "m, Radius: " + zone.getRadius() + "m");

        if (distanceMeters <= zone.getRadius()) {
            Log.d(TAG, "User is INSIDE zone: " + zone.getZoneName() + "! Triggering automation.");
            SoundAutomationManager.getInstance(context).onGeofenceEnter(zone.getId());
        }
    }

    /**
     * Evaluates current GPS location against all enabled zones.
     */
    @SuppressLint("MissingPermission")
    public void checkAllZonesProximity() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        SoundAutomationManager.getInstance(context).checkProximityToAllZones(location);
                    }
                });
    }

    /**
     * Starts continuous proximity monitoring when app is active.
     */
    @SuppressLint("MissingPermission")
    public void startProximityMonitoring() {
        if (isMonitoringProximity || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15000)
                .setMinUpdateIntervalMillis(8000)
                .setMinUpdateDistanceMeters(15.0f)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || locationResult.getLastLocation() == null) return;
                SoundAutomationManager.getInstance(context).checkProximityToAllZones(locationResult.getLastLocation());
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
            isMonitoringProximity = true;
            Log.d(TAG, "Active proximity monitoring started");
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException starting location updates: " + se.getMessage());
        }
    }

    public void stopProximityMonitoring() {
        if (isMonitoringProximity && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isMonitoringProximity = false;
            Log.d(TAG, "Active proximity monitoring stopped");
        }
    }

    public interface OnGeofenceActionListener {
        void onSuccess();
        void onError(String error);
    }
}
