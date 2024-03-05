package com.example.bluetoothjava;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;

/*
 TODO:
    - Luftfeuchtigkeit
    - Luftdruck
    - Frosttemperatur
    - Barom. Druck + normierung
    - Höheninfo
    - GPS pos: DONE
    - Celcius -> Farenheit
    - Diagramm zu temp: DONE
 */
public class MainActivity extends AppCompatActivity {
    private BluetoothService btService;
    private ProcessData processData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        MapView map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        LineChart chart = findViewById(R.id.chart2);
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

        Button loadButton = findViewById(R.id.loadButton);
        loadButton.setOnClickListener(v -> openActivity2());
    }


    private void openActivity2() {
        Intent intent = new Intent(this, MainActivity2.class);
        startActivity(intent);
    }

}
