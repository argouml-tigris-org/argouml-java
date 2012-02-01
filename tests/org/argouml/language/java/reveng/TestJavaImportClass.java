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

import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.argouml.application.api.Argo;
import org.argouml.model.Model;
import org.argouml.profile.Profile;
import org.argouml.profile.ProfileException;

/**
 * Test case to test the import of a Java source file. The content of the Java
 * source file is a private constant at the bottom of the source of this class.
 * The test methods are specially designed for this Java source constant.
 * Feeding of the diagram subsystem is disabled; only model elements are
 * created and checked. For testing with another Java source file, copy this
 * test case, change the Java source constant and modify the test method (the
 * setUp method need not be changed).
 */
public class TestJavaImportClass extends TestCase {
    private ImportFixture importFixture;

    /**
     * Construct a test case with the given name to test import of a Java class.
     *
     * @param str name of the test case
     */
    public TestJavaImportClass(String str) {
        super(str);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        importFixture = new JavaImportFixture(PARSERINPUT, "TestClass.java");
        importFixture.setUp();
        profileJava = importFixture.getProfileJava();
        parsedModel = importFixture.getParsedModel();
    }

    @Override
    protected void tearDown() throws Exception {
        importFixture.tearDown();
        super.tearDown();
    }

    /**
     * Test if the package was processed correctly.
     */
    public void testPackage() {
        parsedPackage = Model.getFacade().lookupIn(parsedModel, "testpackage");
        assertNotNull("No package \"testpackage\" found in model.",
                parsedPackage);
        assertEquals("Inconsistent package name.",
                "testpackage", Model.getFacade().getName(parsedPackage));
        assertEquals("The namespace of the package should be the model.",
                parsedModel, Model.getFacade().getNamespace(parsedPackage));
        assertTrue("The package should be recognized as a package.",
                Model.getFacade().isAPackage(parsedPackage));
    }

    /**
     * Test if the import was processed correctly.
     * @throws ProfileException if Profile subsystem operations throw errors
     */
    @SuppressWarnings("unchecked")
    public void testImport() throws ProfileException {
        parsedPackage = Model.getFacade().lookupIn(parsedModel, "testpackage");
        assertNotNull("No package \"testpackage\" found in model.",
            parsedPackage);
        Collection ownedElements =
            Model.getFacade().getOwnedElements(parsedPackage);
        assertNotNull("No elements owned by  \"testpackage\".", ownedElements);
        Object component = null;
        Iterator iter = ownedElements.iterator();
        while (iter.hasNext()) {
            Object element = iter.next();
            if (Model.getFacade().isAComponent(element)) {
                component = element;
                break;
            }
        }
        assertNotNull("No component found.", component);
        assertEquals("The component name is wrong.",
            "TestClass.java", Model.getFacade().getName(component));
        Collection dependencies =
            Model.getFacade().getClientDependencies(component);
        assertNotNull("No dependencies found for component.", dependencies);
        Object importDep = null;
        for (Object o : Model.getFacade().getClientDependencies(component)) {
            for (Object stereotype : Model.getFacade().getStereotypes(o)) {
                if ("javaImport".equals(Model.getFacade()
                        .getName(stereotype))) {
                    importDep = o;
                    break;
                }
            }
        }
        assertNotNull("No import found.", importDep);
        assertEquals("The import name is wrong.",
            "TestClass.java -> Observer",
            Model.getFacade().getName(importDep));
        Collection suppliers = Model.getFacade().getSuppliers(importDep);
        assertNotNull("No suppliers found in import.", suppliers);
        Object supplier = null;
        iter = suppliers.iterator();
        if (iter.hasNext()) {
            supplier = iter.next();
        }
        assertNotNull("No supplier found in import.", supplier);
        assertEquals("The import supplier name is wrong.",
            "Observer", Model.getFacade().getName(supplier));
        Object namespace = Model.getFacade().getNamespace(supplier);
        assertNotNull("The import supplier has no namespace.", namespace);
        assertEquals("Expected namespace name \"util\".",
            "util", Model.getFacade().getName(namespace));
        namespace = Model.getFacade().getNamespace(namespace);
        assertNotNull("The namespace \"util\" has no namespace.", namespace);
        assertEquals("Expected namespace name \"java\".",
            "java", Model.getFacade().getName(namespace));
        Object javaProfileModel =
            profileJava.getProfilePackages().iterator().next();
        assertEquals(
            "The namespace of \"java\" should be the Java profile model.",
            javaProfileModel, Model.getFacade().getNamespace(namespace));
    }

