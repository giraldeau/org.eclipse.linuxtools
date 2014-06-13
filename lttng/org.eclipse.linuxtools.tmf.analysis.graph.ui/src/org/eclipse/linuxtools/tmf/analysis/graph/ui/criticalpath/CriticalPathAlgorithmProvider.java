/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.analysis.graph.ui.criticalpath;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.linuxtools.tmf.analysis.graph.core.criticalpath.ICriticalPathAlgorithm;
import org.eclipse.linuxtools.tmf.core.analysis.TmfAbstractAnalysisParamProvider;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

public class CriticalPathAlgorithmProvider extends
		TmfAbstractAnalysisParamProvider {

    private static CriticalPathAlgorithmProvider fInstance = null;

    private Class<?> fAlgorithm = null;

    public CriticalPathAlgorithmProvider() {
        super();
        fInstance = this;
    }

    @NonNull
    public static CriticalPathAlgorithmProvider getInstance() {
        if (fInstance == null) {
            fInstance = new CriticalPathAlgorithmProvider();
        }
        return fInstance;
    }

	@Override
	public String getName() {
		return "Critical Path algorithm provider";
	}

	@Override
	public Object getParameter(String name) {
	    if (name.equals(CriticalPathModule.PARAM_ALGORITHM)) {
            return fAlgorithm;
        }
        return null;
	}

	/**
	 * Set the algorithm to use for critical path computation
	 *
	 * @param algorithmClass the new algorithm class
	 */
	public <T extends ICriticalPathAlgorithm> void setAlgorithm(@NonNull Class<T> algorithmClass) {
	    if (!algorithmClass.equals(fAlgorithm)) {
	        fAlgorithm = algorithmClass;
	        this.notifyParameterChanged(CriticalPathModule.PARAM_ALGORITHM);
	    }
	}

	@Override
	public boolean appliesToTrace(ITmfTrace trace) {
		return true;
	}

}
