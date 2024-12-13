package com.example.mywather.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    private static final String PREFS_NAME = "WeatherNotificationPrefs";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String cityName = prefs.getString("notification_city", null);
            int dayOfWeek = prefs.getInt("notification_day", -1);
            int hour = prefs.getInt("notification_hour", -1);
            int minute = prefs.getInt("notification_minute", -1);

            if (cityName != null && dayOfWeek != -1 && hour != -1 && minute != -1) {
                NotificationHelper notificationHelper = new NotificationHelper(context);
                notificationHelper.scheduleNotification(cityName, dayOfWeek, hour, minute);
            }
        }
    }
} 