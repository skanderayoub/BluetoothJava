package com.example.bluetoothjava;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.config.Configuration;
import org.osmdroid.views.overlay.Marker;


public class MainActivitybis extends AppCompatActivity{

    private MapView map;
    private Marker marker;
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private IMapController mapController;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final UUID sppUuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
    private BluetoothAdapter bluetoothAdapter;
    private LineChart chart;

    private ConnectThread connectThread;
    private ConnectedThread connectedThread;




    LineDataSet outsideTemp;
    LineDataSet insideTemp;

    ArrayList<Entry> insideValues;
    ArrayList<Entry> outsideValue;

    ArrayList<ILineDataSet> tempDataSets = new ArrayList<>();
    private LineData tempLineData;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);


        requestPermissionsIfNecessary(new String[]{
                // if you need to show the current location, uncomment the line below
                // Manifest.permission.ACCESS_FINE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        });

        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        mapController = map.getController();
        mapController.setZoom(16);
        GeoPoint startPoint = new GeoPoint(48.689199, 9.004848);
        mapController.setCenter(startPoint);
        marker = new Marker(map);
        marker.setPosition(startPoint);
        map.getOverlays().add(marker);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(getBaseContext(),
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        MainActivitybis.this,
                        new String[]{android.Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        1);
            }
        }

        //enableBluetooth();


        Button connectButton = findViewById(R.id.connectButton);
        connectButton.setOnClickListener(view -> enableBluetooth());

        Button disconnectButton = findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(view -> disconnect());

        chart = findViewById(R.id.chart2);
    }



    //@Override
    //public void onResume() {
    //    super.onResume();
    //    //this will refresh the osmdroid configuration on resuming.
    //    //if you make changes to the configuration, use
    //    //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    //    //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    //    map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    //}
//
    //@Override
    //public void onPause() {
    //    super.onPause();
    //    //this will refresh the osmdroid configuration on resuming.
    //    //if you make changes to the configuration, use
    //    //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    //    //Configuration.getInstance().save(this, prefs);
    //    map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    //}

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            permissionsToRequest.add(permissions[i]);
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void enableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            connect();
        }
    }


    private void connect() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getAddress().equals("B8:27:EB:AB:C5:BC")) {
                    connectToDevice(device);
                    break;
                }
            }
        }
    }

    private void disconnect(){
        try {
            connectThread.cancel();
        } catch (Exception e) {
            ;
        }
    }


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        String TAG = "Bluetooth";

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(sppUuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.d(TAG, "Connected");
                connectedThread = new ConnectedThread(mmSocket);
                connectedThread.start();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            //manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }


    }

    private void connectToDevice(BluetoothDevice device) {
        connectThread = new ConnectThread(device);
        connectThread.start();

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        String TAG = "Connected";

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");

            byte[] mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                                        // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    if (numBytes > 0) {
                        // Log received data
                        String receivedData = new String(mmBuffer, 0, numBytes);
                        Log.d(TAG, "Received data: " + receivedData);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                processData(receivedData);
                            }
                        });
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }
    }

    public void processData(String data) {
        String[] newData = data.split(":");
        String type = newData[0];
        String val = newData[1];
        switch (type) {
            case "pressure":
                break;
            case "temp":
                Log.d("Data", val);
                //chart2.clear();
                addTempData(chart, val);
                break;
            case "hum":
                break;
            case "GPS":
                panToPos(val);
                break;
        }
    }

    private void addTempData(LineChart chart, String receivedData) {
        // Split the received data into two floats (assuming comma-separated format)
        String[] temp = receivedData.split(",");
        float outsideVal = Float.parseFloat(temp[1]);
        float insideVal = Float.parseFloat(temp[0]);

        // Get or create the line chart data
        LineData data = chart.getData();
        Log.d("data", String.valueOf(data==null));
        if (data == null) {
            // Initialize data structures if data is not set yet
            ArrayList<Entry> outsideValues = new ArrayList<>();
            ArrayList<Entry> insideValues = new ArrayList<>();

            outsideValues.add(new Entry(1f, outsideVal));
            insideValues.add(new Entry(1f, insideVal));

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

            // Set the data for the chart and redraw
            chart.setData(tempLineData);
            chart.invalidate();
        } else {
            // Get the existing datasets from the LineData
            data.addEntry(new Entry(data.getDataSetByIndex(0).getEntryCount() + 1, outsideVal), 0);
            data.addEntry(new Entry(data.getDataSetByIndex(1).getEntryCount() + 1, insideVal), 1);

            data.notifyDataChanged();

            // let the chart know it's data has changed
            chart.notifyDataSetChanged();


            chart.setVisibleXRangeMaximum(30);
            chart.moveViewToX(data.getEntryCount());
            chart.invalidate();

            // Notify the data about the change and invalidate the chart for redraw
            //data.notifyDataChanged();
            //chart.invalidate();
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