package org.eclipse.linuxtools.lttng2.kernel.core.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * Abstract base command handler
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class BaseCommand implements ICommand {

    @Override
    public void handle(CommandLine opts) {
        System.out.println(opts.hasOption("t"));
    }

    @Override
    public void createOptions(Options options) {
        options.addOption("t", "test", false, "hello");
    }

}
