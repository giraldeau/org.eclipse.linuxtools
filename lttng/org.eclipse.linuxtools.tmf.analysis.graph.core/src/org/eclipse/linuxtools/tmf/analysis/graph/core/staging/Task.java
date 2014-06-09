package org.eclipse.linuxtools.tmf.analysis.graph.core.staging;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * POJO state object for efficiency
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 *
 */
public class Task {

    /**
     * Available task states
     *
     * @author Francis Giraldeau <francis.giraldeau@gmail.com>
     */
    public enum StateEnum {
        /**
         * Unkown state
         */
        UNKNOWN(-1),

        /**
         * Running state
         */
        RUN(0),

        /**
         * Preempted state
         */
        PREEMPTED(1),

        /**
         * Blocked
         */
        BLOCKED(2),

        /**
         * Exit state
         */
        EXIT(3);

        private final Integer value;
        private StateEnum(Integer value) {
            this.value = value;
        }
        /**
         * Return the integer value of this label
         * @return integer value
         */
        public Integer value() {
            return this.value;
        }

        public static StateEnum fromValue(Integer v) {
            switch(v) {
            case 0:
                return RUN;
            case 1:
                return PREEMPTED;
            case 2:
                return BLOCKED;
            case 3:
                return EXIT;
            default:
                break;
            }
            return UNKNOWN;
        }
    }


    private static final HashFunction hf = Hashing.md5();
    private String host;
    private Long ts;
    private Long tid;
    private Long last;
    private StateEnum state;
    private String comm;
    private int hc;

    public Task(String host, long tid, long ts) {
        this.host = host; this.tid = tid; this.ts = ts;
        this.state = StateEnum.RUN;
        this.last = ts;
        this.hc = hf.newHasher()
                .putLong(this.ts)
                .putLong(this.tid)
                .putString(this.host)
                .hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Task)) {
            return false;
        }
        Task other = (Task) o;
        return other.host.equals(this.host) &&
                other.ts == this.ts &&
                other.tid == this.tid;
    }

    @Override
    public int hashCode() {
        return this.hc;
    }

    public Long getTID() {
        return this.tid;
    }

    public String getHostID() {
        return this.host;
    }

    public Long getStart() {
        return ts;
    }

    public void setState(Long ts, StateEnum state) {
        this.last = ts; this.state = state;
    }

    public StateEnum getState() {
        return state;
    }

    public Long getLastUpdate() {
        return last;
    }

    public String getComm() {
        return comm;
    }

    public void setComm(String comm) {
        this.comm = comm;
    }

    @Override
    public String toString() {
        return "" + this.tid;
    }

}