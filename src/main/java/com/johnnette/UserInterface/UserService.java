package com.johnnette.UserInterface;

import com.johnnette.Connection.ConnectionManager;
import com.johnnette.LoadConnectionData.DeviceRegistry;
import com.johnnette.MavlinkManager.MavlinkConnectionService;
import com.johnnette.MavlinkManager.MavlinkManager;
import com.johnnette.savedConnection.Device;
import com.johnnette.savedConnection.DeviceJsonUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    public static void autoConnect(){
        ConnectionManager manager = new ConnectionManager();
        manager.autoConnect();

    }
    public static void connect(Device connect) {
        ConnectionManager manager = new ConnectionManager();
        manager.mavStartService(connect);
    }


    public static void editConnection(Device connect) {
        try {
            DeviceJsonUtil.updateConnection(connect);   
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    public static void deleteConnection(String name) {
        try{
            DeviceJsonUtil.deleteConnectionByName(name);     
        }
       catch (IOException e){
            throw  new RuntimeException();
       }

    }


    public static void disconnect() {
        MavlinkConnectionService.stop();
    }


    public static void addConnection(Device connect) {
        try{
            DeviceJsonUtil.addConnection(connect);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static List<Device> getConnectionList()
    {
        return DeviceRegistry.devices;
    }

    public static void setFilePath(String path) {
        DeviceRegistry.setPath(path);
    }
    public static String getFilePath(){
       return DeviceRegistry.getPath();
    }
}
