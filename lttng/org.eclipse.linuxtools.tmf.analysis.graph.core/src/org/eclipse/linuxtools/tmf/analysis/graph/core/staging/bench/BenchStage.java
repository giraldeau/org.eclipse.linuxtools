package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.bench;

public class BenchStage {

    private IBenchRunner fRunner;
    private String fName;

    public BenchStage(String name, IBenchRunner runner) {
        this.fName = name;
        this.fRunner = runner;
    }

    public IBenchRunner getRunner() {
        return fRunner;
    }

    public String getName() {
        return fName;
    }

}
