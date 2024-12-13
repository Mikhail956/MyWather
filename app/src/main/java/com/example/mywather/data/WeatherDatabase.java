package com.example.mywather.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
    entities = {
        SavedCity.class,
        SavedWeather.class,
        TemperatureHistory.class
    },
    version = 4,
    exportSchema = false
)
public abstract class WeatherDatabase extends RoomDatabase {
    public abstract CityDao cityDao();
    public abstract WeatherDao weatherDao();
    public abstract TemperatureHistoryDao temperatureHistoryDao();

    private static volatile WeatherDatabase INSTANCE;

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Пустая миграция, так как структура не менялась
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Пересоздаем таблицы с новой схемой
            database.execSQL("DROP TABLE IF EXISTS temperature_history");
            database.execSQL("DROP TABLE IF EXISTS cities");
            database.execSQL("DROP TABLE IF EXISTS weather");
            
            // Создаем таблицы заново
            database.execSQL("CREATE TABLE IF NOT EXISTS `cities` (`id` INTEGER NOT NULL, "
                    + "`name` TEXT, `lat` REAL NOT NULL, `lon` REAL NOT NULL, "
                    + "`isHome` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))");
            
            database.execSQL("CREATE TABLE IF NOT EXISTS `temperature_history` "
                    + "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`cityName` TEXT, `temperature` REAL NOT NULL, "
                    + "`timestamp` INTEGER NOT NULL)");
            
            database.execSQL("CREATE TABLE IF NOT EXISTS `weather` "
                    + "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`cityId` INTEGER NOT NULL, `temperature` REAL NOT NULL, "
                    + "`humidity` INTEGER NOT NULL, `description` TEXT, "
                    + "`timestamp` INTEGER NOT NULL)");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Создаем временную таблицу с новой схемой для cities
            database.execSQL("CREATE TABLE IF NOT EXISTS `cities_new` ("
                    + "`id` INTEGER NOT NULL PRIMARY KEY, "
                    + "`name` TEXT, "
                    + "`latitude` REAL NOT NULL, "
                    + "`longitude` REAL NOT NULL, "
                    + "`isHome` INTEGER NOT NULL DEFAULT 0, "
                    + "`isTracked` INTEGER NOT NULL DEFAULT 0, "
                    + "`timeZone` TEXT, "
                    + "`region` TEXT, "
                    + "`population` INTEGER NOT NULL DEFAULT 0)");

            // Копируем данные из старой таблицы в новую
            database.execSQL("INSERT INTO cities_new (id, name, latitude, longitude, isHome) "
                    + "SELECT id, name, lat, lon, isHome FROM cities");

            // Удаляем старую таблицу
            database.execSQL("DROP TABLE cities");

            // Переименовываем новую таблицу
            database.execSQL("ALTER TABLE cities_new RENAME TO cities");

            // Создаем таблицу saved_weather
            database.execSQL("DROP TABLE IF EXISTS saved_weather");
            database.execSQL("CREATE TABLE IF NOT EXISTS `saved_weather` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`cityId` INTEGER NOT NULL, "
                    + "`temperature` REAL NOT NULL, "
                    + "`humidity` INTEGER NOT NULL, "
                    + "`windSpeed` REAL NOT NULL, "
                    + "`description` TEXT, "
                    + "`icon` TEXT, "
                    + "`timestamp` INTEGER NOT NULL)");

            // Обновляем temperature_history
            database.execSQL("DROP TABLE IF EXISTS temperature_history");
            database.execSQL("CREATE TABLE IF NOT EXISTS `temperature_history` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`cityName` TEXT, "
                    + "`temperature` REAL NOT NULL, "
                    + "`timestamp` INTEGER NOT NULL)");

            // Обновляем weather
            database.execSQL("DROP TABLE IF EXISTS weather");
            database.execSQL("CREATE TABLE IF NOT EXISTS `weather` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`cityId` INTEGER NOT NULL, "
                    + "`temperature` REAL NOT NULL, "
                    + "`humidity` INTEGER NOT NULL, "
                    + "`description` TEXT, "
                    + "`timestamp` INTEGER NOT NULL)");
        }
    };

    public static WeatherDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (WeatherDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        WeatherDatabase.class, 
                        "weather_database"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
} 