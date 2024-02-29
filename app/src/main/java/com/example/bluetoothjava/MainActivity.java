package com.example.bluetoothjava;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.LineDrawer;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private MapView map;
    private BluetoothService btService;
    private ProcessData processData;
    private LineChart chart;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        chart = findViewById(R.id.chart2);
        chart.getDescription().setEnabled(false);

        // enable touch gestures
        chart.setTouchEnabled(true);
        chart.setDrawGridBackground(false);
        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        //chart.setScaleXEnabled(true);
        chart.setScaleYEnabled(true);

        // force pinch zoom along both axis
        //chart.setPinchZoom(true);

        processData = new ProcessData(this, chart, map);
        btService = new BluetoothService(this, processData);


        Button connectButton = findViewById(R.id.connectButton);
        connectButton.setOnClickListener(view -> btService.enableBluetooth());

        Button disconnectButton = findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(view -> btService.disconnect());

        //Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        //chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
        //chooseFile.setType("*/*");  // Set MIME type for CSV files
////
        //// Specify the Downloads folder as the starting point
        //Uri downloadsUri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath());
        //Log.d("directory", String.valueOf(downloadsUri));
        //chooseFile.setDataAndType(downloadsUri, "*/*");  // Set both URI and type
////
        //startActivityForResult(
        //        Intent.createChooser(chooseFile, "Choose a CSV file"),
        //        // Replace REQUEST_CODE with your desired request code
        //        1
        //);

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
}
