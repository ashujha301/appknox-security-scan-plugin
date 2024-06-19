package io.jenkins.plugins.appknox;

import java.io.IOException;

public class AppknoxTool {
    public static void execute(String accessToken, String filePath, String riskThreshold)
            throws IOException, InterruptedException {
        AppknoxCommands.whoAmi(accessToken);
        int fileID = AppknoxCommands.uploadFile(filePath, accessToken);
        AppknoxCommands.runCicheck(riskThreshold, fileID, accessToken);
    }
}