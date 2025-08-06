package com.johnnette.Connection;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.minimal.Heartbeat;
import io.dronefleet.mavlink.minimal.MavAutopilot;
import io.dronefleet.mavlink.minimal.MavState;
import io.dronefleet.mavlink.minimal.MavType;

public class UDPClient implements MavlinkClient {
    private static final int BUFFER_SIZE = 65535;

    private DatagramSocket socket;
    private InetAddress remoteAddress;
    private int remotePort;
    private boolean isConnected = false;
    private boolean isRunning = false;

    private MavlinkConnection mavlinkConnection;
    private Thread receiveThread;
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService receiverExecutor = Executors.newSingleThreadExecutor();
    private PipedInputStream udpIn;
    private PipedOutputStream appOut;
    private OutputStream udpOut;

    public MavlinkConnection getMavlinkConnection() {
        return mavlinkConnection;
    }

    @Override
    public void connect(String host, int port) throws Exception {
        if (isConnected) disconnect();

        try {
            SocketAddress remoteSocketAddress = new InetSocketAddress(host, port);
            remoteAddress = InetAddress.getByName(host);
            remotePort = port;

            socket = new DatagramSocket();
            socket.setSoTimeout(3000);
            socket.connect(remoteSocketAddress);
            isConnected = true;

            System.out.println("[INFO] UDP connected to " + host + ":" + port);

            setupMavlinkStreams();
            startReceiver();
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

        receiverExecutor.shutdownNow();
        sendExecutor.shutdownNow();

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        closeMavlinkStreams();

        System.out.println("[INFO] UDP disconnected");
    }

    @Override
    public void sendData(byte[] data) throws Exception {
        if (!isConnected) throw new IOException("Not connected");

        sendExecutor.submit(() -> {
            try {
                DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, remotePort);
                socket.send(packet);
                System.out.println("[SEND] " + data.length + " bytes to " + remoteAddress.getHostAddress());
            } catch (IOException e) {
                System.err.println("[ERROR] Send failed: " + e.getMessage());
                e.printStackTrace();
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
                if ((position + len) > buffer.length) flush();
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
            System.err.println("[ERROR] Closing MAVLink streams: " + e.getMessage());
        }
    }

    private void startReceiver() {
        if (isRunning) return;

        isRunning = true;
        receiverExecutor.submit(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (isRunning) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    appOut.write(packet.getData(), packet.getOffset(), packet.getLength());
                    appOut.flush();

                    if (!packet.getAddress().equals(remoteAddress) || packet.getPort() != remotePort) {
                        remoteAddress = packet.getAddress();
                        remotePort = packet.getPort();
                        System.out.println("[INFO] Remote updated to " + remoteAddress.getHostAddress() + ":" + remotePort);
                    }

                } catch (SocketTimeoutException timeout) {
                    // ignore or log periodically
                } catch (IOException e) {
                    if (isRunning) e.printStackTrace();
                }
            }
        });
    }

    void initializeMavlink() throws IOException {
        mavlinkConnection = MavlinkConnection.create(udpIn, udpOut);
        System.out.println("[INFO] MAVLink connection initialized");

        sendExecutor.submit(() -> {
            Heartbeat heartbeat = Heartbeat.builder()
                    .type(MavType.MAV_TYPE_GCS)
                    .autopilot(MavAutopilot.MAV_AUTOPILOT_INVALID)
                    .systemStatus(MavState.MAV_STATE_UNINIT)
                    .mavlinkVersion(3)
                    .build();

//                mavlinkConnection.send(255, 1, heartbeat);
            System.out.println("[INFO] Initial heartbeat sent");
        });
    }

    public void sendMavlinkCommand(int systemId, int componentId, Object command) {
        if (mavlinkConnection != null) {
            sendExecutor.submit(() -> {
                //                    mavlinkConnection.send1(systemId, componentId, command);
                System.out.println("[COMMAND] Sent: " + command.getClass().getSimpleName());
            });
        }
    }
}
