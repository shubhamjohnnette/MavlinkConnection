package com.johnnette.Connection;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;

public class TCP implements MavlinkClient {
    private static final int CONNECTION_TIMEOUT = 2000;

    private Socket socket;
    private MavlinkConnection mavlinkConnection;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Thread readThread;
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

                System.out.println("✅ Connected to " + host + ":" + port);

//                startReading(); // Start background read loop

            } else {
                throw new IOException("Connection failed: Socket not connected");
            }
        } catch (IOException e) {
            disconnect();  // Clean up resources
            throw new IOException("Connection error: " + e.getMessage(), e);
        }
    }


    @Override
    public void disconnect() {
        isReading = false;

        if (readThread != null && readThread.isAlive()) {
            readThread.interrupt();
            try {
                readThread.join();
            } catch (InterruptedException e) {
                System.err.println("⚠️ Read thread shutdown interrupted: " + e.getMessage());
            }
            readThread = null;
        }

        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("🔌 Disconnected");
        } catch (IOException e) {
            System.err.println("❌ Error during disconnect: " + e.getMessage());
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
            System.out.println("📤 Sent " + data.length + " bytes");
        } catch (IOException e) {
            throw new IOException("Send error: " + e.getMessage(), e);
        }
    }

    public MavlinkConnection getMavlinkConnection() {
        try {
            InputStream bufferedIn = new BufferedInputStream(socket.getInputStream());
            OutputStream out = socket.getOutputStream();
            return MavlinkConnection.create(bufferedIn, out);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}


