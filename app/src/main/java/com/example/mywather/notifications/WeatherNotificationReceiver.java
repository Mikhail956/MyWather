package com.example.mywather.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import android.net.Uri;
import static com.example.mywather.Constants.API_KEY;
import static com.example.mywather.Constants.BASE_URL;

public class WeatherNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "WeatherNotification";

    @Override
    public void onReceive(Context context, Intent intent) {
        String cityName = intent.getStringExtra("cityName");
        Log.d("WeatherNotification", "Received notification request for city: " + cityName);
        
        if (cityName == null) {
            Log.e("WeatherNotification", "City name is null!");
            return;
        }

        // Создаем URL для запроса погоды
        String url = String.format("%sweather?q=%s&appid=%s&units=metric&lang=ru",
                BASE_URL, Uri.encode(cityName), API_KEY);
        
        Log.d("WeatherNotification", "Requesting weather data from: " + url);

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d("WeatherNotification", "Received weather data: " + response.toString());
                        
                        JSONObject main = response.getJSONObject("main");
                        double temperature = main.getDouble("temp");
                        int humidity = main.getInt("humidity");

                        String message = String.format("Температура: %.0f°C\nВлажность: %d%%",
                                temperature, humidity);

                        Log.d("WeatherNotification", "Showing notification with message: " + message);
                        
                        NotificationHelper notificationHelper = new NotificationHelper(context);
                        notificationHelper.showNotification(cityName, message);

                    } catch (JSONException e) {
                        Log.e("WeatherNotification", "Error parsing weather data", e);
                    }
                },
                error -> Log.e("WeatherNotification", "Error fetching weather data: " + error.toString()));

        requestQueue.add(request);
    }

    private void rescheduleNotification(Context context, String cityName) {
        // Получаем сохраненные настройки уведомлений для города
        android.content.SharedPreferences prefs = 
            context.getSharedPreferences("WeatherNotificationPrefs", Context.MODE_PRIVATE);
        
        int dayOfWeek = prefs.getInt("notification_day", -1);
        int hour = prefs.getInt("notification_hour", -1);
        int minute = prefs.getInt("notification_minute", -1);

        if (dayOfWeek != -1 && hour != -1 && minute != -1) {
            NotificationHelper notificationHelper = new NotificationHelper(context);
            notificationHelper.scheduleNotification(cityName, dayOfWeek, hour, minute);
        }
    }
} 