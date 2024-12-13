package com.example.mywather;

import java.util.Locale;

public class WeatherData {
    public String cityName;
    public double temperature;
    public int humidity;
    public double windSpeed;
    public String description;
    public String iconCode;
    public double latitude;
    public double longitude;
    private boolean isCelsius = true;

    public WeatherData(String cityName, double temperature, int humidity, 
                      double windSpeed, String description, String iconCode,
                      double latitude, double longitude) {
        this.cityName = cityName;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.description = description;
        this.iconCode = iconCode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void setTemperatureUnit(boolean celsius) {
        if (this.isCelsius != celsius) {
            this.isCelsius = celsius;
            if (!celsius) {
                // Конвертируем в Фаренгейты
                this.temperature = (this.temperature * 9/5) + 32;
            } else {
                // Конвертируем обратно в Цельсии
                this.temperature = (this.temperature - 32) * 5/9;
            }
        }
    }

    public String getTemperatureFormatted() {
        return String.format(Locale.getDefault(), "%.0f°%s",
            temperature, isCelsius ? "C" : "F");
    }

    public String getHumidityFormatted() {
        return humidity + "%";
    }

    public String getWindSpeedFormatted() {
        return String.format("%.1f м/с", windSpeed);
    }

    public String getIconUrl() {
        return String.format("https://openweathermap.org/img/wn/%s@2x.png", iconCode);
    }
}