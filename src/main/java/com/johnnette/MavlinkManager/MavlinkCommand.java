package com.johnnette.MavlinkManager;



import io.dronefleet.mavlink.common.*;

import io.dronefleet.mavlink.minimal.MavType;

public class MavlinkCommand {

    private MavType droneType;

    public MavlinkCommand(){
        droneType = GlobalVariables.DRONE_TYPE;

        if(droneType == null) droneType  = MavType.MAV_TYPE_FIXED_WING;
    }

    public enum COMMANDS {
        SELECT_MODE, TAKEOFF, MANUAL, STABILIZE, LOITER, START_MISSION, AUTO, GUIDED, RTL, RESTART
    }


    ///////////////// VARIABLE FOR FAILSAFE ///////////////////////////

    // for battery FAIL SAFE
    String LOW_VOLT = "BATT_LOW_VOLT";
    String LOW_VOLT_ACTION = "BATT_FS_LOW_ACT";

    // for RC FAIL SAFE
    String THR_FAILSAFE_ACTION = "THR_FAILSAFE";
    String THR_FS_VALUE = "THR_FS_VALUE";

    // for GCS FAIL SAFE
    String VALUE = "FS_LONG_TIMEOUT";
    String ACTION = "FS_LONG_ACTION";
    String ENABLE = "FS_GCS_ENABL";

    String []  strArrayForDataRead =  {
            "BATT_LOW_VOLT",
            "BATT_FS_LOW_ACT",

            "THR_FAILSAFE",
            "THR_FS_VALUE",

            "FS_LONG_TIMEOUT",
            "FS_LONG_ACTION",
            "FS_GCS_ENABL"

    };
    /////////////////  END VARIABLE FOR FAILSAFE ///////////////////////////

    public void ArmDisarm(boolean armIt, boolean force ){

        final float magic_force_arm_value = 2989.0f;
        final float magic_force_disarm_value = 21196.0f;

        if( force ) {

            if( armIt ) {

                MavlinkManager.SendCommandMessage(
                        MavCmd.MAV_CMD_COMPONENT_ARM_DISARM, 1,
                        magic_force_arm_value, 0, 0, 0, 0, 0);


            }

            else {
                MavlinkManager.SendCommandMessage(
                        MavCmd.MAV_CMD_COMPONENT_ARM_DISARM,0,
                        magic_force_disarm_value, 0, 0, 0, 0, 0);
            }
        }

        else {

            if( armIt ){
                MavlinkManager.SendCommandMessage(
                        MavCmd.MAV_CMD_COMPONENT_ARM_DISARM,
                        1, 0, 0, 0, 0, 0, 0);
            }
            else {
                MavlinkManager.SendCommandMessage(
                        MavCmd.MAV_CMD_COMPONENT_ARM_DISARM,
                        0, 0, 0, 0, 0, 0, 0);
            }
        }

    }

    public void SetMode(COMMANDS selectedMode) {
        int droneMode ;

        switch (selectedMode) {
            case TAKEOFF -> {
                droneMode = (droneType == MavType.MAV_TYPE_FIXED_WING) ? GlobalVariables.MODE_MAPPING_APM.TAKEOFF.ordinal() : -1; // TODO: find some other no. to pass for quadcopter


                // getting the first time home location
                MavlinkManager.SendCommandMessage(MavCmd.MAV_CMD_REQUEST_MESSAGE, 242, 0, 0, 0, 0, 0, 0);

                // taking off
                MavlinkManager.SendCommandMessage(MavCmd.MAV_CMD_DO_SET_MODE, 1, droneMode, 0, 0, 0, 0, 0);

            }
            case STABILIZE -> {
                droneMode = (droneType == MavType.MAV_TYPE_FIXED_WING) ? GlobalVariables.MODE_MAPPING_APM.STABILIZE.ordinal() : GlobalVariables.MODE_MAPPING_ACM.STABILIZE.ordinal();
                MavlinkManager.SendCommandMessage(MavCmd.MAV_CMD_DO_SET_MODE, 1, droneMode, 0, 0, 0, 0, 0);
            }
            case MANUAL ->
                    MavlinkManager.SendCommandMessage(MavCmd.MAV_CMD_DO_SET_MODE, 1, GlobalVariables.MODE_MAPPING_APM.MANUAL.ordinal(), 0, 0, 0, 0, 0);
            case GUIDED -> {
                droneMode = (droneType == MavType.MAV_TYPE_FIXED_WING) ? GlobalVariables.MODE_MAPPING_APM.GUIDED.ordinal() : GlobalVariables.MODE_MAPPING_ACM.GUIDED.ordinal();
                MavlinkManager.SendCommandMessage(MavCmd.MAV_CMD_DO_SET_MODE, 1, droneMode, 0, 0, 0, 0, 0);
            }
            case RTL ->
                    MavlinkManager.SendCommandMessage(MavCmd.MAV_CMD_NAV_RETURN_TO_LAUNCH, 0, 0, 0, 0, 0, 0, 0);
            case LOITER ->
                    MavlinkManager.SendCommandMessage(MavCmd.MAV_CMD_NAV_LOITER_UNLIM, 0, 0, 50, 0, 0, 0, 100);
            case START_MISSION ->
                    MavlinkManager.SendCommandMessage(MavCmd.MAV_CMD_MISSION_START, 1, 0, 0, 0, 0, 0, 0);
            case AUTO ->
                    MavlinkManager.SendCommandMessage(MavCmd.MAV_CMD_DO_SET_MODE, 1, GlobalVariables.MODE_MAPPING_APM.AUTO.ordinal(), 0, 0, 0, 0, 0);
            case RESTART -> SetRestartMission();
            
        }
    }

