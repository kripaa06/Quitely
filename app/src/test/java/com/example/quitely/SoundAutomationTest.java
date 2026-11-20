package com.example.quitely;

import android.media.AudioManager;

import com.example.quitely.database.entities.SilentZone;
import com.example.quitely.database.entities.SoundSchedule;
import com.example.quitely.managers.ScheduleManager;
import com.example.quitely.managers.SoundAutomationManager;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SoundAutomationTest {

    @Test
    public void testSoundModeMapping() {
        // Entity mode 0 -> AudioManager.RINGER_MODE_SILENT (0)
        assertEquals(AudioManager.RINGER_MODE_SILENT, SoundAutomationManager.mapSoundModeToRingerMode(0));

        // Entity mode 1 -> AudioManager.RINGER_MODE_VIBRATE (1)
        assertEquals(AudioManager.RINGER_MODE_VIBRATE, SoundAutomationManager.mapSoundModeToRingerMode(1));

        // Entity mode 2 -> AudioManager.RINGER_MODE_NORMAL (2)
        assertEquals(AudioManager.RINGER_MODE_NORMAL, SoundAutomationManager.mapSoundModeToRingerMode(2));
    }

    @Test
    public void testSoundModeString() {
        assertEquals("Silent", SoundAutomationManager.modeToString(AudioManager.RINGER_MODE_SILENT));
        assertEquals("Vibrate", SoundAutomationManager.modeToString(AudioManager.RINGER_MODE_VIBRATE));
        assertEquals("Normal", SoundAutomationManager.modeToString(AudioManager.RINGER_MODE_NORMAL));
    }

    @Test
    public void testScheduleDaySelection() {
        String weekdays = "2,3,4,5,6"; // Mon - Fri
        assertTrue(ScheduleManager.isDaySelected(Calendar.MONDAY, weekdays));
        assertTrue(ScheduleManager.isDaySelected(Calendar.FRIDAY, weekdays));
        assertFalse(ScheduleManager.isDaySelected(Calendar.SUNDAY, weekdays));
        assertFalse(ScheduleManager.isDaySelected(Calendar.SATURDAY, weekdays));
    }

    @Test
    public void testScheduleAcrossMidnightCalculation() {
        // Schedule starts at 22:00 (10 PM) and ends at 06:00 (6 AM)
        Calendar start = Calendar.getInstance();
        start.set(2026, Calendar.NOVEMBER, 19, 22, 0, 0);
        long startTime = start.getTimeInMillis();

        long endTime = ScheduleManager.calculateNextEndTime(22, 0, 6, 0, "2,3,4,5,6", startTime);

        assertTrue("End time must be after start time even when crossing midnight", endTime > startTime);

        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(endTime);
        assertEquals(6, endCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, endCal.get(Calendar.MINUTE));
    }

    @Test
    public void testSilentZoneEntityDefaults() {
        SilentZone zone = new SilentZone();
        assertTrue(zone.isEnabled());
        assertTrue(zone.isEntryNotification());
        assertTrue(zone.isExitRestoration());
        assertEquals(0, zone.getSoundMode());
        assertEquals("Silent", zone.getSoundModeLabel());
    }

    @Test
    public void testSoundScheduleEntityFormatting() {
        SoundSchedule schedule = new SoundSchedule();
        schedule.setScheduleName("Lectures");
        schedule.setStartHour(9);
        schedule.setStartMinute(30);
        schedule.setEndHour(16);
        schedule.setEndMinute(45);
        schedule.setSelectedDays("2,3,4,5,6");

        assertEquals("09:30 AM", schedule.getFormattedStartTime());
        assertEquals("04:45 PM", schedule.getFormattedEndTime());
        assertEquals("Mon – Fri", schedule.getDaysDisplayString());
    }
}
