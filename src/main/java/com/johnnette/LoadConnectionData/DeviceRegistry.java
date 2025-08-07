package com.johnnette.LoadConnectionData;

import com.johnnette.savedConnection.Device;
import com.johnnette.utils.DocumentsPathFinder;

import java.util.ArrayList;
import java.util.List;

public class DeviceRegistry {
    public static final List<Device> devices = new ArrayList<>();

    // Start with default path
    private static String path = DocumentsPathFinder.DOCUMENTS_PATH;

    public static String getPath() {
        return path;
    }

    public static void setPath(String customPath) {
        path = customPath;
    }
}
