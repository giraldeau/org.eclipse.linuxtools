/*******************************************************************************
 * Copyright (c) 2000, 2006 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     Markus Schorn (Wind River Systems)
 *******************************************************************************/
package org.eclipse.linuxtools.cdt.libhover.tests;

import java.util.Collections;
import java.util.Map;

import org.eclipse.cdt.core.parser.ExtendedScannerInfo;

public class TestScannerInfo extends ExtendedScannerInfo {
	private static final String[] EMPTY = {};
	private String[] fIncludes;
	private String[] fIncludeFiles;
	private String[] fMacroFiles;
	private Map<String, String> fDefinedSymbols;

	public TestScannerInfo(String[] includes, String[] macroFiles, String[] includeFiles,
			Map<String, String> definedSymbols) {
		fIncludes= includes;
		fIncludeFiles= includeFiles;
		fMacroFiles= macroFiles;
		fDefinedSymbols= definedSymbols;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map getDefinedSymbols() {
		return fDefinedSymbols == null ? Collections.emptyMap() : fDefinedSymbols;
	}

	@Override
	public String[] getIncludePaths() {
		return fIncludes == null ? EMPTY : fIncludes;
	}

	@Override
	public String[] getIncludeFiles() {
		return fIncludeFiles == null ? EMPTY: fIncludeFiles;
	}

	@Override
	public String[] getLocalIncludePath() {
		return null;
	}

	@Override
	public String[] getMacroFiles() {
		return fMacroFiles == null ? EMPTY: fMacroFiles;
	}
}
