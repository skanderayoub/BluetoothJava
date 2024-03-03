package com.example.bluetoothjava;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessData {

    private LineChart chart;
    private MapView map;
    private List<GeoPoint> geoPoints = new ArrayList<>();
    Polyline line = new Polyline();
    private IMapController mapController;
    private Marker marker;
    private MainActivity activity;
    private StringBuilder csvData = new StringBuilder();
    private Date today;
    private File mainFile;
    private List<DataPoint> dataPoints = new ArrayList<>();
    private Map<String, DataPoint> dataPointsMap = new HashMap<>(); // Use HashMap or other Map implementation

    private TextView humidityText;

    ArrayList<Entry> outsideValues = new ArrayList<>();
    ArrayList<Entry> insideValues = new ArrayList<>();
    ArrayList<Entry> pressurevalues = new ArrayList<>();

    public ProcessData(MainActivity activity, LineChart chart, MapView map) {
        this.chart = chart;
        this.activity = activity;
        this.map = map;

        humidityText = activity.findViewById(R.id.humidity);

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

        Button saveButton = activity.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(view -> {
            try {
                onSaveDataPush();
                Toast.makeText(activity.getBaseContext(), "File Saved", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }



    private void prepareChart() {
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        LineDataSet set1 = new LineDataSet(outsideValues, "Lufttemperatur");
        LineDataSet set2 = new LineDataSet(insideValues, "Innentemperatur");
        LineDataSet set3 = new LineDataSet(pressurevalues, "Luftdruck");

        set1.setLineWidth(2.5f);
        set1.setColor(Color.BLUE);
        set1.setCircleColor(Color.BLACK);
        set1.setDrawCircles(false);
        set1.setDrawValues(false);
        // line thickness and point size
        //set1.setCircleRadius(3f);
        //set1.setDrawCircleHole(false);

        set2.setLineWidth(2.5f);
        set2.setColor(Color.RED);
        set2.setCircleColor(Color.BLACK);
        set2.setDrawCircles(false);
        set2.setDrawValues(false);

        set3.setLineWidth(2.5f);
        set3.setColor(Color.GREEN);
        set3.setAxisDependency(YAxis.AxisDependency.RIGHT);
        set3.setCircleColor(Color.BLACK);
        set3.setDrawCircles(false);
        set3.setDrawValues(false);



        dataSets.add(set1); // add the data sets
        dataSets.add(set2);
        dataSets.add(set3);
        // create a data object with the data sets
        LineData data = new LineData(dataSets);
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new MyValueFormatter(chart));
        MyMarkerView mv = new MyMarkerView(chart.getContext(), R.layout.mymarker);
        // Set the marker to the chart
        mv.setChartView(chart);
        chart.setMarker(mv);
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

        // Convert Map entries to a list of DataPoints
        List<DataPoint> sortedDataPoints = new ArrayList<>(dataPointsMap.values());

        // Sort the list by time
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sortedDataPoints.sort((dataPoint1, dataPoint2) -> dataPoint1.time.compareTo(dataPoint2.time));
        }

        // Iterate through the sorted list and append data to the file
        for (DataPoint dataPoint : sortedDataPoints) {
            csvData.append(dataPoint.time).append(",").
                    append(dataPoint.out_temp).append(",").
                    append(dataPoint.in_temp).append(",").
                    append(dataPoint.lat).append(",").
                    append(dataPoint.longi).append(",").
                    append(dataPoint.hum).append(",").
                    append(dataPoint.pressure).append("\n");
        }

        addData(file);

    }

    private void addData(File file) {
        Log.d("File", String.valueOf(file.exists()));
        if (!file.exists()) {
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                // Create the parent directory and any intermediate directories
                parentDir.mkdirs(); // Creates all non-existent parent directories
            }
            csvData.insert(0, "time,outside_temp,inside_temp,latitude,longitude,hum,pressure\n");
        }

        try {
            FileOutputStream outputStream = new FileOutputStream(file, true);
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);
            writer.append(csvData.toString());
            writer.close();
            outputStream.close();
            dataPoints.clear();
            dataPointsMap.clear();
            csvData.setLength(0);
        } catch (IOException e) {
            Log.e("CSV", "Error saving data to file", e);
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
        GeoPoint startPoint = new GeoPoint(48.68920, 9.00481);
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

    public class DataPoint {
        String time;
        String out_temp;
        String in_temp;
        String lat;
        String longi;
        String hum;
        String pressure;
    }

    public void processData(String data){
        String[] newData = data.trim().split(";");
        String type = newData[0];
        String val = newData[1];
        String time = newData[2];

        if (convertTimeToSeconds(time) == -1) {
            return;
        }

        DataPoint dataPoint = dataPointsMap.get(time);

        if (dataPoint == null) {
            // Create a new data point if none exists for this timestamp
            dataPoint = new DataPoint();
            dataPoint.time = time;
            dataPointsMap.put(time, dataPoint);
        }

        switch (type) {
            case "pressure":
                addData(chart, val, time, 2);
                //addPressureData(chart, val, time);
                dataPoint.pressure = val;
                break;
            case "temp":
                //chart2.clear();
                String[] temp = val.split(",");
                String outsideVal = temp[1];
                String insideVal = temp[0];
                addData(chart, outsideVal, time, 0);
                addData(chart, insideVal, time, 1);
                dataPoint.out_temp = outsideVal;
                dataPoint.in_temp = insideVal;
                //addTempData(chart, val, time);
                break;
            case "hum":
                dataPoint.hum = val;
                addHumidity(val);
                break;
            case "GPS":
                panToPos(val);
                String[] GPS = val.split(",");
                String lat = GPS[0];
                String longi = GPS[1];
                dataPoint.lat = lat;
                dataPoint.longi = longi;
                break;
        }
        if (dataPointsMap.size() > 100) {
            try {
                onSaveDataPush();
            } catch (Exception ignored) { }

        }
    }

    private void addHumidity(String val) {
        humidityText.setText(val);
    }

    private void addData(LineChart chart, String receivedData, String time, int idx) {
        float myTime = convertTimeToSeconds(time);
        float value = Float.parseFloat(receivedData);

        // Get or create the line chart data
        LineData data = chart.getData();
        // Get the existing datasets from the LineData
        data.addEntry(new Entry(myTime, value), idx);
        data.notifyDataChanged();
        // let the chart know it's data has changed
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private void addPressureData(LineChart chart, String receivedData, String time) {
        float myTime = convertTimeToSeconds(time);
        float pressure = Float.parseFloat(receivedData);

        // Get or create the line chart data
        LineData data = chart.getData();
        // Get the existing datasets from the LineData
        data.addEntry(new Entry(myTime, pressure), 2);
        data.notifyDataChanged();
        // let the chart know it's data has changed
        chart.notifyDataSetChanged();
        chart.setVisibleXRangeMaximum(30);
        chart.moveViewToX(data.getEntryCount());
        chart.invalidate();

    }

    private void addTempData(LineChart chart, String receivedData, String time) {
        // Split the received data into two floats (assuming comma-separated format)
        float myTime = convertTimeToSeconds(time);
        String[] temp = receivedData.split(",");
        float outsideVal = Float.parseFloat(temp[1]);
        float insideVal = Float.parseFloat(temp[0]);
        // Get or create the line chart data
        LineData data = chart.getData();
        // Get the existing datasets from the LineData
        data.addEntry(new Entry(myTime, outsideVal), 0);
        data.addEntry(new Entry(myTime, insideVal), 1);
        data.notifyDataChanged();

        // let the chart know it's data has changed
        chart.notifyDataSetChanged();

        chart.setVisibleXRangeMaximum(30);
        chart.moveViewToX(data.getEntryCount());
        chart.invalidate();
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
        geoPoints.add(startPoint);
        map.getOverlayManager().add(line);
        mapController.setCenter(startPoint);
        marker.setPosition(startPoint);
        map.getOverlays().add(marker);
        map.invalidate();
    }

    public void loadData(ArrayList<String[]> data) {
        int length = data.size();
        chart.getData().getDataSetByIndex(0).clear();
        chart.getData().getDataSetByIndex(1).clear();
        chart.getData().getDataSetByIndex(2).clear();
        //Log.d(TAG, String.valueOf(chart.getData().getDataSetByIndex(5)));

        for (int i = 0; i < length; i++) {
            chart.getData().addEntry(new Entry(i,Float.parseFloat(data.get(i)[0])), 0);
            chart.getData().addEntry(new Entry(i,Float.parseFloat(data.get(i)[1])), 1);
        }


        chart.notifyDataSetChanged();
        chart.invalidate();
        //chart.animateX(2000);
    }

    private float norm_bar_druck(float altitude) {
        float p_0 = 1013.25f;
        float temp = 0.0065f * altitude / 288.15f;

        return p_0 * (float) Math.pow((1- temp), 5.255f);
    }

    float dew_point(float temp_degree, float rel_hum) {
        return temp_degree - (100 - rel_hum) / 5;
    }

    float degree_to_kelvin(float temp_degree) {return temp_degree + 273.15f;}

}
