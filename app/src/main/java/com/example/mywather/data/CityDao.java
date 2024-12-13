package com.example.mywather.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CityDao {
    @Query("SELECT * FROM cities")
    List<SavedCity> getAllCities();
    
    @Query("SELECT * FROM cities WHERE isHome = 1 LIMIT 1")
    SavedCity getHomeCity();
    
    @Query("SELECT * FROM cities WHERE id = :cityId")
    SavedCity getCityById(int cityId);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SavedCity city);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SavedCity> cities);
    
    @Update
    void update(SavedCity city);
    
    @Delete
    void delete(SavedCity city);
    
    @Query("DELETE FROM cities")
    void deleteAll();
    
    @Query("SELECT * FROM cities WHERE name LIKE :searchQuery || '%'")
    List<SavedCity> searchCities(String searchQuery);
} 