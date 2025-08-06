package com.johnnette.MavlinkManager;



import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.CommandLong;
import io.dronefleet.mavlink.common.MavCmd;
import io.dronefleet.mavlink.common.MissionItemInt;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public  class MavlinkManager {

    private static final String TAG = "MavlinkManager";
    private static MavlinkManager instance;

    private static MavlinkConnection mavlinkConnection;
    private ExecutorService executorService;
    private final List<MavlinkMessageListener> listeners = new ArrayList<>();
//    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mainThreadExecutor = Executors.newSingleThreadExecutor(); // Simulates "main thread"
    private volatile boolean reading = false;



    // Interface for UI or other components to listen MAVLink messages
    public interface MavlinkMessageListener {
        void onMavlinkMessage(MavlinkMessage message);
    }

    private MavlinkManager() {
        executorService = Executors.newSingleThreadExecutor();
    }

    public static synchronized MavlinkManager getInstance() {
        if (instance == null) {
            instance = new MavlinkManager();
        }
        return instance;
    }

    // Set / switch active MavlinkConnection
    public synchronized void setMavlinkConnection(MavlinkConnection connection) {
        stopReading();
        this.mavlinkConnection = connection;
        if (connection != null) {
//            startReading();
        }
    }

    public synchronized void addListener(MavlinkMessageListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(MavlinkMessageListener listener) {
        listeners.remove(listener);
    }

    public void startReading() {
        reading = true;
        executorService.submit(() -> {
            try {
                while (reading && mavlinkConnection != null) {
                    MavlinkMessage message = mavlinkConnection.next();
                    if (message != null) {
                        // Post to main thread executor
//                        mainThreadExecutor.submit(() -> notifyListeners(message));
                    }
                }
            } catch (Exception e) {
                System.err.println("Error reading MAVLink messages");
                e.printStackTrace();
            }
        });
    }


    private void notifyListeners(MavlinkMessage message) {
        for (MavlinkMessageListener listener : listeners) {
            listener.onMavlinkMessage(message);
        }
    }

    public void stopReading() {
        reading = false;
        executorService.shutdownNow();
        mainThreadExecutor.shutdownNow();
    }

    // Send MAVLink command through current connection
    public synchronized void sendCommand(int systemId, int componentId, Object command) {
        if (mavlinkConnection != null) {
            try { System.out.println( "Cannot send message. No active MAVLink connection.");

                mavlinkConnection.send1(systemId, componentId, command);
            } catch (IOException e) {
                System.out.println( "Cannot send message. No active MAVLink connection.");
//                Log.e(TAG, "Failed to send MAVLink message", e);
            }
        } else {
            System.out.println( "Cannot send message. No active MAVLink connection.");
//            Log.w(TAG, "Cannot send message. No active MAVLink connection.");
        }
    }

    // Call this on app close or disconnect
    public synchronized void close() {
        stopReading();
        if (mavlinkConnection != null) {
//            try {
////                mavlinkConnection
//            } catch (IOException e) {
//                Log.e(TAG, "Error closing MAVLink connection", e);
//            }
            mavlinkConnection = null;
        }
        executorService.shutdownNow();
        listeners.clear();
    }
    private static void SendMavlinkCommands(int systemId, int componentId, Object command) throws IOException {

        if ( mavlinkConnection!= null || GlobalVariables.CONNECTION_EXIST.get())//commandTrigger.set(true);
            mavlinkConnection.send1(systemId, componentId, command);

    }

    /**
     * Function for identifying the command type and sending messages
     *
     * @param mavCmd Mavlink command strings
     * @param param  variable arguments for command parameters
     */
    public static void SendCommandMessage(MavCmd mavCmd, float... param) {

        CommandLong finalCmd = new CommandLong.Builder()
                .command(mavCmd)
                .confirmation(1)
                .param1(param[0])
                .param2(param[1])
                .param3(param[2])
                .param4(param[3])
                .param5(param[4])
                .param6(param[5])
                .param7(param[6])
                .build();


        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    MavlinkManager.SendMavlinkCommands(GlobalVariables.SYSTEM_ID, GlobalVariables.COMPONENT_ID, finalCmd);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }

        }).start();
    }

    public static void SendMavlinkMessageOrCommand(Object object, boolean inThread) {

        if (inThread) {
            new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        MavlinkManager.SendMavlinkCommands(GlobalVariables.SYSTEM_ID, GlobalVariables.COMPONENT_ID, object);
                    } catch (IOException e) {
                        System.out.println("MAVLINK"+ "ERROR IN SENDING COMMAND!!");
//                        Log.e("MAVLINK", "ERROR IN SENDING COMMAND!!");
                    }

                }

            }, "SEND_CONNECTION").start();
        } else {

            try {
                MavlinkManager.SendMavlinkCommands(GlobalVariables.SYSTEM_ID, GlobalVariables.COMPONENT_ID, object);
            } catch (IOException e) {
                System.out.println("MAVLINK"+ "ERROR IN SENDING COMMAND!!");
//                Log.e("MAVLINK", "ERROR IN SENDING COMMAND!!");
            }

        }

    }


//    public static void WriteMission(Context context, MissionTaskListener listener) {
//        Log.i("checkingReadWrite","ExecutorService in service");
//        ExecutorService writeService = Executors.newSingleThreadScheduledExecutor();
//        WriteMission writeMission = new WriteMission(context, listener);
//        writeService.submit(writeMission);
//
//    }


////    /**
////     * Function for fetching the waypoints
////     *
////     * @param context : list of Waypoint and MissionItemInt
////     * @param missionTaskListener    : to notify the function about the status of this function
////     */
//    public static void ReadMission(Context context, MissionTaskListener missionTaskListener) {
//
//        MissionBuilder downloadedItemsList = new MissionBuilder();
//        ReadMission readMission = new ReadMission(context, missionTaskListener);
//
//        ExecutorService readService = Executors.newSingleThreadExecutor();
//        Future<List<MissionItemInt>> future = readService.submit(readMission);
//
//    }


}
