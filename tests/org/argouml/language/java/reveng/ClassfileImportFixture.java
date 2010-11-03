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

import java.io.DataInputStream;
import java.io.FileInputStream;

import org.argouml.language.java.reveng.classfile.ClassfileParser;
import org.argouml.language.java.reveng.classfile.ClassfileTreeParser;
import org.argouml.language.java.reveng.classfile.SimpleByteLexer;

/**
 * An helper class for implementing aspects of import fixtures of Classfile 
 * import test cases to avoid code duplication.
 * @author Luis Sergio Oliveira (euluis)
 */
class ClassfileImportFixture extends ImportFixture {

    ClassfileImportFixture(String theParserInput, String theFileName) {
        super(theParserInput, theFileName);
    }

    @Override
    void setUp() throws Exception {
        super.setUp();
        SimpleByteLexer lexer = new SimpleByteLexer(new DataInputStream(
                new FileInputStream(getFileName())));
        ClassfileParser parser = new ClassfileParser(lexer);
        // The following call to parser causes side effects needed for the
        // correct working of the test.
        parser.classfile();
        ClassfileTreeParser p = new ClassfileTreeParser();
        p.classfile(parser.getAST(), getModeller());
    }

}
