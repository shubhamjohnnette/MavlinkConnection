package com.johnnette.gcs.connections;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;

public class UDP implements MavlinkClient {
    private static final String TAG = "UDPConnection";
    private static final int BUFFER_SIZE = 1024;
    private static final int LOCAL_PORT = 14550;

    private DatagramSocket socket;
    private HandlerThread handlerThread;
    private Handler handler;

    private InetAddress remoteAddress;
    private int remotePort;
    private boolean isConnected = false;
    private boolean isRunning = false;

    private MavlinkConnection mavlinkConnection;
    private Thread mavlinkReaderThread;
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();

    private final PipedInputStream pipedInputStream = new PipedInputStream(4096);
    private final PipedOutputStream pipedOutputStream = new PipedOutputStream();

    public UDP() {
        try {
            pipedOutputStream.connect(pipedInputStream);
        } catch (IOException e) {
            Log.e(TAG, "Failed to connect pipes", e);
        }
    }

    @Override
    public void connect(String host, int port) throws Exception {
        if (isConnected) {
            disconnect();
        }

        try {
            remoteAddress = InetAddress.getByName(host);
            remotePort = port;

            socket = new DatagramSocket(LOCAL_PORT);
            socket.setSoTimeout(3000);
            isConnected = true;

            Log.d(TAG, "UDP connected to " + host + ":" + port + " on local port " + LOCAL_PORT);

            startReceiver();
            initializeMavlink();
            startMavlinkReader();

        } catch (UnknownHostException e) {
            throw new IOException("Unknown host: " + host, e);
        } catch (SocketException e) {
            throw new IOException("Socket error: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect() {
        isConnected = false;
        isRunning = false;

        if (handlerThread != null) {
            handlerThread.quitSafely();
            handlerThread = null;
            handler = null;
        }

        if (mavlinkReaderThread != null && mavlinkReaderThread.isAlive()) {
            mavlinkReaderThread.interrupt();
            mavlinkReaderThread = null;
        }

        sendExecutor.shutdownNow();

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        Log.d(TAG, "UDP disconnected");
    }

    @Override
    public void sendData(byte[] data) throws Exception {
        if (!isConnected) {
            throw new IOException("Not connected");
        }

        sendExecutor.submit(() -> {
            try {
                DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, remotePort);
                socket.send(packet);
                Log.v(TAG, "Sent " + data.length + " bytes to " + remoteAddress.getHostAddress());
            } catch (IOException e) {
                Log.e(TAG, "Send error: " + e.getMessage(), e);
            }
        });
    }

    private void startReceiver() {
        if (isRunning) return;

        handlerThread = new HandlerThread("UDP-Receiver-Thread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        isRunning = true;

        handler.post(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];

            while (isRunning) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    if (!packet.getAddress().equals(remoteAddress) || packet.getPort() != remotePort) {
                        remoteAddress = packet.getAddress();
                        remotePort = packet.getPort();
                        Log.d(TAG, "Remote updated: " + remoteAddress.getHostAddress() + ":" + remotePort);
                    }

                    byte[] received = new byte[packet.getLength()];
                    System.arraycopy(buffer, 0, received, 0, packet.getLength());

                    processReceivedData(received);

                } catch (IOException e) {
                    if (isRunning) {
                        Log.e(TAG, "UDP receive error: " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    private void processReceivedData(byte[] data) {
        try {
            pipedOutputStream.write(data);
            pipedOutputStream.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error writing to MAVLink input pipe", e);
        }

        Log.v(TAG, "Received " + data.length + " bytes from " + remoteAddress.getHostAddress());
    }

    public void initializeMavlink() {
        if (mavlinkConnection == null) {
            mavlinkConnection = MavlinkConnection.create(pipedInputStream, new UdpOutputStream());
            Log.d(TAG, "MAVLink connection initialized");
        }
    }

    private void startMavlinkReader() {
        if (mavlinkConnection == null) return;

        mavlinkReaderThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    MavlinkMessage<?> msg = mavlinkConnection.next();
                    if (msg != null) {
                        Log.i(TAG, "📥 MAVLink message: " + msg.getPayload().getClass().getSimpleName());
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "MAVLink reader stopped", e);
            }
        }, "MAVLink-Reader-Thread");
        mavlinkReaderThread.start();
    }

    private class UdpOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            try {
                sendData(java.util.Arrays.copyOfRange(b, off, off + len));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendMavlinkCommand(int systemId, int componentId, Object command) {
        if (mavlinkConnection != null) {
            try {
                mavlinkConnection.send1(systemId, componentId, command);
                Log.d(TAG, "MAVLink command sent");
            } catch (IOException e) {
                Log.e(TAG, "Failed to send MAVLink command", e);
            }
        }
    }

    public MavlinkConnection getMavlinkConnection() {
        return mavlinkConnection;
    }
}
