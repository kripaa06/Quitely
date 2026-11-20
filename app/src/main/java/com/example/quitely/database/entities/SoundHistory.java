package com.example.quitely.database.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Entity representing an activity history log entry.
 */
@Entity(tableName = "sound_history")
public class SoundHistory {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String eventType;
    private String triggerSource; // LOCATION, SCHEDULE, MANUAL, SYSTEM
    private String zoneOrScheduleName;
    private String previousMode;
    private String newMode;
    private long timestamp;

    public SoundHistory() {
        this.timestamp = System.currentTimeMillis();
    }

    @Ignore
    public SoundHistory(String eventType, String triggerSource, String zoneOrScheduleName, String previousMode, String newMode, long timestamp) {
        this.eventType = eventType;
        this.triggerSource = triggerSource;
        this.zoneOrScheduleName = zoneOrScheduleName;
        this.previousMode = previousMode;
        this.newMode = newMode;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }

    public String getZoneOrScheduleName() {
        return zoneOrScheduleName;
    }

    public void setZoneOrScheduleName(String zoneOrScheduleName) {
        this.zoneOrScheduleName = zoneOrScheduleName;
    }

    public String getPreviousMode() {
        return previousMode;
    }

    public void setPreviousMode(String previousMode) {
        this.previousMode = previousMode;
    }

    public String getNewMode() {
        return newMode;
    }

    public void setNewMode(String newMode) {
        this.newMode = newMode;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
