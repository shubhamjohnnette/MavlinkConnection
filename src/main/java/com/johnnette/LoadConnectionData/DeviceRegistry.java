package com.johnnette.LoadConnectionData;

import com.johnnette.savedConnection.Device;
import com.johnnette.utils.DocumentsPathFinder;

import java.util.ArrayList;
import java.util.List;

public class DeviceRegistry {
    public static final List<Device> devices = new ArrayList<>();

    public static String getPath() {
        return DocumentsPathFinder.getDocumentsPath(); // Always initialized
    }

    public static void setPath(String customPath) {
        DocumentsPathFinder.setDocumentsPath(customPath);
    }
}
