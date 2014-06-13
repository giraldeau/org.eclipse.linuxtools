package org.eclipse.linuxtools.internal.lttng2.kernel.core.model;

public class TimeInterval {

	private long start;
	private long end;

	public TimeInterval(long start, long end) {
		this.setStart(start);
		this.setEnd(end);
	}

	public long duration() {
		return getEnd() - getStart();
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

}
