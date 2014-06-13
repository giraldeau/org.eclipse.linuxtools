package org.eclipse.linuxtools.tmf.analysis.graph.core.model;


public interface ITmfWorkerFactory {

    public TmfWorker createModelElement(String host, int cpu, long wid);

}