    /**
     * Test if the import was processed correctly.
     */
    @SuppressWarnings("unchecked")
    public void testClass() {
        parsedPackage = Model.getFacade().lookupIn(parsedModel, "testpackage");
        assertNotNull("No package \"testpackage\" found in model.",
            parsedPackage);
        parsedClass = Model.getFacade().lookupIn(parsedPackage, "TestClass");
        assertNotNull("No class \"TestClass\" found.", parsedClass);
        assertEquals("Inconsistent class name.",
            "TestClass", Model.getFacade().getName(parsedClass));
        assertEquals("The namespace of the class should be \"testpackage\".",
            parsedPackage, Model.getFacade().getNamespace(parsedClass));
        assertTrue("The class should be recognized as a class.",
                Model.getFacade().isAClass(parsedClass));
        assertTrue("The class should be abstract.",
                Model.getFacade().isAbstract(parsedClass));
        assertTrue("The class should be public.",
                Model.getFacade().isPublic(parsedClass));
        Collection generalizations =
            Model.getFacade().getGeneralizations(parsedClass);
        assertNotNull("No generalizations found for class.", generalizations);
        Object generalization = null;
        Iterator iter = generalizations.iterator();
        if (iter.hasNext()) {
            generalization = iter.next();
        }
        assertNotNull("No generalization found for class.", generalization);
        assertEquals("The child of the generalization should be the class.",
            parsedClass, Model.getFacade().getSpecific(generalization));
        assertEquals("The parent of the generalization should be \"Object\".",
            "Object",
            Model.getFacade().getName(
                        Model.getFacade().getGeneral(generalization)));
        Collection dependencies =
            Model.getFacade().getClientDependencies(parsedClass);
        assertNotNull("No dependencies found for class.", dependencies);
        Object abstraction = null;
        iter = dependencies.iterator();
        if (iter.hasNext()) {
            abstraction = iter.next();
        }
        assertNotNull("No abstraction found for class.", abstraction);
        assertEquals("The abstraction name is wrong.",
            "TestClass -> Observer", Model.getFacade().getName(abstraction));
        try {
            assertEquals("The client of the abstraction should be the class.",
                parsedClass,
                Model.getFacade().getClients(abstraction).iterator().next());
        } catch (IllegalArgumentException ex) {
            fail("The implementation dependency has no clients.");
        }
        try {
            assertEquals(
                "The supplier of the abstraction should be \"Observer\".",
                "Observer",
                Model.getFacade().getName(
                    Model.getFacade().getSuppliers(abstraction)
                    .iterator().next()));
        } catch (IllegalArgumentException ex) {
            fail("The abstraction has no suppliers.");
        }
    }

    /**
     * Test if the attributes were processed correctly.
     */
    @SuppressWarnings("unchecked")
    public void testAttributes() {
        parsedPackage = Model.getFacade().lookupIn(parsedModel, "testpackage");
        assertNotNull("No package \"testpackage\" found in model.",
            parsedPackage);
        parsedClass = Model.getFacade().lookupIn(parsedPackage, "TestClass");
        assertNotNull("No class \"TestClass\" found.", parsedClass);
        Collection attributes = Model.getFacade().getAttributes(parsedClass);
        assertNotNull("No attributes found ib class.", attributes);
        assertEquals("Number of attributes is wrong", 2, attributes.size());
        Object attribute = null;
        Object attributeForn = null;
        Object attributeFors = null;
        Iterator iter = attributes.iterator();
        while (iter.hasNext()) {
            attribute = iter.next();
            assertTrue("The attribute should be recognized as an attribute.",
                Model.getFacade().isAAttribute(attribute));
            if ("n".equals(Model.getFacade().getName(attribute))) {
                attributeForn = attribute;
            } else if ("s".equals(Model.getFacade().getName(attribute))) {
                attributeFors = attribute;
            }
        }
        assertTrue("The attributes have wrong names.",
            attributeForn != null && attributeFors != null);
        Object attribType = Model.getFacade().getType(attributeForn);
        Object initializer = Model.getFacade().getInitialValue(attributeForn);
        assertTrue("Attribute n should be private.",
            Model.getFacade().isPrivate(attributeForn));
        assertFalse("Attribute n should not be final.",
            Model.getFacade().isReadOnly(attributeForn));
        assertTrue("Attribute n should have type int.",
            "int".equals(Model.getFacade().getName(attribType)));
        assertTrue("Attribute n has no initializer.",
            Model.getFacade().isInitialized(attributeForn)
            && initializer != null);
        assertEquals("The initializer of attribute n is wrong.",
            " 0", Model.getFacade().getBody(initializer));
        attribType = Model.getFacade().getType(attributeFors);
        initializer = Model.getFacade().getInitialValue(attributeFors);
        assertTrue("Attribute s should be public.",
            Model.getFacade().isPublic(attributeFors));
        assertTrue("Attribute s should be static.",
            Model.getFacade().isStatic(attributeFors));
        assertTrue("Attribute s should be final.",
            Model.getFacade().isReadOnly(attributeFors));
        assertTrue("Attribute s should have type String.",
            "String".equals(Model.getFacade().getName(attribType)));
        assertTrue("Attribute s has no initializer.",
            Model.getFacade().isInitialized(attributeFors)
            && initializer != null);
        assertEquals("The initializer of attribute s is wrong.",
            " \"final String object\"",
            Model.getFacade().getBody(initializer));
    }

