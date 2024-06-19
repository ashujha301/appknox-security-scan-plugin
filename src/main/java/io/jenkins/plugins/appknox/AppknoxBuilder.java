package io.jenkins.plugins.appknox;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

public class AppknoxBuilder extends Notifier {

    private final String accessToken;
    private final String filePath;
    private final String riskThreshold;

    @DataBoundConstructor
    public AppknoxBuilder(String accessToken, String filePath, String riskThreshold) {
        this.accessToken = accessToken;
        this.filePath = filePath;
        this.riskThreshold = riskThreshold;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getRiskThreshold() {
        return riskThreshold;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        listener.getLogger().println("Starting Appknox security scan...");

        try {
            // Download Appknox CLI
            AppknoxCommands.downloadAppknoxCLI();
            listener.getLogger().println("Appknox CLI downloaded successfully.");

            // Authenticate and get user details
            int whoamiExitCode = AppknoxCommands.whoAmi(accessToken);
            if (whoamiExitCode != 0) {
                listener.getLogger().println("Authentication with Appknox failed.");
                return false;
            }
            listener.getLogger().println("Authenticated with Appknox successfully.");

            // Upload file and get file ID
            int fileID = AppknoxCommands.uploadFile(filePath, accessToken);
            listener.getLogger().println("File uploaded to Appknox successfully.");

            // Run CI check with risk threshold
            int cicheckExitCode = AppknoxCommands.runCicheck(riskThreshold, fileID, accessToken);
            if (cicheckExitCode != 0) {
                listener.getLogger().println("CI check with Appknox failed.");
                return false;
            }
            listener.getLogger().println("CI check with Appknox completed successfully.");
            return true;
        } catch (Exception e) {
            listener.getLogger().println("Error during Appknox scan: " + e.getMessage());
            e.printStackTrace(listener.getLogger());
            return false;
        }
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            load();
        }

        public FormValidation doCheckAccessToken(@QueryParameter String value) {
            if (value.isEmpty()) {
                return FormValidation.error("Please provide an access token.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckFilePath(@QueryParameter String value) {
            if (value.isEmpty()) {
                return FormValidation.error("Please provide a file path.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckRiskThreshold(@QueryParameter String value) {
            if (value.isEmpty()) {
                return FormValidation.error("Please provide a risk threshold.");
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Appknox Security Scan";
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
}
