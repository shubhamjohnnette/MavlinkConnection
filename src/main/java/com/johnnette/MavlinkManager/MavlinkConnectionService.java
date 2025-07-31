package com.johnnette.gcs.MavlinkManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.johnnette.gcs.connections.ConnectionManager;

public class MavlinkConnectionService extends Service {
    private static final String TAG = "MavlinkConnectionService";
    private Thread connectionThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        connectionThread = new Thread(() -> {
            try {
                ConnectionManager connectionManager = new ConnectionManager();
                connectionManager.autoConnect(getApplicationContext());
                Log.d(TAG, "MAVLink AutoConnect successful.");
            } catch (Exception e) {
                Log.e(TAG, "AutoConnect failed", e);
            }
        });
        connectionThread.start();

        return START_STICKY; // Keeps service running
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (connectionThread != null && connectionThread.isAlive()) {
            connectionThread.interrupt();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // not a bound service
    }
}
