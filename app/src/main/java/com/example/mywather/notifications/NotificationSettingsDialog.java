package com.example.mywather.notifications;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.example.mywather.R;
import com.example.mywather.data.WeatherDatabase;
import com.example.mywather.data.SavedCity;
import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationSettingsDialog extends DialogFragment {
    private NotificationHelper notificationHelper;
    private Spinner citySpinner;
    private Spinner daySpinner;
    private TimePicker timePicker;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_notification_settings, null);

        notificationHelper = new NotificationHelper(requireContext());

        // Инициализируем компоненты
        citySpinner = view.findViewById(R.id.citySpinner);
        daySpinner = view.findViewById(R.id.daySpinner);
        timePicker = view.findViewById(R.id.timePicker);

        // Настраиваем спиннер с днями недели
        ArrayAdapter<CharSequence> daysAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.notification_days,
            android.R.layout.simple_spinner_item
        );
        daysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        daySpinner.setAdapter(daysAdapter);

        // Добавляем стандартные города
        List<String> defaultCities = new ArrayList<>();
        defaultCities.add("Челябинск");
        defaultCities.add("Москва");
        defaultCities.add("Санкт-Петербург");
        defaultCities.add("Екатеринбург");
        defaultCities.add("Казань");
        defaultCities.add("Новосибирск");
        defaultCities.add("Нижний Новгород");
        defaultCities.add("Самара");
        defaultCities.add("Омск");
        defaultCities.add("Ростов-на-Дону");

        // Создаем адаптер для списка городов
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            defaultCities
        );
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        citySpinner.setAdapter(cityAdapter);

        builder.setView(view)
            .setTitle("Настройка уведомлений")
            .setPositiveButton("Сохранить", (dialog, id) -> {
                String selectedCity = citySpinner.getSelectedItem().toString();
                int dayOfWeek = daySpinner.getSelectedItemPosition() + Calendar.SUNDAY;
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();

                notificationHelper.scheduleNotification(selectedCity, dayOfWeek, hour, minute);
            })
            .setNegativeButton("Отмена", null);

        return builder.create();
    }
} 