package com.johnnette.MavlinkManager;



import java.util.concurrent.atomic.AtomicBoolean;

import com.johnnette.savedConnection.Device;
//import io.dronefleet.mavlink.common.MavType;
import io.dronefleet.mavlink.minimal.MavType;

public class GlobalVariables {

    /// ################################### ///
    ///     FOR DEFINING THE TYPE OF        ///
    ///    CONNECTION TRYING TO ESTABLISH   ///
    /// ################################### ///
    public enum MavlinkConnectionType {
        TCP,
        USB,
        UDP
    }



    /// ################################### ///
    ///   MAPPING FOR THE FIXED WING PLANE  ///
    /// ################################### ///
    public enum MODE_MAPPING_APM {
        MANUAL, CIRCLE, STABILIZE, TRAINING, ACRO, FBWA, FBWB, CRUISE, AUTOTUNE, NONE, AUTO, RTL, LOITER, TAKEOFF,
        AVOID_ADSB, GUIDED, INITIALISING, QSTABILIZE, QHOVER, QLOITER, QLAND, QRTL, QAUTOTUNE, QACRO, THERMAL, LOITERALTQLAND
    }

    public enum MODE_MAPPING_ACM {
        STABILIZE, ACRO, ALT_HOLD ,  AUTO ,GUIDED ,LOITER ,RTL ,CIRCLE ,POSITION ,LAND ,OF_LOITER ,DRIFT ,SPORT ,FLIP ,AUTOTUNE ,
        POSHOLD ,BRAKE ,THROW ,AVOID_ADSB ,GUIDED_NOGPS ,SMART_RTL ,FLOWHOLD ,FOLLOW ,ZIGZAG ,SYSTEMID ,AUTOROTATE ,AUTO_RTL ,
    }



    public static MavlinkConnectionType CONNECTION_TYPE;


    // ref for ConnectionData class
    public static Device DATA;



    /// ############################# ///
    ///     VARIABLES FOR MAVLINK     ///
    /// ############################# ///
    public static int SYSTEM_ID = 255; // system id of ground station
    public static int COMPONENT_ID = 1; // component id
    public static int BAUD_RATE     = 57600;
    public static int TIMEOUT = 8000;

    public static boolean IS_ARM ;
    public static boolean IS_GUIDED;
    public static final AtomicBoolean CONNECTION_EXIST = new AtomicBoolean(false) ;
    public static MavType DRONE_TYPE;
    public static int BATTERY_CELLS ;
    public static int WP_RADIUS = 50;

    public static String VIDEO_LINK ;
    public static boolean Heartbeat_flag ;
    public static boolean IS_GOOGLE_SERVICE;


    public GlobalVariables() {
        CONNECTION_TYPE     = null;
        DATA = null;
        IS_ARM              = false;
        IS_GUIDED           = false;



    }


}
