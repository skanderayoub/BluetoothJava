package com.example.bluetoothjava;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TableLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;

import ir.androidexception.datatable.DataTable;
import ir.androidexception.datatable.model.DataTableHeader;
import ir.androidexception.datatable.model.DataTableRow;


public class MainActivity2 extends AppCompatActivity {
    private LineChart chart;
    private MapView map;

    IMapController mapController;
    ArrayList<GeoPoint> geoPoints = new ArrayList<>();
    //add your points here
    Polyline line = new Polyline();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        chart = findViewById(R.id.chart2);
        map = findViewById(R.id.map);
        mapController = map.getController();

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        mapController = map.getController();
        mapController.setZoom(16);


        Button loadButton = findViewById(R.id.loadButton);
        loadButton.setOnClickListener(view -> chooseFile());

        Button returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(v -> openMainActivity());



    }

    private void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void chooseFile() {
        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFile.setType("*/*");  // Set MIME type for CSV files

        // Specify the Downloads folder as the starting point
        Uri downloadsUri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath());
        Log.d("directory", String.valueOf(downloadsUri));
        chooseFile.setDataAndType(downloadsUri, "*/*");  // Set both URI and type

        startActivityForResult(
                Intent.createChooser(chooseFile, "Choose a CSV file"),
                // Replace REQUEST_CODE with your desired request code
                1
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check if the request code matches the file selection request
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Get the selected file URI
            Uri selectedFileUri = data.getData();
            Log.d("URI", selectedFileUri.toString());

            // Proceed with storing and reading the file content (see below)
            try {
                readFile(selectedFileUri);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void readFile(Uri selectedFileUri) throws IOException {
        // Open an input stream
        InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
        // Create a StringBuilder to store the data
        ArrayList<String[]> data = new ArrayList<>();
        ArrayList<String[]> tempData = new ArrayList<String[]>();
        ArrayList<String[]> gpsData = new ArrayList<String[]>();
        ArrayList<String[]> humData = new ArrayList<String[]>();
        ArrayList<String[]> pressureData = new ArrayList<String[]>();
        // Create a BufferedReader for efficient reading
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        if (reader.ready()) { // Check if there's data available
            //reader.readLine(); // Skip the first line
            data.add(reader.readLine().split(","));
        }
        // Read each line of the CSV file and append it to the StringBuilder
        String line;
        while ((line = reader.readLine()) != null) {
            data.add(line.split(",")); // Append line with a newline character
            String[] lineData = line.split(",");
            if (convertTimeToSeconds(lineData[0]) == -1) {
                continue;
            }
            if (!Objects.equals(lineData[1], "null") && !Objects.equals(lineData[2], "null")) {
                tempData.add(new String[]{lineData[0], lineData[1], lineData[2]});
            }
            if (!Objects.equals(lineData[3], "null") && !Objects.equals(lineData[4], "null")) {
                gpsData.add(new String[]{lineData[0], lineData[3], lineData[4]});
            }
            if (!Objects.equals(lineData[5], "null")) {
                humData.add(new String[]{lineData[0], lineData[5]});
            }
            if (!Objects.equals(lineData[6], "null")) {
                pressureData.add(new String[]{lineData[0], lineData[6]});
            }
        }
        // Close the reader
        reader.close();





        fileData allData = new fileData(tempData, gpsData, humData, pressureData);


        loadData(allData);

        // CSVParser parser = new CSVParser(this);
        // TableLayout table = findViewById(R.id.table);
        // ScrollView scrollView = findViewById(R.id.scrollView);
        // parser.populateTable(data, table);

        prepareTable(data);
    }

    private void prepareTable(ArrayList<String[]> data) {
        DataTable dataTable = findViewById(R.id.data_table);
        String[] headers = data.get(0);
        DataTableHeader header = new DataTableHeader.Builder()
                .item(headers[0], 1)
                .item(headers[1], 1)
                .item(headers[2], 1)
                .item(headers[3], 1)
                .item(headers[4], 1)
                .item(headers[5], 1)
                .item(headers[6], 1)
                .build();

        ArrayList<DataTableRow> rows = new ArrayList<>();
        // define 200 fake rows for table
        for(int i=1;i<data.size();i++) {
            String[] dataLine = data.get(i);
            DataTableRow row = new DataTableRow.Builder()
                    .value(dataLine[0])
                    .value(dataLine[1])
                    .value(dataLine[2])
                    .value(dataLine[3])
                    .value(dataLine[4])
                    .value(dataLine[5])
                    .value(dataLine[6])
                    .build();
            rows.add(row);
        }

        dataTable.setHeader(header);
        dataTable.setRows(rows);
        dataTable.inflate(this);
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
    private static class fileData {
        ArrayList<String[]> tempData;
        ArrayList<String[]> gpsData;
        ArrayList<String[]> humData;
        ArrayList<String[]> pressureData;

        public fileData(ArrayList<String[]> tempData, ArrayList<String[]> gpsData, ArrayList<String[]> humData, ArrayList<String[]> pressureData) {
            this.tempData = tempData;
            this.gpsData = gpsData;
            this.humData = humData;
            this.pressureData = pressureData;
        }
    }

    public void loadData(fileData data) {
        prepareChart(data.tempData, data.pressureData);
        prepareMap(data.gpsData);
    }


    private void prepareMap(ArrayList<String[]> gpsData) {
        geoPoints.clear();

        int dataSize = gpsData.size();

        for (int i = 0; i < dataSize; i++) {
            geoPoints.add(new GeoPoint(Float.parseFloat(gpsData.get(i)[1]), Float.parseFloat(gpsData.get(i)[2])));
        }
        //Only start and end points
        //geoPoints.clear();
        //geoPoints.add(new GeoPoint(Float.parseFloat(gpsData.get(0)[1]), Float.parseFloat(gpsData.get(0)[2])));
        //geoPoints.add(new GeoPoint(Float.parseFloat(gpsData.get(dataSize-1)[1]), Float.parseFloat(gpsData.get(dataSize-1)[2])));

        createRoad(geoPoints);


    }

    private void createRoad(ArrayList<GeoPoint> geoPoints) {
        map.getOverlays().clear();

        RoadManager roadManager = new OSRMRoadManager(this, "MyUserAgent");
        Road road = roadManager.getRoad(geoPoints);
        Polyline roadOverlay = RoadManager.buildRoadOverlay(road);

        Drawable nodeIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.marker_node, null);

        Marker startPoint = new Marker(map);
        startPoint.setPosition(geoPoints.get(0));
        startPoint.setIcon(nodeIcon);
        startPoint.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        startPoint.setTitle("Start");


        Marker endPoint = new Marker(map);
        endPoint.setPosition(geoPoints.get(geoPoints.size() - 1));
        endPoint.setIcon(nodeIcon);
        endPoint.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        endPoint.setTitle("End");

        map.getOverlays().add(startPoint);
        map.getOverlays().add(endPoint);
        mapController.setCenter(geoPoints.get(0));
        map.getOverlays().add(roadOverlay);
        map.invalidate();
    }

    private void prepareChart(ArrayList<String[]> tempData, ArrayList<String[]> pressureData) {
        int tempDataSize = tempData.size();
        int pressureDataSize = pressureData.size();
        chart.clear();
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new MyValueFormatter(chart));

        MyMarkerView mv = new MyMarkerView(this, R.layout.mymarker);
        mv.setChartView(chart);
        chart.setMarker(mv);
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {

                //Highlight highlight[] = new Highlight[chart.getData().getDataSets().size()];
                int foundVals = 0;
                ArrayList<Highlight> highlights = new ArrayList<>();
                for (int j = 0; j < chart.getData().getDataSets().size(); j++) {

                    IDataSet<Entry> iDataSet = chart.getData().getDataSets().get(j);


                    for (int i = 0; i < ((LineDataSet) iDataSet).getValues().size(); i++) {
                        try {
                            if (((LineDataSet) iDataSet).getValues().get(i).getX() == e.getX()) {
                                //highlight[j] = new Highlight(e.getX(), e.getY(), j);
                                highlights.add(new Highlight(e.getX(), e.getY(), j));
                                foundVals += 1;
                            }
                        } catch (Exception ignored) {}
                    }

                }
                Highlight highlight[] = new Highlight[foundVals];
                for (int i = 0; i < foundVals; i++) {
                    highlight[i] = highlights.get(i);
                }
                chart.highlightValues(highlight);
            }

            @Override
            public void onNothingSelected() {
            }
        });

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();

        ArrayList<Entry> outsideTemp = new ArrayList<>();
        ArrayList<Entry> insideTemp = new ArrayList<>();
        ArrayList<Entry> pressureVals = new ArrayList<>();

        for (int i = 0; i < tempDataSize; i++) {
            float time = convertTimeToSeconds(tempData.get(i)[0]);
            outsideTemp.add(new Entry(time, Float.parseFloat(tempData.get(i)[1])));
            insideTemp.add(new Entry(time, Float.parseFloat(tempData.get(i)[2])));
        }


        for (int i = 0; i < pressureDataSize; i++) {
            float time = convertTimeToSeconds(pressureData.get(i)[0]);
            pressureVals.add(new Entry(time, Float.parseFloat(pressureData.get(i)[1])));
        }



        LineDataSet outside = new LineDataSet(outsideTemp, "Outside temp");
        LineDataSet inside = new LineDataSet(insideTemp, "Inside temp");
        LineDataSet pressure = new LineDataSet(pressureVals, "Pressure");

        outside.setLineWidth(2.5f);
        outside.setColor(Color.BLUE);
        outside.setCircleColor(Color.BLACK);

        inside.setLineWidth(2.5f);
        inside.setColor(Color.RED);
        inside.setCircleColor(Color.BLACK);

        pressure.setLineWidth(2.5f);
        pressure.setColor(Color.GREEN);
        pressure.setCircleColor(Color.BLACK);
        pressure.setAxisDependency(YAxis.AxisDependency.RIGHT);

        dataSets.add(outside);
        dataSets.add(inside);
        dataSets.add(pressure);

        LineData finalData = new LineData(dataSets);
        chart.setData(finalData);

        for (ILineDataSet iSet : chart.getData().getDataSets()) {

            LineDataSet set = (LineDataSet) iSet;
            set.setDrawValues(false);
            set.setDrawCircles(false);
        }

        chart.getData().getYMax(YAxis.AxisDependency.LEFT);


        chart.notifyDataSetChanged();

        //chart.invalidate();
        chart.animateX(2000);
    }



}