package com.johnnette.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Cross-platform utility to find (and create if missing) the Documents folder.
 * Works on:
 *  - Android
 *  - Windows
 *  - macOS
 *  - Linux
 */
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
            documentsPath = Paths.get(customPath).toAbsolutePath().normalize().toString();
            ensureDirectoryExists(documentsPath);
        }
    }

    private static String resolveDefaultDocumentsPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");

        try {
            // ✅ Detect Android
            if (isAndroid()) {
                try {
                    // Use reflection to avoid compile-time Android dependency
                    Class<?> envClass = Class.forName("android.os.Environment");
                    Object directoryType = envClass.getField("DIRECTORY_DOCUMENTS").get(null);
                    java.io.File docsDir = (java.io.File) envClass
                            .getMethod("getExternalStoragePublicDirectory", String.class)
                            .invoke(null, directoryType);

                    if (docsDir != null) {
                        ensureDirectoryExists(docsDir.getAbsolutePath());
                        return docsDir.getAbsolutePath();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // ✅ Windows
            if (os.contains("win")) {
                Path docPath = Paths.get(System.getenv("USERPROFILE"), "Documents");
                ensureDirectoryExists(docPath.toString());
                return docPath.toString();
            }

            // ✅ macOS
            if (os.contains("mac")) {
                Path docPath = Paths.get(userHome, "Documents");
                ensureDirectoryExists(docPath.toString());
                return docPath.toString();
            }

            // ✅ Linux
            try {
                Process process = new ProcessBuilder("xdg-user-dir", "DOCUMENTS")
                        .redirectErrorStream(true)
                        .start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String path = reader.readLine();
                    process.waitFor();

                    if (path != null && !path.isBlank()) {
                        ensureDirectoryExists(path);
                        return path;
                    }
                }
            } catch (Exception ignored) {
                // fallback below
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallback (any OS)
        Path fallback = Paths.get(userHome, "Documents");
        ensureDirectoryExists(fallback.toString());
        return fallback.toString();
    }

    private static void ensureDirectoryExists(String dirPath) {
        try {
            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isAndroid() {
        try {
            Class.forName("android.os.Build");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
