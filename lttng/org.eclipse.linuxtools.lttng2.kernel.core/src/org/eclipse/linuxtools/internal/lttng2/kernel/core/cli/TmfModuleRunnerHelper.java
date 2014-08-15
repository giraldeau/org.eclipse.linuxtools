package org.eclipse.linuxtools.internal.lttng2.kernel.core.cli;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.linuxtools.tmf.core.analysis.IAnalysisModule;
import org.eclipse.linuxtools.tmf.core.analysis.TmfAbstractAnalysisModule;

/**
 * Helper to run module in non-interactive mode.
 *
 * FIXME: this is a copy-paste of TmfTestHelper class, which is not in the API.
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class TmfModuleRunnerHelper {

    /**
     * Calls the {@link TmfAbstractAnalysisModule#executeAnalysis} method of an
     * analysis module. This method does not return until the analysis is
     * completed and it returns the result of the method. It allows to execute
     * the analysis without requiring an Eclipse job and waiting for completion.
     *
     * @param module
     *            The analysis module to execute
     * @return The return value of the
     *         {@link TmfAbstractAnalysisModule#executeAnalysis} method
     */
    public static boolean executeAnalysis(IAnalysisModule module, IProgressMonitor monitor) {
        if (module instanceof TmfAbstractAnalysisModule) {
            try {
                Class<?>[] argTypes = new Class[] { IProgressMonitor.class };
                Method method = TmfAbstractAnalysisModule.class.getDeclaredMethod("executeAnalysis", argTypes);
                method.setAccessible(true);
                Object obj = method.invoke(module, monitor);
                return (Boolean) obj;
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("This analysis module does not have a protected method to execute. Maybe it can be executed differently? Or it is not supported yet in this method?");
    }
}
