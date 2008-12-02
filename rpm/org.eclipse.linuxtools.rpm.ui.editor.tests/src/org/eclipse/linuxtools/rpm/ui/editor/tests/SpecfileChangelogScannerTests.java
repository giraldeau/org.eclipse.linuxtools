/*******************************************************************************
 * Copyright (c) 2007 Alphonse Van Assche.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Alphonse Van Assche - initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.rpm.ui.editor.tests;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.linuxtools.rpm.ui.editor.ColorManager;
import org.eclipse.linuxtools.rpm.ui.editor.ISpecfileColorConstants;
import org.eclipse.linuxtools.rpm.ui.editor.scanners.SpecfileChangelogScanner;

public class SpecfileChangelogScannerTests extends AScannerTest {

	private IToken token;

	private TextAttribute ta;

	private SpecfileChangelogScanner scanner;

	public SpecfileChangelogScannerTests() {
		scanner = new SpecfileChangelogScanner(new ColorManager());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.linuxtools.rpm.ui.editor.tests.AScannerTest#getContents()
	 */
	@Override
	protected String getContents() {
		return "%changelog <toto@test.com> - 1.1-4";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.linuxtools.rpm.ui.editor.tests.AScannerTest#getScanner()
	 */
	@Override
	protected RuleBasedScanner getScanner() {
		return scanner;
	}

	public void testSection() {
		token = getNextToken();
		assertTrue(token instanceof Token);
		assertEquals(10, rulesBasedScanner.getTokenLength());
		assertEquals(0, rulesBasedScanner.getTokenOffset());
		ta = (TextAttribute) token.getData();
		assertEquals(ta.getForeground().getRGB(),
				ISpecfileColorConstants.SECTIONS);
	}

	public void testMail() {
		token = getToken(3);
		assertTrue(token instanceof Token);
		assertEquals(15, rulesBasedScanner.getTokenLength());
		assertEquals(11, rulesBasedScanner.getTokenOffset());
		ta = (TextAttribute) token.getData();
		assertEquals(ta.getForeground().getRGB(),
				ISpecfileColorConstants.AUTHOR_MAIL);
	}

	public void testVerRel() {
		token = getToken(4);
		assertTrue(token instanceof Token);
		assertEquals(8, rulesBasedScanner.getTokenLength());
		assertEquals(26, rulesBasedScanner.getTokenOffset());
		ta = (TextAttribute) token.getData();
		assertEquals(ta.getForeground().getRGB(),
				ISpecfileColorConstants.VER_REL);
	}
}
