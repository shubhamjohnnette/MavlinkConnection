package com.johnnette.utils;

import java.io.*;
import java.nio.file.*;

public class DocumentsPathFinder {

//    public static void main(String[] args) {
//        File file = new File(getDocumentsFolder(), "devices.json");
//        System.out.println("Devices file path: " + file.getAbsolutePath());
//    }

    public static String getDocumentsFolder() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");

        try {
            if (os.contains("win")) {
                // Windows: Usually in %USERPROFILE%\Documents
                Path docPath = Paths.get(System.getenv("USERPROFILE"), "Documents");
                if (Files.exists(docPath)) return docPath.toString();
            }
            else if (os.contains("mac")) {
                // macOS: Usually ~/Documents
                Path docPath = Paths.get(userHome, "Documents");
                if (Files.exists(docPath)) return docPath.toString();
            }
            else {
                // Linux/Ubuntu: Use xdg-user-dir for localization support
                Process process = new ProcessBuilder("xdg-user-dir", "DOCUMENTS")
                        .redirectErrorStream(true)
                        .start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String path = reader.readLine();
                process.waitFor();

                if (path != null && !path.isBlank()) {
                    return path;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallback: Default Documents under home directory
        return Paths.get(userHome, "Documents").toString();
    }
}

