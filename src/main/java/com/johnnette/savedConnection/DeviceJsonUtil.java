package com.johnnette.savedConnection;

import com.johnnette.LoadConnectionData.DeviceRegistry;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

public class DeviceJsonUtil {

    // CREATE: Save a single device entry to JSON file
    public static void addConnection(Device device) throws IOException {
        List<Device> devices = loadConnection(); // load existing
        devices.add(device);                      // add new device
        saveAll(devices);            // overwrite file with updated list
//        loadConnectionToGlobalList(file);
    }

    // Helper: Load all devices from JSON file
    private static List<Device> loadConnection() throws IOException {
        List<Device> list = new ArrayList<>();
        if (DeviceRegistry.getPath()==null) return list;

        StringBuilder jsonStr = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(DeviceRegistry.getPath()))) {
            String line;
            while ((line = reader.readLine()) != null)
                jsonStr.append(line);
        }

        JSONArray array = new JSONArray(jsonStr.toString());
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            Device d = new Device(
                    obj.getString("name"),
                    obj.getString("ip"),
                    obj.getInt("port"),
                    obj.optString("type", "tcp"),
                    obj.getBoolean("isActive")

            );
            list.add(d);
        }
        return list;
    }

    // Helper: Save all devices to JSON file
    private static void saveAll(List<Device> devices) throws IOException {
        JSONArray array = new JSONArray();
        for (Device d : devices) {
            JSONObject obj = new JSONObject();
            obj.put("name", d.name);
            obj.put("ip", d.ip);
            obj.put("port", d.port);
            obj.put("type", d.type);
            obj.put("isActive", d.isActive);

            array.put(obj);
        }

        try (FileWriter writer = new FileWriter(DeviceRegistry.getPath())) {
            writer.write(array.toString(2)); // pretty print
        }
    }

    // GLOBAL LOAD: Load JSON devices to global list
    public static void loadConnectionToGlobalList() throws Exception {
        File file = new File(DeviceRegistry.getPath(),"devices.json");

        if (DeviceRegistry.getPath()== null){
            System.out.println("path empty");
            return;
        }

        StringBuilder json = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            System.out.println(DeviceRegistry.getPath());
            String line;
            while ((line = br.readLine()) != null) {
                json.append(line);
            }
        }

        JSONArray arr = new JSONArray(json.toString());
        List<Device> devices = DeviceRegistry.devices;
        devices.clear(); // clear previous data

        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            devices.add(new Device(
                    obj.getString("name"),
                    obj.getString("ip"),
                    obj.getInt("port"),
                    obj.optString("type"),
                    obj.getBoolean("isActive")

            ));
        }
    }

//    update the device
    public static boolean updateConnection(Device updatedDevice) throws IOException {
        List<Device> devices = loadConnection();
        boolean found = false;

        for (int i = 0; i < devices.size(); i++) {
            Device d = devices.get(i);
            if (d.name.equals(updatedDevice.name)) {
                devices.set(i, updatedDevice);  // Replace old with updated
                found = true;
                break;
            }
        }

        if (found) {
            saveAll(devices);  // Write updated list
        }

        return found;  // true if updated, false if not found
    }

//    delete a device

    public static boolean deleteConnectionByName(String name) throws IOException {
        List<Device> devices = loadConnection();
        boolean removed = devices.removeIf(d -> d.name.equals(name));  // Remove matching

        if (removed) {
            saveAll(devices);  // Rewrite file with filtered list
        }

        return removed;  // true if something was deleted
    }


}
