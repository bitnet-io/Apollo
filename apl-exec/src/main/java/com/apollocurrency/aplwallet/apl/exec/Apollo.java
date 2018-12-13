package com.apollocurrency.aplwallet.apl.exec;

import com.apollocurrency.aplwallet.apl.core.app.AplCore;
import com.apollocurrency.aplwallet.apl.core.app.AplCoreRuntime;
import com.apollocurrency.aplwallet.apl.core.app.AplGlobalObjects;
import com.apollocurrency.aplwallet.apl.core.app.Constants;
import com.apollocurrency.aplwallet.apl.core.app.UpdaterMediatorImpl;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterCore;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterMediator;
import com.apollocurrency.aplwallet.apl.updater.core.UpdaterCoreImpl;
import com.apollocurrency.aplwallet.apl.util.AppStatus;
import com.apollocurrency.aplwallet.apl.util.AppStatusUpdater;
import com.apollocurrency.aplwallet.apl.util.cdi.AplContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.apollocurrency.aplwallet.apl.util.env.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.PropertiesLoader;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeMode;
import com.apollocurrency.aplwallet.apldesktop.DesktopMode;
import com.beust.jcommander.JCommander;

/**
 * Main Apollo startup class
 *
 * @author alukin@gmail.com
 */
public class Apollo {

    private static Logger log;// = LoggerFactory.getLogger(Apollo.class);

    public static RuntimeMode runtimeMode;
    public static DirProvider dirProvider;
    
    private static AplContainer container;
    
    private static AplCore core;
    private static AplGlobalObjects aplGlobalObjects; // TODO: YL remove static later

    private static PropertiesLoader propertiesLoader;
    
    private void initCore() {
        AplCoreRuntime.getInstance().setup(runtimeMode, dirProvider);
        core = new AplCore();
        AplCoreRuntime.getInstance().addCore(core);
        core.init();
    }

    private void initUpdater() {
        if (aplGlobalObjects == null) {
            aplGlobalObjects = CDI.current().select(AplGlobalObjects.class).get();
        }
        if (!propertiesLoader.getBooleanProperty("apl.allowUpdates", false)) {
            return;
        }
        UpdaterMediator mediator = new UpdaterMediatorImpl();
        UpdaterCore updaterCore = new UpdaterCoreImpl(mediator);
        AplGlobalObjects.createUpdaterCore(true, updaterCore);
    }

    private void initAppStatusMsg() {
        AppStatus.setUpdater(new AppStatusUpdater() {
            @Override
            public void updateStatus(String status) {
                runtimeMode.updateAppStatus(status);
            }

            @Override
            public void alert(String message) {
                runtimeMode.alert(message);
            }

            @Override
            public void error(String message) {
                runtimeMode.displayError(message);
            }
        });
    }

    private void launchDesktopApplication() {
        runtimeMode.launchDesktopApplication();
    }

    public static void shutdown() {
        container.shutdown(); 
        core.shutdown();
    }

    private static void redirectSystemStreams(String streamName) {
        String isStandardRedirect = System.getProperty("apl.redirect.system." + streamName);
        Path path = null;
        if (isStandardRedirect != null) {
            try {
                path = Files.createTempFile("apl.system." + streamName + ".", ".log");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } else {
            String explicitFileName = System.getProperty("apl.system." + streamName);
            if (explicitFileName != null) {
                path = Paths.get(explicitFileName);
            }
        }
        if (path != null) {
            try {
                PrintStream stream = new PrintStream(Files.newOutputStream(path));
                if (streamName.equals("out")) {
                    System.setOut(new PrintStream(stream));
                } else {
                    System.setErr(new PrintStream(stream));
                }
            } catch (IOException e) {
                // e.printStackTrace();
            }
        }
    }

    /**
     * @param argv the command line arguments
     */
    public static void main(String[] argv) {

        Apollo app = new Apollo();
        container = AplContainer.builder().containerId("MAIN-APL-CDI").recursiveScanPackages(AplCore.class)
                        .annotatedDiscoveryMode().build();
                  
        System.out.println("Initializing " + Constants.APPLICATION + " server version " + Constants.VERSION);
        CmdLineArgs args = new CmdLineArgs();
        JCommander jc = JCommander.newBuilder()
                .addObject(args)
                .build();
        jc.setProgramName(Constants.APPLICATION);
        try {
            jc.parse(argv);
        } catch (RuntimeException ex) {
            System.err.println("Error parsing command line arguments.");
            System.err.println(ex.getMessage());
            jc.usage();
            System.exit(PosixExitCodes.EX_USAGE.exitCode());
        }
        if (args.help) {
            jc.usage();
            System.exit(PosixExitCodes.OK.exitCode());
        }

        dirProvider = RuntimeEnvironment.getDirProvider();
//TODO: remove this plumb, descktop UI should be separate and use Core's API            
        if (RuntimeEnvironment.isDesktopApplicationEnabled()) {
            runtimeMode = new DesktopMode();
        } else {
            runtimeMode = RuntimeEnvironment.getRuntimeMode();
        }
        runtimeMode.init();
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(Apollo::shutdown));
            app.initCore();
            log = LoggerFactory.getLogger(Apollo.class);
//           redirectSystemStreams("out");
//            redirectSystemStreams("err");
            app.initAppStatusMsg();
            app.launchDesktopApplication();
            app.initUpdater();

        } catch (Throwable t) {
            System.out.println("Fatal error: " + t.toString());
            t.printStackTrace();
        }
    }

}
