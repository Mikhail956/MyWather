package com.example.mywather.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "saved_weather")
public class SavedWeather {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int cityId;
    public double temperature;
    public int humidity;
    public double windSpeed;
    public String description;
    public String icon;
    public long timestamp;

    public SavedWeather(int cityId, double temperature, int humidity, 
                       double windSpeed, String description, String icon) {
        this.cityId = cityId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.description = description;
        this.icon = icon;
        this.timestamp = System.currentTimeMillis();
    }
} 