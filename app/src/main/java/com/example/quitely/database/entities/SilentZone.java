package com.example.quitely.database.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entity representing a geographic Silent Zone (e.g., Classroom, Library, Lab).
 */
@Entity(tableName = "silent_zones")
public class SilentZone {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String zoneName;
    private String zoneType; // Classroom, Library, Laboratory, Exam Hall, Auditorium, Meeting Room, Other
    private double latitude;
    private double longitude;
    private float radius; // in meters (e.g., 50, 100, 200, 500)
    private int soundMode; // 0 = Silent, 1 = Vibrate, 2 = Normal
    private boolean isEnabled;
    private boolean entryNotification;
    private boolean exitRestoration;

    // Transient runtime field to track whether the device is currently inside this zone
    @Ignore
    private boolean isInside;

    @Ignore
    private float distanceMeters = -1;

    public SilentZone() {
        this.isEnabled = true;
        this.entryNotification = true;
        this.exitRestoration = true;
        this.soundMode = 0; // default to Silent
        this.radius = 100.0f; // default 100m
        this.zoneType = "Classroom";
    }

    @Ignore
    public SilentZone(String zoneName, String zoneType, double latitude, double longitude, float radius, int soundMode, boolean isEnabled, boolean entryNotification, boolean exitRestoration) {
        this.zoneName = zoneName;
        this.zoneType = zoneType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.soundMode = soundMode;
        this.isEnabled = isEnabled;
        this.entryNotification = entryNotification;
        this.exitRestoration = exitRestoration;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getZoneType() {
        return zoneType;
    }

    public void setZoneType(String zoneType) {
        this.zoneType = zoneType;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public int getSoundMode() {
        return soundMode;
    }

    public void setSoundMode(int soundMode) {
        this.soundMode = soundMode;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public boolean isEntryNotification() {
        return entryNotification;
    }

    public void setEntryNotification(boolean entryNotification) {
        this.entryNotification = entryNotification;
    }

    public boolean isExitRestoration() {
        return exitRestoration;
    }

    public void setExitRestoration(boolean exitRestoration) {
        this.exitRestoration = exitRestoration;
    }

    public boolean isInside() {
        return isInside;
    }

    public void setInside(boolean inside) {
        isInside = inside;
    }

    public float getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(float distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public String getSoundModeLabel() {
        switch (soundMode) {
            case 0:
                return "Silent";
            case 1:
                return "Vibrate";
            case 2:
                return "Normal";
            default:
                return "Silent";
        }
    }
}
