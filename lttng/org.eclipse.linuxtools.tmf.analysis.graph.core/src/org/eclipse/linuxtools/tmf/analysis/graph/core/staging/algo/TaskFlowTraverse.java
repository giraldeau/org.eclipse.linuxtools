package org.eclipse.linuxtools.tmf.analysis.graph.core.staging.algo;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystem;
import org.eclipse.linuxtools.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.linuxtools.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.linuxtools.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.linuxtools.statesystem.core.statevalue.ITmfStateValue.Type;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.PackedLongValue;
import org.eclipse.linuxtools.tmf.analysis.graph.core.staging.Task.StateEnum;

public class TaskFlowTraverse implements IntervalTraverse {

    @Override
    public void traverse(ITmfStateSystem s, int qTask, long start, long end, IntervalVisitor visitor) {
        try {
            doTraverse(s, qTask, start, end, visitor);
        } catch (AttributeNotFoundException | StateSystemDisposedException e) {
            e.printStackTrace();
        }
    }

    private static void doTraverse(ITmfStateSystem s, int qTask, long start, long end, IntervalVisitor visitor) throws AttributeNotFoundException, StateSystemDisposedException {
        List<ITmfStateInterval> hist = s.queryHistoryRange(qTask, start, end);
        for (ITmfStateInterval i: hist) {
            if (i.getStateValue().getType() == Type.LONG) {
                // visit the interval
                long val = i.getStateValue().unboxLong();
                StateEnum state = StateEnum.fromValue(PackedLongValue.unpack(0, val));
                visitor.visit(s, qTask, i);
                switch(state) {
                case EXIT:
                    break;
                case WAIT_CPU:
                    resolvePreempt(s, i, visitor);
                    break;
                case RUNNING:
                    break;
                case UNKNOWN:
                    break;
                case WAIT_UNKNOWN:
                case WAIT_BLOCK_DEV:
                case WAIT_NETWORK:
                case WAIT_TASK:
                    resolveBlocking(s, qTask, i, visitor);
                    break;
                case WAIT_TIMER:
                case WAIT_USER_INPUT:
                case INTERRUPTED:
                default:
                    break;
                }
            }
        }
    }

    // reverse iteration
//    ITmfStateInterval i = null;
//    long cursor = ss.getCurrentEndTime();
//    do {
//        i = ss.querySingleState(cursor, p1q);
//        System.out.println(i);
//        cursor = i.getStartTime() - 1;
//    } while(!i.getStateValue().isNull() && cursor >= ss.getStartTime());



    public static class Rec {
        public ITmfStateInterval wait;  // parent wait
        public int qChild;              // child task
        public long cursor;             // time location to query
    }

    /**
     * @param s
     * @param qTask
     * @param wait
     * @param visitor
     * @throws StateSystemDisposedException
     * @throws AttributeNotFoundException
     */
    private static void resolveBlocking(ITmfStateSystem s, int qTask, ITmfStateInterval wait, IntervalVisitor visitor)
            throws AttributeNotFoundException, StateSystemDisposedException {
        //long rootBound = wait.getEndTime();
        long val = wait.getStateValue().unboxLong();
        LinkedList<ITmfStateInterval> result = new LinkedList<>(); // accumulate
        Rec top = new Rec();
        top.qChild = PackedLongValue.unpack(1, val);
        top.wait = wait;
        top.cursor = wait.getEndTime();
        Stack<Rec> stack = new Stack<>();
        stack.push(top);

//        while(!stack.isEmpty()) {
//            // current wait task interval
//            top = stack.peek();
//            ITmfStateInterval item = s.querySingleState(top.cursor, top.qChild);
//            top.cursor = item.getStartTime() - 1;
//
//            if (item.getStateValue().isNull()) {
//                // dead end, insert unknown interval
//
//                stack.pop();
//            }
//            val = item.getStateValue().unboxLong();
//            StateEnum state = StateEnum.fromValue(PackedLongValue.unpack(0, val));
//            if (state == StateEnum.WAIT_TASK) {
//                Rec rec = new Rec();
//                rec.wait = item;
//                rec.qChild = PackedLongValue.unpack(1, val);
//                rec.cursor = item.getEndTime();
//                stack.push(rec);
//            } else {
//                result.addFirst(item);
//            }
//            if (item.getStartTime() <= top.wait.getStartTime()) {
//
//            }
//        }

        for (ITmfStateInterval i: result) {
            System.out.println(i);
        }
    }


//        LinkedList<ITmfStateInterval> result = new LinkedList<>();
//        Stack<ITmfStateInterval> stack = new Stack<>();
//        stack.push(i);
//        boolean restart = false;
//        while(!stack.isEmpty()) {
//            ITmfStateInterval wait = stack.peek();
//            long val = wait.getStateValue().unboxLong();
//            int qTask = PackedLongValue.unpack(1, val);
//            long cursor = wait.getEndTime();
//            long bound = wait.getStartTime();
//
//            ITmfStateInterval item = s.querySingleState(cursor, qTask);
//            while(!item.getStateValue().isNull() && item.getStartTime() > bound) {
//                result.addFirst(item);
//                long itemVal = item.getStateValue().unboxLong();
//                StateEnum state = StateEnum.fromValue(PackedLongValue.unpack(0, itemVal));
//                if (state == StateEnum.WAIT_TASK) {
//                    stack.push(item);
//                    restart = true;
//                    break;
//                }
//                item = s.querySingleStackTop(item.getStartTime() - 1, qTask);
//            }
//            if (restart) {
//                restart = false;
//                continue;
//            }
//            // Fill/cut intervals to avoid discontinuity
//            wait = stack.pop();
//        }

//      if (state == StateEnum.WAIT_TASK) {
//
//      } else {
//
//      }
//        for (ITmfStateInterval item: queue) {
//            visitor.visit(task, state, t1, t2);
//        }

    private static void resolvePreempt(ITmfStateSystem s, ITmfStateInterval interval, IntervalVisitor visitor) throws AttributeNotFoundException, StateSystemDisposedException {
        long val = interval.getStateValue().unboxLong();
        int cpuQuark = PackedLongValue.unpack(1, val);
        List<ITmfStateInterval> preemptRange = s.queryHistoryRange(cpuQuark, interval.getStartTime(), interval.getEndTime());
        for (ITmfStateInterval preempt: preemptRange) {
            ITmfStateValue stateValue = preempt.getStateValue();
            if (stateValue.getType() == Type.INTEGER) {
                int qPreempt = stateValue.unboxInt();
                visitor.visit(s, qPreempt, preempt);
            }
        }
    }
}
