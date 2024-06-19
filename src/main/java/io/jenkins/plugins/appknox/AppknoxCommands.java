package io.jenkins.plugins.appknox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class AppknoxCommands {
    private static final String binaryVersion = "1.3";

    public static void downloadAppknoxCLI() throws IOException, InterruptedException {
        String os_name = System.getProperty("os.name").toLowerCase();
        String os = normalizeOs(os_name);
        String downloadUrl = getAppknoxDownloadURL(os);

        String binaryName = "appknox";
        if (os.equals("windows")) {
            binaryName += ".exe";
        }

        ProcessBuilder builder = new ProcessBuilder("curl", "-LO", downloadUrl);
        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Failed to download appknox-go binary");
        }

        // Rename downloaded binary to 'appknox' or 'appknox.exe' for Windows
        Files.move(Paths.get(getFileNameFromUrl(downloadUrl)), Paths.get(binaryName),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private static String normalizeOs(String os_name) {
        if (os_name.contains("windows")) {
            return "windows";
        } else if (os_name.contains("linux")) {
            return "linux";
        } else if (os_name.contains("mac") || os_name.contains("darwin")) {
            return "mac";
        }
        return null;
    }

    private static String getAppknoxDownloadURL(String os) throws IOException {
        switch (os) {
            case "linux":
                return "https://github.com/appknox/appknox-go/releases/download/" + binaryVersion
                        + "/appknox-Linux-x86_64";
            case "mac":
                return "https://github.com/appknox/appknox-go/releases/download/" + binaryVersion
                        + "/appknox-Darwin-x86_64";
            case "windows":
                return "https://github.com/ashujha301/appknox-go/releases/download/" + binaryVersion
                        + "/appknox-Windows-x86_64.exe";
            default:
                throw new IOException("Unsupported Operating System : " + os);
        }
    }

    private static String getFileNameFromUrl(String downloadUrl) {
        return downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
    }

    public static int whoAmi(String accessToken) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("appknox", "whoami", "--access-token", accessToken);
        Process process = builder.start();
        process.waitFor();
        return process.exitValue();
    }

    public static int uploadFile(String filePath, String accessToken) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("appknox", "upload", filePath, "--access-token", accessToken);
        Process process = builder.start();
        process.waitFor();
        return process.exitValue();
    }

    public static int runCicheck(String riskThreshold, int fileID, String accessToken)
            throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("appknox", "cicheck", String.valueOf(fileID), "--risk-threshold",
                riskThreshold, "--access-token", accessToken);
        Process process = builder.start();
        process.waitFor();
        return process.exitValue();
    }
}