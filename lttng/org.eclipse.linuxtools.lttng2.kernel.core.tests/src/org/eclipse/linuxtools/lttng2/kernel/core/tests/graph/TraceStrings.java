package org.eclipse.linuxtools.lttng2.kernel.core.tests.graph;

/**
 * Strings to retrieve traces
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class TraceStrings {

    /**
     * Base trace directory
     */
    public static final String TRACE_DIR = "traces";

    /**
     * rcp-hog experiment on two hosts
     * http://secretaire.dorsal.polymtl.ca/~fgiraldeau/traces/phd-hog.tar.gz
     */
    public static final String EXP_PHD_HOG = "phd-hog";

    /**
     * django-index experiment on three hosts
     * http://secretaire.dorsal.polymtl.ca
     * /~fgiraldeau/traces/django-index.tar.gz
     */
    public static final String EXP_DJANGO_INDEX = "django-index";

    /**
     * Base directory of django benchmark traces
     */
    public static final String DJANGO_BENCHMARK = "django-benchmark";

    /**
     * django-index experiment on three hosts
     * http://secretaire.dorsal.polymtl.ca/~fgiraldeau/traces/wget-100M.tar.gz
     */
    public static final String EXP_WGET = "wget-100M";

    /**
     * Reproduce synchronization bug
     */
    public static final String EXP_BUG_SYNC = "django-benchmark-subset/django-benchmark-0";

    /**
     * The command of the python clien
     */
    public static final String DJANGO_CLIENT_NAME = "/home/ubuntu/.virtualenvs/wkdb/bin/python";

}
