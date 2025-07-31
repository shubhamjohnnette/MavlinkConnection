package com.johnnette.gcs.connections;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;

public class TCP implements MavlinkClient {
    private static final String TAG = "TCPConnection";
    private static final int CONNECTION_TIMEOUT = 2000;

    private Socket socket;
    private MavlinkConnection mavlinkConnection;
    private InputStream inputStream;
    private OutputStream outputStream;
    private HandlerThread readThread;
    private Handler readHandler;
    private volatile boolean isReading = false;


    @Override
    public void connect(String host, int port) throws Exception {
        SocketAddress address = new InetSocketAddress(host, port);
        socket = new Socket();

        try {
            socket.connect(address, CONNECTION_TIMEOUT);

            if (socket.isConnected()) {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                mavlinkConnection = MavlinkConnection.create(inputStream, outputStream);

                Log.d(TAG, "Connected to " + host + ":" + port);

                startReading(); // 🔄 Start background read loop

            } else {
                throw new IOException("Connection failed: Socket not connected");
            }
        } catch (IOException e) {
            disconnect();  // Clean up resources
            throw new IOException("Connection error: " + e.getMessage(), e);
        }
    }
    private void startReading() {
        if (readThread != null) return;

        isReading = true;
        readThread = new HandlerThread("TCP-Mavlink-Reader");
        readThread.start();
        readHandler = new Handler(readThread.getLooper());

        readHandler.post(() -> {
            while (isReading && !Thread.currentThread().isInterrupted()) {
                try {
                    Object message = mavlinkConnection.next();
                    if (message != null) {
                        Log.d(TAG, "Received MAVLink message: " +  ((MavlinkMessage<?>) message).getPayload().toString());
                        // You may post this to an event bus, listener, or LiveData here
                        ((MavlinkMessage<?>) message).getPayload().toString();
                    }
                } catch (IOException e) {
                    if (isReading) {
                        Log.e(TAG, "MAVLink read error: " + e.getMessage(), e);
                    }
                }
            }
        });
    }


    @Override
    public void disconnect() {
        isReading = false;

        if (readThread != null) {
            readThread.quitSafely();
            try {
                readThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "HandlerThread shutdown interrupted", e);
            }
            readThread = null;
            readHandler = null;
        }

        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
            Log.d(TAG, "Disconnected");
        } catch (IOException e) {
            Log.e(TAG, "Error during disconnect: " + e.getMessage());
        } finally {
            socket = null;
            mavlinkConnection = null;
            inputStream = null;
            outputStream = null;
        }
    }


    @Override
    public void sendData(byte[] data) throws Exception {
        if (outputStream == null) {
            throw new IOException("Not connected to any server");
        }

        try {
            outputStream.write(data);
            outputStream.flush();
            Log.v(TAG, "Sent " + data.length + " bytes");
        } catch (IOException e) {
            throw new IOException("Send error: " + e.getMessage(), e);
        }
    }

    // Optional: Mavlink-specific interface if needed
    public MavlinkConnection getMavlinkConnection() {
        return mavlinkConnection;
    }
}