    /**
     * Test if the association was processed correctly.
     */
    @SuppressWarnings("unchecked")
    public void testAssociation() {
        parsedPackage = Model.getFacade().lookupIn(parsedModel, "testpackage");
        assertNotNull("No package \"testpackage\" found in model.",
            parsedPackage);
        parsedClass = Model.getFacade().lookupIn(parsedPackage, "TestClass");
        assertNotNull("No class \"TestClass\" found.", parsedClass);
        Collection associationEnds =
            Model.getFacade().getAssociationEnds(parsedClass);
        assertNotNull("No association ends found ib class.", associationEnds);
        assertEquals("Number of association ends is wrong", 2,
            associationEnds.size());
        Object associationEnd = null;
        Object association = null;
        int navigableCount = 0;
        Iterator iter = associationEnds.iterator();
        while (iter.hasNext()) {
            associationEnd = iter.next();
            assertTrue("The attribute end should be recognized as "
                + "an attribute end.",
                Model.getFacade().isAAssociationEnd(associationEnd));
            assertEquals("The type of both association ends must be the class.",
                parsedClass, Model.getFacade().getType(associationEnd));
            if (association == null) {
                association = Model.getFacade().getAssociation(associationEnd);
                assertTrue(
                    "The attribute should be recognized as an attribute.",
                    Model.getFacade().isAAssociation(association));
                assertEquals("The association name is wrong.",
                    "TestClass -> TestClass",
                    Model.getFacade().getName(association));
            } else {
                assertEquals(
                    "Association end must belong to the same association.",
                    association,
                    Model.getFacade().getAssociation(associationEnd));
            }
            if (Model.getFacade().isNavigable(associationEnd)) {
                ++navigableCount;
            }
        }
        assertEquals("Only one association end must be navigable.",
            1, navigableCount);
    }

    /**
     * Test if the operations were processed correctly.
     */
    @SuppressWarnings("unchecked")
    public void testOperations() {
        parsedPackage = Model.getFacade().lookupIn(parsedModel, "testpackage");
        assertNotNull("No package \"testpackage\" found in model.",
            parsedPackage);
        parsedClass = Model.getFacade().lookupIn(parsedPackage, "TestClass");
        assertNotNull("No class \"TestClass\" found.", parsedClass);
        Collection operations = Model.getFacade().getOperations(parsedClass);
        assertNotNull("No operations found in class.", operations);
        assertEquals("Number of operations is wrong", 4, operations.size());
        Object operation = null;
        Object operationForTestClass = null;
        Object operationForupdate = null;
        Object operationForx = null;
        Iterator iter = operations.iterator();
        while (iter.hasNext()) {
            operation = iter.next();
            assertTrue("The operation should be recognized as an operation.",
                Model.getFacade().isAOperation(operation));
            if ("TestClass".equals(Model.getFacade().getName(operation))) {
                operationForTestClass = operation;
            } else if ("update".equals(Model.getFacade().getName(operation))) {
                operationForupdate = operation;
            } else if ("getString".equals(
                    Model.getFacade().getName(operation))) {
                operationForgetString = operation;
            } else if ("x".equals(Model.getFacade().getName(operation))) {
                operationForx = operation;
            }
        }
        assertTrue("The operations have wrong names.",
            operationForTestClass != null
            && operationForupdate != null
            && operationForgetString != null
            && operationForx != null);
        assertTrue("Operation TestClass should be protected.",
            Model.getFacade().isProtected(operationForTestClass));
        assertEquals("The body of operation TestClass is wrong.",
            BODY2, getBody(operationForTestClass));
        assertTrue("Operation update should be public.",
            Model.getFacade().isPublic(operationForupdate));
        assertEquals("The body of operation update is wrong.", BODY1,
            getBody(operationForupdate));
        assertTrue("Operation getString should be static.",
            Model.getFacade().isStatic(operationForgetString));
        assertTrue("Operation getString should be private.",
            Model.getFacade().isPrivate(operationForgetString));
        assertEquals("The body of operation getString is wrong.", BODY3,
            getBody(operationForgetString));
        assertTrue("Operation x should be abstract.",
            Model.getFacade().isAbstract(operationForx));
        assertTrue("Operation x should have package visibility.",
            Model.getFacade().isPackage(operationForx));
    }

