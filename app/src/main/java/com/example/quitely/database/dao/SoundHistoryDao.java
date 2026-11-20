package com.example.quitely.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.quitely.database.entities.SoundHistory;

import java.util.List;

@Dao
public interface SoundHistoryDao {

    @Insert
    long insert(SoundHistory history);

    @Query("SELECT * FROM sound_history ORDER BY timestamp DESC")
    LiveData<List<SoundHistory>> getAllHistoryLive();

    @Query("SELECT * FROM sound_history ORDER BY timestamp DESC")
    List<SoundHistory> getAllHistory();

    @Query("SELECT * FROM sound_history ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<SoundHistory>> getRecentHistoryLive(int limit);

    @Query("SELECT * FROM sound_history WHERE triggerSource = :source ORDER BY timestamp DESC")
    LiveData<List<SoundHistory>> getHistoryBySourceLive(String source);

    @Query("SELECT * FROM sound_history ORDER BY timestamp DESC LIMIT 1")
    SoundHistory getLastHistory();

    @Query("DELETE FROM sound_history")
    void clearAll();
}
