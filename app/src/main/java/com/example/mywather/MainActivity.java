package com.example.mywather;

import android.os.Bundle;
import android.widget.SearchView;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import android.widget.Toast;
import com.android.volley.DefaultRetryPolicy;
import static com.example.mywather.Constants.API_KEY;
import static com.example.mywather.Constants.BASE_URL;
import android.util.Log;
import android.app.TimePickerDialog;
import java.util.Calendar;
import androidx.recyclerview.widget.DividerItemDecoration;
import com.android.volley.NetworkError;
import com.android.volley.TimeoutError;
import com.android.volley.NoConnectionError;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.net.Uri;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.core.widget.NestedScrollView;
import android.media.AudioAttributes;
import android.graphics.Color;
import android.app.Notification;
import android.media.RingtoneManager;
import android.content.SharedPreferences;
import android.app.AlarmManager;
import androidx.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Locale;
import android.content.ActivityNotFoundException;
import android.os.Environment;
import java.io.FileWriter;
import java.io.IOException;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.mywather.data.WeatherDatabase;
import com.example.mywather.data.CityDao;
import com.example.mywather.data.SavedCity;
import com.example.mywather.data.TemperatureHistory;
import com.example.mywather.data.TemperatureHistoryDao;
import com.example.mywather.location.LocationManager;
import com.example.mywather.notifications.NotificationSettingsDialog;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private RecyclerView citiesRecyclerView;
    private WeatherAdapter weatherAdapter;
    private List<WeatherData> weatherDataList;
    private RequestQueue requestQueue;
    private boolean isDarkTheme = false;
    private boolean isCelsius = true;
    private LocationManager locationManager;
    private static final int NOTIFICATION_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Устанавливаем тему до setContentView
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        isDarkTheme = prefs.getBoolean("isDarkTheme", false);
        isCelsius = prefs.getBoolean("isCelsius", true);
        
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        
        setContentView(R.layout.activity_main);
        
        // Инициализируем LocationManager
        locationManager = new LocationManager(this);
        
        // Иници��лизируем RequestQueue
        requestQueue = Volley.newRequestQueue(this);

        // Инициализируем основные компоненты
        initializeViews();
        setupRecyclerView();
        setupSearchView();
        setupThemeButton();
        setupNotificationButton();
        setupSwipeRefresh();

        // Загружаем города по умолчанию
        loadDefaultCities();

        // Запрашиваем разрешения
        requestNotificationPermissions();
        createNotificationChannel();

        // Добавляем обработчик для кнопки переключения единиц измерения
        ImageButton unitToggleButton = findViewById(R.id.unitToggleButton);
        unitToggleButton.setOnClickListener(v -> toggleTemperatureUnit());
    }

    private void initializeViews() {
        citiesRecyclerView = findViewById(R.id.citiesRecyclerView);
        weatherDataList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        citiesRecyclerView.setLayoutManager(layoutManager);
        
        // Инициализируем список, если он null
        if (weatherDataList == null) {
            weatherDataList = new ArrayList<>();
        }
        
        // Инициализируем адаптер с реализацией всех методов интерфейса
        weatherAdapter = new WeatherAdapter(weatherDataList, new WeatherAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(WeatherData weatherData) {
                WeatherDetailDialog dialog = new WeatherDetailDialog(weatherData);
                dialog.show(getSupportFragmentManager(), "weather_detail");
            }

            @Override
            public void onLocationClick(WeatherData weatherData) {
                String uri = String.format("geo:0,0?q=%s", Uri.encode(weatherData.cityName));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(mapIntent);
            }

            @Override
            public void onTemperatureClick(WeatherData weatherData) {
                showTemperatureHistory(weatherData.cityName);
            }

            @Override
            public void onDownloadClick(WeatherData weatherData) {
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Скачать историю")
                    .setMessage("Скачать историю погоды за последние 24 часа для города " + weatherData.cityName + "?")
                    .setPositiveButton("Скачать", (dialog, which) -> {
                        downloadWeatherHistory(weatherData.cityName);
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
            }
        });
        
        citiesRecyclerView.setAdapter(weatherAdapter);

        // Добавляем обработчик нажатия на кнопку локации
        findViewById(R.id.locationButton).setOnClickListener(v -> {
            // Координаты Челябинска
            if (requestQueue != null) {
                fetchWeatherByLocation(55.1644, 61.4368);
            }
        });

        // Добавляем разделитель между элементами
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                citiesRecyclerView.getContext(),
                layoutManager.getOrientation()
        );
        citiesRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    private void setupSearchView() {
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                weatherDataList.clear();
                weatherAdapter.notifyDataSetChanged();
                fetchWeatherData(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void setupThemeButton() {
        findViewById(R.id.themeButton).setOnClickListener(v -> toggleTheme());
    }

    private void setupNotificationButton() {
        findViewById(R.id.notificationButton).setOnClickListener(v -> {
            NotificationSettingsDialog dialog = new NotificationSettingsDialog();
            dialog.show(getSupportFragmentManager(), "notification_settings");
        });
    }

    private void loadDefaultCities() {
        String[] cities = {"Челябинск", "Москва", "Санкт-Петербург", "Екатеринбург", "Казань"};
        
        for (String city : cities) {
            fetchWeatherData(city);
        }
    }

    private void fetchWeatherData(String city) {
        String url = String.format("%sweather?q=%s&appid=%s&units=metric&lang=ru",
                BASE_URL, Uri.encode(city), API_KEY);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        WeatherData weatherData = parseWeatherData(response);
                        runOnUiThread(() -> updateWeatherList(weatherData));
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing data for " + city, e);
                        showError("Ошибка при загрузке данных для " + city);
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching data for " + city, error);
                    showError("Ошибка при загрузке данных для " + city);
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                2,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    private void updateWeatherList(WeatherData newData) {
        if (weatherDataList != null && weatherAdapter != null) {
            // Сохраняем температурную историю
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                WeatherDatabase db = WeatherDatabase.getDatabase(this);
                TemperatureHistoryDao historyDao = db.temperatureHistoryDao();
                
                // Сохраняем текущую температуру
                TemperatureHistory history = new TemperatureHistory(
                    newData.cityName,
                    newData.temperature,
                    System.currentTimeMillis()
                );
                historyDao.insert(history);
                
                // Удаляем старые записи (старше 7 дней)
                long weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
                historyDao.deleteOldRecords(weekAgo);
            });
            executor.shutdown();

            // Обновляем UI
            for (int i = 0; i < weatherDataList.size(); i++) {
                if (weatherDataList.get(i).cityName.equals(newData.cityName)) {
                    weatherDataList.set(i, newData);
                    weatherAdapter.notifyItemChanged(i);
                    return;
                }
            }

            weatherDataList.add(newData);
            weatherAdapter.notifyItemInserted(weatherDataList.size() - 1);
        }
    }

    private void showError(String message) {
        Log.e(TAG, "Error: " + message);
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
        editor.putBoolean("isDarkTheme", isDarkTheme);
        editor.apply();
        
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        
        // Пересоздаем активность для применения темы
        recreate();
    }

    private void toggleTemperatureUnit() {
        isCelsius = !isCelsius;
        
        // Сохраняем выбор пользователя
        SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
        editor.putBoolean("isCelsius", isCelsius);
        editor.apply();
        
        // Обновляем отображение температуры для всех городов
        if (weatherDataList != null) {
            for (WeatherData data : weatherDataList) {
                data.setTemperatureUnit(isCelsius);
            }
            weatherAdapter.notifyDataSetChanged();
        }
        
        // Показываем уведомление о смене единиц измерения
        String unit = isCelsius ? "°C" : "°F";
        Toast.makeText(this, 
            "Температура теперь отображается в " + unit, 
            Toast.LENGTH_SHORT).show();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.CHANNEL_ID,
                    Constants.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(Constants.CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            channel.setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
            );
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setBypassDnd(true);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void requestNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, 
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.SCHEDULE_EXACT_ALARM,
                        Manifest.permission.USE_EXACT_ALARM,
                        Manifest.permission.WAKE_LOCK,
                        Manifest.permission.VIBRATE
                    },
                    NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Уведомления включены", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, 
                    "Для получения уведомлений о погоде необходимо разрешение", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupSwipeRefresh() {
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Очищаем кеш при обновлении
            requestQueue.getCache().clear();
            clearAppCache();

            weatherDataList.clear();
            weatherAdapter.notifyDataSetChanged();
            loadDefaultCitiesParallel();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void clearAppCache() {
        try {
            // Очищаем кеш приложения
            File cacheDir = getCacheDir();
            File appDir = new File(cacheDir.getParent());
            if (appDir.exists()) {
                String[] children = appDir.list();
                for (String s : children) {
                    if (!s.equals("lib")) {
                        deleteDir(new File(appDir, s));
                    }
                }
            }
            // Очищаем внешний кеш если он есть
            if (getExternalCacheDir() != null) {
                deleteDir(getExternalCacheDir());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir != null && dir.delete();
    }

    private void loadDefaultCitiesParallel() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            WeatherDatabase db = WeatherDatabase.getDatabase(this);
            CityDao cityDao = db.cityDao();

            // Список крупных российских городов (170)
            List<SavedCity> defaultCities = Arrays.asList(
                // Города-миллионники (15)
                new SavedCity(1, "Москва", 55.7558, 37.6173),
                new SavedCity(2, "Санкт-Петербург", 59.9343, 30.3351),
                new SavedCity(3, "Новосибирск", 55.0084, 82.9357),
                new SavedCity(4, "Екатеринбург", 56.8389, 60.6057),
                new SavedCity(5, "Казань", 55.7887, 49.1221),
                new SavedCity(6, "Нижний Новгород", 56.2965, 43.9361),
                new SavedCity(7, "Челябинск", 55.1644, 61.4368),
                new SavedCity(8, "Самара", 53.1959, 50.1001),
                new SavedCity(9, "Омск", 54.9885, 73.3242),
                new SavedCity(10, "Ростов-на-Дону", 47.2357, 39.7015),
                new SavedCity(11, "Уфа", 54.7431, 55.9678),
                new SavedCity(12, "Красноярск", 56.0090, 92.8725),
                new SavedCity(13, "Воронеж", 51.6720, 39.1843),
                new SavedCity(14, "Пермь", 58.0105, 56.2502),
                new SavedCity(15, "Волгоград", 48.7194, 44.5018),

                // Крупные города (500k-1M) (21)
                new SavedCity(16, "Краснодар", 45.0448, 38.9760),
                new SavedCity(17, "Саратов", 51.5330, 46.0344),
                new SavedCity(18, "Тюмень", 57.1522, 65.5272),
                new SavedCity(19, "Тольятти", 53.5303, 49.3461),
                new SavedCity(20, "Ижевск", 56.8498, 53.2045),
                new SavedCity(21, "Барнаул", 53.3548, 83.7697),
                new SavedCity(22, "Ульяновск", 54.3187, 48.3978),
                new SavedCity(23, "Иркутск", 52.2978, 104.2964),
                new SavedCity(24, "Хабаровск", 48.4827, 135.0846),
                new SavedCity(25, "Ярославль", 57.6299, 39.8737),
                new SavedCity(26, "Владивосток", 43.1198, 131.8869),
                new SavedCity(27, "Махачкала", 42.9849, 47.5047),
                new SavedCity(28, "Томск", 56.4977, 84.9744),
                new SavedCity(29, "Оренбург", 51.7727, 55.1007),
                new SavedCity(30, "Кемерово", 55.3333, 86.0833),
                new SavedCity(31, "Новокузнецк", 53.7557, 87.1099),
                new SavedCity(32, "Рязань", 54.6269, 39.6916),
                new SavedCity(33, "Астрахань", 46.3497, 48.0408),
                new SavedCity(34, "Набережные Челны", 55.7437, 52.3959),
                new SavedCity(35, "Пенза", 53.1959, 45.0144),
                new SavedCity(36, "Киров", 58.6035, 49.6668),

                // Города 250k-500k (40)
                new SavedCity(37, "Липецк", 52.6031, 39.5708),
                new SavedCity(38, "Чебоксары", 56.1322, 47.2519),
                new SavedCity(39, "Тула", 54.1961, 37.6182),
                new SavedCity(40, "Калининград", 54.7065, 20.5109),
                new SavedCity(41, "Курск", 51.7373, 36.1874),
                new SavedCity(42, "Севастополь", 44.6166, 33.5254),
                new SavedCity(43, "Сочи", 43.5992, 39.7257),
                new SavedCity(44, "Ставрополь", 45.0428, 41.9734),
                new SavedCity(45, "Улан-Удэ", 51.8334, 107.5841),
                new SavedCity(46, "Тверь", 56.8587, 35.9176),
                new SavedCity(47, "Магнитогорск", 53.4186, 58.9725),
                new SavedCity(48, "Иваново", 56.9994, 40.9728),
                new SavedCity(49, "Брянск", 53.2521, 34.3717),
                new SavedCity(50, "Белгород", 50.5977, 36.5858),
                new SavedCity(51, "Владимир", 56.1366, 40.3966),
                new SavedCity(52, "Архангельск", 64.5401, 40.5433),
                new SavedCity(53, "Алуга", 54.5293, 36.2754),
                new SavedCity(54, "Смоленск", 54.7818, 32.0401),
                new SavedCity(55, "Курган", 55.4649, 65.3053),
                new SavedCity(56, "Чита", 52.0340, 113.4994),
                
                // Города 100k-250k (первая часть)
                new SavedCity(57, "Орёл", 52.9668, 36.0624),
                new SavedCity(58, "Волжский", 48.7867, 44.7516),
                new SavedCity(59, "Череповец", 59.1223, 37.9092),
                new SavedCity(60, "Владикавз", 43.0205, 44.6819),
                new SavedCity(61, "Якутск", 62.0355, 129.6754),
                new SavedCity(62, "Саранск", 54.1838, 45.1749),
                new SavedCity(63, "Мурманск", 68.9585, 33.0827),
                new SavedCity(64, "Подольск", 55.4312, 37.5447),
                new SavedCity(65, "Тамбов", 52.7317, 41.4433),
                new SavedCity(66, "Грозный", 43.3179, 45.6981),
                new SavedCity(67, "Стерлитамак", 53.6302, 55.9315),
                new SavedCity(68, "Петрозаводск", 61.7849, 34.3469),
                new SavedCity(69, "Кострома", 57.7665, 40.9269),
                new SavedCity(70, "Нижневартовск", 60.9397, 76.5696),
                new SavedCity(71, "Новороссийск", 44.7239, 37.7689),
                new SavedCity(72, "Йошкар-Ола", 56.6343, 47.8998),
                new SavedCity(73, "Комсомольск-на-Амуре", 50.5503, 137.0079),
                new SavedCity(74, "Таганрог", 47.2362, 38.8969),
                new SavedCity(75, "Сыктывкар", 61.6688, 50.8366),
                new SavedCity(76, "Нальчик", 43.4981, 43.6189),

                // Города 100k-250k (продолжение)
                new SavedCity(77, "Дзержинск", 56.2376, 43.4599),
                new SavedCity(78, "Братск", 56.1325, 101.6142),
                new SavedCity(79, "Энгельс", 51.4853, 46.1265),
                new SavedCity(80, "Ангарск", 52.5448, 103.8885),
                new SavedCity(81, "Благовещенск", 50.2785, 127.5391),
                new SavedCity(82, "Великий Новгород", 58.5213, 31.2718),
                new SavedCity(83, "Старый Оскол", 51.2967, 37.8349),
                new SavedCity(84, "Королёв", 55.9226, 37.8540),
                new SavedCity(85, "Псков", 57.8136, 28.3496),
                new SavedCity(86, "Мытищи", 55.9104, 37.7329),
                new SavedCity(87, "Бийск", 52.5394, 85.2072),
                new SavedCity(88, "Люберцы", 55.6767, 37.8929),
                new SavedCity(89, "Прокопьевск", 53.8604, 86.7102),
                new SavedCity(90, "Южно-Сахалинск", 46.9641, 142.7285),
                new SavedCity(91, "Балашиха", 55.7963, 37.9382),
                new SavedCity(92, "Рыбноск", 58.0446, 38.8427),
                new SavedCity(93, "Армавир", 44.9892, 41.1234),
                new SavedCity(94, "Абакан", 53.7156, 91.4292),
                new SavedCity(95, "Северодвинск", 64.5635, 39.8302),
                new SavedCity(96, "Петропавловск-Камчатский", 53.0370, 158.6559),
                new SavedCity(97, "Норильск", 69.3535, 88.2027),
                new SavedCity(98, "Сызрань", 53.1557, 48.4745),
                new SavedCity(99, "Волгодонск", 47.5147, 42.1539),
                new SavedCity(100, "Новочеркасск", 47.4220, 40.0930),
                new SavedCity(101, "Златоуст", 55.1719, 59.6508),
                new SavedCity(102, "Уссурийск", 43.7972, 131.9517),
                new SavedCity(103, "Электросталь", 55.7847, 38.4447),
                new SavedCity(104, "Салават", 53.3616, 55.9245),
                new SavedCity(105, "Находка", 42.8138, 132.8735),
                new SavedCity(106, "Альметьевск", 54.9014, 52.2970),
                new SavedCity(107, "Рубцовск", 51.5012, 81.2078),
                new SavedCity(108, "Копейск", 55.1167, 61.6178),
                new SavedCity(109, "Птигорск", 44.0486, 43.0594),
                new SavedCity(110, "Красногорск", 55.8318, 37.3295),
                new SavedCity(111, "Майкоп", 44.6098, 40.1006),
                new SavedCity(112, "Коломна", 55.1030, 38.7531),
                new SavedCity(113, "Одинцово", 55.6789, 37.2636),
                new SavedCity(114, "Ковров", 56.3574, 41.3169),
                new SavedCity(115, "Хасавюрт", 43.2509, 46.5865),
                new SavedCity(116, "Кисловодск", 43.9133, 42.7208),
                new SavedCity(117, "Серпухов", 54.9137, 37.4146),
                new SavedCity(118, "Новомосковск", 54.0105, 38.2846),
                new SavedCity(119, "Нефтекамск", 56.0921, 54.2722),
                new SavedCity(120, "Новочебоксарск", 56.1099, 47.4791),
                new SavedCity(121, "Нефтеюганск", 61.0998, 72.6035),
                new SavedCity(122, "Первоуральск", 56.9081, 59.9429),
                new SavedCity(123, "Щёлково", 55.9217, 37.9714),
                new SavedCity(124, "Дербент", 42.0578, 48.2889),
                new SavedCity(125, "Орехово-Зуево", 55.8040, 38.9796),
                new SavedCity(126, "Каменск-Уральский", 56.4185, 61.9329),
                new SavedCity(127, "Новый Уренгой", 66.0833, 76.6333),
                new SavedCity(128, "Батайск", 47.1397, 39.7518),
                new SavedCity(129, "Новошахтинск", 47.7576, 39.9375),
                new SavedCity(130, "Северск", 56.6031, 84.8864),
                new SavedCity(131, "Домодедово", 55.4369, 37.7676),
                new SavedCity(132, "Ленинск-Кузнецкий", 54.6674, 86.1796),
                new SavedCity(133, "Окябрьский", 54.4815, 53.4710),
                new SavedCity(134, "Химки", 55.8970, 37.4296),
                new SavedCity(135, "Муром", 55.5785, 42.0523),
                new SavedCity(136, "Обнинск", 55.0943, 36.6121),
                new SavedCity(137, "Березники", 59.4091, 56.8204),
                new SavedCity(138, "Реутов", 55.7600, 37.8558),
                new SavedCity(139, "Пушкино", 56.0104, 37.8471),
                new SavedCity(140, "Киселёвск", 54.0060, 86.6400),
                new SavedCity(141, "Ачинск", 56.2694, 90.4993),
                new SavedCity(142, "Арзамас", 55.3947, 43.8407),
                new SavedCity(143, "Елец", 52.6216, 38.5043),
                new SavedCity(144, "Новокуйбышевск", 53.0995, 49.9477),
                new SavedCity(145, "Ногинск", 55.8565, 38.4419),
                new SavedCity(146, "Сергиев Посад", 56.3153, 38.1358),
                new SavedCity(147, "Бердск", 54.7583, 83.1072),
                new SavedCity(148, "Ухта", 63.5565, 53.7014),
                new SavedCity(149, "Междуреченск", 53.6865, 88.0703),
                new SavedCity(150, "Великие Луки", 56.3439, 30.5233),
                new SavedCity(151, "Мичуринск", 52.8969, 40.4907),
                new SavedCity(152, "Железнодорожный", 55.7471, 38.0076),
                new SavedCity(153, "Магадан", 59.5619, 150.8083),
                new SavedCity(154, "Глазов", 58.1401, 52.6587),
                new SavedCity(155, "Невинномыск", 44.6380, 41.9428),
                new SavedCity(156, "Назрань", 43.2257, 44.7516),
                new SavedCity(157, "Кызыл", 51.7147, 94.4534),
                new SavedCity(158, "Камышин", 50.0652, 45.4142),
                new SavedCity(159, "Уссурийск", 43.7972, 131.9517),
                new SavedCity(160, "Новотроицк", 51.1965, 58.3017),
                new SavedCity(161, "Жуковский", 55.5952, 38.1197),
                new SavedCity(162, "Северодвинск", 64.5635, 39.8302),
                new SavedCity(163, "Димитровград", 54.2168, 49.6261),
                new SavedCity(164, "Губкин", 51.2833, 37.5500),
                new SavedCity(165, "Евпатория", 45.1900, 33.3668),
                new SavedCity(166, "Бузулук", 52.7881, 52.2623),
                new SavedCity(167, "Буйнакск", 42.8213, 47.1172),
                new SavedCity(168, "Прохладный", 43.7589, 44.0103),
                new SavedCity(169, "Саров", 54.9227, 43.3447),
                new SavedCity(170, "Белово", 54.4165, 86.3054)
            );

            // Сохранение городов в базу данных
            for (SavedCity city : defaultCities) {
                try {
                    cityDao.insert(city);
                } catch (Exception e) {
                    Log.e("MainActivity", "Error inserting city: " + city.name, e);
                }
            }

            // Установка Москвы как домашнего города по умолчанию
            SavedCity moscow = defaultCities.get(0);
            moscow.setHome(true);
            try {
                cityDao.update(moscow);
            } catch (Exception e) {
                Log.e("MainActivity", "Error setting Moscow as home city", e);
            }
        });
        executor.shutdown();
    }

    private void fetchWeatherByLocation(double lat, double lon) {
        String url = String.format("%sweather?lat=%f&lon=%f&appid=%s&units=metric&lang=ru",
                BASE_URL, lat, lon, API_KEY);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        WeatherData weatherData = parseWeatherData(response);
                        runOnUiThread(() -> {
                            // Очищаем список безопасно
                            if (weatherDataList != null) {
                                weatherDataList.clear();
                                weatherAdapter.notifyDataSetChanged();
                                // Добавляем новые данные
                                weatherDataList.add(weatherData);
                                weatherAdapter.notifyItemInserted(0);
                            }
                        });
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing location data", e);
                        showError("Ошибка при загрузке данных для вашей локации");
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching location data", error);
                    showError("Ошибка при загрузке данных для вашей локации");
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                2,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    private WeatherData parseWeatherData(JSONObject response) throws JSONException {
        String cityName = response.getString("name");
        
        JSONObject main = response.getJSONObject("main");
        double temperature = main.getDouble("temp");
        int humidity = main.getInt("humidity");
        
        JSONObject wind = response.getJSONObject("wind");
        double windSpeed = wind.getDouble("speed");
        
        JSONArray weatherArray = response.getJSONArray("weather");
        JSONObject weather = weatherArray.getJSONObject(0);
        String description = weather.getString("description");
        String iconCode = weather.getString("icon");
        
        JSONObject coord = response.getJSONObject("coord");
        double lat = coord.getDouble("lat");
        double lon = coord.getDouble("lon");
        
        return new WeatherData(
            cityName,
            temperature,
            humidity,
            windSpeed,
            description,
            iconCode,
            lat,
            lon
        );
    }

    // Метод для получения истории температур города
    private void showTemperatureHistory(String cityName) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            WeatherDatabase db = WeatherDatabase.getDatabase(this);
            TemperatureHistoryDao historyDao = db.temperatureHistoryDao();
            
            // Получаем данные за последние 24 часа
            long dayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
            List<TemperatureHistory> history = historyDao.getHistoryForCity(cityName, dayAgo);
            
            runOnUiThread(() -> {
                // Показываем диалог с историей
                StringBuilder message = new StringBuilder();
                message.append("История температур за 24 часа:\n\n");
                
                for (TemperatureHistory record : history) {
                    String time = new java.text.SimpleDateFormat("HH:mm")
                        .format(new java.util.Date(record.timestamp));
                    message.append(String.format("%s: %.0f°C\n", time, record.temperature));
                }
                
                new AlertDialog.Builder(this)
                    .setTitle(cityName)
                    .setMessage(message.toString())
                    .setPositiveButton("OK", null)
                    .show();
            });
        });
        executor.shutdown();
    }

    private void downloadWeatherHistory(String cityName) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            WeatherDatabase db = WeatherDatabase.getDatabase(this);
            TemperatureHistoryDao historyDao = db.temperatureHistoryDao();
            
            // Получаем данные за последние 24 часа
            long dayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
            List<TemperatureHistory> history = historyDao.getHistoryForCity(cityName, dayAgo);
            
            // Создаем файл с историей
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File historyFile = new File(downloadsDir, cityName + "_weather_history.txt");
            
            try {
                FileWriter writer = new FileWriter(historyFile);
                writer.write("История погоды для города " + cityName + "\n\n");
                
                for (TemperatureHistory record : history) {
                    String time = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm")
                        .format(new java.util.Date(record.timestamp));
                    writer.write(String.format("%s: %.1f°C\n", time, record.temperature));
                }
                
                writer.close();
                
                runOnUiThread(() -> {
                    Toast.makeText(this, 
                        "История сохранена в " + historyFile.getPath(), 
                        Toast.LENGTH_LONG).show();
                });
            } catch (IOException e) {
                Log.e(TAG, "Error saving history", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, 
                        "Ошибка при сохранении истории", 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
        executor.shutdown();
    }
} 