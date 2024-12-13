package com.example.mywather.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "forecast_data")
public class ForecastData {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String cityName;
    public long date;
    public double tempMin;
    public double tempMax;
    public int humidity;
    public double windSpeed;
    public String description;
    public String iconCode;
    public long timestamp;

    public ForecastData(String cityName, long date, double tempMin, double tempMax, 
                       int humidity, double windSpeed, String description, 
                       String iconCode, long timestamp) {
        this.cityName = cityName;
        this.date = date;
        this.tempMin = tempMin;
        this.tempMax = tempMax;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.description = description;
        this.iconCode = iconCode;
        this.timestamp = timestamp;
    }
} 