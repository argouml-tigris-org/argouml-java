/* $Id$
 *****************************************************************************
 * Copyright (c) 2009-2013 Contributors - see below
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thomas Neustupny
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.argouml.application.api.Argo;
import org.argouml.application.helpers.ResourceLoaderWrapper;
import org.argouml.configuration.Configuration;
import org.argouml.language.java.JavaModuleGlobals;
import org.argouml.model.Model;
import org.argouml.moduleloader.ModuleInterface;
import org.argouml.ocl.ArgoFacade;
import org.argouml.uml.DocumentationManager;
import org.argouml.uml.generator.CodeGenerator;
import org.argouml.uml.generator.GeneratorHelper;
import org.argouml.uml.generator.GeneratorManager;
import org.argouml.uml.generator.Language;
import org.argouml.uml.generator.TempFileUtils;
import org.argouml.uml.reveng.ImportInterface;

import tudresden.ocl.OclTree;
import tudresden.ocl.parser.analysis.DepthFirstAdapter;
import tudresden.ocl.parser.node.AConstraintBody;

/**
 * FileGenerator implementing class to generate Java for display in diagrams and
 * in text fields in the ArgoUML user interface.
 */
public class GeneratorJava implements CodeGenerator, ModuleInterface {

    /**
     * Logger.
     */
    private static final Logger LOG =
        Logger.getLogger(GeneratorJava.class.getName());

    private boolean verboseDocs;

    private boolean lfBeforeCurly;

    private static final boolean VERBOSE_DOCS = false;

    private static final String LINE_SEPARATOR = System
            .getProperty("line.separator");

    private static final String LANG_PACKAGE = "java.lang";

    private static final Set<String> JAVA_TYPES;
    static {
        Set<String> types = new HashSet<String>();
        types.add("void");
        types.add("boolean");
        types.add("byte");
        types.add("char");
        types.add("int");
        types.add("short");
        types.add("long");
        types.add("float");
        types.add("double");
        JAVA_TYPES = Collections.unmodifiableSet(types);
    }

    // TODO: make it configurable
    // next two flags shows in what mode we are working
    /**
     * <code>true</code> when GenerateFile.
     */
    private static boolean isFileGeneration;

    /**
     * <code>true</code> if GenerateFile in Update Mode.
     */
    private static boolean isInUpdateMode;

    /**
     * Two spaces used for indenting code in classes.
     */
    private static final String INDENT = "  ";

    /**
     * The Language instance.
     */
    private static Language java = GeneratorHelper.makeLanguage("Java", "Java",
            ResourceLoaderWrapper.lookupIconResource("JavaNotation"));

