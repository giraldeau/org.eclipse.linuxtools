/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.internal.systemtap.ui.ide.structures;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * A helper class for performing tapset-loading operations,
 * and sharing the results with other {@link TapsetParser}s.
 * Note that class this does not implement {@link ITreeParser},
 * as no front-end operations are performed.
 */
public final class SharedParser extends TapsetParser {

    private static final String[] STAP_OPTIONS = new String[] {"-v", "-p1", "-e"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    private static final String STAP_DUMMYPROBE = "probe begin{}"; //$NON-NLS-1$

    /*
     * Note: tapset content dumps (as printed by a pass 1 call of stap) list the contents
     * of each script file found in all tapset directores. The contents of each file are
     * prefaced/tagged with the line "# file <filename>", so contents of individual files
     * can be found by searching the dump for such a tag.
     */
    /**
     * The start of the tag that is always printed immediately before a file's contents in
     * a tapset dump. Useful for searching through dumps for the contents of an individual file.
     */
    static final String TAG_FILE = "# file "; //$NON-NLS-1$
    private static final Pattern FILE_PATTERN = Pattern.compile(TAG_FILE.concat("(/.*\\.stp)")); //$NON-NLS-1$

    /**
     * Returns the entire tag that is printed immediately before a given file's contents in
     * a tapset dump.
     * @param fileName The name of the file (or a regex string) to get a tag for.
     * @return The tag for the given file's contents as it appears in the tapset dump.
     */
    static String makeFileTag(String fileName) {
        return TAG_FILE.concat(fileName);
    }

    /**
     * Searches a string of tapset contents for a file tag and extracts the filename
     * found in the tag.
     * @param contents The tapset contents to search through. Preferably pass just a file tag
     * here (generated by a call to {@linkplain #makeFileTag(String)}.
     * @return The file name found.
     */
    static String findFileNameInTag(String contents) {
        Matcher matcher = FILE_PATTERN.matcher(contents);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String tapsetContents = null;

    private static SharedParser parser = null;
    public static SharedParser getInstance(){
        if (parser != null) {
            return parser;
        }
        parser = new SharedParser();
        return parser;
    }

    private SharedParser() {
        super(Messages.SharedParser_name);
    }

    /**
     * Clear the cached tapset contents, so that the next call to {@link #getTapsetContents()}
     * will be guaranteed to use a new call of stap to gather up-to-date tapset contents.
     */
    synchronized void clearTapsetContents() {
        tapsetContents = null;
    }

    /**
     * Get the contents of default & all imported tapsets. When calling this method
     * for the first time (or after changing the list of tapset directories), this will
     * run a dummy stap script to obtain the tapset contents, which will be cached into
     * memory. Subsequent calls will simply read the saved contents.
     * @return The string contents of tapsets, or <code>null</code> if there was an
     * error in obtaining this information.
     */
    synchronized String getTapsetContents() {
        if (tapsetContents == null) {
            run(null);
        }
        return tapsetContents;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        String contents = runStap(STAP_OPTIONS, STAP_DUMMYPROBE, false);
        int result = verifyRunResult(contents);
        if (result != IStatus.OK) {
            return createStatus(result);
        }
        // Exclude the dump of the test script by excluding everything before the second pathname
        // (which is the first actual tapset file, not the input script).
        int firstTagIndex = contents.indexOf(TAG_FILE);
        if (firstTagIndex != -1) {
            int beginIndex = contents.indexOf(TAG_FILE, firstTagIndex + 1);
            if (beginIndex != -1) {
                tapsetContents = contents.substring(beginIndex);
            }
        }
        return createStatus(IStatus.OK);
    }

}
