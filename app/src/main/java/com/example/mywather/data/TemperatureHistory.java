package com.example.mywather.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "temperature_history")
public class TemperatureHistory {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String cityName;
    public double temperature;
    public long timestamp;

    public TemperatureHistory(String cityName, double temperature, long timestamp) {
        this.cityName = cityName;
        this.temperature = temperature;
        this.timestamp = timestamp;
    }
} 