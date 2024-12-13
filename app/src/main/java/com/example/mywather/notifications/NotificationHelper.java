package com.example.mywather.notifications;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.mywather.R;
import com.example.mywather.MainActivity;
import java.util.Calendar;
import android.util.Log;
import android.widget.Toast;
import java.util.Locale;
import android.provider.Settings;
import java.util.Date;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;

public class NotificationHelper {
    private static final String CHANNEL_ID = "weather_channel";
    private final Context context;
    private final NotificationManager notificationManager;
    private static final String PREFS_NAME = "WeatherNotificationPrefs";

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    public void scheduleNotification(String cityName, int dayOfWeek, int hour, int minute) {
        // Сохраняем настройки
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString("notification_city", cityName);
        editor.putInt("notification_day", dayOfWeek);
        editor.putInt("notification_hour", hour);
        editor.putInt("notification_minute", minute);
        editor.apply();

        Intent intent = new Intent(context, WeatherNotificationReceiver.class);
        intent.putExtra("cityName", cityName);
        
        // Отменяем предыдущее уведомление для этого города
        PendingIntent previousIntent = PendingIntent.getBroadcast(
            context, 
            cityName.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(previousIntent);

        // Создаем новое уведомление
        Calendar calendar = Calendar.getInstance();
        // Устанавливаем текущую дату: 12 декабря 2024 (четверг)
        calendar.set(2024, Calendar.DECEMBER, 12);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // Вычисляем разницу дней до выбранного дня недели
        int currentDay = calendar.get(Calendar.DAY_OF_WEEK);
        int daysUntilTarget = dayOfWeek - currentDay;
        if (daysUntilTarget <= 0) {
            daysUntilTarget += 7;
        }

        // Если выбранный день - сегодня и время уже прошло, добавляем неделю
        if (daysUntilTarget == 0 && calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            daysUntilTarget = 7;
        }

        calendar.add(Calendar.DAY_OF_YEAR, daysUntilTarget);

        // Создаем новый PendingIntent
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            cityName.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAlarmClock(
                    new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pendingIntent),
                    pendingIntent
                );
            } else {
                Intent settingsIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(settingsIntent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
            );
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
            );
        }

        // Показываем подтверждение с датой следующего уведомления
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        String dateStr = new java.text.SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            .format(calendar.getTime());
        
        Toast.makeText(context, 
            "Уведомление установлено на " + dateStr + " " + timeStr, 
            Toast.LENGTH_LONG).show();
        
        // Логируем для отладки
        Log.d("NotificationHelper", "Scheduled notification for " + cityName + 
            " at " + dateStr + " " + timeStr);
    }

    public void scheduleNotification(String cityName, Calendar date) {
        Intent intent = new Intent(context, WeatherNotificationReceiver.class);
        intent.putExtra("cityName", cityName);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            cityName.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            date.getTimeInMillis(),
            pendingIntent
        );
    }

    public void showNotification(String title, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Настраиваем звук уведомления
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle()
            .setBigContentTitle("Погода в " + title)
            .bigText(message);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications) // Убедитесь, что эта иконка существует
            .setContentTitle("Погода в " + title)
            .setContentText(message)
            .setStyle(bigTextStyle)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX) // Максимальный приоритет
            .setDefaults(Notification.DEFAULT_ALL)
            .setSound(defaultSoundUri)
            .setVibrate(new long[]{1000, 1000}) // Вибрация
            .setLights(context.getResources().getColor(R.color.blue_500, null), 3000, 3000)
            .setContentIntent(pendingIntent)
            .setColor(context.getResources().getColor(R.color.blue_500, null));

        // Используем уникальный ID для каждого уведомления
        int notificationId = (title + System.currentTimeMillis()).hashCode();
        notificationManager.notify(notificationId, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Weather Notifications",
                NotificationManager.IMPORTANCE_HIGH
            );
            
            // Настраиваем канал
            channel.setDescription("Уведомления о погоде");
            channel.enableLights(true);
            channel.setLightColor(context.getResources().getColor(R.color.blue_500, null));
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{1000, 1000});
            channel.setShowBadge(true);
            channel.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            );
            
            notificationManager.createNotificationChannel(channel);
        }
    }
} 