package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.cli;

import java.util.Arrays;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

@SuppressWarnings({"javadoc", "nls"})
public class MainCLI implements IApplication {

    @Override
    public Object start(IApplicationContext context) throws Exception {
        System.out.println("MainCLI start");
        String[] arguments = (String[]) context.getArguments().get("application.args");
        System.out.println("args: " + Arrays.deepToString(arguments));
        return IApplication.EXIT_OK;
    }

    @Override
    public void stop() {
        System.out.println("MainCLI stop");
    }

}
