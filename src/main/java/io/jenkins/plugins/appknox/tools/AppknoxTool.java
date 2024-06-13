package io.jenkins.plugins.appknox.tools;

import java.io.IOException;
import io.jenkins.plugins.appknox.commands.AppknoxCommands;

public class AppknoxTool {
    public static void execute(String accessToken, String filePath, String riskThreshold) throws IOException, InterruptedException {
        // AppknoxCommands.exportAccessToken(accessToken);
        AppknoxCommands.whoAmi(accessToken);
        int fileID = AppknoxCommands.uploadFile(filePath, accessToken);
        AppknoxCommands.runCicheck(riskThreshold, fileID, accessToken);
    }
}
