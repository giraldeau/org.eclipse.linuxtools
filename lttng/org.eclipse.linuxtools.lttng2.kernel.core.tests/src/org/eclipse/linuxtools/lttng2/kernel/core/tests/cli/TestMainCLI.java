package org.eclipse.linuxtools.lttng2.kernel.core.tests.cli;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.linuxtools.lttng2.kernel.core.cli.MainCLI;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Test the command line interface
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class TestMainCLI {

    private static class DummyAppContext implements IApplicationContext {

        private static final Map<Object, Object> map = new HashMap<>();

        public DummyAppContext(String[] args) {
            map.put(MainCLI.ARGS_KEY, args);
        }

        @Override
        public Map getArguments() {
            return map;
        }

        @Override
        public void applicationRunning() {
        }

        @Override
        public String getBrandingApplication() {
            return null;
        }

        @Override
        public String getBrandingName() {
            return null;
        }

        @Override
        public String getBrandingDescription() {
            return null;
        }

        @Override
        public String getBrandingId() {
            return null;
        }

        @Override
        public String getBrandingProperty(String key) {
            return null;
        }

        @Override
        public Bundle getBrandingBundle() {
            return null;
        }

        @Override
        public void setResult(Object result, IApplication application) {
        }

    }

    /**
     * Test available commands
     * @throws Exception any exception
     */
    @Test
    public void testAvailCommand() throws Exception {
        checkCommand("foo", MainCLI.QUIT_ERROR);
        checkCommand("base", MainCLI.QUIT_OK);
    }

    private static void checkCommand(String cmd, Integer code) throws Exception {
        MainCLI cli = new MainCLI();
        String[] args = cmd.split("\\s+"); // split on spaces
        IApplicationContext ctx = new DummyAppContext(args);
        Integer ret = (Integer) cli.start(ctx);
        assertEquals(code, ret);
    }

}
