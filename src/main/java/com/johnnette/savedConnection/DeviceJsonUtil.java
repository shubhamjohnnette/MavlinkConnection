package com.johnnette.savedConnection;

import com.johnnette.LoadConnectionData.DeviceRegistry;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

public class DeviceJsonUtil {

    // CREATE: Save a single device entry to JSON file
    public static void addConnection(Device device, File file) throws IOException {
        List<Device> devices = loadConnection(file); // load existing
        devices.add(device);                      // add new device
        saveAll(devices, file);            // overwrite file with updated list
    }

    // Helper: Load all devices from JSON file
    private static List<Device> loadConnection(File file) throws IOException {
        List<Device> list = new ArrayList<>();
        if (!file.exists()) return list;

        StringBuilder jsonStr = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
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
    private static void saveAll(List<Device> devices, File file) throws IOException {
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

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(array.toString(2)); // pretty print
        }
    }

    // GLOBAL LOAD: Load JSON devices to global list
    public static void loadConnectionToGlobalList(File file) throws Exception {
        if (!file.exists()) return;

        StringBuilder json = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
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
    public static boolean updateConnection(Device updatedDevice, File file) throws IOException {
        List<Device> devices = loadConnection(file);
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
            saveAll(devices, file);  // Write updated list
        }

        return found;  // true if updated, false if not found
    }

//    delete a device

    public static boolean deleteConnectionByName(String name, File file) throws IOException {
        List<Device> devices = loadConnection(file);
        boolean removed = devices.removeIf(d -> d.name.equals(name));  // Remove matching

        if (removed) {
            saveAll(devices, file);  // Rewrite file with filtered list
        }

        return removed;  // true if something was deleted
    }


}
