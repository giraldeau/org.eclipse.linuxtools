package org.eclipse.linuxtools.lttng2.kernel.core.cli;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

@SuppressWarnings({"javadoc", "nls"})
public class MainCLI implements IApplication {

    private static final Map<String, ICommand> commands = new HashMap<>();
    static {
        commands.put("base", new BaseCommand());
        commands.put("list", new ListTaskCommand());
        commands.put("bench", new BenchCommand());
    }

    public static final String ARGS_KEY = "application.args";
    public static final Integer QUIT_OK = new Integer(0);
    public static final Integer QUIT_ERROR = new Integer(-1);

    @Override
    public Object start(IApplicationContext context) throws Exception {
        System.out.println("MainCLI start");
        String[] arguments = (String[]) context.getArguments().get(ARGS_KEY);
        System.out.println("args: " + Arrays.deepToString(arguments));

        if (arguments.length == 0) {
            showUsage("specify a command to execute");
            return QUIT_ERROR;
        }

        String cmdName = arguments[0];
        // help built-in command
        if (cmdName.equals("help") && arguments.length > 1) {
            showCommandUsage(arguments[1]);
            return QUIT_ERROR;
        }

        if (!commands.containsKey(cmdName)) {
            showUsage("unkown commmand " + cmdName);
            return QUIT_ERROR;
        }

        CommandLineParser parser = new GnuParser();
        Options opts = new Options();
        ICommand cmdHandler = commands.get(cmdName);
        cmdHandler.createOptions(opts);
        String[] cleanArgs = Arrays.copyOfRange(arguments, 1, arguments.length);
        try {
            CommandLine line = parser.parse(opts, cleanArgs);
            cmdHandler.handle(line);
        } catch (ParseException exp) {
            System.out.println(exp.getMessage());
            return QUIT_ERROR;
        }

        return IApplication.EXIT_OK;
    }

    private static void showCommandUsage(String cmd) {
        if (!commands.containsKey(cmd)) {
            showUsage("unkown command " + cmd);
            return;
        }
        Options opts = new Options();
        ICommand cmdHandler = commands.get(cmd);
        cmdHandler.createOptions(opts);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(cmd, opts);
    }

    private static void showUsage(String msg) {
        System.out.println("message: " + msg);
        System.out.println("Commands:");
        for (String cmd: commands.keySet()) {
            System.out.println("  " + cmd);
        }
    }

    @Override
    public void stop() {
        // weird: stop method is never called
        // System.out.println("MainCLI stop");
    }

}