    /**
     * Generates a file for the classifier. This method could have been static
     * if it where not for the need to call it through the Generatorinterface.
     * Returns the full path name of the the generated file or null if no file
     * can be generated.
     * 
     * @param modelElement the element to be generated
     * @param path the path where the element will be generated
     * @return String full path name of the the generated file
     */
    private String generateFile(Object modelElement, String path) {
        String name = Model.getFacade().getName(modelElement);
        if (name == null || name.length() == 0) {
            return null;
        }
        Object classifier = modelElement;
        String filename = name + ".java";
        StringBuilder sbPath = new StringBuilder(path);
        if (!path.endsWith(FILE_SEPARATOR)) {
            sbPath.append(FILE_SEPARATOR);
        }

        String packagePath = getPackageName(Model.getFacade().getNamespace(
                classifier));

        int lastIndex = -1;
        do {
            File f = new File(sbPath.toString());
            if (!f.isDirectory() && !f.mkdir()) {
                LOG.severe(" could not make directory " + path);
                return null;
            }

            if (lastIndex == packagePath.length()) {
                break;
            }

            int index = packagePath.indexOf(".", lastIndex + 1);
            if (index == -1) {
                index = packagePath.length();
            }

            sbPath.append(packagePath.substring(lastIndex + 1, index)
                    + FILE_SEPARATOR);
            lastIndex = index;
        } while (true);

        String pathname = sbPath.toString() + filename;
        // cat.info("-----" + pathname + "-----");

        // now decide whether file exist and need an update or is to be
        // newly generated
        File f = new File(pathname);
        isFileGeneration = true; // used to produce method javadoc

        // String pathname = path + filename;
        // TODO: package, project basepath, tagged values to configure
        LOG.info("Generating " + f.getPath());
        isFileGeneration = true;
        String header = generateHeader(classifier, pathname, packagePath);
        String src = generateClassifier(classifier);
        BufferedWriter fos = null;
        try {
            if (Configuration.getString(Argo.KEY_INPUT_SOURCE_ENCODING) == null
                    || Configuration.getString(Argo.KEY_INPUT_SOURCE_ENCODING)
                            .trim().equals("")) {
                fos = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(f),
                        System.getProperty("file.encoding")));
            } else {
                fos = new BufferedWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(f),
                                Configuration.getString(
                                        Argo.KEY_INPUT_SOURCE_ENCODING)));
            }
            fos.write(header);
            fos.write(src);
        } catch (IOException exp) {
            LOG.severe("IO Exception: " + exp + ", for file: " + f.getPath());
        } finally {
            isFileGeneration = false;
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException exp) {
                LOG.severe("FAILED: " + f.getPath());
            }
        }

        // cat.info("----- end updating -----");
        return pathname;
    }

    private String generateHeader(Object cls, String pathname,
            String packagePath) {
        StringBuffer sb = new StringBuffer(80);
        // TODO: add user-defined copyright
        if (VERBOSE_DOCS) {
            sb.append("// FILE: ").append(pathname.replace('\\', '/'));
            sb.append(LINE_SEPARATOR).append(LINE_SEPARATOR);
        }
        if (packagePath.length() > 0) {
            sb.append("package ").append(packagePath).append(";");
            sb.append(LINE_SEPARATOR).append(LINE_SEPARATOR);
        }
        sb.append(generateImports(cls, packagePath));
        return sb.toString();
    }

    /**
     * Generates code for some modelelement. Subclasses should implement this to
     * generate code for different notations.
     * 
     * @param o the element to be generated
     * @return String the generated code
     */
    private String generate(Object o) {
        if (o == null) {
            return "";
        }
        if (Model.getFacade().isAActionState(o)) {
            return generateActionState(o);
        }
        if (Model.getFacade().isAExtensionPoint(o)) {
            return generateExtensionPoint(o);
        }
        if (Model.getFacade().isAOperation(o)) {
            return generateOperation(o, false);
        }
        if (Model.getFacade().isAAttribute(o)) {
            return generateAttribute(o, false);
        }
        if (Model.getFacade().isAParameter(o)) {
            return generateParameter(o);
        }
        if (Model.getFacade().isAPackage(o)) {
            return generatePackage(o);
        }
        if (Model.getFacade().isAClassifier(o)) {
            return generateClassifier(o);
        }
        if (Model.getFacade().isAExpression(o)) {
            return generateExpression(o);
        }
        if (o instanceof String) {
            return generateName((String) o);
        }
        if (o instanceof String) {
            return generateUninterpreted((String) o);
        }
        if (Model.getFacade().isAStereotype(o)) {
            return generateStereotype(o);
        }
        if (Model.getFacade().isATaggedValue(o)) {
            return generateTaggedValue(o);
        }
        if (Model.getFacade().isAAssociationEnd(o)) {
            return generateAssociationEnd(o);
        }
        if (Model.getFacade().isAMultiplicity(o)) {
            return generateMultiplicity(o);
        }
        if (Model.getFacade().isAState(o)) {
            return generateState(o);
        }
        if (Model.getFacade().isATransition(o)) {
            return generateTransition(o);
        }
        if (Model.getFacade().isAAction(o)) {
            return generateAction(o);
        }
        if (Model.getFacade().isACallAction(o)) {
            return generateAction(o);
        }
        if (Model.getFacade().isAGuard(o)) {
            return generateGuard(o);
        }
        if (Model.getFacade().isAMessage(o)) {
            return generateMessage(o);
        }
        if (Model.getFacade().isAEvent(o)) {
            return generateEvent(o);
        }
        if (Model.getFacade().isAVisibilityKind(o)) {
            return generateVisibility(o);
        }

        if (Model.getFacade().isAModelElement(o)) {
            return generateName(Model.getFacade().getName(o));
        }

        return o.toString();
    }

    /**
     * Generates the import statements for the class
     * 
     * @param cls The class object
     * @param packagePath The package path
     * @return a string containing the import statements
     */
    private String generateImports(Object cls, String packagePath) {
        // If the model is built by the import of Java source code, then a
        // component named after the filename was created, which manages the
        // import statements for all included classes/interfaces. This
        // component is now searched for cls in order to extract the imports.
        Object ns = Model.getFacade().getNamespace(cls);
        if (ns != null) {
            for (Object oe : Model.getFacade().getOwnedElements(ns)) {
                if (Model.getFacade().getUmlVersion().charAt(0) == '1'
                        && Model.getFacade().isAComponent(oe)) {
                    for (Object re
                             : Model.getFacade().getResidentElements(oe)) {
                        Object r = Model.getFacade().getResident(re);
                        if (r.equals(cls)) {
                            return generateArtifactImports(oe);
                        }
                    }
                } else if (Model.getFacade().isAArtifact(oe)
                           && (Model.getCoreHelper().getUtilizedElements(oe)
                               .contains(cls))) {
                    return generateArtifactImports(oe);
                }
            }
        }
        // We now have the situation that no component with package imports
        // was found, so the import statements are guessed from the used model
        // elements inside cls.
        StringBuffer sb = new StringBuffer(80);
        Set<String> importSet = new HashSet<String>();

        // now check packages of all feature types
        for (Object mFeature : Model.getFacade().getFeatures(cls)) {
            if (Model.getFacade().isAAttribute(mFeature)) {
                String ftype = generateImportType(
                        Model.getFacade().getType(mFeature), packagePath);
                if (ftype != null) {
                    importSet.add(ftype);
                }
            } else if (Model.getFacade().isAOperation(mFeature)) {
                // check the parameter types
                for (Object parameter : Model.getFacade().getParameters(
                        mFeature)) {
                    String ftype = generateImportType(Model.getFacade()
                            .getType(parameter), packagePath);
                    if (ftype != null) {
                        importSet.add(ftype);
                    }
                }

                // check the return parameter types
                for (Object parameter : Model.getCoreHelper()
                        .getReturnParameters(mFeature)) {
                    String ftype = generateImportType(Model.getFacade()
                            .getType(parameter), packagePath);
                    if (ftype != null) {
                        importSet.add(ftype);
                    }
                }

                // check raised signals
                for (Object signal : Model.getFacade().getRaisedSignals(
                        mFeature)) {
                    if (!Model.getFacade().isAException(signal)) {
                        continue;
                    }
                    String ftype = generateImportType(Model.getFacade()
                            .getType(signal), packagePath);
                    if (ftype != null) {
                        importSet.add(ftype);
                    }
                }
            }
        }

        // now check generalizations
        for (Object gen : Model.getFacade().getGeneralizations(cls)) {
            Object parent = Model.getFacade().getGeneral(gen);
            if (parent == cls) {
                continue;
            }

            String ftype = generateImportType(parent, packagePath);
            if (ftype != null) {
                importSet.add(ftype);
            }
        }

        // now check packages of the interfaces
        for (Object iface : Model.getFacade().getSpecifications(cls)) {
            String ftype = generateImportType(iface, packagePath);
            if (ftype != null) {
                importSet.add(ftype);
            }
        }

        // check association end types
        for (Object associationEnd
                 : Model.getFacade().getAssociationEnds(cls)) {
            Object association = Model.getFacade().getAssociation(
                    associationEnd);
            for (Object associationEnd2 : Model.getFacade().getConnections(
                    association)) {
                if (associationEnd2 != associationEnd
                        && Model.getFacade().isNavigable(associationEnd2)
                        && !Model.getFacade().isAbstract(
                                Model.getFacade().getAssociation(
                                        associationEnd2))) {
                    // association end found
                    if (Model.getFacade().getUpper(associationEnd2) != 1) {
                        importSet.add("java.util.List");
                    } else {
                        String ftype = generateImportType(Model.getFacade()
                                .getType(associationEnd2), packagePath);
                        if (ftype != null) {
                            importSet.add(ftype);
                        }
                    }
                }
            }

        }
        // finally generate the import statements
        for (String importType : importSet) {
            sb.append("import ").append(importType).append(";");
            sb.append(LINE_SEPARATOR);
        }
        if (!importSet.isEmpty()) {
            sb.append(LINE_SEPARATOR);
        }
        return sb.toString();
    }

    private String generateArtifactImports(Object artifact) {
        StringBuffer ret = new StringBuffer();
        Object compNamespace = Model.getFacade().getNamespace(artifact);
        boolean found = false;
        for (Object o : Model.getFacade().getClientDependencies(artifact)) {
            boolean isJavaImport = false;
            for (Object stereotype : Model.getFacade().getStereotypes(o)) {
                if ("javaImport".equals(
                        Model.getFacade().getName(stereotype))) {
                    isJavaImport = true;
                    break;
                }
            }
            if (isJavaImport) {
                for (Object elem : Model.getFacade().getSuppliers(o)) {
                    Object ns = Model.getFacade().getNamespace(elem);
                    if (ns != null && !ns.equals(compNamespace)) {
                        String packageName = getPackageName(ns);
                        ret.append("import ");
                        if (packageName != null && packageName.length() > 0) {
                            ret.append(getPackageName(ns));
                            ret.append('.');
                        }
                        ret.append(Model.getFacade().getName(elem));
                        if (Model.getFacade().isAPackage(elem)) {
                            ret.append(".*");
                        }
                        ret.append(";");
                        ret.append(LINE_SEPARATOR);
                        found = true;
                    }
                }
            }
        }
        if (found) {
            ret.append(LINE_SEPARATOR);
        }
        return ret.toString();
    }

    private String generateImportType(Object type, String exclude) {
        String ret = null;

        if (Model.getFacade().isADataType(type)
                && JAVA_TYPES.contains(Model.getFacade().getName(type))) {
            return null;
        }

        if (type != null && Model.getFacade().getNamespace(type) != null) {
            String p = getPackageName(Model.getFacade().getNamespace(type));
            if (!p.equals(exclude) && !p.equals(LANG_PACKAGE)) {
                if (p.length() > 0) {
                    ret = p + '.' + Model.getFacade().getName(type);
                } else {
                    ret = Model.getFacade().getName(type);
                }
            }
        }
        return ret;
    }

    /**
     * Generate code for an extension point.
     * <p>
     * 
     * @param ep The extension point to generate for
     * 
     * @return The generated code string. Always empty in this implementation.
     */
    private String generateExtensionPoint(Object ep) {
        return null;
    }

    /**
     * Generate source code for an operation.
     * <p>
     * NOTE: This needs to be package visibility because it is used in
     * OperationCodePiece.
     * 
     * @param op UML Operation to generate code for
     * @param documented flag indicating documentation comments should be
     *            included.
     * @return String containing generated code.
     */
    String generateOperation(Object op, boolean documented) {
        if (isFileGeneration) {
            documented = true; // fix Issue 1506
        }
        StringBuffer sb = new StringBuffer(80);
        String nameStr = null;
        boolean constructor = false;

        if (Model.getExtensionMechanismsHelper().hasStereotype(op, "create")) {
            nameStr = generateName(Model.getFacade().getName(
                    Model.getFacade().getOwner(op)));
            constructor = true;
        } else {
            nameStr = generateName(Model.getFacade().getName(op));
        }

        // Each pattern here must be similar to corresponding code piece
        // Operation code piece doesn't start with '\n'
        // so the next line is commented. See Issue 1505
        // sb.append(LINE_SEPARATOR); // begin with a blank line
        if (documented) {
            String s = generateConstraintEnrichedDocComment(op, documented,
                    INDENT);
            if (s != null && s.trim().length() > 0) {
                // should starts as the code piece
                sb.append(s).append(INDENT);
            }
        }

        sb.append(generateVisibility(op));
        sb.append(generateAbstractness(op));
        sb.append(generateScope(op));
        sb.append(generateChangeability(op));
        sb.append(generateConcurrency(op));

        // pick out return type
        Collection returnParams = Model.getCoreHelper().getReturnParameters(op);
        Object rp;
        if (returnParams.size() == 0) {
            rp = null;
        } else {
            rp = returnParams.iterator().next();
        }
        if (returnParams.size() > 1) {
            LOG.warning("Java generator only handles one return parameter"
                    + " - Found " + returnParams.size() + " for "
                    + Model.getFacade().getName(op));
        }
        if (rp != null && !constructor) {
            Object returnType = Model.getFacade().getType(rp);
            if (returnType == null) {
                sb.append("void ");
            } else {
                sb.append(generateClassifierRef(returnType)).append(' ');
            }
        }

        // name and params
        List params = new ArrayList(Model.getFacade().getParameters(op));
        params.remove(rp);

        sb.append(nameStr).append('(');

        if (params != null) {
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(generateParameter(params.get(i)));
            }
        }

        sb.append(')');

        Collection c = Model.getFacade().getRaisedSignals(op);
        if (!c.isEmpty()) {
            Iterator it = c.iterator();
            boolean first = true;

            while (it.hasNext()) {
                Object signal = it.next();

                if (!Model.getFacade().isAException(signal)) {
                    continue;
                }

                if (first) {
                    sb.append(" throws ");
                } else {
                    sb.append(", ");
                }

                sb.append(Model.getFacade().getName(it.next()));
                first = false;
            }
        }

        return sb.toString();
    }

    private String generateAttribute(Object attr, boolean documented) {
        if (isFileGeneration) {
            documented = true; // always "documented" if we generate file.
        }
        StringBuffer sb = new StringBuffer(80);
        if (documented) {
            String s = generateConstraintEnrichedDocComment(attr, documented,
                    INDENT);
            if (s != null && s.trim().length() > 0) {
                sb.append(s).append(INDENT);
            }
        }
        sb.append(generateCoreAttribute(attr));
        sb.append(";").append(LINE_SEPARATOR);

        return sb.toString();
    }

    /**
     * Generates the core attribute for a multiplicity
     * 
     * @param attr The attribute
     * @return a string containing the attribute reference.
     */
    String generateCoreAttribute(Object attr) {
        StringBuffer sb = new StringBuffer(80);
        sb.append(generateVisibility(attr));
        sb.append(generateScope(attr));
        sb.append(generateChangability(attr));
        Object type = Model.getFacade().getType(attr);
        Object multi = Model.getFacade().getMultiplicity(attr);
        // handle multiplicity here since we need the type
        // actually the API of generator is buggy since to generate
        // multiplicity correctly we need the attribute too
        if (type != null && multi != null) {
            if (Model.getFacade().getUpper(multi) == 1) {
                sb.append(generateClassifierRef(type)).append(' ');
            } else if (Model.getFacade().isADataType(type)) {
                sb.append(generateClassifierRef(type)).append("[] ");
            } else {
                sb.append("java.util.List ");
            }
        }

        sb.append(generateName(Model.getFacade().getName(attr)));
        Object init = Model.getFacade().getInitialValue(attr);
        if (init != null) {
            String initStr = generateExpression(init).trim();
            if (initStr.length() > 0) {
                sb.append(" = ").append(initStr);
            }
        }

        return sb.toString();
    }

    /**
     * Generates the literal value for an enumeration
     * 
     * @param literal a literal value for an enumeration.
     * @param documented indicates whether or not the documentation for the
     *            enumeration literal should be generated
     * @param sep a separator character
     * @return a string containing the literal value of an enumeration
     */
    private String generateEnumerationLiteral(Object literal,
            boolean documented, char sep) {
        if (isFileGeneration) {
            documented = true; // always "documented" if we generate file.
        }
        StringBuffer sb = new StringBuffer(80);
        if (documented) {
            String s = generateConstraintEnrichedDocComment(literal,
                    documented, INDENT);
            if (s != null && s.trim().length() > 0) {
                sb.append(s).append(INDENT);
            }
        }
        sb.append(generateName(Model.getFacade().getName(literal)));
        sb.append(sep).append(LINE_SEPARATOR);

        return sb.toString();
    }

    /**
     * Generates a parameter object.
     * 
     * @param parameter the parameter
     * @return a string containing a parameter
     */
    private String generateParameter(Object parameter) {
        StringBuffer sb = new StringBuffer(20);
        // TODO: qualifiers (e.g., const)
        // TODO: stereotypes...
        sb.append(generateClassifierRef(Model.getFacade().getType(parameter)));
        sb.append(' ');
        sb.append(generateName(Model.getFacade().getName(parameter)));
        // TODO: initial value
        return sb.toString();
    }

    /**
     * Generates the package statement
     * 
     * @param p The package object
     * @return a string containing the package statement
     */
    private String generatePackage(Object p) {
        StringBuffer sb = new StringBuffer(80);
        String packName = generateName(Model.getFacade().getName(p));
        sb.append("package ").append(packName).append(" {");
        sb.append(LINE_SEPARATOR);
        Collection ownedElements = Model.getFacade().getOwnedElements(p);
        for (Object modelElement : ownedElements) {
            // This is the only remaining references to generate(), if it
            // can be made more specific, we can remove that method - tfm
            // (do we support anything other than classifiers in a package?)
            sb.append(generate(modelElement));
            sb.append(LINE_SEPARATOR).append(LINE_SEPARATOR);
        }
        sb.append(LINE_SEPARATOR).append("})").append(LINE_SEPARATOR);
        return sb.toString();
    }

    /**
     * Generate the start sequence for a classifier. The start sequence is
     * everything from the preceding javadoc comment to the opening curly brace.
     * Start sequences are non-empty for classes and interfaces only.
     * 
     * This method is intended for package internal usage only.
     * 
     * @param cls the classifier for which to generate the start sequence
     * 
     * @return the generated start sequence
     */
    StringBuffer generateClassifierStart(Object cls) {
        String sClassifierKeyword;
        if (Model.getFacade().isAClass(cls)) {
            sClassifierKeyword = "class";
        } else if (Model.getFacade().isAInterface(cls)) {
            sClassifierKeyword = "interface";
        } else if (Model.getFacade().isAEnumeration(cls)) {
            sClassifierKeyword = "enum";
        } else {
            return null; // actors, use cases etc.
        }

        StringBuffer sb = new StringBuffer(80);

        // Add the comments for this classifier first.
        // Each pattern here must be similar to corresponding code piece
        // Classfier code piece doesn't start with LINE_SEPARATOR
        // so the next line is commented. See Issue 1505
        // sb.append (LINE_SEPARATOR);
        sb.append(DocumentationManager.getComments(cls));
        sb.append(generateConstraintEnrichedDocComment(cls, true, ""));

        // Now add visibility, but not for non public top level classifiers
        if (Model.getFacade().isPublic(cls)
                || Model.getFacade().isAClassifier(
                        Model.getFacade().getNamespace(cls))) {
            sb.append(generateVisibility(Model.getFacade().getVisibility(cls)));
        }

        // Add other modifiers in JLS order
        if (Model.getFacade().isAbstract(cls)
                && !(Model.getFacade().isAInterface(cls))) {
            sb.append("abstract ");
        }

        if (Model.getFacade().isLeaf(cls)) {
            sb.append("final ");
        }

        // add additional modifiers
        // TODO: This is for backward compatibility with old models reverse
        // engineered with earlier versions of ArgoUML. As of 0.24, and
        // probably earlier, ArgoUML should be able to capture all necessary
        // information in the model itself. - tfm - 20070217
        Object smod = Model.getFacade().getTaggedValue(cls,
                ImportInterface.SOURCE_MODIFIERS_TAG);
        if (smod != null && Model.getFacade().getValue(smod) != null) {
            sb.append(" ");
            sb.append(Model.getFacade().getValue(smod));
            sb.append(" ");
        }

        // add classifier keyword and classifier name
        sb.append(sClassifierKeyword).append(" ");
        sb.append(generateName(Model.getFacade().getName(cls)));
        // add type parameters
        List templateParameters = Model.getFacade().getTemplateParameters(cls);
        for (int i = 0; i < templateParameters.size(); i++) {
            if (i == 0) {
                sb.append("<");
            }
            Object param = Model.getFacade().getParameter(
                    templateParameters.get(i));
            sb.append(Model.getFacade().getName(param));
            for (String bound : new String[] {"extends", "super"}) {
                Object s = Model.getFacade().getTaggedValueValue(param, bound);
                if (s != null && s.toString().trim().length() > 0) {
                    sb.append(" " + bound + " " + s);
                }
            }
            if (i == templateParameters.size() - 1) {
                sb.append(">");
            } else {
                sb.append(", ");
            }
        }

        // add base class/interface
        String baseClass = generateGeneralization(Model.getFacade()
                .getGeneralizations(cls));
        if (!baseClass.equals("")) {
            sb.append(" ").append("extends ").append(baseClass);
        }

        // add implemented interfaces, if needed
        // UML: realizations!
        if (Model.getFacade().isAClass(cls)) {
            String interfaces = generateSpecification(cls);
            LOG.fine("Specification: " + interfaces);
            if (!interfaces.equals("")) {
                sb.append(" ").append("implements ").append(interfaces);
            }
        }

        // add opening brace
        sb.append(lfBeforeCurly ? (LINE_SEPARATOR + "{") : " {");

        // list tagged values for documentation
        String tv = generateTaggedValues(cls);
        if (tv != null && tv.length() > 0) {
            sb.append(LINE_SEPARATOR).append(INDENT).append(tv);
        }

        return sb;
    }

    /**
     * Generates the code for a classifer end
     * 
     * @param cls The classifier
     * @return a StringBuffer containing the classifier end
     */
    private StringBuffer generateClassifierEnd(Object cls) {
        StringBuffer sb = new StringBuffer();
        if (Model.getFacade().isAClass(cls)
                || Model.getFacade().isAInterface(cls)
                || Model.getFacade().isAEnumeration(cls)) {
            if (verboseDocs) {
                String classifierkeyword = null;
                if (Model.getFacade().isAClass(cls)) {
                    classifierkeyword = "class";
                } else {
                    classifierkeyword = "interface";
                }
                sb.append(LINE_SEPARATOR);
                sb.append("//end of ").append(classifierkeyword);
                sb.append(" ").append(Model.getFacade().getName(cls));
                sb.append(LINE_SEPARATOR);
            }
            sb.append(LINE_SEPARATOR);
            sb.append("}");
        }
        return sb;
    }

    /**
     * Append the classifier end sequence to the prefix text specified. The
     * classifier end sequence is the closing curly brace together with any
     * comments marking the end of the classifier.
     * 
     * This method is intended for package internal usage.
     * 
     * @param sbPrefix the prefix text to be amended. It is OK to call append on
     *            this parameter.
     * @param cls the classifier for which to generate the classifier end
     *            sequence. Only classes and interfaces have a classifier end
     *            sequence.
     * @return the complete classifier code, i.e., sbPrefix plus the classifier
     *         end sequence
     */
    StringBuffer appendClassifierEnd(StringBuffer sbPrefix, Object cls) {
        sbPrefix.append(generateClassifierEnd(cls));

        return sbPrefix;
    }

    /**
     * Generates code for a classifier. In case of Java code is generated for
     * classes and interfaces only at the moment.
     * 
     * @param cls a classifier object
     */
    private String generateClassifier(Object cls) {
        StringBuffer returnValue = new StringBuffer();
        StringBuffer start = generateClassifierStart(cls);
        if ((start != null) && (start.length() > 0)) {
            StringBuffer body = generateClassifierBody(cls);
            StringBuffer end = generateClassifierEnd(cls);
            returnValue.append(start.toString());
            if ((body != null) && (body.length() > 0)) {
                returnValue.append(LINE_SEPARATOR);
                returnValue.append(body);
                if (lfBeforeCurly) {
                    returnValue.append(LINE_SEPARATOR);
                }
            }
            returnValue.append((end != null) ? end.toString() : "");
        }
        return returnValue.toString();
    }

    /**
     * Generates the body of a class or interface.
     * 
     * @param cls The classifier object (either a class, interface or
     *            enumeration.
     */
    private StringBuffer generateClassifierBody(Object cls) {
        StringBuffer sb = new StringBuffer();
        if (Model.getFacade().isAClass(cls)
                || Model.getFacade().isAInterface(cls)
                || Model.getFacade().isAEnumeration(cls)) {
            String tv = null; // helper for tagged values

            // add attributes
            Collection sFeatures = Model.getFacade().getStructuralFeatures(cls);

            if (!sFeatures.isEmpty()) {
                sb.append(LINE_SEPARATOR);
                if (verboseDocs && Model.getFacade().isAClass(cls)) {
                    sb.append(INDENT).append("// Attributes");
                    sb.append(LINE_SEPARATOR);
                }

                boolean first = true;
                for (Object structuralFeature : sFeatures) {
                    if (!first) {
                        sb.append(LINE_SEPARATOR);
                    }
                    sb.append(INDENT);
                    // The only type of StructuralFeature is an Attribute
                    sb.append(generateAttribute(structuralFeature, false));

                    tv = generateTaggedValues(structuralFeature);
                    if (tv != null && tv.length() > 0) {
                        sb.append(INDENT).append(tv);
                    }
                    first = false;
                }
            }

            if (Model.getFacade().isAEnumeration(cls)) {
                addLiterals(cls, sb);
            }

            Collection ends = Model.getFacade().getAssociationEnds(cls);
            if (!ends.isEmpty()) {
                addAttributesImplementingAssociations(cls, sb, ends);
            }

            // Inner classes
            Collection elements = Model.getFacade().getOwnedElements(cls);
            for (Iterator i = elements.iterator(); i.hasNext();) {
                Object element = i.next();
                if (Model.getFacade().isAClass(element)
                        || Model.getFacade().isAInterface(element)) {

                    sb.append(generateClassifier(element));
                }
            }

            // add operations
            // TODO: constructors
            Collection bFeatures = Model.getFacade().getOperations(cls);

            if (!bFeatures.isEmpty()) {
                addOperations(cls, sb, bFeatures);
            }
        }
        return sb;
    }

    /**
     * Generates the operations
     * 
     * @param cls The classifier object
     * @param sb The StringBuffer to which the operations will be added
     * @param bFeatures a collection of behavioral features
     */
    private void addOperations(Object cls,
                               StringBuffer sb,
                               Collection bFeatures) {
        String tv;
        sb.append(LINE_SEPARATOR);
        if (verboseDocs) {
            sb.append(INDENT).append("// Operations");
            sb.append(LINE_SEPARATOR);
        }

        boolean first = true;
        for (Object behavioralFeature : bFeatures) {

            if (!first) {
                sb.append(LINE_SEPARATOR);
            }
            sb.append(INDENT);
            sb.append(generateOperation(behavioralFeature, false));

            tv = generateTaggedValues(behavioralFeature);

            if ((Model.getFacade().isAClass(cls))
                    && (Model.getFacade().isAOperation(behavioralFeature))
                    && (!Model.getFacade().isAbstract(behavioralFeature))) {
                if (lfBeforeCurly) {
                    sb.append(LINE_SEPARATOR).append(INDENT);
                } else {
                    sb.append(' ');
                }
                sb.append('{');

                if (tv.length() > 0) {
                    sb.append(LINE_SEPARATOR).append(INDENT).append(tv);
                }

                // there is no ReturnType in behavioral feature (UML)
                sb.append(LINE_SEPARATOR);
                sb.append(generateMethodBody(behavioralFeature));
                sb.append(INDENT);
                sb.append("}").append(LINE_SEPARATOR);
            } else {
                sb.append(";").append(LINE_SEPARATOR);
                if (tv.length() > 0) {
                    sb.append(INDENT).append(tv).append(LINE_SEPARATOR);
                }
            }

            first = false;
        }
    }

    /**
     * This method adds attribute associations to the string buffer.
     * 
     * @param cls The class object
     * @param sb The StringBuffer containing the text of the generated class.
     * @param ends The association ends
     */
    private void addAttributesImplementingAssociations(Object cls,
            StringBuffer sb, Collection ends) {
        String tv;
        sb.append(LINE_SEPARATOR);
        if (verboseDocs && Model.getFacade().isAClass(cls)) {
            sb.append(INDENT).append("// Associations");
            sb.append(LINE_SEPARATOR);
        }

        for (Object associationEnd : ends) {
            Object association = Model.getFacade().getAssociation(
                    associationEnd);

            sb.append(generateAssociationFrom(association, associationEnd));

            tv = generateTaggedValues(association);
            if (tv != null && tv.length() > 0) {
                sb.append(INDENT).append(tv);
            }
        }
    }

    /**
     * This method adds enumeration literals to the StringBuffer
     * 
     * @param cls The class object
     * @param sb The StringBuffer containing the generated class
     */
    private void addLiterals(Object cls, StringBuffer sb) {
        String tv;
        Collection literals = Model.getFacade().getEnumerationLiterals(cls);

        if (!literals.isEmpty()) {
            sb.append(LINE_SEPARATOR);
            if (verboseDocs) {
                sb.append(INDENT).append("// Literals");
                sb.append(LINE_SEPARATOR);
            }

            boolean first = true;
            int size = literals.size();
            int cnt = 0;
            for (Object literal : literals) {
                cnt++;
                if (!first) {
                    sb.append(LINE_SEPARATOR);
                }
                sb.append(INDENT);
                char sep = cnt != size ? ',' : ';';
                sb.append(generateEnumerationLiteral(literal, false, sep));

                tv = generateTaggedValues(literal);
                if (tv != null && tv.length() > 0) {
                    sb.append(INDENT).append(tv);
                }
                first = false;
            }
        }
    }

    /**
     * Generate the body of a method associated with the given operation. This
     * assumes there's at most one method associated!
     * 
     * If no method is associated with the operation, a default method body will
     * be generated.
     * 
     * @param op The operation object
     */
    private String generateMethodBody(Object op) {
        // cat.info("generateMethodBody");
        if (op != null) {
            for (Object m : Model.getFacade().getMethods(op)) {
                if (m != null) {
                    if (Model.getFacade().getBody(m) != null) {
                        String body = (String) Model.getFacade().getBody(
                                Model.getFacade().getBody(m));
                        // Note that this will not preserve empty lines
                        // in the body
                        StringTokenizer tokenizer = new StringTokenizer(body,
                                "\r\n");
                        StringBuffer bsb = new StringBuffer();
                        while (tokenizer.hasMoreTokens()) {
                            String token = tokenizer.nextToken();
                            if (token.length() > 0) {
                                bsb.append(token);
                                bsb.append(LINE_SEPARATOR);
                            }
                        }
                        if (bsb.length() <= 0) {
                            // generateClassifierBody relies on the string
                            // ending with a new-line
                            bsb.append(LINE_SEPARATOR);
                        }
                        return bsb.toString();
                    }
                    return "";
                }
            }

            // pick out return type
            Collection returnParams = Model.getCoreHelper()
                    .getReturnParameters(op);
            Object rp;
            if (returnParams.size() == 0) {
                rp = null;
            } else {
                rp = returnParams.iterator().next();
            }
            if (returnParams.size() > 1) {
                LOG.warning("Java generator only handles one return parameter"
                            + " - Found " + returnParams.size() + " for "
                            + Model.getFacade().getName(op));
            }
            if (rp != null) {
                Object returnType = Model.getFacade().getType(rp);
                return generateDefaultReturnStatement(returnType);
            }
        }

        return generateDefaultReturnStatement(null);
    }

    /**
     * This method generates default return statements for methods
     * 
     * @param cls The class object
     * @return
     */
    private String generateDefaultReturnStatement(Object cls) {
        if (cls == null) {
            return "";
        }

        String clsName = Model.getFacade().getName(cls);
        if (clsName.equals("void")) {
            return "";
        }
        if (clsName.equals("char")) {
            return INDENT + "return 'x';" + LINE_SEPARATOR;
        }
        if (clsName.equals("int")) {
            return INDENT + "return 0;" + LINE_SEPARATOR;
        }
        if (clsName.equals("boolean")) {
            return INDENT + "return false;" + LINE_SEPARATOR;
        }
        if (clsName.equals("byte")) {
            return INDENT + "return 0;" + LINE_SEPARATOR;
        }
        if (clsName.equals("long")) {
            return INDENT + "return 0;" + LINE_SEPARATOR;
        }
        if (clsName.equals("float")) {
            return INDENT + "return 0.0;" + LINE_SEPARATOR;
        }
        if (clsName.equals("double")) {
            return INDENT + "return 0.0;" + LINE_SEPARATOR;
        }
        return INDENT + "return null;" + LINE_SEPARATOR;
    }

    /**
     * This method generates the tagged values
     * 
     * @param e
     * @return a String containing the tagged values.
     */
    private String generateTaggedValues(Object e) {
        if (isInUpdateMode) {
            return ""; // no tagged values are generated in update mode.
        }
        Iterator iter = Model.getFacade().getTaggedValues(e);
        if (iter == null) {
            return "";
        }
        boolean first = true;
        StringBuffer buf = new StringBuffer();
        String s = null;
        while (iter.hasNext()) {
            /*
             * 2002-11-07 Jaap Branderhorst Was
             * 
             * s = generateTaggedValue((MTaggedValue) iter.next());
             * 
             * which caused problems because the test tags (i.e. tags with name
             * <NotationName.getName()>+TEST_SUFFIX) were still generated.
             * 
             * New code:
             */
            s = generateTaggedValue(iter.next());
            // end new code
            if (s != null && s.length() > 0) {
                if (first) {
                    buf.append("/* {");

                    first = false;
                } else {
                    buf.append(", ");
                }
                buf.append(s);
            }
        }
        /*
         * Corrected 2001-09-26 STEFFEN ZSCHALER
         * 
         * Was: if (!first) buf.append("}\n");
         * 
         * which caused problems with new-lines in tagged values.
         */
        if (!first) {
            buf.append("}*/").append(LINE_SEPARATOR);
        }

        return buf.toString();
    }

    /**
     * Generates a tagged value
     * 
     * @param tv a tagged value object
     * @return a String containing the tagged values (usually comments).
     */
    private String generateTaggedValue(Object tv) {
        if (tv == null) {
            return "";
        }
        String s = generateUninterpreted(Model.getFacade().getValueOfTag(tv));
        if (s == null || s.length() == 0 || s.equals("/** */")) {
            return "";
        }
        String t = Model.getFacade().getTagOfTag(tv);
        if (Argo.DOCUMENTATION_TAG.equals(t)) {
            return "";
        }
        return generateName(t) + "=" + s;
    }

    /**
     * Enhance/Create the doc comment for the given model element, including
     * tags for any OCL constraints connected to the model element. The tags
     * generated are suitable for use with the ocl injector which is part of the
     * Dresden OCL Toolkit and are in detail:
     * 
     * &nbsp;@invariant for each invariant specified &nbsp;@precondition for
     * each precondition specified &nbsp;@postcondition for each postcondition
     * specified &nbsp;@key-type specifying the class of the keys of a mapped
     * association &nbsp; Currently mapped associations are not supported yet...
     * &nbsp;@element-type specifying the class referenced in an association
     * 
     * @since 2001-09-26 ArgoUML 0.9.3
     * @author Steffen Zschaler
     * 
     * @param me the model element for which the documentation comment is needed
     * @param ae the association end which is represented by the model element
     * @return the documentation comment for the specified model element, either
     *         enhanced or completely generated
     */
    private String generateConstraintEnrichedDocComment(Object me, Object ae) {
        String s = generateConstraintEnrichedDocComment(me, true, INDENT);

        if (isCollection(ae)) {
            // Multiplicity greater 1, that means we will generate some sort of
            // collection, so we need to specify the element type tag
            StringBuffer sDocComment = new StringBuffer(80);

            // Prepare doc comment
            if (!(s == null || "".equals(s))) {
                // Just remove closing "*/"
                sDocComment.append(s.substring(0, s.indexOf("*/") + 1));
            } else {
                sDocComment.append(INDENT).append("/**").append(LINE_SEPARATOR);
                sDocComment.append(INDENT).append("  * ")
                        .append(LINE_SEPARATOR);
                sDocComment.append(INDENT).append("  *");
            }

            // Build doc comment
            // Object type = Model.getFacade().getType(ae);
            // if (type != null) {
            // sDocComment.append(" @element-type ");
            // sDocComment.append(Model.getFacade().getName(type));
            // }

            // REMOVED: 2002-03-11 STEFFEN ZSCHALER: element type
            // unknown is not recognized by the OCL injector...
            // else {
            // sDocComment += " @element-type unknown";
            // }
            sDocComment.append(LINE_SEPARATOR).append(INDENT).append(" */");
            sDocComment.append(LINE_SEPARATOR);
            return sDocComment.toString();
        }
        return (s != null) ? s : "";
    }

    /**
     * @param element ModelElement which has the Multiplicity
     * @return true if multiplicity is non-null and upper bound is greater than
     *         1
     */
    private boolean isCollection(Object element) {
        Object multiplicity = Model.getFacade().getMultiplicity(element);
        if (multiplicity != null) {
            int upper = Model.getFacade().getUpper(multiplicity);
            // -1 is UML's special 'unlimited integer'
            if (upper > 1 || upper == -1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enhance/Create the doc comment for the given model element, including
     * tags for any OCL constraints connected to the model element. The tags
     * generated are suitable for use with the ocl injector which is part of the
     * Dresden OCL Toolkit and are in detail:
     * 
     * &nbsp;@invariant for each invariant specified &nbsp;@precondition for
     * each precondition specified &nbsp;@postcondition for each postcondition
     * specified
     * 
     * @since 2001-09-26 ArgoUML 0.9.3
     * @author Steffen Zschaler
     * 
     * @param me the model element for which the documentation comment is needed
     * @param documented if existing tagged values should be generated in
     *            addition to javadoc
     * @param indent indent String (usually blanks) for indentation of generated
     *            comments
     * @return the documentation comment for the specified model element, either
     *         enhanced or completely generated
     */
    public static String generateConstraintEnrichedDocComment(Object me,
            boolean documented, String indent) {
        if (isFileGeneration) {
            documented = true; // always "documented" if we generate file
        }
        // Retrieve any existing doc comment
        String s = (VERBOSE_DOCS || DocumentationManager.hasDocs(me))
            ? DocumentationManager.getDocs(me, indent)
            : null;
        StringBuffer sDocComment = new StringBuffer(80);

        if (s != null && s.trim().length() > 0) {
            sDocComment.append(s).append(LINE_SEPARATOR);
        }
        LOG.fine("documented=" + documented);
        if (!documented) {
            return sDocComment.toString();
        }

        // Extract constraints
        Collection cConstraints = Model.getFacade().getConstraints(me);

        if (cConstraints.size() == 0) {
            return sDocComment.toString();
        }

        // Prepare doc comment
        if (s != null) {
            // Just remove closing */
            s = sDocComment.toString();
            sDocComment = new StringBuffer(s.substring(0, s.indexOf("*/") + 1));
        } else {
            sDocComment.append(INDENT).append("/**").append(LINE_SEPARATOR);
            sDocComment.append(INDENT).append(" * ").append(LINE_SEPARATOR);
            sDocComment.append(INDENT).append(" *");
        }

        // Add each constraint

        class TagExtractor extends DepthFirstAdapter {
            private LinkedList<String> llsTags = new LinkedList<String>();

            private String constraintName;

            private int constraintID;

            /**
             * Constructor.
             * 
             * @param sConstraintName The constraint name.
             */
            public TagExtractor(String sConstraintName) {
                super();

                constraintName = sConstraintName;
            }

            public Iterator getTags() {
                return llsTags.iterator();
            }

            /*
             * @see tudresden.ocl.parser.analysis.Analysis#caseAConstraintBody(tudresden.ocl.parser.node.AConstraintBody)
             */
            @Override
            public void caseAConstraintBody(AConstraintBody node) {
                // We don't care for anything below this node, so we
                // do not use apply anymore.
                String sKind = (node.getStereotype() != null) ? (node
                        .getStereotype().toString()) : (null);
                String sExpression = (node.getExpression() != null) ? (node
                        .getExpression().toString()) : (null);
                String sName = (node.getName() != null) ? (node.getName()
                        .getText()) : (constraintName + "_" + (constraintID++));

                if ((sKind == null) || (sExpression == null)) {
                    return;
                }

                String sTag;
                if (sKind.equals("inv ")) {
                    sTag = "@invariant ";
                } else if (sKind.equals("post ")) {
                    sTag = "@postcondition ";
                } else if (sKind.equals("pre ")) {
                    sTag = "@precondition ";
                } else {
                    return;
                }

                sTag += sName + ": " + sExpression;
                llsTags.addLast(sTag);
            }
        }

        tudresden.ocl.check.types.ModelFacade mf = new ArgoFacade(me);
        for (Object constraint : cConstraints) {
            try {
                String body = (String) Model.getFacade().getBody(
                        Model.getFacade().getBody(constraint));
                OclTree otParsed = OclTree.createTree(body, mf);

                TagExtractor te = new TagExtractor(Model.getFacade().getName(
                        constraint));
                otParsed.apply(te);

                for (Iterator j = te.getTags(); j.hasNext();) {
                    sDocComment.append(' ').append(j.next());
                    sDocComment.append(LINE_SEPARATOR);
                    sDocComment.append(INDENT).append(" *");
                }
            } catch (IOException ioe) {
                LOG.log(Level.SEVERE,
                        "Nothing to be done, should not happen",
                        ioe);
            }
        }

        sDocComment.append("/").append(LINE_SEPARATOR);

        return sDocComment.toString();
    }

    /**
     * Generates the code for a specific association
     * 
     * @param a The association
     * @param associationEnd an association end
     * @return a string containing the code for the "from" end of the
     *         association
     */
    private String generateAssociationFrom(Object a, Object associationEnd) {
        // TODO: does not handle n-ary associations
        StringBuffer sb = new StringBuffer(80);

        Collection connections = Model.getFacade().getConnections(a);
        for (Object associationEnd2 : connections) {
            if (associationEnd2 != associationEnd) {
                sb.append(INDENT);
                sb.append(generateConstraintEnrichedDocComment(a,
                        associationEnd2));
                sb.append(generateAssociationEnd(associationEnd2));
            }
        }

        return sb.toString();
    }

    /**
     * Generates the code for an association end
     * 
     * @param ae the association end
     * @return a String containing a reference to the association end
     */
    private String generateAssociationEnd(Object ae) {
        if (!Model.getFacade().isNavigable(ae)) {
            return "";
        }
        if (Model.getFacade().isAbstract(
                Model.getFacade().getAssociation(ae))) {
            return "";
        }
        // String s = INDENT + "protected ";
        // must be public or generate public navigation method!
        // String s = INDENT + "public ";
        StringBuffer sb = new StringBuffer(80);
        sb.append(INDENT).append(generateCoreAssociationEnd(ae));

        return (sb.append(";").append(LINE_SEPARATOR)).toString();
    }

    /**
     * Generates the code for an association end
     * @param ae  The association end.
     * @return  a string containing the code for an association end
     */
    String generateCoreAssociationEnd(Object ae) {
        StringBuffer sb = new StringBuffer(80);
        sb.append(generateVisibility(Model.getFacade().getVisibility(ae)));
        // sb.append(generateVisibility(ae));
        if (Model.getFacade().isStatic(ae)) {
            sb.append("static ");
        }
        if (Model.getFacade().isFrozen(ae)) {
            sb.append("final ");
        }
        // String n = ae.getName();
        // if (n != null && !String.UNSPEC.equals(n))
        // s += generateName(n) + " ";
        // if (ae.isNavigable()) s += "navigable ";
        // if (ae.getIsOrdered()) s += "ordered ";
        if (Model.getFacade().getUpper(ae) == 1) {
            sb.append(generateClassifierRef(Model.getFacade().getType(ae)));
        } else {
            sb.append("List"); // generateMultiplicity(m) + " ";

            // create generic type for list
            Object type = Model.getFacade().getType(ae);
            sb.append(String.format("<%s>", Model.getFacade().getName(type)));
        }

        sb.append(' ').append(generateAscEndName(ae));

        return sb.toString();
    }

    // //////////////////////////////////////////////////////////////
    // internal methods?

    private String generateGeneralization(Collection generalizations) {
        if (generalizations == null) {
            return "";
        }
        Collection classes = new ArrayList();
        for (Object generalization : generalizations) {
            Object generalizableElement = Model.getFacade().getGeneral(
                    generalization);
            // assert ge != null
            if (generalizableElement != null) {
                classes.add(generalizableElement);
            }
        }
        return generateClassList(classes);
    }

    /**
     * Generates the code for a specification
     * (usually the classifier references).
     *
     * @param cls  the class object
     * @return  
     */
    private String generateSpecification(Object cls) {
        Collection realizations = Model.getFacade().getSpecifications(cls);
        if (realizations == null) {
            return "";
        }
        LOG.fine("realizations: " + realizations.size());
        StringBuffer sb = new StringBuffer(80);
        Iterator clsEnum = realizations.iterator();
        while (clsEnum.hasNext()) {
            Object inter = clsEnum.next();
            sb.append(generateClassifierRef(inter));
            if (clsEnum.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * Generates the class list
     * @param classifiers a collection of classifiers
     * @return a String containing the list of classifiers.
     */
    private String generateClassList(Collection classifiers) {
        if (classifiers == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer(80);
        Iterator clsEnum = classifiers.iterator();
        while (clsEnum.hasNext()) {
            sb.append(generateClassifierRef(clsEnum.next()));
            if (clsEnum.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * Returns a visibility String either for a VisibilityKind (according to the
     * definition in NotationProvider2), but also for a model element, because
     * if it is a Feature, then the tag 'src_visibility' is to be taken into
     * account for generating language dependent visibilities.
     * @param an object containing visibility data
     */
    private String generateVisibility(Object o) {
        if (Model.getFacade().isAFeature(o)) {
            // TODO: The src_visibility tag doesn't appear to be created
            // anywhere by ArgoUML currently
            Object tv = Model.getFacade().getTaggedValue(o, "src_visibility");
            if (tv != null) {
                String tagged = (String) Model.getFacade().getValue(tv);
                if (tagged != null) {
                    if (tagged.trim().equals("")
                            || tagged.trim().toLowerCase().equals("package")
                            || tagged.trim().toLowerCase().equals("default")) {
                        return "";
                    }
                    return tagged + " ";
                }
            }
        }

        if (Model.getFacade().isAModelElement(o)) {
            if (Model.getFacade().isPublic(o)) {
                return "public ";
            }
            if (Model.getFacade().isPrivate(o)) {
                return "private ";
            }
            if (Model.getFacade().isProtected(o)) {
                return "protected ";
            }
            if (Model.getFacade().isPackage(o)) {
                return "";
            }
        }
        if (Model.getFacade().isAVisibilityKind(o)) {
            if (Model.getVisibilityKind().getPublic().equals(o)) {
                return "public ";
            }
            if (Model.getVisibilityKind().getPrivate().equals(o)) {
                return "private ";
            }
            if (Model.getVisibilityKind().getProtected().equals(o)) {
                return "protected ";
            }
            if (Model.getVisibilityKind().getPackage().equals(o)) {
                return "";
            }
        }
        return "";
    }

    private String generateScope(Object f) {
        if (Model.getFacade().isStatic(f)) {
            return "static ";
        }
        return "";
    }

    /**
     * Generate "abstract" keyword for an abstract operation.
     */
    private String generateAbstractness(Object op) {
        if (Model.getFacade().isAbstract(op)) {
            return "abstract ";
        }
        return "";
    }

    /**
     * Generate "final" keyword for final operations.
     * @param op the operation
     */
    private String generateChangeability(Object op) {
        if (Model.getFacade().isLeaf(op)) {
            return "final ";
        }
        return "";
    }

    /**
     * Generates the changeability for the object
     * @param sf indicates whether the object is read-only i.e. "static final"
     * @return a String containing the "final" keyword if necessary,
     *           an empty string otherwise
     */
    private String generateChangability(Object sf) {
        if (Model.getFacade().isReadOnly(sf)) {
            return "final ";
        }
        return "";
    }

    /**
     * Generates "synchronized" keyword for guarded operations.
     * 
     * @param op The operation
     * @return String The synchronized keyword if the operation is guarded, else
     *         "".
     */
    private String generateConcurrency(Object op) {
        if (Model.getFacade().getConcurrency(op) != null
                && Model.getConcurrencyKind().getGuarded()
                        .equals(Model.getFacade().getConcurrency(op))) {
            return "synchronized ";
        }
        return "";
    }

    /**
     * Generates a String representation of a Multiplicity.
     * 
     * @param m the Multiplicity.
     * 
     * @return a human readable String.
     */
    private String generateMultiplicity(Object m) {
        if (m == null || "1".equals(Model.getFacade().toString(m))) {
            return "";
        } else {
            return Model.getFacade().toString(m);
        }
    }

    private String generateState(Object m) {
        return Model.getFacade().getName(m);
    }


    private String generateTransition(Object m) {
        StringBuffer sb = new StringBuffer(generateName(Model.getFacade()
                .getName(m)));
        String t = generateEvent(Model.getFacade().getTrigger(m));
        String g = generateGuard(Model.getFacade().getGuard(m));
        String e = generateAction(Model.getFacade().getEffect(m));
        if (sb.length() > 0) {
            sb.append(": ");
        }
        sb.append(t);
        if (g.length() > 0) {
            sb.append(" [").append(g).append(']');
        }
        if (e.length() > 0) {
            sb.append(" / ").append(e);
        }
        return sb.toString();

    }

    private String generateAction(Object m) {
        // return m.getName();

        if (m != null) {
            Object script = Model.getFacade().getScript(m);
            if ((script != null)
                && (Model.getFacade().getBody(script) != null)) {

                return Model.getFacade().getBody(script).toString();

            }
        }
        return "";
    }

    private String generateGuard(Object m) {
        // return generateExpression(Model.getFacade().getExpression(m));
        if (m != null && Model.getFacade().getExpression(m) != null) {
            return generateExpression(Model.getFacade().getExpression(m));
        }
        return "";
    }

    private String generateMessage(Object m) {
        if (m == null) {
            return "";
        }
        return generateName(Model.getFacade().getName(m)) + "::"
                + generateAction(Model.getFacade().getAction(m));
    }

    /*
     * Generates the text for a (trigger) event.
     * 
     * @author MVW
     * 
     * @param m Object of any MEvent kind
     * 
     * @return The generated event (as a String).
     */
    private String generateEvent(Object m) {
        if (Model.getFacade().isAChangeEvent(m)) {
            return "when("
                    + generateExpression(Model.getFacade().getExpression(m))
                    + ")";
        }
        if (Model.getFacade().isATimeEvent(m)) {
            return "after("
                    + generateExpression(Model.getFacade().getExpression(m))
                    + ")";
        }
        if (Model.getFacade().isASignalEvent(m)) {
            return generateName(Model.getFacade().getName(m));
        }
        if (Model.getFacade().isACallEvent(m)) {
            return generateName(Model.getFacade().getName(m));
        }
        return "";
    }

    /**
     * This method is responsible for generating the association end name. If
     * the association is named, then that name is returned. If the association
     * end is named, then that name is returned; otherwise the classifier name
     * is returned.s
     * 
     * @param ae The association
     * @return a string containing the name of the association end.
     */
    String generateAscEndName(Object ae) {
        String n = Model.getFacade().getName(ae);
        Object asc = Model.getFacade().getAssociation(ae);
        String ascName = Model.getFacade().getName(asc);
        if (n != null && n.length() > 0) {
            n = generateName(n);
        } else if (ascName != null && ascName.length() > 0) {
            n = generateName(ascName);
        } else {
            n = generateClassifierRef(Model.getFacade().getType(ae));
        }

        n = String.valueOf(n.charAt(0)).toLowerCase() + n.substring(1);

        return n;
    }

    /**
     * Gets the Java package name for a given namespace, ignoring the root
     * namespace (which is the model).
     * 
     * @param namespace the namespace
     * @return the Java package name
     */
    public String getPackageName(Object namespace) {
        if (namespace == null || !Model.getFacade().isANamespace(namespace)
                || Model.getFacade().getNamespace(namespace) == null) {
            return "";
        }
        String packagePath = Model.getFacade().getName(namespace);
        if (packagePath == null) {
            return "";
        }
        while ((namespace = Model.getFacade().getNamespace(namespace))
               != null) {
            // omit root package name; it's the model's root
            if (Model.getFacade().getNamespace(namespace) != null) {
                packagePath = Model.getFacade().getName(namespace) + '.'
                        + packagePath;
            }
        }
        return packagePath;
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#getName()
     */
    public String getName() {
        return "GeneratorJava";
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#getInfo(int)
     */
    public String getInfo(int type) {
        switch (type) {
        case DESCRIPTION:
            return "Java notation and code generator";
        case AUTHOR:
            return JavaModuleGlobals.MODULE_AUTHOR;
        case VERSION:
            return JavaModuleGlobals.MODULE_VERSION;
        case DOWNLOADSITE:
            return JavaModuleGlobals.MODULE_DOWNLOADSITE;
        default:
            return null;
        }
    }

    /*
     * Initialize the Java code generator.
     * 
     * @see org.argouml.moduleloader.ModuleInterface#enable()
     */
    public boolean enable() {
        GeneratorManager.getInstance().addGenerator(java, this);
        return true;
    }

    /*
     * Disable the Java code generator.
     * 
     * @see org.argouml.moduleloader.ModuleInterface#disable()
     */
    public boolean disable() {
        GeneratorManager.getInstance().removeGenerator(java);
        return true;
    }

    /**
     * Returns the _lfBeforeCurly.
     * 
     * @return boolean
     */
    public boolean isLfBeforeCurly() {
        return lfBeforeCurly;
    }

    /**
     * Returns the _verboseDocs.
     * 
     * @return boolean
     */
    public boolean isVerboseDocs() {
        return verboseDocs;
    }

    /**
     * Sets the lfBeforeCurly.
     * 
     * @param beforeCurl The new value.
     */
    public void setLfBeforeCurly(boolean beforeCurl) {
        lfBeforeCurly = beforeCurl;
    }

    /**
     * Sets the verboseDocs.
     * 
     * @param verbose The new value.
     */
    public void setVerboseDocs(boolean verbose) {
        verboseDocs = verbose;
    }

    private String generateActionState(Object actionState) {
        String ret = "";
        Object action = Model.getFacade().getEntry(actionState);
        if (action != null) {
            Object expression = Model.getFacade().getScript(action);
            if (expression != null) {
                ret = generateExpression(expression);
            }
        }
        return ret;
    }

    private String generateExpression(Object expr) {
        if (Model.getFacade().isAExpression(expr)) {
            return generateUninterpreted((String) Model.getFacade().getBody(
                    expr));
        } else if (Model.getFacade().isAConstraint(expr)) {
            return generateExpression(Model.getFacade().getBody(expr));
        }
        return "";
    }

    private String generateName(String n) {
        return n;
    }

    /**
     * Make a string non-null.
     * <p>
     * 
     * What is the purpose of this function? Shouldn't it be private static?
     * 
     * @param un The String.
     * @return a non-null string.
     */
    private String generateUninterpreted(String un) {
        if (un == null) {
            return "";
        }
        return un;
    }

    private String generateClassifierRef(Object cls) {
        if (cls == null) {
            return "";
        }
        return Model.getFacade().getName(cls);
    }

    private String generateStereotype(Object st) {
        /*
         * TODO: This code is not used. Why is it here? It causes an unwanted
         * dependency to the org.argouml.kernel. (Project,...)
         */
        // if (st == null)
        // return "";
        // Project project =
        // ProjectManager.getManager().getCurrentProject();
        // ProjectSettings ps = project.getProjectSettings();
        // if (Model.getFacade().isAModelElement(st)) {
        // if (Model.getFacade().getName(st) == null)
        // return ""; // Patch by Jeremy Bennett
        // if (Model.getFacade().getName(st).length() == 0)
        // return "";
        // return ps.getLeftGuillemot()
        // + generateName(Model.getFacade().getName(st))
        // + ps.getRightGuillemot();
        // }
        // if (st instanceof Collection) {
        // Object o;
        // StringBuffer sb = new StringBuffer(10);
        // boolean first = true;
        // Iterator iter = ((Collection) st).iterator();
        // while (iter.hasNext()) {
        // if (!first)
        // sb.append(',');
        // o = iter.next();
        // if (o != null) {
        // sb.append(generateName(Model.getFacade().getName(o)));
        // first = false;
        // }
        // }
        // if (!first) {
        // return ps.getLeftGuillemot()
        // + sb.toString()
        // + ps.getRightGuillemot();
        // }
        // }
        return "";
    }

    /*
     * @see
     * org.argouml.uml.generator.CodeGenerator#generate(java.util.Collection,
     * boolean)
     */
    public Collection generate(Collection elements, boolean deps) {
        LOG.fine("generate() called");
        File tmpdir = null;
        try {
            tmpdir = TempFileUtils.createTempDir();
            if (tmpdir != null) {
                generateFiles(elements, tmpdir.getPath(), deps);
                return TempFileUtils.readAllFiles(tmpdir);
            }
            return Collections.EMPTY_LIST;
        } finally {
            if (tmpdir != null) {
                TempFileUtils.deleteDir(tmpdir);
            }
            LOG.fine("generate() terminated");
        }
    }

    /*
     * @see org.argouml.uml.generator.CodeGenerator#generateFiles(
     *         java.util.Collection, java.lang.String, boolean)
     */
    public Collection generateFiles(Collection elements, String path,
            boolean deps) {
        LOG.fine("generateFiles() called");
        // TODO: 'deps' is ignored here
        for (Object element : elements) {
            generateFile(element, path);
        }
        return TempFileUtils.readFileNames(new File(path));
    }

    /*
     * @see org.argouml.uml.generator.CodeGenerator#generateFileList(
     *         java.util.Collection, boolean)
     */
    public Collection generateFileList(Collection elements, boolean deps) {
        LOG.fine("generateFileList() called");
        // TODO: 'deps' is ignored here
        File tmpdir = null;
        try {
            tmpdir = TempFileUtils.createTempDir();
            for (Object element : elements) {
                generateFile(element, tmpdir.getName());
            }
            return TempFileUtils.readFileNames(tmpdir);
        } finally {
            if (tmpdir != null) {
                TempFileUtils.deleteDir(tmpdir);
            }
        }
    }
}
