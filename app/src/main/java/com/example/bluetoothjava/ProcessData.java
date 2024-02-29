package com.example.bluetoothjava;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ProcessData {

    private LineChart chart;
    private MapView map;
    private IMapController mapController;
    private Marker marker;
    private MainActivity activity;
    private StringBuilder csvData;
    private Date today;
    private File mainFile;

    ArrayList<Entry> outsideValues = new ArrayList<>();
    ArrayList<Entry> insideValues = new ArrayList<>();
    ArrayList<Entry> pressurevalues = new ArrayList<>();

    ArrayList<String> xAxisValueList = new ArrayList<>();

    public ProcessData(MainActivity activity, LineChart chart, MapView map) {
        this.chart = chart;
        this.activity = activity;
        this.map = map;

        //Chart stuff
        prepareChart();

        //Map stuff
        requestPermissionsIfNecessary(new String[]{
                // if you need to show the current location, uncomment the line below
                // Manifest.permission.ACCESS_FINE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
        prepareMap();

        today = new Date();
        // SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd"); // Format the date as YYYY-MM-DD
        // String formattedDate = formatter.format(today);
        // formattedDate = formattedDate + ".csv";
        // csvData = new StringBuilder();
        // csvData.append("Time,Temperature,Humidity\n");
        // for (int i = 0; i < 5; i++) {
        //     csvData.append("1").append(",").append("2").append(",").append("3").append("\n");
        // }
        // saveDataToCSV(csvData, formattedDate);

        Button saveButton = activity.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(view -> {
            try {
                onSaveDataPush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void prepareChart() {
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        LineDataSet set1 = new LineDataSet(outsideValues, "outside");
        LineDataSet set2 = new LineDataSet(insideValues, "inside");
        LineDataSet set3 = new LineDataSet(pressurevalues, "pressure");

        set1.setLineWidth(2.5f);
        set1.setColor(Color.BLUE);
        set1.setCircleColor(Color.BLACK);
        // line thickness and point size
        //set1.setCircleRadius(3f);
        //set1.setDrawCircleHole(false);

        set2.setLineWidth(2.5f);
        set2.setColor(Color.RED);
        set2.setCircleColor(Color.BLACK);

        set3.setLineWidth(2.5f);
        set3.setColor(Color.GREEN);
        set3.setAxisDependency(YAxis.AxisDependency.RIGHT);
        set3.setCircleColor(Color.BLACK);



        dataSets.add(set1); // add the data sets
        dataSets.add(set2);
        dataSets.add(set3);
        // create a data object with the data sets
        LineData data = new LineData(dataSets);
        // set data
        chart.setData(data);
    }

    private void onSaveDataPush() throws IOException {
        // Create the complete path with dynamic date components
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd"); // Format the date as YYYY-MM-DD
        String formattedDate = formatter.format(today);
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                + File.separator + formattedDate + ".csv";
        // Create a File object
        File file = new File(path);
        // Check if the file exists
        if (file.exists()) {
            // Load the file content using your preferred method (e.g., BufferedReader)
            // ...
            Log.d("FileCheck", "File exists: " + path);
            mainFile = file;
            String content = readFile(path);
            Log.d("Content", content);
        } else {
            // Create the file if it doesn't exist
            try {
                Log.d("FileCheck", "File does not exist " + path);
                csvData = new StringBuilder();
                csvData.append("Time,Inside Temp,Outside Temp\n");
                csvData.append("Time,Inside Temp,Outside Temp\n");
                csvData.append("Time,Inside Temp,Outside Temp\n");
                csvData.append("Time,Inside Temp,Outside Temp\n");
                try {
                    FileOutputStream outputStream = new FileOutputStream(file);
                    OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                    writer.write(csvData.toString());
                    writer.close();
                    outputStream.close();
                    Log.d("CSV", "Data saved to file: " + file.getAbsolutePath());
                    mainFile = file;
                } catch (IOException e) {
                    Log.e("CSV", "Error saving data to file", e);
                }
            } catch (Exception e) {
                Log.e("FileCheck", "Error creating file: " + e.getMessage());
            }
        }
    }

    private void readFile(Uri selectedFileUri) throws IOException {
        // Open an input stream
        InputStream inputStream = activity.getContentResolver().openInputStream(selectedFileUri);

        // Create a BufferedReader for efficient reading
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        // Read each line of the CSV file
        String line;
        while ((line = reader.readLine()) != null) {
            // Process the line (e.g., parse CSV data)
            // ...
            Log.d("CSVReader", line);
        }

        // Close the reader
        reader.close();
    }

    public String readFile(String filePath) throws IOException {
        // Create a File object
        File file = new File(filePath);

        // Check if the file exists
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        // Create a StringBuilder to store the content
        StringBuilder content = new StringBuilder();

        // Open a FileReader
        FileReader reader = new FileReader(file);

        // Read content into a buffer
        char[] buffer = new char[1024];
        int read;

        // Read data from the file until the end
        while ((read = reader.read(buffer)) != -1) {
            content.append(buffer, 0, read);
        }

        // Close the reader
        reader.close();

        // Return the file content as a string
        return content.toString();
    }

    public void saveDataToCSV(StringBuilder csvData, String path) {
        String fileName = path;
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);
            writer.write(csvData.toString());
            writer.close();
            outputStream.close();
            Log.d("CSV", "Data saved to file: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e("CSV", "Error saving data to file", e);
        }
    }

    private void prepareMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        mapController = map.getController();
        mapController.setZoom(16);
        GeoPoint startPoint = new GeoPoint(48.689199, 9.004848);
        mapController.setCenter(startPoint);
        marker = new Marker(map);
        marker.setPosition(startPoint);
        map.getOverlays().add(marker);
    }


    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            permissionsToRequest.add(permissions[i]);
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    activity,
                    permissionsToRequest.toArray(new String[0]),
                    1);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    activity,
                    permissionsToRequest.toArray(new String[0]),
                    1);
        }
    }

    public void processData(String data) {
        String[] newData = data.trim().split(";");
        String type = newData[0];
        String val = newData[1];
        String time = newData[2];
        switch (type) {
            case "pressure":
                addPressureData(chart, val, time);
                break;
            case "temp":
                Log.d("Data", val);
                //chart2.clear();
                addTempData(chart, val, time);
                break;
            case "hum":
                break;
            case "GPS":
                panToPos(val);
                break;
        }
    }

    private void addPressureData(LineChart chart, String receivedData, String time) {
        float myTime = convertTimeToSeconds(time);
        Log.d("Time", String.valueOf(myTime));
        float pressure = Float.parseFloat(receivedData);

        // Get or create the line chart data
        LineData data = chart.getData();
        // Get the existing datasets from the LineData
        data.addEntry(new Entry(myTime, pressure), 2);

        data.notifyDataChanged();

        // let the chart know it's data has changed
        chart.notifyDataSetChanged();
        chart.invalidate();

    }

    private void addTempData(LineChart chart, String receivedData, String time) {
        // Split the received data into two floats (assuming comma-separated format)
        xAxisValueList.add(time);
        float myTime = convertTimeToSeconds(time);
        Log.d("Time", String.valueOf(myTime));
        String[] temp = receivedData.split(",");
        float outsideVal = Float.parseFloat(temp[1]);
        float insideVal = Float.parseFloat(temp[0]);

        // Get or create the line chart data
        LineData data = chart.getData();

        // Create entries with the formatted x-axis label and temperature as y-value
        if (data == null) {
            // Initialize data structures if data is not set yet
            ArrayList<Entry> outsideValues = new ArrayList<>();
            ArrayList<Entry> insideValues = new ArrayList<>();


            outsideValues.add(new Entry(myTime, outsideVal));
            insideValues.add(new Entry(myTime, insideVal));

            // Create LineDataSets for outside and inside temperatures
            LineDataSet outsideTemp = new LineDataSet(outsideValues, "Outside Temp");
            LineDataSet insideTemp = new LineDataSet(insideValues, "Inside Temp");

            // Add initial entries (assuming x = 0 for both)

            // Set line properties (color and width)
            outsideTemp.setLineWidth(2.5f);
            outsideTemp.setColor(Color.BLUE);
            insideTemp.setLineWidth(2.5f);
            insideTemp.setColor(Color.RED);

            // Add both datasets to a new LineData object
            ArrayList<ILineDataSet> tempDataSets = new ArrayList<>();
            tempDataSets.add(outsideTemp);
            tempDataSets.add(insideTemp);
            LineData tempLineData = new LineData(tempDataSets);


            chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xAxisValueList));

            // Set the data for the chart and redraw
            chart.setData(tempLineData);
            chart.invalidate();
        } else {
            // Get the existing datasets from the LineData
            data.addEntry(new Entry(myTime, outsideVal), 0);
            data.addEntry(new Entry(myTime, insideVal), 1);

            data.notifyDataChanged();

            // let the chart know it's data has changed
            chart.notifyDataSetChanged();


            chart.setVisibleXRangeMaximum(30);
            chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xAxisValueList));

            chart.moveViewToX(data.getEntryCount());
            chart.getXAxis().setLabelCount(30, true);
            chart.invalidate();
        }
    }

    private static int convertTimeToSeconds(String timeStr) {
        try {
            // Split the string into hours, minutes, and seconds
            String[] timeParts = timeStr.split(":");
            int hours = Integer.parseInt(timeParts[0]);
            int minutes = Integer.parseInt(timeParts[1]);
            int seconds = Integer.parseInt(timeParts[2]);

            // Validate the time components
            if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59 || seconds < 0 || seconds > 59) {
                return -1; // Invalid time format
            }

            // Calculate total seconds
            return hours * 3600 + minutes * 60 + seconds;
        } catch (NumberFormatException e) {
            // Handle potential parsing errors
            return -1; // Invalid format
        }
    }

    private void panToPos(String pos) {
        float lat = Float.parseFloat(pos.split(",")[0]);
        float longitude = Float.parseFloat(pos.split(",")[1]);
        GeoPoint startPoint = new GeoPoint(lat, longitude);
        mapController.setCenter(startPoint);
        marker.setPosition(startPoint);
        map.getOverlays().add(marker);
    }
}
