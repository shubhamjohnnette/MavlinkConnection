package com.johnnette;


import com.johnnette.LoadConnectionData.DeviceRegistry;
import com.johnnette.MavlinkManager.MavlinkCommand;
import com.johnnette.MavlinkManager.MavlinkConnectionService;
import com.johnnette.savedConnection.Device;
import com.johnnette.savedConnection.DeviceJsonUtil;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {
    public static void main(String[] args) {


        try {
            Device device = new Device("localhost","192.168.1.13",14550,"tcp",true);
            File file = new File(System.getProperty("user.home") + File.separator + "Documents" + File.separator + "devices.json");

//            Device edited = new Device("192.168.1.2", "192.168.0.100", 5555, "sensor", true);
//            boolean updated = DeviceJsonUtil.updateDevice(edited, file);
//            System.out.println(updated ? "Updated!" : "Device not found!");
//
//// Delete device example
//            boolean deleted = DeviceJsonUtil.deleteDeviceByName("192.168.1.2", file);
//            System.out.println(deleted ? "Deleted!" : "Device not found!");


            DeviceJsonUtil.loadConnectionToGlobalList(file);
//            DeviceJsonUtil.addConnection(device,file);
            System.out.println("Loaded Devices:");
            MavlinkConnectionService.start();
            for (Device d : DeviceRegistry.devices) {
                System.out.println("- " + d.name + " [" + d.type + "]"+ d.ip);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

            scheduler.schedule(() -> {
                System.out.println("Commandttt: onCreate: entered");

                MavlinkCommand mavlinkCommand = new MavlinkCommand();
                System.out.println("Commandttt: onCreate: instance");

                mavlinkCommand.ArmDisarm(true, true);
                System.out.println("Commandttt: onCreate: success");
                mavlinkCommand.sendCommandReadParam();
                // Shutdown after the task is executed
                scheduler.shutdown();
            }, 5, TimeUnit.SECONDS);

            // Optionally shutdown the scheduler after the task (if only one task)
            scheduler.shutdown();


        }

    }


