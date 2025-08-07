package com.johnnette.Connection;

import com.johnnette.LoadConnectionData.DeviceRegistry;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.johnnette.MavlinkManager.GlobalVariables;
import com.johnnette.MavlinkManager.MavlinkManager;
import com.johnnette.savedConnection.Device;
import io.dronefleet.mavlink.*;

public class ConnectionManager {

    private final String TAG = "ConnectionManager";
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private List<Device> autoConnectionOn;
    private MavlinkClient client;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final int MAVLINK_V2_MIN_LENGTH = 12;
    public void autoConnect() {
        executor.execute(() -> {
            List<Device> allConnections = DeviceRegistry.devices;

            autoConnectionOn = new ArrayList<>();
            for (Device conn : allConnections) {
                if (conn.isActive) {
                    autoConnectionOn.add(conn);
                }
            }

            if (autoConnectionOn.isEmpty()) {
                System.out.println(TAG + ": No auto-connect entries found. Using fallback UDP connection.");
                connectFallbackUDP();
                return;
            }

            for (Device conn : autoConnectionOn) {
                if (connected.get()) break;
                startMavlinkReceiver(conn);
//                Save connection data to rety connection if disconnected between or with some error
                GlobalVariables.DATA= conn;


            }

            if (!connected.get()) {
                System.out.println(TAG + ": No auto-connected devices responded. Using fallback UDP connection.");
                connectFallbackUDP();
            }
        });
    }

    private boolean isMavlinkPacket(byte[] data) {
        return data != null && data.length > 0 && (data[0] == (byte) 0xFE || data[0] == (byte) 0xFD);
    }

    private void startMavlinkReceiver(Device conn) {
        System.out.println(TAG + ": 📡 Connecting to MAVLink: " + conn.ip + ":" + conn.port);

        try {
            switch (conn.type.toUpperCase()) {
                case "TCP":
                    client = new TCP();
                    GlobalVariables.CONNECTION_TYPE= GlobalVariables.MavlinkConnectionType.TCP;
                    break;
                case "UDP":
                    client = new UDP();
                    GlobalVariables.CONNECTION_TYPE= GlobalVariables.MavlinkConnectionType.UDP;
                    break;
                case "UDPCLIENT":
                    client = new UDPClient();
                    GlobalVariables.CONNECTION_TYPE= GlobalVariables.MavlinkConnectionType.UDP;
                    break;
                case "USB" :
                    GlobalVariables.CONNECTION_TYPE= GlobalVariables.MavlinkConnectionType.USB;
                    System.out.println("Upcoming Feature");
                    default:
                    System.out.println(TAG + ": ❌ Unknown connection type: " + conn.type);
                    return;
            }

            client.connect(conn.ip, conn.port);  // 🔌 Connect first

            if (client instanceof TCP) {
                ((TCP) client).getMavlinkConnection();
            } else if (client instanceof UDPClient) {
                ((UDPClient) client).initializeMavlink();
            } else if (client instanceof UDP) {
                ((UDP) client).initializeMavlink();
            }

            connected.set(true);
            System.out.println(TAG + ": ✅ Connected to " + conn.type + ": " + conn.ip + ":" + conn.port);

            startMavlinkReadingLoop(client);

        } catch (Exception e) {
            System.out.println(TAG + ": ❌ Failed to connect to " + conn.type + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startMavlinkReadingLoop(MavlinkClient client) {
        new Thread(() -> {
            try {
                MavlinkConnection mavConn = null;

                if (client instanceof TCP) {
                    mavConn = ((TCP) client).getMavlinkConnection();
                } else if (client instanceof UDP) {
                    System.out.println(TAG + ": startMavlinkReadingLoop: instance of UDP");
                    mavConn = ((UDP) client).getMavlinkConnection();
                } else if (client instanceof UDPClient) {
                    mavConn = ((UDPClient) client).getMavlinkConnection();
                }

                if (mavConn == null) {
                    System.out.println(TAG + ": ❌ MavlinkConnection is null");
                    return;
                }

                MavlinkManager.getInstance().setMavlinkConnection(mavConn);

//                while (true) {
//                    try {
//                        MavlinkMessage<?> msg = mavConn.next();
//                        if (msg != null) {
//                            String name = msg.getPayload().getClass().getSimpleName();
//                            System.out.println(TAG + ": 📥 MAVLink message received: " + name);
//                        }
//                    } catch (Exception e) {
//                        System.out.println(TAG + ": ⚠️ MAVLink decoding error: " + e.getMessage());
//                        e.printStackTrace();
//                    }
//                }

            } catch (Exception e) {
                System.out.println(TAG + ": ❌ Error reading MAVLink messages: " + e.getMessage());
                e.printStackTrace();
            }
        }, "MAVLink-Reader").start();
    }

    private void connectFallbackUDP() {
        try {
            String ip = getDeviceIpAddress();
            int port = 14550;
            client = new UDP();
            System.out.println(TAG + ": 🌐 Fallback to default UDP " + ip + ":" + port);

            client.connect(ip, port);
            startMavlinkReadingLoop(client);
        } catch (Exception e) {
            System.out.println(TAG + ": ❌ Fallback UDP failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getDeviceIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return "Unable to get IP";
    }
}
