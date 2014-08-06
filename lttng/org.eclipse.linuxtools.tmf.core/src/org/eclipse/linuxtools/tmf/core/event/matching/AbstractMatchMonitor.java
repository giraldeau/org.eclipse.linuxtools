package org.eclipse.linuxtools.tmf.core.event.matching;

import org.eclipse.linuxtools.tmf.core.event.matching.TmfNetworkEventMatching.PacketKey;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;

/**
 * @since 4.0
 */
public abstract class AbstractMatchMonitor implements IMatchMonitor {

    private TmfNetworkEventMatching fParent; // FIXME: move removeKey() to base class
    private TmfEventRequest fRequest;

    @Override
    public void init() {
    }

    @Override
    public void cacheMiss(PacketKey key) {
    }

    @Override
    public void cacheHit(TmfEventDependency dep) {
    }

    @Override
    public TmfNetworkEventMatching getParent() {
        return fParent;
    }

    @Override
    public void setParent(TmfNetworkEventMatching evmatching) {
        fParent = evmatching;
    }

    @Override
    public TmfEventRequest getRequest() {
        return fRequest;
    }

    @Override
    public void setRequest(TmfEventRequest request) {
        fRequest = request;
    }

}
