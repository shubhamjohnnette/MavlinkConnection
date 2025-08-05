package Connection;

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
    private static final int BUFFER_SIZE = 1024;
    private static final int LOCAL_PORT = 14550;

    private DatagramSocket socket;
    private InetAddress remoteAddress;
    private int remotePort;
    private boolean isConnected = false;
    private boolean isRunning = false;

    private MavlinkConnection mavlinkConnection;
    private Thread receiverThread;
    private Thread mavlinkReaderThread;
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();

    private final PipedInputStream pipedInputStream = new PipedInputStream(4096);
    private final PipedOutputStream pipedOutputStream = new PipedOutputStream();

    public UDP() {
        try {
            pipedOutputStream.connect(pipedInputStream);
        } catch (IOException e) {
            System.err.println("Failed to connect pipes: " + e.getMessage());
        }
    }

    @Override
    public void connect(String host, int port) throws Exception {
        if (isConnected) disconnect();

        try {
            remoteAddress = InetAddress.getByName(host);
            remotePort = port;

            socket = new DatagramSocket(LOCAL_PORT);
            socket.setSoTimeout(3000);
            isConnected = true;

            System.out.println("UDP connected to " + host + ":" + port + " on local port " + LOCAL_PORT);

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

        if (receiverThread != null && receiverThread.isAlive()) {
            receiverThread.interrupt();
            receiverThread = null;
        }

        if (mavlinkReaderThread != null && mavlinkReaderThread.isAlive()) {
            mavlinkReaderThread.interrupt();
            mavlinkReaderThread = null;
        }

        sendExecutor.shutdownNow();

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        System.out.println("UDP disconnected");
    }

    @Override
    public void sendData(byte[] data) throws Exception {
        if (!isConnected) throw new IOException("Not connected");

        sendExecutor.submit(() -> {
            try {
                DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, remotePort);
                socket.send(packet);
                System.out.println("Sent " + data.length + " bytes to " + remoteAddress.getHostAddress());
            } catch (IOException e) {
                System.err.println("Send error: " + e.getMessage());
            }
        });
    }

    private void startReceiver() {
        if (isRunning) return;

        isRunning = true;
        receiverThread = new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];

            while (isRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    if (!packet.getAddress().equals(remoteAddress) || packet.getPort() != remotePort) {
                        remoteAddress = packet.getAddress();
                        remotePort = packet.getPort();
                        System.out.println("Remote updated: " + remoteAddress.getHostAddress() + ":" + remotePort);
                    }

                    byte[] received = new byte[packet.getLength()];
                    System.arraycopy(buffer, 0, received, 0, packet.getLength());

                    processReceivedData(received);
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("UDP receive error: " + e.getMessage());
                    }
                }
            }
        }, "UDP-Receiver");
        receiverThread.start();
    }

    private void processReceivedData(byte[] data) {
        try {
            pipedOutputStream.write(data);
            pipedOutputStream.flush();
        } catch (IOException e) {
            System.err.println("Error writing to MAVLink input pipe: " + e.getMessage());
        }
    }

    void initializeMavlink() {
        if (mavlinkConnection == null) {
            mavlinkConnection = MavlinkConnection.create(pipedInputStream, new UdpOutputStream());
            System.out.println("MAVLink connection initialized");
        }
    }

    private void startMavlinkReader() {
        if (mavlinkConnection == null) return;

        mavlinkReaderThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    MavlinkMessage<?> msg = mavlinkConnection.next();
                    if (msg != null) {
                        System.out.println("📥 MAVLink message: " + msg.getPayload().getClass().getSimpleName());
                    }
                }
            } catch (IOException e) {
                System.err.println("MAVLink reader stopped: " + e.getMessage());
            }
        }, "MAVLink-Reader");
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
                throw new IOException("UDP output write error", e);
            }
        }
    }

//    public void sendMavlinkCommand(int systemId, int componentId, Object command) {
//        if (mavlinkConnection != null) {
//            try {
//                mavlinkConnection.send1(systemId, componentId, command);
//                System.out.println("MAVLink command sent");
//            } catch (IOException e) {
//                System.err.println("Failed to send MAVLink command: " + e.getMessage());
//            }
//        }
//    }

    public MavlinkConnection getMavlinkConnection() {
        return mavlinkConnection;
    }
}
