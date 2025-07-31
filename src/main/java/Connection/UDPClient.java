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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.minimal.Heartbeat;
import io.dronefleet.mavlink.minimal.MavAutopilot;
import io.dronefleet.mavlink.minimal.MavState;
import io.dronefleet.mavlink.minimal.MavType;

public class UDPClient implements MavlinkClient {
    private static final String TAG = "UDPClient";
    private static final int BUFFER_SIZE = 65535;

    private DatagramSocket socket;
    private InetAddress remoteAddress;
    private int remotePort;
    private boolean isConnected = false;
    private boolean isRunning = false;

    private MavlinkConnection mavlinkConnection;
    private Thread receiveThread;
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();
    private PipedInputStream udpIn;
    private PipedOutputStream appOut;
    private OutputStream udpOut;
    private HandlerThread receiveHandlerThread;
    private Handler receiveHandler;

    public MavlinkConnection getMavlinkConnection() {
        return mavlinkConnection;
    }

    @Override
    public void connect(String host, int port) throws Exception {
        if (isConnected) {
            disconnect();
        }

        try {
            // Resolve remote address
            SocketAddress remoteAddress = new InetSocketAddress(host,port);


            // Create and configure socket
            socket = new DatagramSocket();  // Default MAVLink port

            socket.setSoTimeout(3000);
            socket.connect(remoteAddress);
            isConnected = true;

            Log.d(TAG, "UDP connected to " + host + ":" + port);

            // Setup MAVLink streams
            setupMavlinkStreams();

            // Start receiving thread
            startReceiver();

            // Initialize MAVLink connection
            initializeMavlink();

        } catch (UnknownHostException e) {
            throw new IOException("Unknown host: " + host, e);
        } catch (SocketException e) {
            throw new IOException("Socket error: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IOException("MAVLink setup error: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect() {
        isConnected = false;
        isRunning = false;

        if (receiveHandlerThread != null) {
            receiveHandlerThread.quitSafely();
            try {
                receiveHandlerThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "HandlerThread shutdown interrupted", e);
            }
            receiveHandlerThread = null;
            receiveHandler = null;
        }

        sendExecutor.shutdownNow();

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        closeMavlinkStreams();

        Log.d(TAG, "UDP disconnected");
    }

    @Override
    public void sendData(byte[] data) throws Exception {
        if (!isConnected) {
            throw new IOException("Not connected");
        }

        sendExecutor.submit(() -> {
            try {
                DatagramPacket packet = new DatagramPacket(
                        data, data.length, remoteAddress, remotePort
                );
                socket.send(packet);
                Log.v(TAG, "Sent " + data.length + " bytes to " + remoteAddress.getHostAddress());
            } catch (IOException e) {
                Log.e(TAG, "Send error: " + e.getMessage(), e);
            }
        });
    }

    private void setupMavlinkStreams() throws IOException {

        udpIn = new PipedInputStream(BUFFER_SIZE);
        appOut = new PipedOutputStream(udpIn);

        udpOut = new OutputStream() {
            final byte[] buffer = new byte[BUFFER_SIZE];
            int position = 0;

            @Override
            public void write(int b) throws IOException {
                write(new byte[]{(byte) b}, 0, 1);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                if ((position + len) > buffer.length) {
                    flush();
                }
                System.arraycopy(b, off, buffer, position, len);
                position += len;
            }

            @Override
            public void flush() throws IOException {
                if (position > 0) {
                    try {
                        sendData(java.util.Arrays.copyOf(buffer, position));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    position = 0;
                }
            }
        };
    }

    private void closeMavlinkStreams() {
        try {
            if (appOut != null) appOut.close();
            if (udpIn != null) udpIn.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing MAVLink streams: " + e.getMessage());
        }
    }

    private void startReceiver() {
        if (isRunning) return;

        isRunning = true;
        receiveHandlerThread = new HandlerThread("UDPClient-Receiver");
        receiveHandlerThread.start();
        receiveHandler = new Handler(receiveHandlerThread.getLooper());

        receiveHandler.post(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (isRunning) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet); // This can timeout
                    appOut.write(packet.getData(), packet.getOffset(), packet.getLength());
                    appOut.flush();

                    // Optionally log or update remote endpoint
                    if (!packet.getAddress().equals(remoteAddress) || packet.getPort() != remotePort) {
                        remoteAddress = packet.getAddress();
                        remotePort = packet.getPort();
                        Log.d(TAG, "Remote endpoint updated to: " + remoteAddress.getHostAddress() + ":" + remotePort);
                    }

                } catch (SocketTimeoutException timeout) {
                    // 🔇 Silently ignore timeout (or log once every N seconds)
                } catch (IOException e) {
                    if (isRunning) {
                        Log.e(TAG, "Receive error: " + e.getMessage(), e);
                    }
                }
            }
        });

    }


    void initializeMavlink() throws IOException {
        mavlinkConnection = MavlinkConnection.create(udpIn, udpOut);
        Log.d(TAG, "MAVLink connection initialized");

        // Send initial heartbeat
        sendExecutor.submit(() -> {
            try {
                Heartbeat heartbeat = Heartbeat.builder()
                        .type(MavType.MAV_TYPE_GCS)
                        .autopilot(MavAutopilot.MAV_AUTOPILOT_INVALID)
                        .systemStatus(MavState.MAV_STATE_UNINIT)
                        .mavlinkVersion(3)
                        .build();

                mavlinkConnection.send1(255, 1, heartbeat);
                Log.d(TAG, "Initial heartbeat sent");
            } catch (IOException e) {
                Log.e(TAG, "Failed to send heartbeat: " + e.getMessage(), e);
            }
        });
    }

    public void sendMavlinkCommand(int systemId, int componentId, Object command) {
        if (mavlinkConnection != null) {
            sendExecutor.submit(() -> {
                try {
                    mavlinkConnection.send1(systemId, componentId, command);
                    Log.d(TAG, "MAVLink command sent: " + command.getClass().getSimpleName());
                } catch (IOException e) {
                    Log.e(TAG, "Failed to send MAVLink command", e);
                }
            });
        }
    }
}