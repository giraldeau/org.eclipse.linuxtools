package org.eclipse.linuxtools.tmf.core.event.matching;

import org.eclipse.linuxtools.tmf.core.synchronization.SyncAlgorithmFullyIncremental;

/**
 * Detect the condition where the synchronization graph is connected (only one
 * partition exists) and cancel the pending request.
 *
 * @author francis
 * @since 4.0
 *
 */
public class StopEarlyMonitor extends AbstractMatchMonitor {

    @Override
    public void cacheHit(TmfEventDependency dep) {
        if (getParent() == null || getRequest() == null) {
            return;
        }
        IMatchProcessingUnit unit = getParent().getProcessingUnit();
        if (unit instanceof SyncAlgorithmFullyIncremental) {
            SyncAlgorithmFullyIncremental sync = (SyncAlgorithmFullyIncremental) unit;
            if (sync.getNumPartitions() == 1) {
                System.out.println("stop early!");
                getRequest().cancel();
            }
        }
    }

}
