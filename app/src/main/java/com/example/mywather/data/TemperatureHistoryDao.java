package com.example.mywather.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TemperatureHistoryDao {
    @Insert
    void insert(TemperatureHistory history);

    @Query("SELECT * FROM temperature_history WHERE cityName = :cityName AND timestamp > :dayStart ORDER BY timestamp DESC")
    List<TemperatureHistory> getHistoryForCity(String cityName, long dayStart);

    @Query("DELETE FROM temperature_history WHERE timestamp < :timestamp")
    void deleteOldRecords(long timestamp);
} 