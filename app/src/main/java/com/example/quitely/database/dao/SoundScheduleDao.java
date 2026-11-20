package com.example.quitely.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.quitely.database.entities.SoundSchedule;

import java.util.List;

@Dao
public interface SoundScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SoundSchedule schedule);

    @Update
    void update(SoundSchedule schedule);

    @Delete
    void delete(SoundSchedule schedule);

    @Query("DELETE FROM sound_schedules WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT * FROM sound_schedules ORDER BY startHour ASC, startMinute ASC")
    LiveData<List<SoundSchedule>> getAllSchedulesLive();

    @Query("SELECT * FROM sound_schedules ORDER BY startHour ASC, startMinute ASC")
    List<SoundSchedule> getAllSchedules();

    @Query("SELECT * FROM sound_schedules WHERE isEnabled = 1")
    List<SoundSchedule> getEnabledSchedules();

    @Query("SELECT * FROM sound_schedules WHERE id = :id LIMIT 1")
    SoundSchedule getScheduleById(int id);

    @Query("SELECT COUNT(*) FROM sound_schedules")
    int getScheduleCount();
}