    /**
     * Test if the Javadoc was imported correctly.
     */
    @SuppressWarnings("unchecked")
    public void testJavadoc() {
        parsedPackage = Model.getFacade().lookupIn(parsedModel, "testpackage");
        assertNotNull("No package \"testpackage\" found in model.",
            parsedPackage);
        parsedClass = Model.getFacade().lookupIn(parsedPackage, "TestClass");
        assertNotNull("No class \"TestClass\" found.", parsedClass);
    	String doc = Model.getFacade().getTaggedValueValue(parsedClass,
            Argo.DOCUMENTATION_TAG);
        assertEquals("The class has the wrong documentation.", JAVADOC1, doc);
        Collection operations = Model.getFacade().getOperations(parsedClass);
        assertNotNull("No operations found in class.", operations);
        Object operation = null;
        Iterator iter = operations.iterator();
        while (iter.hasNext()) {
            operation = iter.next();
            if ("getString".equals(Model.getFacade().getName(operation))) {
                operationForgetString = operation;
            }
        }
    	doc = Model.getFacade().getTaggedValueValue(operationForgetString,
            Argo.DOCUMENTATION_TAG);
        assertEquals("The operation getString has the wrong documentation.",
            JAVADOC2, doc);
    }

    /**
     * Gets the (first) body of an operation.
     *
     * @param operation The operation.
     * @return The first body.
     */
    @SuppressWarnings("unchecked")
    private static String getBody(Object operation) {
        String body = null;
        Collection methods = Model.getFacade().getMethods(operation);
        if (methods != null && !methods.isEmpty()) {
            Object expression =
                Model.getFacade().getBody(methods.iterator().next());
            body = (String) Model.getFacade().getBody(expression);
        }
        return body;
    }

    /**
     * Instances of the model and its components.
     */
    private static Object parsedModel;
    private static Object parsedPackage;
    private static Object parsedClass;
    private static Object operationForgetString;

    private static final String JAVADOC1 =
        " A Javadoc comment ";

    private static final String JAVADOC2 =
        " Another Javadoc comment ";

    private static final String BODY1 =
        "\n        if (arg instanceof TestClass) testClass = (TestClass)arg;\n";

    private static final String BODY2 =
        "\n        // A constructor with a single line comment\n"
        + "        this.n = n;\n";

    private static final String BODY3 =
        "\n        // A static method\n        return s;\n";

    /**
     * Test input for the parser. It's the content of a Java source file. It's
     * hardcoded here, because this test case strongly depends on this.
     */
    private static final String PARSERINPUT =
              "package testpackage;\n"
            + "import java.util.Observer;\n"
            + "/**" + JAVADOC1 + "*/\n"
            + "public abstract class TestClass "
            + "extends Object implements Observer {\n"
            + "    private int n = 0;\n"
            + "    protected static TestClass testClass;\n"
            + "    public static final String s = \"final String object\";\n"
            + "    protected TestClass(int n) {"
            + BODY2
            + "    }\n"
            + "    public void update(java.util.Observable o, Object arg) {"
            + BODY1
            + "    }\n"
            + "    /**" + JAVADOC2 + "*/\n"
            + "    private static String getString() {"
            + BODY3
            + "    }\n"
            + "    abstract void x();\n"
            + "    /* A multiline\n"
            + "       comment\n"
            + "    */\n"
            + "}";

    private Profile profileJava;
}
