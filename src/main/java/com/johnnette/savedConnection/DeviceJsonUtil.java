package com.johnnette.savedConnection;

import com.johnnette.LoadConnectionData.DeviceRegistry;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DeviceJsonUtil {

    // CREATE: Save a single device entry to JSON file
    public static void addConnection(Device device) throws IOException {
        List<Device> devices = loadConnection(); // load existing
        devices.add(device);                     // add new device
        saveAll(devices);                        // overwrite file
    }

    // Helper: Load all devices from JSON file
    private static List<Device> loadConnection() throws IOException {
        List<Device> list = new ArrayList<>();

        File file = getDeviceFile();
        if (!file.exists()) {
            return list; // No file → empty list
        }

        StringBuilder jsonStr = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonStr.append(line);
            }
        }

        JSONArray array = new JSONArray(jsonStr.toString());
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            Device d = new Device(
                    obj.getString("name"),
                    obj.getString("ip"),
                    obj.getInt("port"),
                    obj.optString("type", "tcp"),
                    obj.optBoolean("isActive", false)
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

        File file = getDeviceFile();
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(array.toString(2));
        }
    }

    // GLOBAL LOAD: Load JSON devices to global list
    public static void loadConnectionToGlobalList() throws Exception {
        File file = getDeviceFile();
        if (!file.exists()) {
            System.out.println("No devices.json found");
            DeviceRegistry.devices.clear();
            return;
        }

        StringBuilder json = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                json.append(line);
            }
        }

        JSONArray arr = new JSONArray(json.toString());
        List<Device> devices = DeviceRegistry.devices;
        devices.clear();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            devices.add(new Device(
                    obj.getString("name"),
                    obj.getString("ip"),
                    obj.getInt("port"),
                    obj.optString("type", "tcp"),
                    obj.optBoolean("isActive", false)
            ));
        }
    }

    // UPDATE: update the device
    public static boolean updateConnection(Device updatedDevice) throws IOException {
        List<Device> devices = loadConnection();
        boolean found = false;

        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).name.equals(updatedDevice.name)) {
                devices.set(i, updatedDevice);
                found = true;
                break;
            }
        }
        if (found) {
            saveAll(devices);
        }
        return found;
    }

    // DELETE: remove device by name
    public static boolean deleteConnectionByName(String name) throws IOException {
        List<Device> devices = loadConnection();
        boolean removed = devices.removeIf(d -> d.name.equals(name));

        if (removed) {
            saveAll(devices);
        }
        return removed;
    }

    // Resolve file location
    private static File getDeviceFile() {
        if (DeviceRegistry.getPath() != null) {
            return new File(DeviceRegistry.getPath(), "devices.json");
        }
        throw new IllegalStateException("DeviceRegistry path is not set");
    }
}
