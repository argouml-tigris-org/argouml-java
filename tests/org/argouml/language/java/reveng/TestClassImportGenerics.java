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
import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;

import junit.framework.TestCase;

import org.argouml.Helper;
import org.argouml.kernel.Project;
import org.argouml.kernel.ProjectManager;
import org.argouml.language.java.reveng.classfile.ClassfileParser;
import org.argouml.language.java.reveng.classfile.ClassfileTreeParser;
import org.argouml.language.java.reveng.classfile.SimpleByteLexer;
import org.argouml.model.Model;
import org.argouml.profile.Profile;
import org.argouml.profile.init.InitProfileSubsystem;

import antlr.ASTFactory;
import antlr.CommonAST;
import antlr.debug.misc.ASTFrame;

/**
 * Test case for the import of source code with generics.
 */
public class TestClassImportGenerics extends TestCase {

    private static Object parsedModel;
    private static Object parsedPackage;
    private static Object parsedClass;
    private static Project project;
    private static Profile profileJava;
    private static final String TESTED_CLASS =
        "build/tests/classes/org/argouml/language/java/reveng/"
        + "TestClassImportGenerics$TestedClass.class";

    public TestClassImportGenerics(String str) {
        super(str);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (!Model.isInitiated()) {
            Helper.initializeMDR();
        }
        new InitProfileSubsystem().init();
        project = ProjectManager.getManager().makeEmptyProject();
    }

    @SuppressWarnings("unchecked")
    public void testParsing() {
        try {
            File testedClassFile = new File(TESTED_CLASS);
            File testedClassAbsoluteFile = testedClassFile.getAbsoluteFile();
            System.out.println("Absolute path of TESTED_CLASS (\""
                    + TESTED_CLASS + "\") is \""
                    + testedClassAbsoluteFile.getPath() + "\".");
            assertTrue("The testedClassAbsoluteFile doesn't exist.",
                    testedClassAbsoluteFile.exists());
            SimpleByteLexer lexer = new SimpleByteLexer(new DataInputStream(
                    new FileInputStream(TESTED_CLASS)));
            ClassfileParser parser = new ClassfileParser(lexer);
            parser.classfile();
            parser.getAST();
            for (Profile profile : project.getProfileConfiguration().
                    getProfiles()) {
                if ("Java".equals(profile.getDisplayName())) {
                    System.err.println("profile is "
                            + profile.getDisplayName());
                    profileJava = profile;
                }
            }
            parsedModel = Model.getModelManagementFactory().createModel();
            assertNotNull("Creation of model failed.", parsedModel);
            Modeller modeller = new Modeller(parsedModel, profileJava, true,
                    true, TESTED_CLASS);
            assertNotNull("Creation of Modeller instance failed.", modeller);
            ClassfileTreeParser p = new ClassfileTreeParser();
            p.classfile(parser.getAST(), modeller);

            if (parsedPackage == null) {
                parsedPackage = parsedModel;
                for (String s : "org.argouml.language.java.reveng".split(
                        "\\.")) {
                    parsedPackage = Model.getFacade().lookupIn(
                            parsedPackage, s);
                }
                assertNotNull("No package \"org.argouml.language.java.reveng\""
                        + " found in model.", parsedPackage);
            }
            if (parsedClass == null) {
                parsedClass =
                        Model.getFacade().lookupIn(parsedPackage,
                                "TestClassImportGenerics$TestedClass");
                assertNotNull(
                    "No class \"TestClassImportGenerics$TestedClass\" found.",
                    parsedClass);
            }
            Iterator iter = Model.getFacade().getTemplateParameters(
                    parsedClass).iterator();
            while (iter.hasNext()) {
            	String name = Model.getFacade().getName(Model.getFacade()
            	    .getParameter(iter.next()));
            	System.err.println("name found:" + name);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
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
                new FileInputStream(TESTED_CLASS)));
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