    public void setGuideMode() {

        if (!GlobalVariables.IS_GUIDED)
            MavlinkManager.SendCommandMessage(MavCmd.MAV_CMD_DO_SET_MODE, 1, GlobalVariables.MODE_MAPPING_APM.GUIDED.ordinal(), 0, 0, 0, 0, 0);
    }

    public void RequestHomeLocation(boolean inThread){
        CommandLong finalCmd = new CommandLong.Builder()
                .command(MavCmd.MAV_CMD_REQUEST_MESSAGE)
                .confirmation(1)
                .param1( 242 ) // requesting home location
                .build();

        MavlinkManager.SendMavlinkMessageOrCommand(finalCmd, inThread);

    }

    public void SetRestartMission(){

        CommandLong finalCmd = new CommandLong.Builder()
                .command(MavCmd.MAV_CMD_DO_SET_MISSION_CURRENT)
                .confirmation(1)
                .param1(1 ) // Mission sequence value to set. -1 for the current mission item (use to reset mission without changing current mission item).
                .param2(1) // Resets mission. 1: true, 0: false. Resets jump counters to initial values and changes mission state "completed" to be "active" or "paused".
                .build();

        MavlinkManager.SendMavlinkMessageOrCommand(finalCmd, true);
    }

    ////////////////////////////////////
    ////     FAILSAFE FUNCTION      ////
    ////////////////////////////////////

    public void setBatteryFailSafe(float paramValue, int doAction){


        // setting the param value for low volt failsafe
        ParamSet batteryFailSafe_LOW_VOLT = new ParamSet.Builder()
                .paramId(LOW_VOLT).paramValue(paramValue).build();

        // setting the param value for do action for low volt
        ParamSet batteryFailSafe_DO_ACTION = new ParamSet.Builder()
                .paramId(LOW_VOLT_ACTION).paramValue(doAction).build();


        // sending the param value to mavlink one by one
        MavlinkManager.SendMavlinkMessageOrCommand(batteryFailSafe_LOW_VOLT, true);
        MavlinkManager.SendMavlinkMessageOrCommand(batteryFailSafe_DO_ACTION, true);

    }

    public void setRadioFailSafe(float paramValue, int doAction){

        // setting the param value for radio failsafe
        ParamSet radioFailsafe_ThrottleValue = new ParamSet.Builder()
                .paramId(THR_FS_VALUE).paramValue(paramValue).build();

        // setting the param value to do action for radio failsafe
        ParamSet radioFailsafe_Throttle_DO_ACTION = new ParamSet.Builder()
                .paramId(THR_FAILSAFE_ACTION).paramValue(doAction).build();


        // sending the param value to mavlink one by one
        MavlinkManager.SendMavlinkMessageOrCommand(radioFailsafe_ThrottleValue, true);
        MavlinkManager.SendMavlinkMessageOrCommand(radioFailsafe_Throttle_DO_ACTION, true);
    }

    public void setGcsFailSafe(float paramValue, int doAction, int enabled){

        // setting the param value for gcs failsafe
        ParamSet gcsFailsafe_LongTimeoutValue = new ParamSet.Builder()
                .paramId(VALUE).paramValue(paramValue).build();

        // setting the param value to do action for gcs failsafe
        ParamSet gcsFailsafe_ACTION = new ParamSet.Builder()
                .paramId(ACTION).paramValue(doAction).build();

        // setting the param value to do action for gcs failsafe
        ParamSet gcsFailsafe_isEnabled = new ParamSet.Builder()
                .paramId(ENABLE).paramValue(enabled).build();

        // sending the param value to mavlink one by one
        MavlinkManager.SendMavlinkMessageOrCommand(gcsFailsafe_LongTimeoutValue, true);
//        MavlinkService.SendMavlinkMessageOrCommand(gcsFailsafe_ACTION, true);
        MavlinkManager.SendMavlinkMessageOrCommand(gcsFailsafe_isEnabled, true);
    }

// =============================== Command for Read Param =======================//
public  void sendCommandReadParam() {
        ParamRequestList cmd = new ParamRequestList.Builder().build();
        MavlinkManager.SendMavlinkMessageOrCommand(cmd, true);


}


}

