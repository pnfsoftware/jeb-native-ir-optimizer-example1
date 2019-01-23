import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import com.pnfsoftware.jeb.client.HeadlessClientContext;
import com.pnfsoftware.jeb.core.Artifact;
import com.pnfsoftware.jeb.core.IEnginesContext;
import com.pnfsoftware.jeb.core.ILiveArtifact;
import com.pnfsoftware.jeb.core.IRuntimeProject;
import com.pnfsoftware.jeb.core.exceptions.JebException;
import com.pnfsoftware.jeb.core.input.FileInput;
import com.pnfsoftware.jeb.core.units.INativeCodeUnit;
import com.pnfsoftware.jeb.core.units.IUnit;
import com.pnfsoftware.jeb.core.units.UnitUtil;
import com.pnfsoftware.jeb.core.units.code.asm.decompiler.INativeDecompilerUnit;
import com.pnfsoftware.jeb.core.units.code.asm.decompiler.INativeSourceUnit;
import com.pnfsoftware.jeb.core.util.DecompilerHelper;
import com.pnfsoftware.jeb.util.io.IO;
import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;

/**
 * Headless JEB client to test a custom native IR optimizer. (Can be adapted for any other type of
 * plugin development.)
 * 
 * @author Nicolas Falliere
 *
 */
public class Tester {
    private static final ILogger logger = GlobalLog.getLogger(Tester.class);

    static {
        try {
            GlobalLog.addDestinationStream(new PrintStream(new File(IO.getTempFolder(), "jeb-plugin-tester.log")));
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws JebException, IOException {
        // create a simple JEB client object; our subclass here is to provide an isDevMode() override and always
        // force development mode, regardless of the settings in jeb-client.cfg
        // (this client will make use of your [JEB]/bin/{jeb-engines.cfg,jeb-client.cfg} configuration files)
        HeadlessClientContext client = new HeadlessClientContext() {
            @Override
            public boolean isDevelopmentMode() {
                return true;
            }
        };

        // initialize and start the client
        client.initialize(args);
        client.start();

        // hot-load the IR optimizer plugin that is being worked on:
        String classpath = new File("bin").getAbsolutePath();
        String classname = EOptExample1.class.getName();
        client.getEnginesContext().getPluginManager().load(classpath, classname);
        // note that an alternative to the 3 lines above is to specify that plugin in the your jeb-engines.cfg file, eg:
        //   .DevPluginClasspath = /.../.../jeb-native-ir-optimizer-example1/bin
        //   .DevPluginClassnames = EOptExample1:1

        try {
            testPlugin(client, new File("samples/1.exe"), "sub_401171");
        }
        catch(Exception e) {
            logger.catching(e);
        }

        client.stop();
    }

    static void testPlugin(HeadlessClientContext client, File binaryFile, String routineName) throws Exception {
        IEnginesContext engctx = client.getEnginesContext();
        IRuntimeProject prj = engctx.loadProject("ProjectTest");
        ILiveArtifact a = prj.processArtifact(new Artifact(binaryFile.getName(), new FileInput(binaryFile)));
        IUnit topLevelUnit = a.getMainUnit();

        INativeCodeUnit<?> cu = (INativeCodeUnit<?>)UnitUtil
                .findDescendantsByType(topLevelUnit, INativeCodeUnit.class, false).get(0);
        logger.info("Code unit: %s", cu);

        INativeDecompilerUnit<?> decomp = (INativeDecompilerUnit<?>)DecompilerHelper.getDecompiler(cu);
        logger.info("Decompiler: %s", decomp);

        INativeSourceUnit src = decomp.decompile(routineName);
        logger.info("SOURCE =>\n%s", src.getRootElement());
    }
}
