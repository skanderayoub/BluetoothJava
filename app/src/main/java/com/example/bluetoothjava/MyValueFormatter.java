package com.example.bluetoothjava;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.formatter.ValueFormatter;

public class MyValueFormatter extends ValueFormatter {
    public MyValueFormatter(LineChart lineChart) {
    }

    public String convertSecondsToTime(float totalSeconds) {
        if (totalSeconds < 0) {
            return null;  // Handle negative input
        }

        // Calculate hours, minutes, and seconds
        int hours = (int) totalSeconds / 3600;
        int remainingSeconds = (int) totalSeconds % 3600;
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;

        // Format the time string with leading zeros
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    @Override
    public String getFormattedValue(float value) {
        return convertSecondsToTime(value);
    }
}
