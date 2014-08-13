package org.eclipse.linuxtools.lttng2.kernel.core.cli;

import java.util.concurrent.ArrayBlockingQueue;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.TmfEvent;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.trace.ITmfContext;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

public class TmfTraceReader {

    private int count;

    ITmfEvent endEvent = new TmfEvent();

    public void readTraceRaw(final ITmfTrace trace, final TmfEventRequest request) {

        final ArrayBlockingQueue<ITmfEvent> fQueue = new ArrayBlockingQueue<>(10000);
        request.handleStarted();
        Runnable producer = new Runnable() {
            @Override
            public void run() {
                ITmfContext pos = trace.seekEvent(0L);
                count = 0;
                try {
                    while(!request.isCancelled()) {
                        ITmfEvent event = trace.getNext(pos);
                        if (event == null) {
                            break;
                        }
                        count++;
                        fQueue.put(event);
                    }
                    fQueue.put(endEvent);
                } catch (InterruptedException e) {
                    new RuntimeException(e);
                }
            }
        };

        Runnable consumer = new Runnable() {
            @Override
            public void run() {
                while (!request.isCancelled()) {
                    try {
                        ITmfEvent event = fQueue.take();
                        if (event == endEvent) {
                            break;
                        }
                        request.handleData(event);
                    } catch (InterruptedException e) {
                        new RuntimeException(e);
                    }
                }
            }
        };

        Thread prodThread = new Thread(producer);
        Thread consThread = new Thread(consumer);
        prodThread.start();
        consThread.start();
        try {
            prodThread.join();
            consThread.join();
        } catch (InterruptedException e) {
            new RuntimeException(e);
        }

        request.handleCompleted();
        System.out.println("\nevents reads: " + count);
    }

}
