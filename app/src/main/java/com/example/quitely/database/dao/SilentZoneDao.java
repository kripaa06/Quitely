package com.example.quitely.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.quitely.database.entities.SilentZone;

import java.util.List;

@Dao
public interface SilentZoneDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SilentZone zone);

    @Update
    void update(SilentZone zone);

    @Delete
    void delete(SilentZone zone);

    @Query("DELETE FROM silent_zones WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT * FROM silent_zones ORDER BY id DESC")
    LiveData<List<SilentZone>> getAllZonesLive();

    @Query("SELECT * FROM silent_zones ORDER BY id DESC")
    List<SilentZone> getAllZones();

    @Query("SELECT * FROM silent_zones WHERE isEnabled = 1")
    List<SilentZone> getEnabledZones();

    @Query("SELECT * FROM silent_zones WHERE id = :id LIMIT 1")
    SilentZone getZoneById(int id);

    @Query("SELECT COUNT(*) FROM silent_zones")
    int getZoneCount();
}
