package io.jenkins.plugins.appknox.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class AppknoxCommands {
    private static final String binaryVersion = "1.3";

    public static void downloadAppknoxCLI() throws IOException, InterruptedException {
        String os = "";
        String os_name = System.getProperty("os.name").toLowerCase();
        if (os_name.contains("win")) {
            os = "win";
        } else if (os_name.contains("linux")) {
            os = "lin";
        } else if (os_name.contains("mac")) {
            os = "mac";
        }else if (os_name.contains("darwin")) {
            os = "mac";
        }
        System.out.println("Detected OS: " + os); // Debugging output
        String downloadUrl =  getAppknoxDownloadURL(os);

        ProcessBuilder builder = new ProcessBuilder("curl", "-LO", downloadUrl);
        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Failed to download appknox-go binary");
        }

        Files.move(Paths.get("appknox"), Paths.get(System.getProperty("user.home") + "/bin/appknox"), StandardCopyOption.REPLACE_EXISTING);
        builder = new ProcessBuilder("chmod", "+x", System.getProperty("user.home") + "/bin/appknox");
        process = builder.start();
        process.waitFor();
    }

    private static String getAppknoxDownloadURL(String os) throws IOException {
        switch (os) {
            case "lin":
                return "https://github.com/appknox/appknox-go/releases/download/" + binaryVersion + "/appknox-Linux-x86_64";
            case "mac":
                return "https://github.com/appknox/appknox-go/releases/download/" + binaryVersion + "/appknox-Darwin-x86_64";
            case "win":
                return "https://github.com/appknox/appknox-go/releases/download/" + binaryVersion + "/appknox-Windows-x86_64.exe";
            default:
                throw new IOException("Unsupported Operating System : " + os);
        }
    }

    // public static void exportAccessToken(String accessToken) throws IOException, InterruptedException {
    //     String os = System.getProperty("os.name").toLowerCase();
    //     String command;
    //     if (os.contains("win")) {
    //         // For Windows, use "set" command
    //         command = "set APPKNOX_ACCESS_TOKEN=" + accessToken;
    //     } else {
    //         // For Unix/Linux/Mac, use "export" command
    //         command = "export APPKNOX_ACCESS_TOKEN=" + accessToken;
    //     }

    //     ProcessBuilder builder;
    //     if (os.contains("win")) {
    //         builder = new ProcessBuilder("cmd", "/c", command);
    //     } else {
    //         builder = new ProcessBuilder("sh", "-c", command);
    //     }

    //     Process process = builder.start();
    //     process.waitFor();
    // }

    public static int whoAmi(String accessToken) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("appknox", "whoami","--access-token",accessToken);
        Process process = builder.start();
        process.waitFor();
        return process.exitValue();
    }

    public static int uploadFile(String filePath, String accessToken) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("appknox", "upload", filePath,"--access-token",accessToken);
        Process process = builder.start();
        process.waitFor();
        return process.exitValue();
    }

    public static int runCicheck(String riskThreshold, int fileID, String accessToken) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("appknox", "cicheck", String.valueOf(fileID), "--risk-threshold", riskThreshold,"--access-token",accessToken);
        Process process = builder.start();
        process.waitFor();
        return process.exitValue();
    }
}
