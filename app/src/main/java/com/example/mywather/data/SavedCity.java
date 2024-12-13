package com.example.mywather.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cities")
public class SavedCity {
    @PrimaryKey
    public int id;
    
    public String name;
    
    public double latitude;
    public double longitude;
    
    public boolean isHome;
    public boolean isTracked;
    
    public String timeZone;
    public String region;
    public int population;

    public SavedCity(int id, String name, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isHome = false;
        this.isTracked = false;
        this.population = 0;
    }

    public void setHome(boolean isHome) {
        this.isHome = isHome;
    }
} 