/* $Id$
 *****************************************************************************
 * Copyright (c) 2009 Contributors - see below
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    thn
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

package org.argouml.language.java.generator;

import static org.argouml.Helper.newModel;

import org.argouml.model.Model;

import junit.framework.TestCase;

/**
 * @author MarkusK
 */
public class TestGeneratorJava extends TestCase {

    private Object namespace;

    private Object class1;

    private Object innerClass;

    private Object inter1;
    
    private GeneratorJava generator;

    /**
     * Constructor for TestGeneratorJava.
     *
     * @param name The name.
     */
    public TestGeneratorJava(String name) {
        super(name);
        newModel();
    }

    /*
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        Object mmodel = Model.getModelManagementFactory().createModel();
        Model.getCoreHelper().setName(mmodel, "untitledModel");
        Model.getModelManagementFactory().setRootModel(mmodel);
        namespace = Model.getModelManagementFactory().createPackage();
        class1 = Model.getCoreFactory().buildClass("Class1", namespace);
        innerClass = Model.getCoreFactory().buildClass("InnerClass", class1);
        inter1 = Model.getCoreFactory().buildInterface("Inter1", namespace);
        generator = new GeneratorJava();
    }

    /**
     * check the Java Code Generator does not generate a protected class ....
     * These tests are applicable for outer classes (inside a package).
     */
    public void testGenerateClassifierStart() {
        StringBuffer result;

        Model.getCoreHelper().setVisibility(class1,
                Model.getVisibilityKind().getPublic());
        result = generator.generateClassifierStart(class1);
        assertTrue(Model.getFacade().isAPackage(
                Model.getFacade().getNamespace(class1)));
        assertTrue("A class should have public in its specification", result
                .indexOf("public") == 0);

        Model.getCoreHelper().setVisibility(class1,
                Model.getVisibilityKind().getProtected());
        assertTrue(Model.getFacade().isAPackage(
                Model.getFacade().getNamespace(class1)));
        result = generator.generateClassifierStart(class1);
        assertTrue("A class should not have protected in its specification",
                result.indexOf("protected") == -1);

        Model.getCoreHelper().setVisibility(class1,
                Model.getVisibilityKind().getPackage());
        assertTrue(Model.getFacade().isAPackage(
                Model.getFacade().getNamespace(class1)));
        result = generator.generateClassifierStart(class1);
        assertTrue("A class with default visibility should not have "
                + "public in its specification",
                result.indexOf("public") == -1);
        assertTrue("A class with package (default) visibility should not have "
                + "protected in its specification",
                result.indexOf("protected") == -1);
    }

    /**
     * Check the Java Code Generator does not generate
     * a protected interface ....
     */
    public void testGenerateClassifierStart2() {
        StringBuffer result;

        Model.getCoreHelper().setVisibility(inter1,
                Model.getVisibilityKind().getPublic());
        result = generator.generateClassifierStart(inter1);
        assertTrue(Model.getFacade().isAPackage(
                Model.getFacade().getNamespace(inter1)));
        assertTrue("A interface should have public in its specification",
                result.indexOf("public") == 0);

        Model.getCoreHelper().setVisibility(inter1,
                Model.getVisibilityKind().getProtected());
        result = generator.generateClassifierStart(inter1);
        assertTrue(Model.getFacade().isAPackage(
                Model.getFacade().getNamespace(inter1)));
        assertTrue(
                "A interface should not have protected in its specification",
                result.indexOf("protected") == -1);

        Model.getCoreHelper().setVisibility(inter1,
                Model.getVisibilityKind().getPackage());
        result = generator.generateClassifierStart(inter1);
        assertTrue(Model.getFacade().isAPackage(
                Model.getFacade().getNamespace(inter1)));
        assertTrue(
                "An interface with package (default) visiblity should not have "
                + "public keyword in its specification",
                result.indexOf("public") == -1);
        assertTrue(
                "An interface with package (default) visiblity should not have "
                + "protected keyword in its specification",
                result.indexOf("protected") == -1);
    }

    /**
     * check the Java Code Generator does not generate a protected class ....
     * These tests are applicable for inner classes (inside a class).
     */
    public void testGenerateClassifierStart3() {
        StringBuffer result;

        Model.getCoreHelper().setVisibility(innerClass,
                Model.getVisibilityKind().getPublic());
        result =
            generator
                .generateClassifierStart(innerClass);
        assertTrue(Model.getFacade().isAClass(
                Model.getFacade().getNamespace(innerClass)));
        assertTrue("A class should have public in its specification",
                result.indexOf("public") == 0);

        Model.getCoreHelper().setVisibility(innerClass,
                Model.getVisibilityKind().getProtected());
        assertTrue(Model.getFacade().isAClass(
                Model.getFacade().getNamespace(innerClass)));
        result =
            generator
                .generateClassifierStart(innerClass);
        assertTrue("A class should have protected in its specification",
                result.indexOf("protected") == 0);

        Model.getCoreHelper().setVisibility(innerClass,
                Model.getVisibilityKind().getPackage());
        assertTrue(Model.getFacade().isAClass(
                Model.getFacade().getNamespace(innerClass)));
        result =
            generator
                .generateClassifierStart(innerClass);
        assertTrue("A inner class with package (default) visibility should not"
                + " have protected in its specification",
                result.indexOf("protected") == -1);
        assertTrue("A inner class with package (default) visibility should not"
                + " have public in its specification",
                result.indexOf("public") == -1);
    }

}
