/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater.pdu;

import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdateInfo;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;

import java.io.IOException;
import java.nio.file.Path;

public class DefaultPlatformDependentUpdater extends AbstractPlatformDependentUpdater {
    private String runTool;
    private String updateScriptPath;

    public DefaultPlatformDependentUpdater(String runTool, String updateScriptPath, UpdaterMediator updaterMediator, UpdateInfo updateInfo) {
        super(updaterMediator, updateInfo);
        this.runTool = runTool;
        this.updateScriptPath = updateScriptPath;
    }

    @Override
    Process runCommand(Path updateDirectory, Path workingDirectory, Path appDirectory, boolean userMode) throws IOException {
        String[] cmdArray = new String[] {
                runTool,
                updateDirectory.resolve(updateScriptPath).toAbsolutePath().toString(), //path to update script should include all subfolders
                appDirectory.toAbsolutePath().toString(),
                updateDirectory.toAbsolutePath().toString(),
                String.valueOf(userMode)
        };
        return Runtime.getRuntime().exec(cmdArray, null, workingDirectory.toFile());
    }
}
