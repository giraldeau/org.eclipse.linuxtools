package org.eclipse.linuxtools.internal.lttng2.kernel.core.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Yet Another Log Facility
 *
 * Because TmfTracerCore is not exported and in different bundle
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
@SuppressWarnings("nls")
public class ALog {

	private BufferedWriter buf;
	private static ALog instance;

	private ALog() {
	}

	/**
	 * Return singleton instance
	 * @return the instance
	 */
	public static ALog getInstance() {
	    if (instance == null) {
            instance = new ALog();
        }
	    return instance;
	}

	/**
	 * Initialize the logger
	 *
	 * @param path the log file path
	 */
	public void init(String path) {
        /*
         * FIXME: Platform.getDebugOption() always returns null, and there are
         * no entries in the Run configuration -> Tracing for the plug-in, even
         * if the .options file is there and has the right content. What is
         * wrong? Let's enable always until we find the problem.
         */

	    /*
	    String value = Platform.getDebugOption(Activator.PLUGIN_ID + "/kernel"); //$NON-NLS-1$
        if (value == null || !Boolean.valueOf(value)) {
            System.out.println("TmfKernel logging not enabled"); //$NON-NLS-1$
            return;
        }
        */
		try {
			buf = new BufferedWriter(new FileWriter(path));
			System.out.println("ALog: " + new File(path).getAbsolutePath());
		} catch (IOException e) {
			System.err.println(String.format("warning: unable to open file %s", path));
		}
	}

	/**
	 * Write the string msg to the log
	 *
	 * @param msg the log message
	 */
	public void entry(String msg) {
		if (buf == null) {
            return;
        }
		try {
			buf.write(String.format("%s\n", msg));
			buf.flush();
		} catch (IOException e) {
		    close();
		}
	}

	/**
	 * Flush and close the log file
	 */
	public void close() {
	    if (buf == null) {
            return;
        }
	    try {
	        buf.flush();
            buf.close();
        } catch (IOException e) {
            System.err.println("exception while closing log");
        }
	    buf = null;
	}

}
