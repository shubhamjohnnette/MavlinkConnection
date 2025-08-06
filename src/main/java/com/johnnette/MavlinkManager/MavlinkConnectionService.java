package com.johnnette.MavlinkManager;

import com.johnnette.Connection.ConnectionManager;

public class MavlinkConnectionService {

    private static Thread connectionThread;
    private static volatile boolean running = false;

    public static void start() {
        if (running) {
            System.out.println("Service already running.");
            return;
        }

        running = true;
        connectionThread = new Thread(() -> {
            try {
                ConnectionManager connectionManager = new ConnectionManager();
                connectionManager.autoConnect();
                System.out.println("MAVLink AutoConnect successful.");
            } catch (Exception e) {
                System.err.println("AutoConnect failed: " + e.getMessage());
                e.printStackTrace();
            }
        });

        connectionThread.start();
    }

    public void stop() {
        running = false;
        if (connectionThread != null && connectionThread.isAlive()) {
            connectionThread.interrupt();
            System.out.println("Service stopped.");
        }
    }

    public boolean isRunning() {
        return running && connectionThread != null && connectionThread.isAlive();
    }
}



