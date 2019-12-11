/* $Id$
 *****************************************************************************
 * Copyright (c) 2009-2010 Contributors - see below
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thomas Neustupny (thn)
 *    Luis Sergio Oliveira (euluis)
 *****************************************************************************
 *
 * Some portions of this file was previously release using the BSD License:
 */

// Copyright (c) 1996-2008 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies.  This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason.  IN NO EVENT SHALL THE
// UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
// SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
// ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
// THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
// PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
// CALIFORNIA HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT,
// UPDATES, ENHANCEMENTS, OR MODIFICATIONS.

package org.argouml.language.java.reveng;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Iterator;

import junit.framework.TestCase;

import org.argouml.language.java.reveng.classfile.ClassfileParser;
import org.argouml.language.java.reveng.classfile.SimpleByteLexer;
import org.argouml.model.Model;

import antlr.ASTFactory;
import antlr.CommonAST;
import antlr.debug.misc.ASTFrame;

/**
 * Test case for the import of source code with generics.
 */
public class TestClassImportGenerics extends TestCase {

    private Object parsedModel;

    private static String getTestedFilename() {
        ClassLoader classLoader = 
            TestClassImportGenerics.class.getClassLoader();
        URL resource = 
            classLoader.getResource("org/argouml/language/java/reveng/"
                    + "TestClassImportGenerics$TestedClass.class");
        assertNotNull(resource);
        String fileName = URLDecoder.decode(resource.getFile());
        return fileName;
    }

    private ImportFixture importFixture;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        importFixture = new ClassfileImportFixture("", getTestedFilename());
        importFixture.setUp();
        parsedModel = importFixture.getParsedModel();
    }

    @Override
    protected void tearDown() throws Exception {
        importFixture.tearDown();
        super.tearDown();
    }

    @SuppressWarnings("unchecked")
    public void testParsing() throws Exception {
        Object parsedPackage = parsedModel;
        for (String s : "org.argouml.language.java.reveng".split("\\.")) {
            parsedPackage = Model.getFacade().lookupIn(
                    parsedPackage, s);
        }
        assertNotNull("No package \"org.argouml.language.java.reveng\""
                + " found in model.", parsedPackage);
        Object parsedClass = Model.getFacade().lookupIn(parsedPackage,
                "TestClassImportGenerics$TestedClass");
        assertNotNull(
                "No class \"TestClassImportGenerics$TestedClass\" found.",
                parsedClass);
        Iterator iter = Model.getFacade().getTemplateParameters(
                parsedClass).iterator();
        // FIXME: this should assert that the template parameters exist instead
        // of printing to System.err.
        while (iter.hasNext()) {
            String name = Model.getFacade().getName(Model.getFacade()
                .getParameter(iter.next()));
            System.err.println("name found:" + name);
        }
    }
    
    /**
     * Test generic class.
     * @param <X> example
     * @param <Y> example
     */
    public static class TestedClass<X, Y> {
    }
    
    public static void main(String[] args) throws Exception {
        SimpleByteLexer lexer = new SimpleByteLexer(new DataInputStream(
                new FileInputStream(getTestedFilename())));
        ClassfileParser parser = new ClassfileParser(lexer);
        parser.classfile();
        CommonAST t = (CommonAST) parser.getAST();
        ASTFactory factory = parser.getASTFactory();
        CommonAST r = (CommonAST) factory.create(0, "ROOT");
        r.addChild(t);
        ASTFrame frame = new ASTFrame("Classfile AST", r);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                System.exit(0);
            }
        });
        frame.setVisible(true);
    }
    
}
