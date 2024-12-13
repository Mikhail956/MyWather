package com.example.mywather.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WeatherDao {
    @Insert
    void insert(SavedWeather weather);
    
    @Query("SELECT * FROM saved_weather WHERE cityId = :cityId ORDER BY timestamp DESC LIMIT 1")
    SavedWeather getLatestWeather(int cityId);
    
    @Query("SELECT * FROM saved_weather WHERE cityId = :cityId AND timestamp BETWEEN :startTime AND :endTime")
    List<SavedWeather> getWeatherForPeriod(int cityId, long startTime, long endTime);
} 