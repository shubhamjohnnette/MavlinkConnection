package com.johnnette.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.*;

public class DocumentsPathFinder {

    private static String documentsPath = null;

    public static String getDocumentsPath() {
        if (documentsPath == null) {
            documentsPath = resolveDefaultDocumentsPath();
        }
        return documentsPath;
    }

    public static void setDocumentsPath(String customPath) {
        if (customPath != null && !customPath.isBlank()) {
            documentsPath = customPath;
        }
    }

    private static String resolveDefaultDocumentsPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");

        try {
            if (os.contains("win")) {
                Path docPath = Paths.get(System.getenv("USERPROFILE"), "Documents");
                if (Files.exists(docPath)) return docPath.toString();
            } else if (os.contains("mac")) {
                Path docPath = Paths.get(userHome, "Documents");
                if (Files.exists(docPath)) return docPath.toString();
            } else {
                Process process = new ProcessBuilder("xdg-user-dir", "DOCUMENTS")
                        .redirectErrorStream(true)
                        .start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String path = reader.readLine();
                process.waitFor();

                if (path != null && !path.isBlank()) return path;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallback
        return Paths.get(userHome, "Documents").toString();
    }
}
