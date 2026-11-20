package com.example.quitely.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.quitely.database.dao.SilentZoneDao;
import com.example.quitely.database.dao.SoundHistoryDao;
import com.example.quitely.database.dao.SoundScheduleDao;
import com.example.quitely.database.entities.SilentZone;
import com.example.quitely.database.entities.SoundHistory;
import com.example.quitely.database.entities.SoundSchedule;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {SilentZone.class, SoundSchedule.class, SoundHistory.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public abstract SilentZoneDao silentZoneDao();
    public abstract SoundScheduleDao soundScheduleDao();
    public abstract SoundHistoryDao soundHistoryDao();

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "quitely_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
