package org.eclipse.linuxtools.tmf.analysis.graph.core.staging;

/**
 * POJO object for efficiency
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 */
public class Interrupt {

    public static final Integer IRQ = 0;
    public static final Integer SOFTIRQ = 1;
    public static final Integer HRTIMER = 2;

    public Integer cpu;
    public Long ts;
    public Integer type;
    public Long vec;

    public Interrupt(Integer cpu, Long ts, Integer type, Long vec) {
        this.cpu = cpu; this.ts = ts; this.type = type; this.vec = vec;
    }

}
