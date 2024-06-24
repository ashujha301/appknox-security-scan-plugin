package io.jenkins.plugins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import hudson.util.FormValidation;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;

public class AppknoxPlugin extends Builder implements SimpleBuildStep {
    private final String credentialsId;
    private final String filePath;
    private final String riskThreshold;
    private static final String binaryVersion = "1.3.1";
    private static final String CLI_DOWNLOAD_PATH = System.getProperty("user.home") + File.separator + "appknox";

    @DataBoundConstructor
    public AppknoxPlugin(String accessTokenID, String filePath, String riskThreshold) {
        this.credentialsId = accessTokenID;
        this.filePath = filePath;
        this.riskThreshold = riskThreshold;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getRiskThreshold() {
        return riskThreshold;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        boolean success = executeAppknoxCommands(listener);
        if (!success) {
            run.setResult(Result.FAILURE);
        }
    }

    private boolean executeAppknoxCommands(TaskListener listener) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String appknoxPath = downloadAndInstallAppknox(os, listener);
    
            String uploadOutput = uploadFile(appknoxPath, listener);
            String fileID = extractFileID(uploadOutput, listener);
            if (fileID == null) {
                return false;
            }
    
            return runCICheck(appknoxPath, fileID, listener);
        } catch (Exception e) {
            listener.getLogger().println("Error executing Appknox commands: " + e.getMessage());
            return false;
        }
    }

    private String extractFileID(String uploadOutput, TaskListener listener) {
    String[] lines = uploadOutput.split("\n");
    if (lines.length > 0) {
        String lastLine = lines[lines.length - 1].trim();
        try {
            return lastLine;
        } catch (NumberFormatException e) {
            listener.getLogger().println("Failed to extract file ID from upload output: " + lastLine);
            return null;
        }
    } else {
        listener.getLogger().println("Upload output does not contain any lines.");
        return null;
    }
}

    private String downloadAndInstallAppknox(String os, TaskListener listener) throws IOException, InterruptedException {
        String appknoxURL = getAppknoxDownloadURL(os);
        File appknoxFile = new File(CLI_DOWNLOAD_PATH);

        if (!appknoxFile.exists()) {
            listener.getLogger().println("Downloading Appknox CLI...");
            downloadFile(appknoxURL, CLI_DOWNLOAD_PATH, listener);
            listener.getLogger().println("Appknox CLI downloaded successfully.");
        } else {
            listener.getLogger().println("Appknox CLI already exists at: " + CLI_DOWNLOAD_PATH);
        }

        addPathToEnvironment(CLI_DOWNLOAD_PATH, listener);
        return CLI_DOWNLOAD_PATH;
    }

    private String getAppknoxDownloadURL(String os) {
        String binaryName;
        if (os.contains("win")) {
            binaryName = "appknox-Windows-x86_64.exe";
        } else if (os.contains("mac")) {
            binaryName = "appknox-Darwin-x86_64";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            binaryName = "appknox-Linux-x86_64";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system for Appknox CLI download.");
        }

        return "https://github.com/appknox/appknox-go/releases/download/" + binaryVersion + "/" + binaryName;
    }

    private void downloadFile(String url, String destinationPath, TaskListener listener) throws IOException {
        @SuppressWarnings("deprecation")
        URL downloadUrl = new URL(url);
        File destinationFile = new File(destinationPath);

        // Ensure the parent directory exists
        File parentDir = destinationFile.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create directories: " + parentDir.getAbsolutePath());
        }

        FileUtils.copyURLToFile(downloadUrl, destinationFile);

        // Make the file executable (for Unix-based systems)
        if (!System.getProperty("os.name").toLowerCase().contains("win") && !destinationFile.setExecutable(true)) {
            listener.getLogger().println("Failed to set executable permission for: " + destinationPath);
        }
    }

    private void addPathToEnvironment(String path, TaskListener listener) {
        String existingPath = System.getenv("PATH");
        String newPath = path + File.pathSeparator + existingPath;
        System.setProperty("PATH", newPath);
        listener.getLogger().println("Updated PATH: " + newPath);
    }

    private String uploadFile(String appknoxPath, TaskListener listener) throws IOException, InterruptedException {
        String accessToken = getAccessToken(listener);
        if (accessToken == null) {
            return null;
        }
        
        List<String> command = new ArrayList<>();
        command.add(appknoxPath);
        command.add("upload");
        command.add(filePath);
        command.add("--access-token");
        command.add(accessToken);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            listener.getLogger().println("Upload Output:");
            listener.getLogger().println(output.toString());

            if (process.exitValue() == 0) {
                return output.toString().trim();
            } else {
                listener.getLogger().println("Upload failed.");
                return null;
            }
        }
    }

    private boolean runCICheck(String appknoxPath, String fileID, TaskListener listener) throws IOException, InterruptedException {
        String accessToken = getAccessToken(listener);
        if (accessToken == null) {
            return false;
        }
        
        List<String> command = new ArrayList<>();
        command.add(appknoxPath);
        command.add("cicheck");
        command.add(fileID);
        command.add("--access-token");
        command.add(accessToken);
        command.add("--risk-threshold");
        command.add(riskThreshold);
    
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
    
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder output = new StringBuilder();
            String line;
            boolean foundStarted = false;
    
            while ((line = reader.readLine()) != null) {
                if (!foundStarted) {
                    // Skip lines until "Found" is encountered
                    if (line.contains("Found")) {
                        output.append(line).append("\n");
                        foundStarted = true;
                    }
                } else {
                    // Start capturing output after "Found"
                    output.append(line).append("\n");
                }
            }

            if (!foundStarted) {
                listener.getLogger().println("No 'Found' line encountered in the output.");
                return false; 
            }
            listener.getLogger().println("CICheck Output:");
            listener.getLogger().println(output.toString());
    
            return process.exitValue() == 0;
        }
    }

    private String getAccessToken(TaskListener listener) {
        Jenkins jenkins = Jenkins.get();
        StringCredentials credentials = CredentialsMatchers.firstOrNull(
            CredentialsProvider.lookupCredentials(StringCredentials.class, jenkins, ACL.SYSTEM, URIRequirementBuilder.create().build()),
            CredentialsMatchers.withId(credentialsId)
        );

        if (credentials != null) {
            return credentials.getSecret().getPlainText();
        } else {
            listener.getLogger().println("Failed to retrieve access token from credentials.");
            return null;
        }
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public DescriptorImpl() {
            super(AppknoxPlugin.class);
            load();
        }

        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Appknox Plugin";
        }

        public FormValidation doCheckFilePath(@QueryParameter String value) {
            if (value.isEmpty()) {
                return FormValidation.error("File Path must not be empty");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckRiskThreshold(@QueryParameter String value) {
            if (value.isEmpty()) {
                return FormValidation.error("Risk Threshold must not be empty");
            }
            return FormValidation.ok();
        }
    }
}