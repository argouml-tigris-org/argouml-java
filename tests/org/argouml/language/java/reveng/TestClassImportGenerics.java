package org.argouml.language.java.reveng;

import antlr.ASTFactory;
import antlr.CommonAST;
import antlr.debug.misc.ASTFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.FileInputStream;
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

public class TestClassImportGenerics extends TestCase {

    private static Object parsedModel;
    private static Object parsedPackage;
    private static Object parsedClass;
    private static Project project;
    private static Profile profileJava;

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
            SimpleByteLexer lexer = new SimpleByteLexer(new DataInputStream(new FileInputStream("build/tests/classes/org/argouml/language/java/reveng/TestClassImportGenerics$TestedClass.class")));
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
                    new Modeller(parsedModel, profileJava, true, true, "build/tests/classes/org/argouml/language/java/reveng/TestClassImportGenerics$TestedClass.class");
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
            Collection attributes = Model.getFacade().getAttributes(parsedClass);
            assertNotNull("No attributes found in class.", attributes);
            assertEquals("Number of attributes is wrong", 3, attributes.size());
            Object attribute = null;
            Iterator iter = attributes.iterator();
            while (iter.hasNext()) {
                attribute = iter.next();
                CoreHelper h = Model.getCoreHelper();
                assertTrue("The attribute should be recognized as an attribute.",
                        Model.getFacade().isAAttribute(attribute));
            //Object attribType = Model.getFacade().getType(attribute);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.toString());
        }
    }

    public static class TestedClass<E> {

        private String arg1;
        private E arg2;
        private List<E> arg3;

        public List method_1() {
            return null;
        }
        public List<E> method_2() {
            return null;
        }
    }

    private static final String SHOW_CLASS="build/tests/classes/org/argouml/language/java/reveng/TestClassImportGenerics$TestedClass.class";

    public static void main(String[] args) throws Exception {
        SimpleByteLexer lexer = new SimpleByteLexer(new DataInputStream(new FileInputStream(SHOW_CLASS)));
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