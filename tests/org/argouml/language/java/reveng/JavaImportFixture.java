/* $Id$
 *******************************************************************************
 * Copyright (c) 2010 Contributors - see below
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Luis Sergio Oliveira (euluis)
 *******************************************************************************
 */

package org.argouml.language.java.reveng;

import junit.framework.Assert;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

/**
 * An helper class for implementing aspects of import fixtures of Java import 
 * test cases to avoid code duplication.
 * @author Luis Sergio Oliveira (euluis)
 */
class JavaImportFixture extends ImportFixture {

    JavaImportFixture(String theParserInput, String theFileName) {
        super(theParserInput, theFileName);
    }

    @Override
    void setUp() throws Exception {
        super.setUp();
        JavaLexer lexer = new JavaLexer(
                new ANTLRStringStream(getParserInput()));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);
        try {
            parser.compilationUnit(getModeller(), lexer);
        } catch (RecognitionException e) {
            Assert.fail("Parsing of Java source failed." + e);
        }
    }

}
