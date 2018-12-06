package com.apollocurrency.aplwallet.apl.exec;

import com.apollocurrency.aplwallet.apl.core.app.AplCore;
import static com.apollocurrency.aplwallet.apl.core.app.AplCore.runtimeMode;
import com.apollocurrency.aplwallet.apl.core.app.AplGlobalObjects;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Apollo startup class
 *
 * @author alukin@gmail.com
 */
public class Apollo {

    private AplCore core = new AplCore();
    private static Logger LOG = LoggerFactory.getLogger(Apollo.class);

    private void initCore() {
        core.init();
    }

    private static void printCommandLineArguments(String[] args) {
        System.out.println("Command line arguments: " + Arrays.toString(args));
    }

    private void initUpdater() {
        if (!core.getBooleanProperty("apl.allowUpdates", false)) {
            return;
        }
        AplGlobalObjects.createUpdaterCore(true);
    }

    private void initAppStatusMsg() {

    }

    private void launchDesktopApplication() {
        core.runtimeMode.launchDesktopApplication();
    }
    public static void shutdown(){
        AplCore.shutdown();
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Initializing " + AplCore.APPLICATION + " server version " + AplCore.VERSION);
        printCommandLineArguments(args);
        Apollo app = new Apollo();
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(Apollo::shutdown));
            app.initCore();
            app.initAppStatusMsg();
            if (app.core.isDesktopApplicationEnabled()) {
                runtimeMode.updateAppStatus("Starting desktop application...");
                app.launchDesktopApplication();
            }
            app.initUpdater();

        } catch (Throwable t) {
            System.out.println("Fatal error: " + t.toString());
            t.printStackTrace();
        }
    }

}
