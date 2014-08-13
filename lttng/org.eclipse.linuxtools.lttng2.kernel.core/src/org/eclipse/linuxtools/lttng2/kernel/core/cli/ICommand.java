package org.eclipse.linuxtools.lttng2.kernel.core.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * Command line handler interface
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public interface ICommand {

    /**
     * Main command method
     * @param opts the parsed options
     */
    public void handle(CommandLine opts);

    /**
     * Create options for parsing and help, appended with addOption()
     * @param options the CLI options
     */
    public void createOptions(Options options);

}
