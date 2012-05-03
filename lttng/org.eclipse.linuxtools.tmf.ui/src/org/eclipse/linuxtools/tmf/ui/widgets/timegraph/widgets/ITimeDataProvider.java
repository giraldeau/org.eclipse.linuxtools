/*****************************************************************************
 * Copyright (c) 2007 Intel Corporation, 2010, 2012 Ericsson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Intel Corporation - Initial API and implementation
 *   Ruslan A. Scherbakov, Intel - Initial API and implementation
 *   Alvaro Sanchez-Leon - Updated for TMF
 *   Patrick Tasse - Refactoring
 *
 *****************************************************************************/

package org.eclipse.linuxtools.tmf.ui.widgets.timegraph.widgets;

public interface ITimeDataProvider {

    long getSelectedTime();

    long getBeginTime();

    long getEndTime();

    long getMinTime();

    long getMaxTime();

    long getTime0();

    long getTime1();

    long getMinTimeInterval();

    /**
     * Updates the time range and notify registered listeners
     * 
     * @param time0
     * @param time1
     */
    void setStartFinishTimeNotify(long time0, long time1);

    /**
     * Update the time range but do not trigger event notification
     * 
     * @param time0
     * @param time1
     */
    void setStartFinishTime(long time0, long time1);

    /**
     * Notify registered listeners without updating the time range
     */
    void notifyStartFinishTime();

    void setSelectedTimeInt(long time, boolean ensureVisible);

    void resetStartFinishTime();

    int getNameSpace();

    void setNameSpace(int width);

    int getTimeSpace();

    boolean isCalendarFormat();
}
