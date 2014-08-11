package org.eclipse.linuxtools.lttng2.kernel.core.cli;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Console progress bar using ascii art
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class CliProgressMonitor implements IProgressMonitor {

    private int prevPercent = 0;
    private int totalWork;

	/* Thanks to:
	 * http://nakkaya.com/2009/11/08/command-line-progress-bar/
	 */
	@SuppressWarnings("nls")
    private void printProgBar(int percent) {
	    if (percent == prevPercent) {
            return;
        }
	    prevPercent = percent;
		StringBuilder bar = new StringBuilder("[");

		for (int i = 0; i < 50; i++) {
			if (i < (percent / 2)) {
				bar.append("=");
			} else if (i == (percent / 2)) {
				bar.append(">");
			} else {
				bar.append(" ");
			}
		}

		bar.append("]   " + percent + "%     ");
		System.err.print("\r" + bar.toString());
	}

	@Override
	public void beginTask(String name, int maxWork) {
		this.totalWork = maxWork;
	}

	@Override
	public void done() {
	    System.err.print("\n");
	}

	@Override
	public void internalWorked(double work) {
	    printProgBar((int) work * 100);
	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public void setCanceled(boolean value) {
	}

	@Override
	public void setTaskName(String name) {
	}

	@Override
	public void subTask(String name) {
	}

	@Override
	public void worked(int work) {
	    int progress = 100;
	    if (totalWork > 0) {
	        progress = 100 * work / totalWork;
	    }
		printProgBar(progress);
	}

}
