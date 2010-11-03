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

// Copyright (c) 2007 The Regents of the University of California. All
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

import org.argouml.model.Model;

/**
 * <p>Test the import of Java sources which have Unicode characters in
 * various locations, including in identifiers.</p>
 *
 * <p>The content of the Java source file is a private constant at the
 * bottom of the source of this class.</p>
 *
 * <p>The constant is written using the ISO-8859-1 encoding so this class
 * needs to be compiled using that encoding.</p>
 */
public class TestJavaImportUnicode extends TestCase {
    private ImportFixture importFixture;
    /*
     * @see junit.framework.TestCase#TestCase(String)
     */
    public TestJavaImportUnicode(String str) {
        super(str);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        importFixture = new JavaImportFixture(PARSERINPUT, "TestClass.java");
        importFixture.setUp();
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
        Object parsedPackage = lookupPackage();
        assertEquals("Inconsistent package name.",
                "téstpackage", Model.getFacade().getName(parsedPackage));
        assertEquals("The namespace of the package should be the model.",
                parsedModel, Model.getFacade().getNamespace(parsedPackage));
        assertTrue("The package should be recognized as a package.",
                Model.getFacade().isAPackage(parsedPackage));
    }

    /**
     * Test if the import was processed correctly.
     */
    @SuppressWarnings("unchecked")
    public void testClass() {
        Object parsedPackage = lookupPackage();
        Object parsedClass = lookupClass(parsedPackage);
        assertEquals("Inconsistent class name.",
            "TéstClass", Model.getFacade().getName(parsedClass));
        assertEquals("The namespace of the class should be \"téstpackage\".",
            parsedPackage, Model.getFacade().getNamespace(parsedClass));
        Collection generalizations =
            Model.getFacade().getGeneralizations(parsedClass);
        assertNotNull("No generalizations found for class.", generalizations);
        Object generalization = null;
        Iterator iter = generalizations.iterator();
        if (iter.hasNext()) {
            generalization = iter.next();
        }
        assertNotNull("No generalization found for class.", generalization);

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
            "TéstClass -> Observer", Model.getFacade().getName(abstraction));

    }

    /**
     * Convenience method to lookup and check Package.
     */
    private Object lookupPackage() {
        Object parsedPackage = Model.getFacade().lookupIn(
            parsedModel, "téstpackage");
        assertNotNull("No package \"téstpackage\" found in model.",
            parsedPackage);
        return parsedPackage;
    }

    /**
     * Convenience method to lookup and check Class.
     */
    private Object lookupClass(Object thePackage) {
        Object parsedClass =
            Model.getFacade().lookupIn(thePackage, "TéstClass");
        assertNotNull("No class \"TéstClass\" found.", parsedClass);
        return parsedClass;
    }

    /**
     * Test if the attributes were processed correctly.
     */
    @SuppressWarnings("unchecked")
    public void testAttributes() {
        Object parsedPackage = lookupPackage();
        Object parsedClass = lookupClass(parsedPackage);
        Collection attributes = Model.getFacade().getAttributes(parsedClass);
        assertNotNull("No attributes found in class.", attributes);
        assertEquals("Number of attributes is wrong", 1, attributes.size());
        Object attribute = null;
        Object attributeFors = null;
        Iterator iter = attributes.iterator();
        while (iter.hasNext()) {
            attribute = iter.next();
            assertTrue("The attribute should be recognized as an attribute.",
                    Model.getFacade().isAAttribute(attribute));
            if ("sé".equals(Model.getFacade().getName(attribute))) {
                attributeFors = attribute;
            }
        }
        assertTrue("The attribute sé has the wrong name.",
                 attributeFors != null);

        Object initializer = Model.getFacade().getInitialValue(attributeFors);
        assertTrue("Attribute sé has no initializer.",
                Model.getFacade().isInitialized(attributeFors)
                && initializer != null);
        assertEquals("The initializer of attribute sé is wrong.",
            " \"final String objéct\"", Model.getFacade().getBody(initializer));
    }

    /**
     * Test if the association was processed correctly.
     */
    @SuppressWarnings("unchecked")
    public void testAssociation() {
        Object parsedPackage = lookupPackage();
        Object parsedClass = lookupClass(parsedPackage);
        Collection associationEnds =
            Model.getFacade().getAssociationEnds(parsedClass);
        assertNotNull("No association ends found in class.", associationEnds);
        assertEquals("Number of association ends is wrong", 2,
                associationEnds.size());
        Object associationEnd = null;
        Object association = null;
        int navigableCount = 0;
        Iterator iter = associationEnds.iterator();
        while (iter.hasNext()) {
            associationEnd = iter.next();
            assertTrue(
                    "The attribute end should be recognized as "
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
                    "TéstClass -> TéstClass",
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
        Object parsedPackage = lookupPackage();
        Object parsedClass = lookupClass(parsedPackage);
        Collection operations = Model.getFacade().getOperations(parsedClass);
        assertNotNull("No operations found in class.", operations);
        assertEquals("Number of operations is wrong", 2, operations.size());
        Object operation = null;
        Object operationForTestClass = null;
        Object operationForupdate = null;
        Iterator iter = operations.iterator();
        while (iter.hasNext()) {
            operation = iter.next();
            assertTrue("The operation should be recognized as an operation.",
                    Model.getFacade().isAOperation(operation));
            if ("TéstClass".equals(Model.getFacade().getName(operation))) {
                operationForTestClass = operation;
            } else if ("updaté".equals(Model.getFacade().getName(operation))) {
                operationForupdate = operation;
            }
        }
        assertTrue("The operations have wrong names.",
            operationForTestClass != null
            && operationForupdate != null);
        assertEquals("The body of operation update is wrong.",
                BODY1,
                getBody(operationForupdate));

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

    private static final String BODY1 =
        "\n        if (arg instanceof TéstClass) téstClass = (TéstClass)arg;\n";

    /**
     * Test input for the parser. It's the content of a Java source file. It's
     * hardcoded here, because this test case strongly depends on this.
     */
    private static final String PARSERINPUT =
              "package téstpackage;\n"
            + "import java.util.Observer;\n"
            + "/** A Javadoc commént */\n"
            + "public abstract class TéstClass "
            + "extends Object implements Observer {\n"
            + "    protected static TéstClass téstClass;\n"
            + "    public static final String sé = \"final String objéct\";\n"
            + "    protected TéstClass(int n) {"
            + "    }\n"
            + "    public void updaté(java.util.Observable o, Object arg) {"
            + BODY1
            + "    }\n"

            + "}";

    private Object parsedModel;
}
