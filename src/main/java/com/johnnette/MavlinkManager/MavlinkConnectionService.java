package com.johnnette.MavlinkManager;

import Connection.ConnectionManager;

public class MavlinkConnectionService {

    private Thread connectionThread;
    private volatile boolean running = false;

    public void start() {
        if (running) {
            System.out.println("Service already running.");
            return;
        }

        running = true;
        connectionThread = new Thread(() -> {
            try {
                ConnectionManager connectionManager = new ConnectionManager();
                connectionManager.autoConnect();  // No context in core Java
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



