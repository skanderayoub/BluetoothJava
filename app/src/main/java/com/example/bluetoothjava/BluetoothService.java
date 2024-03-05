package com.example.bluetoothjava;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothService {
    ProcessData processData;
    ConnectedThread connectedThread;
    ConnectThread connectThread;
    private BluetoothAdapter bluetoothAdapter;
    private MainActivity activity;

    public BluetoothService(MainActivity activity, ProcessData processData) {
        this.activity = activity;
        this.processData = processData;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestBackgroundLocationPermission();
        }
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
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                processData.processData(receivedData);
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

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        private UUID sppUuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");

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

    private void requestBackgroundLocationPermission() {
        if (ContextCompat.checkSelfPermission(activity.getBaseContext(),
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{android.Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    1);
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

    public void disconnect(){
        try {
            connectThread.cancel();
        } catch (Exception e) {
            ;
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        connectThread = new ConnectThread(device);
        connectThread.start();

    }

    public void enableBluetooth() {
        if (bluetoothAdapter.isEnabled()) {
            connect();
        }
    }
}
