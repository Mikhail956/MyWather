package com.example.mywather;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.util.Log;
import android.content.Context;
import com.bumptech.glide.Glide;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageButton;

public class WeatherAdapter extends RecyclerView.Adapter<WeatherAdapter.ViewHolder> {
    private List<WeatherData> weatherDataList;
    private OnItemClickListener listener;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(WeatherData weatherData);
        void onLocationClick(WeatherData weatherData);
        void onTemperatureClick(WeatherData weatherData);
        void onDownloadClick(WeatherData weatherData);
    }

    public WeatherAdapter(List<WeatherData> weatherDataList, OnItemClickListener listener) {
        this.weatherDataList = weatherDataList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_weather, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeatherData weatherData = weatherDataList.get(position);
        
        // Устанавливаем данные
        holder.cityNameText.setText(weatherData.cityName);
        holder.temperatureText.setText(weatherData.getTemperatureFormatted());
        
        // Загружаем иконку погоды
        Glide.with(context)
                .load(weatherData.getIconUrl())
                .into(holder.weatherIcon);

        // Устанавливаем слушатели кликов
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(weatherData);
            }
        });

        // Обра��отчик нажатия на иконку локации
        holder.locationIcon.setOnClickListener(v -> {
            if (listener != null) {
                String uri = String.format("geo:0,0?q=%s", Uri.encode(weatherData.cityName));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                context.startActivity(mapIntent);
            }
        });

        holder.temperatureText.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTemperatureClick(weatherData);
            }
        });

        holder.downloadButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDownloadClick(weatherData);
            }
        });
    }

    @Override
    public int getItemCount() {
        return weatherDataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView cityNameText;
        TextView temperatureText;
        ImageView weatherIcon;
        ImageView locationIcon;
        ImageButton downloadButton;

        ViewHolder(View itemView) {
            super(itemView);
            cityNameText = itemView.findViewById(R.id.cityNameText);
            temperatureText = itemView.findViewById(R.id.temperatureText);
            weatherIcon = itemView.findViewById(R.id.weatherIcon);
            locationIcon = itemView.findViewById(R.id.locationIcon);
            downloadButton = itemView.findViewById(R.id.downloadButton);
        }
    }
}