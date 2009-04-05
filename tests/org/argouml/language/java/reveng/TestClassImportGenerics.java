// $Id: TestJavaImportClass.java 96 2008-12-07 20:23:53Z tfmorris $
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

import antlr.ASTFactory;
import antlr.CommonAST;
import antlr.debug.misc.ASTFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import junit.framework.TestCase;
import org.argouml.language.java.reveng.classfile.ClassfileParser;
import org.argouml.language.java.reveng.classfile.ClassfileTreeParser;
import org.argouml.language.java.reveng.classfile.SimpleByteLexer;
import org.argouml.Helper;
import org.argouml.kernel.Project;
import org.argouml.kernel.ProjectManager;
import org.argouml.model.CoreHelper;
import org.argouml.model.Model;
import org.argouml.profile.Profile;
import org.argouml.profile.init.InitProfileSubsystem;
import org.omg.uml.foundation.core.ModelElement;
import org.omg.uml.foundation.core.TemplateParameter;

public class TestClassImportGenerics extends TestCase {

    private static Object parsedModel;
    private static Object parsedPackage;
    private static Object parsedClass;
    private static Project project;
    private static Profile profileJava;
    private static final String TESTED_CLASS="build/tests/classes/org/argouml/language/java/reveng/TestClassImportGenerics$TestedClass.class";


    public TestClassImportGenerics(String str) {
        super(str);
        if (!Model.isInitiated()) {
            Helper.initializeMDR();
        }
        new InitProfileSubsystem().init();
        ProjectManager.getManager().makeEmptyProject();
        project = (Project) ProjectManager.getManager().getOpenProjects().toArray()[0];
    }

    public void testParsing() {
        try {
            SimpleByteLexer lexer = new SimpleByteLexer(new DataInputStream(new FileInputStream(TESTED_CLASS)));
            ClassfileParser parser = new ClassfileParser(lexer);
            parser.classfile();
            CommonAST t = (CommonAST) parser.getAST();
            for (Profile profile : project.getProfileConfiguration().getProfiles()) {
                if ("Java".equals(profile.getDisplayName())) {
                    System.err.println("profile is " + profile.getDisplayName());
                    profileJava = profile;
                }
            }
            parsedModel = Model.getModelManagementFactory().createModel();
            assertNotNull("Creation of model failed.", parsedModel);
            Modeller modeller =
                    new Modeller(parsedModel, profileJava, true, true, TESTED_CLASS);
            assertNotNull("Creation of Modeller instance failed.", modeller);
            ClassfileTreeParser p = new ClassfileTreeParser();
            p.classfile(parser.getAST(), modeller);

            if (parsedPackage == null) {
                parsedPackage = parsedModel;
                for (String s : "org.argouml.language.java.reveng".split("\\.")) {
                    parsedPackage = Model.getFacade().lookupIn(parsedPackage, s);
                }
                assertNotNull("No package \"org.argouml.language.java.reveng\" found in model.",
                        parsedPackage);
            }
            if (parsedClass == null) {
                parsedClass =
                        Model.getFacade().lookupIn(parsedPackage, "TestClassImportGenerics$TestedClass");
                assertNotNull("No class \"TestClassImportGenerics$TestedClass\" found.", parsedClass);
            }
            ModelElement element = (ModelElement)parsedClass;
            for (TemplateParameter p2 : element.getTemplateParameter()) {
            	String name = p2.getParameter().getName();
            	System.err.println("name found:"+name);
            }
//            Collection attributes = Model.getFacade().getAttributes(parsedClass);
//            assertNotNull("No attributes found in class.", attributes);
//            assertEquals("Number of attributes is wrong", 3, attributes.size());
//            Object attribute = null;
//            Iterator iter = attributes.iterator();
//            while (iter.hasNext()) {
//                attribute = iter.next();
//                CoreHelper h = Model.getCoreHelper();
//                assertTrue("The attribute should be recognized as an attribute.",
//                        Model.getFacade().isAAttribute(attribute));
//            //Object attribType = Model.getFacade().getType(attribute);
//            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        }
    }

//    public static class TestedClass<X extends List<String>&Comparable<X>, Y extends ArrayList<Integer>> {
//    }
    
    public static class TestedClass<X, Y> {
    }
    
    public static void main(String[] args) throws Exception {
        SimpleByteLexer lexer = new SimpleByteLexer(new DataInputStream(new FileInputStream(TESTED_CLASS)));
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