package com.example.quitely.database.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Locale;

/**
 * Entity representing an automated sound schedule.
 */
@Entity(tableName = "sound_schedules")
public class SoundSchedule {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String scheduleName;
    private int startHour;   // 0-23
    private int startMinute; // 0-59
    private int endHour;     // 0-23
    private int endMinute;   // 0-59
    private String selectedDays; // Comma-separated day integers (Calendar constants: 1=Sun, 2=Mon, ... 7=Sat)
    private int soundMode;   // 0 = Silent, 1 = Vibrate
    private boolean isEnabled;

    public SoundSchedule() {
        this.isEnabled = true;
        this.soundMode = 0;
        this.selectedDays = "2,3,4,5,6"; // Mon-Fri by default
    }

    @Ignore
    public SoundSchedule(String scheduleName, int startHour, int startMinute, int endHour, int endMinute, String selectedDays, int soundMode, boolean isEnabled) {
        this.scheduleName = scheduleName;
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.endHour = endHour;
        this.endMinute = endMinute;
        this.selectedDays = selectedDays;
        this.soundMode = soundMode;
        this.isEnabled = isEnabled;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getScheduleName() {
        return scheduleName;
    }

    public void setScheduleName(String scheduleName) {
        this.scheduleName = scheduleName;
    }

    public int getStartHour() {
        return startHour;
    }

    public void setStartHour(int startHour) {
        this.startHour = startHour;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public void setStartMinute(int startMinute) {
        this.startMinute = startMinute;
    }

    public int getEndHour() {
        return endHour;
    }

    public void setEndHour(int endHour) {
        this.endHour = endHour;
    }

    public int getEndMinute() {
        return endMinute;
    }

    public void setEndMinute(int endMinute) {
        this.endMinute = endMinute;
    }

    public String getSelectedDays() {
        return selectedDays;
    }

    public void setSelectedDays(String selectedDays) {
        this.selectedDays = selectedDays;
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

    public String getFormattedStartTime() {
        return formatTime(startHour, startMinute);
    }

    public String getFormattedEndTime() {
        return formatTime(endHour, endMinute);
    }

    private String formatTime(int hour, int minute) {
        String period = (hour >= 12) ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, period);
    }

    public String getDaysDisplayString() {
        if (selectedDays == null || selectedDays.trim().isEmpty()) {
            return "No days";
        }
        if (selectedDays.equals("2,3,4,5,6")) {
            return "Mon – Fri";
        }
        if (selectedDays.equals("1,7")) {
            return "Weekends";
        }
        if (selectedDays.equals("1,2,3,4,5,6,7") || selectedDays.split(",").length == 7) {
            return "Every day";
        }
        StringBuilder sb = new StringBuilder();
        String[] days = selectedDays.split(",");
        for (int i = 0; i < days.length; i++) {
            try {
                int day = Integer.parseInt(days[i].trim());
                if (i > 0) sb.append(", ");
                switch (day) {
                    case 1: sb.append("Sun"); break;
                    case 2: sb.append("Mon"); break;
                    case 3: sb.append("Tue"); break;
                    case 4: sb.append("Wed"); break;
                    case 5: sb.append("Thu"); break;
                    case 6: sb.append("Fri"); break;
                    case 7: sb.append("Sat"); break;
                }
            } catch (NumberFormatException ignored) {}
        }
        return sb.toString();
    }

    public String getSoundModeLabel() {
        return (soundMode == 0) ? "Silent" : "Vibrate";
    }
}
