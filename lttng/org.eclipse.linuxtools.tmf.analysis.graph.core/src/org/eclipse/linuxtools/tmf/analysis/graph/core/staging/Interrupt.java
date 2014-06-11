package org.eclipse.linuxtools.tmf.analysis.graph.core.staging;

/**
 * POJO object for efficiency
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 */
public class Interrupt {

    public enum Softirq {
        HI(0),
        TIMER(1),
        NET_TX(2),
        NET_RX(3),
        BLOCK(4),
        BLOCK_IOPOLL(5),
        TASKLET(6),
        SCHED(7),
        HRTIMER(8),
        RCU(9),
        UNKNOWN(-1);
        private final Integer value;
        private Softirq(Integer value) {
            this.value = value;
        }
        /**
         * Return the integer value of this label
         * @return integer value
         */
        public Integer value() {
            return this.value;
        }

        public static Softirq fromValue(Integer v) {
            if (v < 0 || v > RCU.value) {
                return UNKNOWN;
            }
            return values()[v];
        }

    }

    public enum Hardirq {
        RESCHED(0),
        EHCI_HCD_1(19),
        EHCI_HCD_2(23),
        UNKNOWN(-1);
        private final Integer value;
        private Hardirq(Integer value) {
            this.value = value;
        }
        /**
         * Return the integer value of this label
         * @return integer value
         */
        public Integer value() {
            return this.value;
        }

        public static Hardirq fromValue(Integer v) {
            for (Hardirq item: Hardirq.values()) {
                if (item.value.equals(v)) {
                    return item;
                }
            }
            return Hardirq.UNKNOWN;
        }
    }

    public enum InterruptType {
        UNKNOWN(-1),
        HARDIRQ(0),
        SOFTIRQ(1),
        HRTIMER(2);
        private final Integer value;
        private InterruptType(Integer value) {
            this.value = value;
        }
        /**
         * Return the integer value of this label
         * @return integer value
         */
        public Integer value() {
            return this.value;
        }

        public static InterruptType fromValue(Integer v) {
            for (InterruptType item: InterruptType.values()) {
                if (item.value.equals(v)) {
                    return item;
                }
            }
            return InterruptType.UNKNOWN;
        }
    }


    private final Integer cpu;
    private final Long ts;
    private final InterruptType type;
    private final Long vec;

    public Interrupt(Integer cpu, Long ts, InterruptType type, Long vec) {
        this.cpu = cpu; this.ts = ts; this.type = type; this.vec = vec;
    }

    public Integer getCpu() {
        return cpu;
    }

    public Long getTs() {
        return ts;
    }

    public InterruptType getType() {
        return type;
    }

    public Long getVec() {
        return vec;
    }

}
