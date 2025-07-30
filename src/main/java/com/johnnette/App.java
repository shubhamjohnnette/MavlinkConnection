package com.johnnette;

import com.johnnette.LoadConnectionData.DeviceRegistry;
import com.johnnette.savedConnection.Device;
import com.johnnette.savedConnection.DeviceJsonUtil;

import java.io.File;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {


        try {
            Device device = new Device("192.168.1.2","192.168.1.9",8080,"udp",true);
            File file = new File(System.getProperty("user.home") + File.separator + "Documents" + File.separator + "devices.json");

//            Device edited = new Device("192.168.1.2", "192.168.0.100", 5555, "sensor", true);
//            boolean updated = DeviceJsonUtil.updateDevice(edited, file);
//            System.out.println(updated ? "Updated!" : "Device not found!");
//
//// Delete device example
//            boolean deleted = DeviceJsonUtil.deleteDeviceByName("192.168.1.2", file);
//            System.out.println(deleted ? "Deleted!" : "Device not found!");


            DeviceJsonUtil.loadConnectionToGlobalList(file);
            DeviceJsonUtil.addConnection(device,file);
            System.out.println("Loaded Devices:");
            for (Device d : DeviceRegistry.devices) {
                System.out.println("- " + d.name + " [" + d.type + "]"+ d.ip);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
