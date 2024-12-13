package com.example.mywather.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.example.mywather.data.SavedCity;
import java.util.Calendar;

public class WeatherNotificationManager {
    private final Context context;
    private final NotificationManager notificationManager;
    private static final String CHANNEL_ID = "weather_channel";

    public WeatherNotificationManager(Context context) {
        this.context = context;
        notificationManager = (NotificationManager) 
            context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Weather Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Channel for weather updates");
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void scheduleNotification(SavedCity city, Calendar calendar) {
        // Schedule notification logic
    }
} 