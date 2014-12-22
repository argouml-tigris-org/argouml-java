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
 *    Alexander Lepekhine
 *    Laurent Braud
 *****************************************************************************
 *
 * Some portions of this file was previously release using the BSD License:
 */

// Copyright (c) 1996-2006 The Regents of the University of California. All
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
// Copyright (c) 2003-2008 The Regents of the University of California. All
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.argouml.application.api.Argo;
import org.argouml.kernel.ProjectManager;
import org.argouml.language.java.reveng.classfile.ParserUtils;
import org.argouml.model.CoreFactory;
import org.argouml.model.Facade;
import org.argouml.model.Model;
import org.argouml.ocl.OCLUtil;
import org.argouml.profile.Profile;
import org.argouml.uml.reveng.ImportCommon;
import org.argouml.uml.reveng.ImportInterface;


/**
 * Modeller maps Java source code(parsed/recognised by ANTLR) to UML model
 * elements, it applies some of the semantics in JSR-26. Note: JSR-26 was
 * withdrawn in March, 2004, so it obviously provides no guidance for more
 * recent language features such as Java 5.
 * 
 * TODO: This really needs a more sophisticated symbol table facility. It
 * currently uses the model repository as its symbol table which makes it easy
 * to merge into an existing model, but it also sometimes requires guessing
 * about what a symbol represents (e.g. interface, class, or package) so that
 * the name can instantiated in a concrete form. - tfm 20070911
 * 
 * @author Marcus Andersson, Thomas Neustupny
 */
public class Modeller {

    private static final Logger LOG =
        Logger.getLogger(Modeller.class.getName());

    private static final String JAVA_PACKAGE = "java.lang";

    private static final List<String> EMPTY_STRING_LIST =
        Collections.emptyList();

    /**
     * Current working model.
     */
    private Object model;

    /**
     * Java profile model.
     */
    private Profile javaProfile;

    /**
     * Current import settings.
     */
    private ImportCommon importSession;

    /**
     * The package which the currentClassifier belongs to.
     */
    private Object currentPackage;

    /**
     * Keeps the data that varies during parsing.
     */
    private ParseState parseState;

    /**
     * Stack up the state when descending inner classes.
     */
    private Stack<ParseState> parseStateStack;

    /**
     * Only attributes will be generated.
     */
    private boolean noAssociations;

    /**
     * Arrays will be modelled as unique datatypes.
     */
    private boolean arraysAsDatatype;

    /**
     * The name of the file being parsed.
     */
    private String fileName;

    /**
     * Arbitrary attributes.
     */
    private Hashtable<String, Object> attributes =
        new Hashtable<String, Object>();

    /**
     * List of the names of parsed method calls.
     */
    private List<String> methodCalls = new ArrayList<String>();

    /**
     * HashMap of parsed local variables. Indexed by variable name with string
     * representation of the type stored as the value.
     */
    private Hashtable<String, String> localVariables =
        new Hashtable<String, String>();

    /**
     * New model elements that were created during this reverse engineering
     * session. TODO: We want a stronger type here, but ArgoUML treats all
     * elements as just simple Objects.
     */
    private Collection<Object> newElements;

    /**
     * Flag to control generation of artificial names for associations. If true,
     * generate names of form "From->To". If false, set name to null.
     */
    private boolean generateNames = true;

    /**
     * Create a new modeller.
     * 
     * @param theModel The model to work with.
     * @param attributeSelected true if associations should be modeled as
     *            attributes
     * @param datatypeSelected true if arrays should be modeled as datatypes
     *            instead of instead of using UML multiplicities
     * @param theFileName the current file name
     * @deprecated for 0.27.2 by thn. Use the other constructor.
     */
    public Modeller(Object theModel, boolean attributeSelected,
            boolean datatypeSelected, String theFileName) {
        this(theModel, null, attributeSelected, datatypeSelected, theFileName);
    }

    /**
     * Create a new modeller.
     * 
     * @param theModel The model to work with.
     * @param theJavaProfile The Java profile.
     * @param attributeSelected true if associations should be modeled as
     *            attributes
     * @param datatypeSelected true if arrays should be modeled as datatypes
     *            instead of instead of using UML multiplicities
     * @param theFileName the current file name
     */
    public Modeller(Object theModel, Profile theJavaProfile,
            boolean attributeSelected, boolean datatypeSelected,
            String theFileName) {
        model = theModel;
        javaProfile = theJavaProfile;
        noAssociations = attributeSelected;
        arraysAsDatatype = datatypeSelected;
        currentPackage = this.model;
        newElements = new HashSet<Object>();
        parseState = new ParseState(this.model, getPackage(JAVA_PACKAGE, true));
        parseStateStack = new Stack<ParseState>();
        fileName = theFileName;
        if (javaProfile == null) {
            LOG.warning("No Java profile activated for Java source import. "
                        + "Why?");
        }
    }

    /**
     * @param key the key of the attribute to get
     * @return the value of the attribute
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * @param key the key of the attribute
     * @param value the value for the attribute
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * This is a mapping from a Java compilation Unit -> a UML artifact.
     * Classes are resident in a component in UML1, and realizing classifiers
     * in UML2. Imports are relationships between artifacts and other
     * classes / packages.
     * <p>
     * 
     * See JSR 26 (for UML1?).
     * <p>
     * 
     * Adding artifacts is a little messy since there are 2 cases:
     * 
     * <ol>
     * <li>source file has package statement, will be added several times since
     * lookup in addComponent() only looks in the model since the package
     * namespace is not yet known.
     * 
     * <li>source file has not package statement: artifact is added to the
     * model namespace. There is no package statement so the lookup will
     * always work.
     * 
     * </ol>
     * Therefore in the case of (1), we need to delete duplicate artifacts in
     * the addPackage() method.
     * <p>
     * 
     * In either case we need to add a package since we don't know in advance if
     * there will be a package statement.
     * <p>
     */
    public void addComponent() {

        // try and find the artifact in the current package
        // to cope with repeated imports
        // [this will never work if a package statement exists:
        // because the package statement is parsed after the component is
        // identified]
        Object artifact = Model.getFacade().lookupIn(currentPackage, fileName);

        if (artifact == null) {

            // remove the java specific ending (per JSR 26).
            // BUT we can't do this because then the component will be confused
            // with its class with the same name when invoking
            // Model.getFacade().lookupIn(Object,String)
            /*
             * if(fileName.endsWith(".java")) fileName = fileName.substring(0,
             * fileName.length()-5);
             */

            if (Model.getFacade().getUmlVersion().charAt(0) == '1') {
                artifact = Model.getCoreFactory().createComponent();
            } else {
                artifact = Model.getCoreFactory().createArtifact();
            }
            Model.getCoreHelper().setName(artifact, fileName);
            newElements.add(artifact);
        }

        parseState.setArtifact(artifact);

        // set the namespace of the component, in the event
        // that the source file does not have a package stmt
        Model.getCoreHelper().setNamespace(parseState.getArtifact(), model);
    }

    /**
     * Called from the parser when a package clause is found.
     * 
     * @param name The name of the package.
     */
    public void addPackage(String name) {
        // We used to add diagrams to the project here for each package
        // but diagram creation is handled in the common code for all
        // reverse engineering modules now

        // Find the top level package
        String ownerPackageName, currentName = name;
        ownerPackageName = getPackageName(currentName);
        while (!"".equals(ownerPackageName)) {
            currentName = ownerPackageName;
            ownerPackageName = getPackageName(currentName);
        }
        // here, getPackage must NOT use the Java profile, because
        // the declared package need to be in the user model
        Object mPackage = getPackage(currentName, false);
        // Save src_path in the upper package
        // TODO: Rework this so that we don't need importSession here.
        // perhaps move to the common import code. - tfm
        if (importSession != null
                && importSession.getSrcPath() != null
                && Model.getFacade().getTaggedValue(mPackage,
                        ImportInterface.SOURCE_PATH_TAG) == null) {
            String[] srcPaths = {
                importSession.getSrcPath()
            };
            buildTaggedValue(mPackage, ImportInterface.SOURCE_PATH_TAG,
                    srcPaths);
        }

        // Find or create a Package model element for this package
        // (in user model only, because a new package must be added).
        mPackage = getPackage(name, false);

        // Set the current package for the following source code.
        currentPackage = mPackage;
        parseState.addPackageContext(mPackage);

        // set the namespace of the artifact
        // check to see if there is already a artifact defined:
        Object artifact = Model.getFacade().lookupIn(currentPackage, fileName);

        if (artifact == null) {

            // set the namespace of the artifact
            Model.getCoreHelper().setNamespace(parseState.getArtifact(),
                    currentPackage);
        } else {

            // an artifact already exists,
            // so delete the latest one(the duplicate)
            Object oldArtifact = parseState.getArtifact();
            Model.getUmlFactory().delete(oldArtifact);
            newElements.remove(oldArtifact);
            // change the parse state to the existing one.
            parseState.setArtifact(artifact);
        }
    }

    /**
     * Called from the parser when an import clause is found.
     * 
     * @param name The name of the import. Can end with a '*'.
     */
    public void addImport(String name) {
        addImport(name, false);
    }

    /*
     * ClassSignature: TypeParametersopt Superopt Interfacesopt
     * 
     * TypeParameters ::= < TypeParameterList > TypeParameterList ::=
     * TypeParameterList , TypeParameter | TypeParameter
     * 
     * TypeParameter: TypeVariable TypeBoundopt TypeBound: extends
     * ClassOrInterfaceType AdditionalBoundListopt AdditionalBoundList:
     * AdditionalBound AdditionalBoundList AdditionalBound AdditionalBound: &
     * InterfaceType
     */
    public void addClassSignature(String signature) {
        addTypeParameters(parseState.getClassifier(),
                ParserUtils.extractTypeParameters(signature));
    }

    /**
     * Called from the parser when an import clause is found.
     * 
     * @param name The name of the import. Can end with a '*'.
     * @param forceIt Force addition by creating all that's missing.
     */
    void addImport(String name, boolean forceIt) {
        // only do imports on the 2nd pass.
        if (getLevel() == 0) {
            return;
        }

        String packageName = getPackageName(name);

        if (packageName == null || "".equals(packageName)) {
            // TODO: This won't happen and can be removed when there is a
            // real symbol table for name lookup instead of guessing based
            // on parsing "." strings
            LOG.log(Level.WARNING,
                    "Import skipped - unable to get package name for {0}",
                    name);
            return;
        }

        // TODO: In the case of an inner class, we probably want either the
        // qualified name with both outer and inner class names, or just the
        // outer class name
        String classifierName = getClassifierName(name);
        Object mPackage = getPackage(packageName, true);

        // import on demand
        if (classifierName.equals("*")) {
            parseState.addPackageContext(mPackage);
            Object srcFile = parseState.getArtifact();
            buildDependency(mPackage, srcFile, "javaImport");
        }
        // single type import
        else {
            Object mClassifier = null;
            try {
                mClassifier = (new PackageContext(null, mPackage)).get(
                        classifierName, false, javaProfile);
            } catch (ClassifierNotFoundException e) {
                if (forceIt && classifierName != null && mPackage != null) {
                    // call getPackage again WITHOUT Java profile, because
                    // class creation is only allowed in the user model
                    mPackage = (packageName.length() > 0) ? getPackage(
                            packageName, false) : model;
                    // a last chance: maybe it's in this user model package:
                    mClassifier = Model.getFacade().lookupIn(mPackage,
                            classifierName);
                    if (mClassifier == null) {
                        // we must guess if it's a class/interface, so: class
                        LOG.info("Modeller.java: "
                                + "forced creation of unknown classifier "
                                + classifierName);
                        // TODO: A better strategy would be to defer creating
                        // this until we can determine what it is
                        mClassifier = Model.getCoreFactory().buildClass(
                                classifierName, mPackage);
                        newElements.add(mClassifier);
                    }
                } else {
                    warnClassifierNotFound(classifierName,
                            "an imported classifier");
                }
            }
            if (mClassifier != null) {
                parseState.addClassifierContext(mClassifier);
                Object srcFile = parseState.getArtifact();
                buildDependency(mClassifier, srcFile, "javaImport");
            }
        }
    }

    /**
     * Called from the parser to add a dependency to a classifier.
     * 
     * @param name The name of the classifier candidate.
     */
    void addClassifierDependency(String name) {
        String classifierName = stripVarargAndGenerics(name);
        Object clientObj = parseState.getClassifier();
        if (clientObj == null) {
            return;
        }
        Object supplierObj = null;
        
        // first try: lookup classifierName in same namespace
        Object ns = Model.getFacade().getNamespace(clientObj);
        if (ns != null) {
            supplierObj = Model.getFacade().lookupIn(ns, classifierName);
        }
        
        // second try: resolve fully qualified classifier (xxx.yyy.Zzz)
        String packageName = getPackageName(name);
        if (supplierObj == null && packageName != null && packageName.length() > 0) {
            classifierName = this.getClassifierName(name);
            Object mPackage = getPackage(packageName, true);
            supplierObj = Model.getFacade().lookupIn(mPackage, classifierName);
        }

        // third try: lookup in imports
        Object srcFile = parseStateStack.lastElement().getArtifact();
        if (supplierObj == null && srcFile != null) {
            Collection dependencies = Model.getFacade().getClientDependencies(srcFile);
            for (Object dep : dependencies) {
                for (Object suppl : Model.getFacade().getSuppliers(dep)) {
                    if (Model.getFacade().isAPackage(suppl)) {
                        supplierObj = Model.getFacade().lookupIn(suppl, classifierName);
                    } else if (classifierName.equals(Model.getFacade().getName(suppl))) {
                        supplierObj = suppl;
                    }
                }
                if (supplierObj != null) {
                    break;
                }
            }
        }
        
        // finally build the dependency
        if (supplierObj != null) {
            buildDependency(supplierObj, clientObj, null);
        }
    }

    /*
     * Build a dependency, e.g. a Java import equivalent in UML. First search
     * for an existing dependency. Create a new one if not found.
     */
    private Object buildDependency(Object supplier, Object client, String stereoname) {
        // TODO: add <<javaImport>> stereotype to Java profile - thn
        Collection dependencies = Model.getCoreHelper().getDependencies(
                supplier, client);
        for (Object dep : dependencies) {
            for (Object stereotype : Model.getFacade().getStereotypes(dep)) {
                if (stereoname == null || stereoname.equals(
                        Model.getFacade().getName(stereotype))) {
                    return dep;
                }
            }
        }

        // Didn't find it. Let's create one.
        Object dependency = Model.getCoreFactory().buildDependency(client,
                supplier);
        if (stereoname != null && Model.getFacade().getUmlVersion().charAt(0) == '1') {
            // TODO: support for stereotypes in eUML
            Model.getCoreHelper().addStereotype(dependency,
                    getUML1Stereotype(stereoname));
            ProjectManager.getManager().updateRoots();
        }
        String newName = makeDependencyName(client, supplier);
        Model.getCoreHelper().setName(dependency, newName);
        newElements.add(dependency);
        return dependency;
    }

    private String makeAbstractionName(Object child, Object parent) {
        return makeFromToName(child, parent);
    }

    private String makeAssociationName(Object from, Object to) {
        return makeFromToName(from, to);
    }

    private String makeDependencyName(Object from, Object to) {
        return makeFromToName(from, to);
    }

    private String makeFromToName(Object from, Object to) {
        if (!generateNames) {
            return null;
        } else {
            return makeFromToName(Model.getFacade().getName(from), Model
                    .getFacade().getName(to));
        }
    }

    private String makeFromToName(String from, String to) {
        if (!generateNames) {
            return null;
        } else {
            // TODO: This isn't localized, but I'm not sure it can be
            // without other side effects - tfm - 20070410
            return from + " -> " + to;
        }
    }

    /**
     * Called from the parser when a class declaration is found.
     * 
     * @param name The name of the class.
     * @param modifiers A sequence of class modifiers.
     * @param superclassName Zero or one string with the name of the superclass.
     *            Can be fully qualified or just a simple class name.
     * @param interfaces Zero or more strings with the names of implemented
     *            interfaces. Can be fully qualified or just a simple interface
     *            name.
     * @param javadoc The javadoc comment. null or "" if no comment available.
     */
    public void addClass(String name, short modifiers, String superclassName,
            List<String> interfaces, String javadoc) {
        addClass(name, modifiers, EMPTY_STRING_LIST, superclassName,
                interfaces, javadoc, false);
    }

    /**
     * Called from the parser when a class declaration is found.
     * 
     * @param name The name of the class.
     * @param modifiers A bitmask of class modifiers.
     * @param typeParameters List of strings containing names of types for
     *            parameters
     * @param superclassName Zero or one string with the name of the superclass.
     *            Can be fully qualified or just a simple class name.
     * @param interfaces Zero or more strings with the names of implemented
     *            interfaces. Can be fully qualified or just a simple interface
     *            name.
     * @param javadoc The javadoc comment. null or "" if no comment available.
     * @param forceIt Force addition by creating all that's missing.
     */
    void addClass(String name, short modifiers, List<String> typeParameters,
            String superclassName, List<String> interfaces, String javadoc,
            boolean forceIt) {
        if (typeParameters != null && typeParameters.size() > 0) {
            logError("type parameters not supported on Class", name);
            for (String s : typeParameters) {
                logError("type parameter ", s);
            }
        }
        Object mClass = addClassifier(Model.getCoreFactory().createClass(),
                name, modifiers, javadoc, typeParameters);

        Model.getCoreHelper().setAbstract(mClass,
                (modifiers & JavaParser.ACC_ABSTRACT) > 0);
        Model.getCoreHelper().setLeaf(mClass,
                (modifiers & JavaParser.ACC_FINAL) > 0);
        if (Model.getFacade().getUmlVersion().charAt(0) == '1') {
            Model.getCoreHelper().setRoot(mClass, false);
        }
        newElements.add(mClass);

        // only do generalizations and realizations on the 2nd pass.
        if (getLevel() == 0) {
            return;
        }

        if (superclassName != null) {
            Object parentClass = null;
            try {
                parentClass = getContext(superclassName).get(
                        getClassifierName(superclassName), false, javaProfile);
                getGeneralization(currentPackage, parentClass, mClass);
            } catch (ClassifierNotFoundException e) {
                if (forceIt && superclassName != null && model != null) {
                    LOG.info("Modeller.java: forced creation of unknown class "
                            + superclassName);
                    String packageName = getPackageName(superclassName);
                    String classifierName = getClassifierName(superclassName);
                    // here, getPackage must NOT use the Java profile, because
                    // class creation is only allowed in the user model
                    Object mPackage = (packageName.length() > 0) ? getPackage(
                            packageName, false) : model;
                    // a last chance: maybe it's in this user model package:
                    parentClass = Model.getFacade().lookupIn(mPackage,
                            classifierName);
                    if (parentClass == null) {
                        parentClass = Model.getCoreFactory().buildClass(
                                classifierName, mPackage);
                        newElements.add(parentClass);
                    }
                    getGeneralization(currentPackage, parentClass, mClass);
                } else {
                    warnClassifierNotFound(superclassName, "a generalization");
                }
            }
        }

        if (interfaces != null) {
            addInterfaces(mClass, interfaces, forceIt);
        }
    }

    /**
     * Called from the parser when an anonymous inner class is found.
     * 
     * @param type The type of this anonymous class.
     */
    public void addAnonymousClass(String type) {
        addAnonymousClass(type, false);
    }

    /**
     * Called from the parser when an anonymous inner class is found.
     * 
     * @param type The type of this anonymous class.
     * @param forceIt Force addition by creating all that's missing.
     */
    void addAnonymousClass(String type, boolean forceIt) {
        String name = parseState.anonymousClass();
        try {
            Object mClassifier = getContext(type).get(getClassifierName(type),
                    false, javaProfile);
            List<String> interfaces = new ArrayList<String>();
            if (Model.getFacade().isAInterface(mClassifier)) {
                interfaces.add(type);
            }

            addClass(name, (short) 0, EMPTY_STRING_LIST, Model.getFacade()
                    .isAClass(mClassifier) ? type : null, interfaces, "",
                    forceIt);
        } catch (ClassifierNotFoundException e) {
            // Must add it anyway, or the class popping will mismatch.
            addClass(name, (short) 0, EMPTY_STRING_LIST, null,
                    EMPTY_STRING_LIST, "", forceIt);
            LOG.info("Modeller.java: an anonymous class was created "
                    + "although it could not be found in the classpath.");
        }
    }

    /**
     * Add an Interface to the model.
     * 
     * TODO: This method preserves the historical public API which is used by
     * other reverse engineering modules such as the Classfile module. This
     * really needs to be decoupled.
     * 
     * @param name The name of the interface.
     * @param modifiers A sequence of interface modifiers.
     * @param interfaces Zero or more strings with the names of extended
     *            interfaces. Can be fully qualified or just a simple interface
     *            name.
     * @param javadoc The javadoc comment. "" if no comment available.
     */
    public void addInterface(String name, short modifiers,
            List<String> interfaces, String javadoc) {
        addInterface(name, modifiers, EMPTY_STRING_LIST, interfaces, javadoc,
                false);
    }

    /**
     * Called from the parser when an interface declaration is found.
     * 
     * @param name The name of the interface.
     * @param modifiers A sequence of interface modifiers.
     * @param interfaces Zero or more strings with the names of extended
     *            interfaces. Can be fully qualified or just a simple interface
     *            name.
     * @param javadoc The javadoc comment. "" if no comment available.
     * @param forceIt Force addition by creating all that's missing.
     */
    void addInterface(String name, short modifiers,
            List<String> typeParameters, List<String> interfaces,
            String javadoc, boolean forceIt) {
        if (typeParameters != null && typeParameters.size() > 0) {
            logError("type parameters not supported on Interface", name);
        }
        Object mInterface = addClassifier(Model.getCoreFactory()
                .createInterface(), name, modifiers, javadoc, typeParameters);

        // only do generalizations and realizations on the 2nd pass.
        if (getLevel() == 0) {
            return;
        }

        for (String interfaceName : interfaces) {
            Object parentInterface = null;
            try {
                parentInterface = getContext(interfaceName).get(
                        getClassifierName(interfaceName), true, javaProfile);
                getGeneralization(currentPackage, parentInterface, mInterface);
            } catch (ClassifierNotFoundException e) {
                if (forceIt && interfaceName != null && model != null) {
                    LOG.info("Modeller.java: "
                            + "forced creation of unknown interface "
                            + interfaceName);
                    String packageName = getPackageName(interfaceName);
                    String classifierName = getClassifierName(interfaceName);
                    // here, getPackage must NOT use the Java profile, because
                    // interface creation is only allowed in the user model
                    Object mPackage = (packageName.length() > 0) ? getPackage(
                            packageName, false) : model;
                    // a last chance: maybe it's in this user model package:
                    parentInterface = Model.getFacade().lookupIn(mPackage,
                            classifierName);
                    if (parentInterface == null) {
                        parentInterface = Model.getCoreFactory()
                                .buildInterface(classifierName, mPackage);
                        newElements.add(parentInterface);
                    }
                    getGeneralization(currentPackage, parentInterface,
                            mInterface);
                } else {
                    warnClassifierNotFound(interfaceName, "a generalization");
                }
            }
        }
    }

    /**
     * Called from the parser when an enumeration declaration is found.
     * 
     * @param name The name of the class.
     * @param modifiers A sequence of class modifiers.
     * @param interfaces Zero or more strings with the names of implemented
     *            interfaces. Can be fully qualified or just a simple interface
     *            name.
     * @param javadoc The javadoc comment. null or "" if no comment available.
     * @param forceIt Force addition by creating all that's missing.
     */
    void addEnumeration(String name, short modifiers, List<String> interfaces,
            String javadoc, boolean forceIt) {
        Object mEnum = null;
        if (Model.getFacade().getUmlVersion().charAt(0) == '1') {
            mEnum = addClassifier(Model.getCoreFactory().createClass(),
                name, modifiers, javadoc, EMPTY_STRING_LIST); // no type params
                                                              // for now
            Model.getCoreHelper().addStereotype(mEnum,
                    getUML1Stereotype("enumeration"));
            ProjectManager.getManager().updateRoots();
        } else {
            // TODO: always use UML Enumerations, like this:
            mEnum = Model.getCoreFactory().createEnumeration();
            Object mNamespace;
            if (parseState.getClassifier() != null) {
                // the new classifier is a java inner enumeration
                mNamespace = parseState.getClassifier();
            } else {
                // the new classifier is a top level java enumeration
                parseState.outerClassifier();
                mNamespace = currentPackage;
            }

            LOG.log(Level.INFO, "Created new enumeration for {0}", name);

            Model.getCoreHelper().setName(mEnum, name);
            Model.getCoreHelper().setNamespace(mEnum, mNamespace);
            newElements.add(mEnum);

            parseState.innerClassifier(mEnum);
            // change the parse state to a classifier parse state
            parseStateStack.push(parseState);
            parseState = new ParseState(parseState, mEnum, currentPackage);

            setVisibility(mEnum, modifiers);

            // Add classifier documentation tags during 
            // first (or only) pass only
            if (getLevel() <= 0) {
                addDocumentationTag(mEnum, javadoc);
            }
        }

        if ((modifiers & JavaParser.ACC_ABSTRACT) > 0) {
            // abstract enums are illegal in Java
            logError("Illegal \"abstract\" modifier on enum ", name);
        } else {
            Model.getCoreHelper().setAbstract(mEnum, false);
        }
        if ((modifiers & JavaParser.ACC_FINAL) > 0) {
            // it's an error to explicitly use the 'final' keyword for an enum
            // declaration
            logError("Illegal \"final\" modifier on enum ", name);
        } else {
            // enums are implicitly final unless they contain a class body
            // (which we won't know until we process the constants
            Model.getCoreHelper().setLeaf(mEnum, true);
        }
        if (Model.getFacade().getUmlVersion().charAt(0) == '1') {
            Model.getCoreHelper().setRoot(mEnum, false);
        }

        // only do realizations on the 2nd pass.
        if (getLevel() == 0) {
            return;
        }

        if (interfaces != null) {
            addInterfaces(mEnum, interfaces, forceIt);
        }
    }

    /**
     * @param mClass
     * @param interfaces
     * @param forceIt
     */
    private void addInterfaces(Object mClass, List<String> interfaces,
            boolean forceIt) {
        for (String interfaceName : interfaces) {
            Object mInterface = null;
            try {
                mInterface = getContext(interfaceName).get(
                        getClassifierName(interfaceName), true, javaProfile);
            } catch (ClassifierNotFoundException e) {
                if (forceIt && interfaceName != null && model != null) {
                    LOG.info("Modeller.java: "
                            + "forced creation of unknown interface "
                            + interfaceName);
                    String packageName = getPackageName(interfaceName);
                    String classifierName = getClassifierName(interfaceName);
                    // here, getPackage must NOT use the Java profile, because
                    // interface creation is only allowed in the user model
                    Object mPackage = (packageName.length() > 0) ? getPackage(
                            packageName, false) : model;
                    // a last chance: maybe it's in this user model package:
                    mInterface = Model.getFacade().lookupIn(mPackage,
                            classifierName);
                    if (mInterface == null) {
                        mInterface = Model.getCoreFactory().buildInterface(
                                classifierName, mPackage);
                        newElements.add(mInterface);
                    }
                } else {
                    warnClassifierNotFound(interfaceName, "an abstraction");
                }
            }
            // TODO: This should use the Model API's buildAbstraction - tfm
            if (mInterface != null && mInterface != mClass) {
                Object mAbstraction = getAbstraction(mInterface, mClass);
                if (Model.getFacade().getSuppliers(mAbstraction).size() == 0) {
                    Model.getCoreHelper().addSupplier(mAbstraction, mInterface);
                    Model.getCoreHelper().addClient(mAbstraction, mClass);
                }
                if (Model.getFacade().getUmlVersion().charAt(0) == '1') {
                    Model.getCoreHelper()
                        .setNamespace(mAbstraction, currentPackage);
                    Model.getCoreHelper().addStereotype(mAbstraction,
                        getUML1Stereotype(CoreFactory.REALIZE_STEREOTYPE));
                    ProjectManager.getManager().updateRoots();
                }
                newElements.add(mAbstraction);
            }
        }
    }

    /**
     * Called from the parser when an enumeration literal is found.
     * 
     * @param name The name of the enumerationLiteral.
     */
    void addEnumerationLiteral(String name) {
        Object enumeration = parseState.getClassifier();
        if (!isAEnumeration(enumeration)) {
            throw new ParseStateException("not an Enumeration");
        }

        short mod = JavaParser.ACC_PUBLIC | JavaParser.ACC_FINAL
                | JavaParser.ACC_STATIC;

        if (Model.getFacade().getUmlVersion().charAt(0) == '1') {
            // TODO: make this obsolete (always use real enumerations)
            addAttribute(mod, null, name, null, null, true);
        } else {
            Model.getCoreFactory().buildEnumerationLiteral(name, enumeration);
        }

        // add an <<enum>> stereotype to distinguish it from fields
        // in the class body?
    }

    /*
     * Recognizer for enumeration. In UML1 an enumeration is a Class with
     * the <<enumeration>> stereotype applied.
     */
    private boolean isAEnumeration(Object element) {
        if (Model.getFacade().isAEnumeration(element)) {
            return true;
        }
        // TODO: make the following obsolete
        if (!Model.getFacade().isAClass(element)) {
            return false;
        }
        return Model.getExtensionMechanismsHelper().hasStereotype(element,
                "enumeration");
    }

    /**
     * Add an annotation declaration
     * 
     * @param name identifier for annotation definition.
     * @param modifiers A sequence of modifiers.
     * @param javadoc The javadoc comment. null or "" if no comment available.
     * @param forceIt Force addition by creating all that's missing.
     */
    void addAnnotationDefinition(String name, short modifiers, String javadoc,
            boolean forceIt) {
        // TODO: Not implemented
        logError("Java 5 annotation definitions not supported", "@" + name);
    }

    /**
     * Called from the parser when an annotation declaration is found.
     * 
     * @param name identifier for annotation.
     */
    void addAnnotation(String name) {
        // TODO: Not implemented
        logError("Java 5 annotations not supported", "@" + name);
    }

    /**
     * Done adding an annotation.
     */
    void endAnnotation() {
        // TODO: Placeholder. Can we use popClassifier here?
    }

    void addTypeParameters(Object modelElement, List<String> typeParameters) {
        if (modelElement == null || typeParameters == null) {
            return;
        }
        if (Model.getFacade().getTemplateParameters(modelElement).size() == 0) {
            for (String parameter : typeParameters) {
                // parse parameter to name and bounds
                Pattern p =
                    Pattern.compile("([^ ]*)( super | extends )?((.*))");
                Matcher m = p.matcher(parameter);
                if (m.matches()) {
                    String templateParameterName = m.group(1);
                    Object param = Model.getCoreFactory().createParameter();
                    Model.getCoreHelper().setName(param, templateParameterName);
                    Object templateParameter =
                        Model.getCoreFactory()
                            .buildTemplateParameter(modelElement, param, null);
                    if (m.group(2) != null) {
                        // bounds are saved as tagged value in param
                        buildTaggedValue(param, 
                                m.group(2).trim(), 
                                new String[]{m.group(3)});
                    }
                    Model.getCoreHelper()
                        .addTemplateParameter(modelElement, templateParameter);
                } 
            }
        }
    }

    /**
     * Common code used by addClass and addInterface.
     * 
     * @param newClassifier Supply one if none is found in the model.
     * @param name Name of the classifier.
     * @param modifiers String of modifiers.
     * @param javadoc The javadoc comment. null or "" if no comment available.
     * @param typeParameters List of types for parameters (not implemented)
     * @return The newly created/found classifier.
     */
    private Object addClassifier(Object newClassifier, String name,
            short modifiers, String javadoc, List<String> typeParameters) {
        Object mClassifier;
        Object mNamespace;

        if (parseState.getClassifier() != null) {
            // the new classifier is a java inner class
            mClassifier = Model.getFacade().lookupIn(
                    parseState.getClassifier(), name);
            mNamespace = parseState.getClassifier();
        } else {
            // the new classifier is a top level java class
            parseState.outerClassifier();
            mClassifier = Model.getFacade().lookupIn(currentPackage, name);
            mNamespace = currentPackage;
        }

        if (mClassifier == null) {
            // if the classifier could not be found in the model
            LOG.log(Level.INFO, "Created new classifier for {0}", name);

            mClassifier = newClassifier;
            Model.getCoreHelper().setName(mClassifier, name);
            Model.getCoreHelper().setNamespace(mClassifier, mNamespace);
            newElements.add(mClassifier);
        } else {
            // it was found and we delete any existing tagged values.
            LOG.log(Level.INFO, "Found existing classifier for {0}", name);

            // TODO: Rewrite existing elements instead? - tfm
            cleanModelElement(mClassifier);
        }

        parseState.innerClassifier(mClassifier);

        // set up the artifact manifestation (only for top level classes)
        if (parseState.getClassifier() == null) {
            if (Model.getFacade().getUmlVersion().charAt(0) == '1') {
                // set the classifier to be a resident in its component:
                // (before we push a new parse state on the stack)
    
                // This test is carried over from a previous implementation,
                // but I'm not sure why it would already be set - tfm
                if (Model.getFacade()
                        .getElementResidences(mClassifier).isEmpty()) {
                    Object resident = Model.getCoreFactory()
                            .createElementResidence();
                    Model.getCoreHelper().setResident(resident, mClassifier);
                    Model.getCoreHelper().setContainer(resident,
                            parseState.getArtifact());
                }
            } else {
                Object artifact = parseState.getArtifact();
                Collection c =
                    Model.getCoreHelper().getUtilizedElements(artifact);
                if (!c.contains(mClassifier)) {
                    Object manifestation = Model.getCoreFactory()
                            .buildManifestation(mClassifier);
                    Model.getCoreHelper()
                            .addManifestation(artifact, manifestation);
                }
            }
        }

        // change the parse state to a classifier parse state
        parseStateStack.push(parseState);
        parseState = new ParseState(parseState, mClassifier, currentPackage);

        setVisibility(mClassifier, modifiers);

        // Add classifier documentation tags during first (or only) pass only
        if (getLevel() <= 0) {
            addDocumentationTag(mClassifier, javadoc);
        }
        addTypeParameters(mClassifier, typeParameters);
        return mClassifier;
    }

    /**
     * Return the current import pass/level.
     * 
     * @return 0, 1, or 2 depending on current import level and pass of
     *         processing. Returns -1 if level isn't defined.
     */
    private int getLevel() {
        Object level = this.getAttribute("level");
        if (level != null) {
            return ((Integer) level).intValue();
        }
        return -1;
    }

    /**
     * Called from the parser when a classifier is completely parsed.
     */
    public void popClassifier() {

        // Remove operations and attributes not in source
        parseState.removeObsoleteFeatures();

        // Remove inner classes not in source
        parseState.removeObsoleteInnerClasses();

        parseState = parseStateStack.pop();
    }

    /**
     * Add an Operation to the current model
     * 
     * @param modifiers A sequence of operation modifiers.
     * @param returnType The return type of the operation.
     * @param name The name of the operation as a string
     * @param parameters A List of parameter declarations containing types and
     *            names.
     * @param javadoc The javadoc comment. null or "" if no comment available.
     * @return The operation.
     */
    public Object addOperation(short modifiers, String returnType, String name,
            List<ParameterDeclaration> parameters, String javadoc) {
        return addOperation(modifiers, EMPTY_STRING_LIST, returnType, name,
                parameters, javadoc, false);
    }

    /**
     * Called from the parser when an operation is found.
     * 
     * @param modifiers A sequence of operation modifiers.
     * @param returnType The return type of the operation.
     * @param name The name of the operation as a string
     * @param parameters A number of lists, each representing a parameter.
     * @param javadoc The javadoc comment. null or "" if no comment available.
     * @param forceIt Force addition by creating all that's missing.
     * @return The operation.
     */
    Object addOperation(short modifiers, List<String> typeParameters,
            String returnType, String name,
            List<ParameterDeclaration> parameters, String javadoc,
            boolean forceIt) {
        if (typeParameters != null && typeParameters.size() > 0) {
            logError("type parameters not supported on operation return type",
                    name);
        }
        Object mOperation = getOperation(name);
        parseState.feature(mOperation);

        Model.getCoreHelper().setAbstract(mOperation,
                (modifiers & JavaParser.ACC_ABSTRACT) > 0);
        Model.getCoreHelper().setLeaf(mOperation,
                (modifiers & JavaParser.ACC_FINAL) > 0);
        if (Model.getFacade().getUmlVersion().charAt(0) == '1') {
            Model.getCoreHelper().setRoot(mOperation, false);
        }
        setOwnerScope(mOperation, modifiers);
        setVisibility(mOperation, modifiers);
        if ((modifiers & JavaParser.ACC_SYNCHRONIZED) > 0) {
            Model.getCoreHelper().setConcurrency(mOperation,
                    Model.getConcurrencyKind().getGuarded());
        } else if (Model.getFacade().getConcurrency(mOperation) == Model
                .getConcurrencyKind().getGuarded()) {
            Model.getCoreHelper().setConcurrency(mOperation,
                    Model.getConcurrencyKind().getSequential());
        }

        Object[] c = Model.getFacade().getParameters(mOperation).toArray();
        for (Object parameter : c) {
            Model.getCoreHelper().removeParameter(mOperation, parameter);
        }

        Object mParameter;
        Object mClassifier = null;

        if (returnType == null
                || ("void".equals(returnType) && name.equals(Model.getFacade()
                        .getName(parseState.getClassifier())))) {
            // Constructor
            Model.getCoreHelper().addStereotype(mOperation,
                    getStereotype(mOperation, "create", "BehavioralFeature"));
            ProjectManager.getManager().updateRoots();
        } else {
            try {
                mClassifier =
                    // FIXME: This can't throw away the fully qualified
                    // name before starting the search!
                    getContext(returnType).get(getClassifierName(returnType),
                        false, javaProfile);
            } catch (ClassifierNotFoundException e) {
                if (forceIt && returnType != null && model != null) {
                    LOG.info("Modeller.java: "
                            + "forced creation of unknown classifier "
                            + returnType);
                    String packageName = getPackageName(returnType);
                    String classifierName = getClassifierName(returnType);
                    // here, getPackage must NOT use the Java profile, because
                    // class creation is only allowed in the user model
                    Object mPackage = (packageName.length() > 0) ? getPackage(
                            packageName, false) : model;
                    // a last chance: maybe it's in this user model package:
                    mClassifier = Model.getFacade().lookupIn(mPackage,
                            classifierName);
                    if (mClassifier == null) {
                        mClassifier = Model.getCoreFactory().buildClass(
                                classifierName, mPackage);
                        newElements.add(mClassifier);
                    }
                } else {
                    warnClassifierNotFound(returnType, "operation return type");
                }
            }
            if (mClassifier != null) {
                mParameter = buildReturnParameter(mOperation, mClassifier);
            }
        }

        for (ParameterDeclaration parameter : parameters) {
            String typeName = parameter.getType();
            // TODO: A type name with a trailing "..." represents
            // a variable length parameter list. It can only be
            // the last parameter and it gets converted to an array
            // on method invocation, so perhaps we should model it that
            // way (ie convert "Foo..." to "Foo[]"). - tfm - 20070329
            if (typeName.endsWith("...")) {
                logError("Unsupported variable length parameter list notation",
                        parameter.getName());
            }
            mClassifier = null;
            try {
                mClassifier = getContext(typeName).get(
                        getClassifierName(typeName), false, javaProfile);
            } catch (ClassifierNotFoundException e) {
                if (forceIt && model != null) {
                    LOG.info("Modeller.java: "
                            + "forced creation of unknown classifier "
                            + typeName);
                    String packageName = getPackageName(typeName);
                    String classifierName = getClassifierName(typeName);
                    // here, getPackage must NOT use the Java profile, because
                    // class creation is only allowed in the user model
                    Object mPackage = (packageName.length() > 0) ? getPackage(
                            packageName, false) : model;
                    // a last chance: maybe it's in this user model package:
                    mClassifier = Model.getFacade().lookupIn(mPackage,
                            classifierName);
                    if (mClassifier == null) {
                        mClassifier = Model.getCoreFactory().buildClass(
                                classifierName, mPackage);
                        newElements.add(mClassifier);
                    }
                } else {
                    warnClassifierNotFound(typeName, "operation params");
                }
            }
            if (mClassifier != null) {
                mParameter = buildInParameter(mOperation, mClassifier,
                        parameter.getName());
                if (!Model.getFacade().isAClassifier(mClassifier)) {
                    // the type resolution failed to find a valid classifier.
                    logError("Modeller.java: a valid type for a parameter "
                            + "could not be resolved:\n " + "In file: "
                            + fileName + ", for operation: "
                            + Model.getFacade().getName(mOperation)
                            + ", for parameter: ", Model.getFacade().getName(
                            mParameter));
                }
            }
        }

        addDocumentationTag(mOperation, javadoc);

        return mOperation;
    }

    private Object buildInParameter(Object operation, Object classifier,
            String name) {
        Object parameter = buildParameter(operation, classifier, name);
        Model.getCoreHelper().setKind(parameter,
                Model.getDirectionKind().getInParameter());
        return parameter;
    }

    private Object buildReturnParameter(Object operation, Object classifier) {
        Object parameter = buildParameter(operation, classifier, "return");
        Model.getCoreHelper().setKind(parameter,
                Model.getDirectionKind().getReturnParameter());
        return parameter;
    }

    private Object buildParameter(Object operation, Object classifier,
            String name) {
        Object parameter = Model.getCoreFactory().buildParameter(operation,
                classifier);
        Model.getCoreHelper().setName(parameter, name);
        return parameter;
    }

    /**
     * Warn user that information available in input source will not be
     * reflected accurately in the model.
     * 
     * @param name name of the classifier which wasn't found
     * @param operation - a string indicating what type of operation was being
     *            attempted
     */
    private void warnClassifierNotFound(String name, String operation) {
        logError("Modeller.java: a classifier (" + name
                + ") that was in the source "
                + "file could not be generated in the model ", operation);
    }

    /**
     * Add an error message to the log to be shown to the user.
     * <p>
     * TODO: This currently just writes to the error log. It needs to return
     * errors some place that the user can see them and deal with them. We also
     * need a way to get the line and column numbers to help the user track the
     * problem down.
     */
    private void logError(String message, String identifier) {
        LOG.warning(message + " : " + identifier);
    }

    /**
     * Called from the parser when a class field is parsed. This can occur in a
     * class block where it indicates a method body to to be added to an
     * operation (An operation will have exactly one Java body) OR it can occur
     * in the enum declaration (not currently supported).
     * 
     * TODO: Support use in an enum declaration
     * 
     * @param op An operation.
     * @param body A method body.
     */
    public void addBodyToOperation(Object op, String body) {
        if (op == null || !Model.getFacade().isAOperation(op)) {
            // This can occur if there's a class field in an enum definition
            throw new ParseStateException(
                    "Found class body in context other than a class");
        }
        if (body == null || body.length() == 0) {
            return;
        }

        Object method = getMethod(Model.getFacade().getName(op));
        parseState.feature(method);
        if (Model.getFacade().getUmlVersion().charAt(0) == '1') {
            Model.getCoreHelper().setBody(
                method,
                Model.getDataTypesFactory().createProcedureExpression("Java",
                    body));
        } else {
            Model.getDataTypesHelper().setBody(method, body);
            Model.getDataTypesHelper().setLanguage(method, "Java");
        }
        // Add the method to it's specification.
        Model.getCoreHelper().addMethod(op, method);

        // Add this method as an element to the classifier that owns
        // the operation.
        if (Model.getFacade().getUmlVersion().charAt(0) == '1') {
            Model.getCoreHelper()
                    .addFeature(Model.getFacade().getOwner(op), method);
        } else {
            Model.getCoreHelper()
                    .addOwnedElement(Model.getFacade().getOwner(op), method);
        }
    }

    /**
     * Called from the parser when an attribute is found.
     * 
     * @param modifiers A sequence of attribute modifiers.
     * @param typeSpec The attribute's type.
     * @param name The name of the attribute.
     * @param initializer The initial value of the attribute.
     * @param javadoc The javadoc comment. null or "" if no comment available.
     */
    public void addAttribute(short modifiers, String typeSpec, String name,
            String initializer, String javadoc) {
        addAttribute(modifiers, typeSpec, name, initializer, javadoc, false);
    }

    /**
     * Called from the parser when an attribute is found.
     * 
     * @param modifiers A sequence of attribute modifiers.
     * @param typeSpec The attribute's type.
     * @param name The name of the attribute.
     * @param initializer The initial value of the attribute.
     * @param javadoc The javadoc comment. null or "" if no comment available.
     * @param forceIt Force addition by creating all that's missing.
     */
    void addAttribute(short modifiers, String typeSpec, String name,
            String initializer, String javadoc, boolean forceIt) {
        String multiplicity = "1_1";
        Object mClassifier = null;

        if (typeSpec != null) {
            if (!arraysAsDatatype && typeSpec.indexOf('[') != -1) {
                typeSpec = typeSpec.substring(0, typeSpec.indexOf('['));
                multiplicity = "1_N";
            }

            // the attribute type
            try {
                // get the attribute type
                mClassifier = getContext(typeSpec).get(
                        getClassifierName(typeSpec), false, javaProfile);
            } catch (ClassifierNotFoundException e) {
                if (forceIt && typeSpec != null && model != null) {
                    LOG.info("Modeller.java: forced creation of"
                            + " unknown classifier " + typeSpec);
                    String packageName = getPackageName(typeSpec);
                    String classifierName = getClassifierName(typeSpec);
                    // here, getPackage must NOT use the Java profile, because
                    // class creation is only allowed in the user model
                    Object mPackage = (packageName.length() > 0) ? getPackage(
                            packageName, false) : model;
                    // a last chance: maybe it's in this user model package:
                    mClassifier = Model.getFacade().lookupIn(mPackage,
                            classifierName);
                    if (mClassifier == null) {
                        mClassifier = Model.getCoreFactory().buildClass(
                                classifierName, mPackage);
                        newElements.add(mClassifier);
                    }
                } else {
                    warnClassifierNotFound(typeSpec, "an attribute");
                }
            }
            if (mClassifier == null) {
                logError("failed to find or create type", typeSpec);
                return;
            }
        }

        // if we want to create a UML attribute:
        if (mClassifier == null
                || noAssociations
                || Model.getFacade().isADataType(mClassifier)
                || (Model.getFacade().getNamespace(mClassifier) == getPackage(
                        JAVA_PACKAGE, true))) {

            Object mAttribute = parseState.getAttribute(name);
            if (mAttribute == null) {
                mAttribute = buildAttribute(parseState.getClassifier(),
                        mClassifier, name);
            }
            parseState.feature(mAttribute);

            setOwnerScope(mAttribute, modifiers);
            setVisibility(mAttribute, modifiers);
            Model.getCoreHelper().setMultiplicity(mAttribute, multiplicity);

            if (Model.getFacade().isAClassifier(mClassifier)) {
                // TODO: This should already have been done in buildAttribute
                Model.getCoreHelper().setType(mAttribute, mClassifier);
            } else {
                // the type resolution failed to find a valid classifier.
                logError("Modeller.java: a valid type for a parameter "
                        + "could not be resolved:\n " + "In file: " + fileName
                        + ", for attribute: ", Model.getFacade().getName(
                        mAttribute));
            }

            // Set the initial value for the attribute.
            if (initializer != null) {

                // we must remove line endings and tabs from the intializer
                // strings, otherwise the classes will display horribly.
                initializer = initializer.replace('\n', ' ');
                initializer = initializer.replace('\t', ' ');

                Object newInitialValue = Model.getDataTypesFactory()
                        .createExpression("Java", initializer);
                Model.getCoreHelper().setInitialValue(mAttribute,
                        newInitialValue);
            }

            if ((modifiers & JavaParser.ACC_FINAL) > 0) {
                Model.getCoreHelper().setReadOnly(mAttribute, true);
            } else if (Model.getFacade().isReadOnly(mAttribute)) {
                Model.getCoreHelper().setReadOnly(mAttribute, true);
            }
            addDocumentationTag(mAttribute, javadoc);
        }
        // we want to create a UML association from the java attribute
        else {

            Object mAssociationEnd = getAssociationEnd(name, mClassifier);
            Model.getCoreHelper().setStatic(mAssociationEnd,
                    (modifiers & JavaParser.ACC_STATIC) > 0);
            setVisibility(mAssociationEnd, modifiers);
            Model.getCoreHelper()
                    .setMultiplicity(mAssociationEnd, multiplicity);
            Model.getCoreHelper().setType(mAssociationEnd, mClassifier);
            Model.getCoreHelper().setName(mAssociationEnd, name);
            if ((modifiers & JavaParser.ACC_FINAL) > 0) {
                Model.getCoreHelper().setReadOnly(mAssociationEnd, true);
            }
            if (!mClassifier.equals(parseState.getClassifier())) {
                // Because if they are equal,
                // then getAssociationEnd(name, mClassifier) could return
                // the wrong assoc end, on the other hand the navigability
                // is already set correctly (at least in this case), so the
                // next line is not necessary. (maybe never necessary?) - thn
                Model.getCoreHelper().setNavigable(mAssociationEnd, true);
            }
            addDocumentationTag(mAssociationEnd, javadoc);
        }
    }

    /**
     * Find a generalization in the model. If it does not exist, a new
     * generalization is created.
     * 
     * @param mPackage Look in this package.
     * @param parent The superclass.
     * @param child The subclass.
     * @return The generalization found or created.
     */
    private Object getGeneralization(Object mPackage, Object parent,
            Object child) {
        Object mGeneralization = Model.getFacade().getGeneralization(child,
                parent);
        if (mGeneralization == null) {
            mGeneralization = Model.getCoreFactory().buildGeneralization(child,
                    parent);
            newElements.add(mGeneralization);
        }
        if (mGeneralization != null
             && Model.getFacade().getUmlVersion().charAt(0) == '1') {
            Model.getCoreHelper().setNamespace(mGeneralization, mPackage);
        }
        return mGeneralization;
    }

    /**
     * Find an abstraction<<realize>> in the model. If it does not exist, a new
     * abstraction is created.
     * 
     * @param parent The superclass.
     * @param child The subclass.
     * @return The abstraction found or created.
     */
    private Object getAbstraction(Object parent, Object child) {
        Object mAbstraction = null;
        for (Iterator i = Model.getFacade().getClientDependencies(child)
                .iterator(); i.hasNext();) {
            mAbstraction = i.next();
            Collection c = Model.getFacade().getSuppliers(mAbstraction);
            if (c == null || c.size() == 0) {
                Model.getCoreHelper().removeClientDependency(child,
                        mAbstraction);
            } else {
                if (parent != c.toArray()[0]) {
                    mAbstraction = null;
                } else {
                    break;
                }
            }
        }

        if (mAbstraction == null) {
            mAbstraction = Model.getCoreFactory().buildAbstraction(
                    makeAbstractionName(child, parent), parent, child);
            newElements.add(mAbstraction);
        }
        return mAbstraction;
    }

    /**
     * Find a class in a package. If it does not exist, a new class is
     * created.
     * 
     * @param mPackage Look in this package.
     * @param name The name of the class.
     * @return The class found or created.
     */
    private Object getClass(Object mPackage, String name) {
        Object mClass = null;
        for (Object c : Model.getCoreHelper().getAllClasses(mPackage)) {
            if (name.equals(Model.getFacade().getName(c))) {
                mClass = c;
                break;
            }
        }
        if (mClass == null) {
            mClass = Model.getCoreFactory().buildClass(name, mPackage);
            newElements.add(mClass);
        }
        return mClass;
    }

    /**
     * Find a package in the project. If it does not exist, a new package is
     * created in the user model.
     * 
     * @param name The name of the package.
     * @param useProfile also look in the Java profile if true
     * @return The package found or created.
     */
    private Object getPackage(String name, boolean useProfile) {
        Object mPackage = searchPackageInModel(name, useProfile);
        if (mPackage == null) {
            // whole or part of the package path need to be built in model:
            Object currentNs = model;
            StringTokenizer st = new StringTokenizer(name, ".");
            while (st.hasMoreTokens()) {
                String rname = st.nextToken();
                mPackage = Model.getFacade().lookupIn(currentNs, rname);
                // the actual package might already exist in the user model
                if (mPackage == null
                        || !Model.getFacade().isAPackage(mPackage)) {
                    mPackage = Model.getModelManagementFactory().buildPackage(
                            getRelativePackageName(rname));
                    // set the owner for this package.
                    Model.getCoreHelper().addOwnedElement(currentNs, mPackage);
                    newElements.add(mPackage);
                }
                currentNs = mPackage;
            }
        }
        return mPackage;
    }

    /**
     * Search recursively for nested packages in the user model. So if you pass
     * a package org.argouml.kernel , this method searches for a package kernel,
     * that is owned by a package argouml, which is owned by a package org. This
     * method is required to nest the parsed packages. It optionally first
     * searches in the Java profile.
     * 
     * @param name The fully qualified package name of the package we are
     *            searching for.
     * @param useProfile first have a look in the Java profile if true
     * @return The found package or null, if it is not in the model.
     */
    private Object searchPackageInModel(String name, boolean useProfile) {
        Object ret = null;
        if ("".equals(getPackageName(name))) {
            if (useProfile && javaProfile != null) {
                try {
                    Object m = javaProfile.getProfilePackages().iterator()
                            .next();
                    ret = Model.getFacade().lookupIn(m, name);
                } catch (Exception e) {
                    ret = null;
                }
            }
            if (ret == null) {
                ret = Model.getFacade().lookupIn(model, name);
            }
            return ret;
        }
        Object owner = searchPackageInModel(getPackageName(name), useProfile);
        return owner == null ? null : Model.getFacade().lookupIn(owner,
                getRelativePackageName(name));
    }

    /**
     * Find an operation in the currentClassifier. If the operation is not
     * found, a new is created.
     * 
     * @param name The name of the operation.
     * @return The operation found or created.
     */
    private Object getOperation(String name) {
        Object mOperation = parseState.getOperation(name);
        if (mOperation != null) {
            LOG.info("Getting the existing operation " + name);
        } else {
            LOG.info("Creating a new operation " + name);
            Object cls = parseState.getClassifier();
            Object returnType = ProjectManager.getManager().getCurrentProject()
                    .getDefaultReturnType();
            mOperation = Model.getCoreFactory().buildOperation2(cls,
                    returnType, name);
            newElements.add(mOperation);
        }
        return mOperation;
    }

    /**
     * Find an operation in the currentClassifier. If the operation is not
     * found, a new is created.
     * 
     * @param name The name of the method.
     * @return The method found or created.
     */
    private Object getMethod(String name) {
        Object method = parseState.getMethod(name);
        if (method != null) {
            LOG.info("Getting the existing method " + name);
        } else {
            LOG.info("Creating a new method " + name);
            method = Model.getCoreFactory().buildMethod(name);
            newElements.add(method);
            if (Model.getFacade().getUmlVersion().charAt(0) == '1') {
                // Is this done twice (see caller of this method)?
                Model.getCoreHelper()
                    .addFeature(parseState.getClassifier(), method);
            }
        }
        return method;
    }

    /**
     * Build a new attribute in the current classifier.
     * 
     * @param classifier the model were are reverse engineering into
     * @param type the the type of the new attribute
     * @param name The name of the attribute.
     * @return The attribute found or created.
     */
    private Object buildAttribute(Object classifier, Object type, String name) {
        Object mAttribute = Model.getCoreFactory().buildAttribute2(classifier,
                type);
        newElements.add(mAttribute);
        Model.getCoreHelper().setName(mAttribute, name);
        return mAttribute;
    }

    /**
     * Find an associationEnd for a binary Association from the
     * currentClassifier to the type specified. If not found, a new is created.
     * 
     * @param name The name of the attribute.
     * @param mClassifier Where the association ends.
     * @return The attribute found or created.
     */
    private Object getAssociationEnd(String name, Object mClassifier) {
        Object mAssociationEnd = null;
        for (Iterator i = Model.getFacade().getAssociationEnds(mClassifier)
                .iterator(); i.hasNext();) {
            Object ae = i.next();
            Object assoc = Model.getFacade().getAssociation(ae);
            if (name.equals(Model.getFacade().getName(ae))
                    && Model.getFacade().getConnections(assoc).size() == 2
                    && Model.getFacade().getType(
                            Model.getFacade().getNextEnd(ae)) == parseState
                            .getClassifier()) {
                mAssociationEnd = ae;
            }
        }
        if (mAssociationEnd == null && !noAssociations) {
            String newName = makeAssociationName(parseState.getClassifier(),
                    mClassifier);

            Object mAssociation = buildDirectedAssociation(newName, parseState
                    .getClassifier(), mClassifier);
            // this causes a problem when mClassifier is not only
            // at one assoc end: (which one is the right one?)
            mAssociationEnd = Model.getFacade().getAssociationEnd(mClassifier,
                    mAssociation);
        }
        return mAssociationEnd;
    }

    /**
     * Build a unidirectional association between two Classifiers.
     * 
     * @param name name of the association
     * @param sourceClassifier source classifier (end which is non-navigable)
     * @param destClassifier destination classifier (end which is navigable)
     * @return newly created Association
     */
    public static Object buildDirectedAssociation(String name,
            Object sourceClassifier, Object destClassifier) {
        return Model.getCoreFactory().buildAssociation(destClassifier, true,
                sourceClassifier, false, name);
    }

    /**
     * Get the stereotype with a specific name. UML 1.x only.
     * 
     * @param name The name of the stereotype.
     * @return The stereotype.
     */
    private Object getUML1Stereotype(String name) {
        LOG.fine("Trying to find a stereotype of name <<" + name + ">>");
        // Is this line really safe wouldn't it just return the first
        // model element of the same name whether or not it is a stereotype
        Object stereotype = Model.getFacade().lookupIn(model, name);

        if (stereotype == null) {
            LOG.fine("Couldn't find so creating it");
            return Model.getExtensionMechanismsFactory().buildStereotype(name,
                    model);
        }

        if (!Model.getFacade().isAStereotype(stereotype)) {
            // and so this piece of code may create an existing stereotype
            // in error.
            LOG.fine("Found something that isn't a stereotype so creating it");
            return Model.getExtensionMechanismsFactory().buildStereotype(name,
                    model);
        }

        LOG.fine("Found it");
        return stereotype;
    }

    /**
     * Find the first suitable stereotype with baseclass for a given object.
     * 
     * @param me
     * @param name
     * @param baseClass
     * @return the stereotype if found
     * 
     * @throws IllegalArgumentException if the desired stereotypes for the
     *             modelelement and baseclass was not found and could not be
     *             created. No stereotype is created.
     */
    private Object getStereotype(Object me, String name, String baseClass) {
        Collection models = ProjectManager.getManager().getCurrentProject()
                .getModels();
        Collection stereos = Model.getExtensionMechanismsHelper()
                .getAllPossibleStereotypes(models, me);
        Object stereotype = null;
        if (stereos != null && stereos.size() > 0) {
            Iterator iter = stereos.iterator();
            while (iter.hasNext()) {
                stereotype = iter.next();
                if (Model.getExtensionMechanismsHelper().isStereotypeInh(
                        stereotype, name, baseClass)) {
                    LOG.info("Returning the existing stereotype of <<"
                            + Model.getFacade().getName(stereotype) + ">>");
                    return stereotype;
                }
            }
        }
        if (Model.getFacade().getUmlVersion().charAt(0) != '1') {
            // For UML2, we must fail now, because stereotypes must be found
            // only in profiles.
            throw new IllegalArgumentException("Could not find "
                + "a suitable stereotype for " + Model.getFacade().getName(me)
                + " -  stereotype: <<" + name + ">> base: " + baseClass
                + ".\n"
                + "Check if environment variable eUML.resources "
                + "is correctly set.");
        }
        // (UML 1.x only from here)
        // Instead of failing, this should create any stereotypes that it
        // requires. Most likely cause of failure is that the stereotype isn't
        // included in the profile that is being used. - tfm 20060224
        stereotype = getUML1Stereotype(name);
        if (stereotype != null) {
            Model.getExtensionMechanismsHelper().addBaseClass(stereotype, me);
            return stereotype;
        }
        // This should never happen then:
        throw new IllegalArgumentException("Could not find "
                + "a suitable stereotype for " + Model.getFacade().getName(me)
                + " -  stereotype: <<" + name + ">> base: " + baseClass);
    }

    /**
     * Return the tagged value with a specific tag.
     * 
     * @param element The tagged value belongs to this.
     * @param name The tag.
     * @return The found tag. A new is created if not found.
     */
    private Object getTaggedValue(Object element, String name) {
        Object tv = Model.getFacade().getTaggedValue(element, name);
        if (tv == null) {
            String[] empties = {
                ""
            };
            buildTaggedValue(element, name, empties);
            tv = Model.getFacade().getTaggedValue(element, name);
        }
        return tv;
    }

    /**
     * This classifier was earlier generated by reference but now it is its time
     * to be parsed so we clean out remnants.
     * 
     * @param element that they are removed from
     */
    private void cleanModelElement(Object element) {
        Object tv = Model.getFacade().getTaggedValue(element,
                Facade.GENERATED_TAG);
        while (tv != null) {
            Model.getUmlFactory().delete(tv);
            tv = Model.getFacade()
                    .getTaggedValue(element, Facade.GENERATED_TAG);
        }
    }

    /**
     * Get the package name from a fully specified classifier name.
     * 
     * @param name A fully specified classifier name.
     * @return The package name.
     */
    private String getPackageName(String name) {
        name = stripVarargAndGenerics(name);
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        String pkgName = name.substring(0, lastDot);

        // If the last element begins with an uppercase character, assume
        // that we've really got a class, not a package.
        // TODO: A better strategy would be to defer until we can disambiguate
        if (Character.isUpperCase(getRelativePackageName(pkgName).charAt(0))) {
            return getPackageName(pkgName);
        } else {
            return pkgName;
        }
    }

    /**
     * Get the relative package name from a fully qualified package name. So if
     * the parameter is 'org.argouml.kernel' the method is supposed to return
     * 'kernel' (the package kernel is in package 'org.argouml').
     * 
     * @param packageName A fully qualified package name.
     * @return The relative package name.
     */
    private String getRelativePackageName(String packageName) {
        // Since the relative package name corresponds
        // to the classifier name of a fully qualified
        // classifier, we simply use this method.

        // TODO: This won't correctly identify the package for an inner class
        // e.g. package.Foo.Bar, but getPackageName() makes an attempt to
        // guess correctly

        return getClassifierName(packageName);
    }

    /**
     * Get the classifier name from a fully specified classifier name.
     * <p>
     * FIXME: Most uses of this method are wrong. We should be adding context
     * such as package names or outer classifier names, not removing it, before
     * doing lookup so that the search methods have the fully qualified name to
     * work with.
     * 
     * @param name A fully specified classifier name.
     * @return The classifier name.
     */
    private String getClassifierName(String name) {
        name = stripVarargAndGenerics(name);
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            return name;
        }
        return name.substring(lastDot + 1);
    }

    /**
     * Set the visibility for a model element.
     * 
     * @param element The model element.
     * @param modifiers A sequence of modifiers which may contain 'private',
     *            'protected' or 'public'.
     */
    private void setVisibility(Object element, short modifiers) {
        if ((modifiers & JavaParser.ACC_PRIVATE) > 0) {
            Model.getCoreHelper().setVisibility(element,
                    Model.getVisibilityKind().getPrivate());
        } else if ((modifiers & JavaParser.ACC_PROTECTED) > 0) {
            Model.getCoreHelper().setVisibility(element,
                    Model.getVisibilityKind().getProtected());
        } else if ((modifiers & JavaParser.ACC_PUBLIC) > 0) {
            Model.getCoreHelper().setVisibility(element,
                    Model.getVisibilityKind().getPublic());
        } else {
            // Default Java visibility is "package"
            Model.getCoreHelper().setVisibility(element,
                    Model.getVisibilityKind().getPackage());
        }
    }

    /**
     * Set the owner scope for a feature.
     * 
     * @param feature The feature.
     * @param modifiers A sequence of modifiers which may contain 'static'.
     */
    private void setOwnerScope(Object feature, short modifiers) {
        Model.getCoreHelper().setStatic(feature,
                (modifiers & JavaParser.ACC_STATIC) > 0);
    }

    /**
     * Get the context for a classifier name that may or may not be fully
     * qualified. The context contains either the user model, or a package
     * or class inside the user model, or a package or class in the Java
     * profile.
     * 
     * @param name the classifier name
     * @return the context
     */
    private Context getContext(String name) {
        Context context = parseState.getContext();
        String packageName = getPackageName(name);
        Object pkg = model;
        if (!"".equals(packageName)) {
            pkg = getPackage(packageName, true);
        }
        String classifierName = name.substring(packageName.length());
        if (classifierName.charAt(0) == '.') {
            classifierName = classifierName.substring(1);
        }
        classifierName = stripVarargAndGenerics(classifierName);
        int lastDot = classifierName.lastIndexOf('.');
        if (lastDot != -1) {
            String clsName = classifierName.substring(0, lastDot);
            Object cls = getClass(pkg, clsName);
            context = 
                new OuterClassifierContext(
                        context.getContext(), cls, pkg, 
                        clsName + '$');
        } else if (!"".equals(packageName)) {
            context = new PackageContext(context, pkg);
        }
        return context;
    }

    /**
     * Add the contents of a single standard javadoc tag to the model element.
     * Usually this will be added as a tagged value.
     * 
     * This is called from {@link #addDocumentationTag} only.
     * 
     * @param me the model element to add to
     * @param sTagName the name of the javadoc tag
     * @param sTagData the contents of the javadoc tag
     */
    private void addJavadocTagContents(Object me, String sTagName,
            String[] sTagData) {
        if (sTagData != null 
            && (sTagData.length == 0 || sTagData[0] == null)) {
            LOG.fine("Called addJavadocTagContents with no tag data!");
            return;
        }
        int colonPos = (sTagData != null) ? sTagData[0].indexOf(':') : -1;
        if (colonPos != -1
                && (("invariant".equals(sTagName))
                        || ("pre-condition".equals(sTagName))
                        || ("post-condition".equals(sTagName)))) {

            // add as OCL constraint
            String sContext = OCLUtil.getContextString(me);
            String name = sTagData[0].substring(0, colonPos);
            String body = null;
            if (sTagName.equals("invariant")) {
                // add as invariant constraint Note that no checking
                // of constraint syntax is performed... BAD!
                body = sContext + " inv " + sTagData;
            } else if (sTagName.equals("pre-condition")) {
                body = sContext + " pre " + sTagData;
            } else {
                body = sContext + " post " + sTagData;
            }
            Object bexpr = Model.getDataTypesFactory().createBooleanExpression(
                    "OCL", body);
            Object mc = Model.getCoreFactory().buildConstraint(name, bexpr);
            Model.getCoreHelper().addConstraint(me, mc);
            if (Model.getFacade().getNamespace(me) != null) {
                // Apparently namespace management is not supported
                // for all model elements. As this does not seem to
                // cause problems, I'll just leave it at that for the
                // moment...
                Model.getCoreHelper().addOwnedElement(
                        Model.getFacade().getNamespace(me), mc);
            }
        } else {
            if ("stereotype".equals(sTagName)) {
                // multiple stereotype support:
                // make one stereotype tag from many stereotype tags
                Object tv = getTaggedValue(me, sTagName);
                if (tv != null) {
                    String sStereotype = Model.getFacade().getValueOfTag(tv);
                    if (sStereotype != null && sStereotype.length() > 0) {
                        sTagData[0] = sStereotype + ',' + sTagData[0];
                    }
                }
                // now eliminate multiple entries in that comma separated list
                HashSet<String> stSet = new HashSet<String>();
                StringTokenizer st = new StringTokenizer(sTagData[0], ", ");
                while (st.hasMoreTokens()) {
                    stSet.add(st.nextToken().trim());
                }
                StringBuffer sb = new StringBuffer();
                Iterator<String> iter = stSet.iterator();
                while (iter.hasNext()) {
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    sb.append(iter.next());
                }
                sTagData[0] = sb.toString();

            }
            buildTaggedValue(me, sTagName, sTagData);
        }
    }

    private void buildTaggedValue(Object me, 
            String sTagName, 
            String[] sTagData) {
        Object tv = Model.getFacade().getTaggedValue(me, sTagName);
        if (tv == null) {
            // using deprecated buildTaggedValue here, because getting the tag
            // definition from a tag name is the critical step, and this is
            // implemented in ExtensionMechanismsFactory in a central place,
            // but not as a public method:
            Model.getExtensionMechanismsHelper().addTaggedValue(
                    me,
                    Model.getExtensionMechanismsFactory()
                    .buildTaggedValue(sTagName, sTagData[0]));
        } else {
            Model.getExtensionMechanismsHelper().setDataValues(tv, sTagData);
        }
    }

    /**
     * Add the javadocs as a tagged value 'documentation' to the model element.
     * All comment delimiters are removed prior to adding the comment.
     * 
     * Added 2001-10-05 STEFFEN ZSCHALER.
     * 
     * @param modelElement the model element to which to add the documentation
     * @param sJavaDocs the documentation comments to add ("" or null if no java
     *            docs)
     */
    private void addDocumentationTag(Object modelElement, String sJavaDocs) {
        if ((sJavaDocs != null) && (sJavaDocs.trim().length() >= 5)) {
            StringBuffer sbPureDocs = new StringBuffer(80);
            String sCurrentTagName = null;
            String[] sCurrentTagData = {
                null
            };
            int nStartPos = 3; // skip the leading /**
            boolean fHadAsterisk = true;

            while (nStartPos < sJavaDocs.length()) {
                switch (sJavaDocs.charAt(nStartPos)) {
                case '*':
                    fHadAsterisk = true;
                    nStartPos++;
                    break;
                case ' ': // all white space, hope I didn't miss any ;-)
                case '\t':
                    // ignore white space before the first asterisk
                    if (!fHadAsterisk) {
                        nStartPos++;
                        break;
                    }
                default:
                    // normal comment text or standard tag
                    // check ahead for tag
                    int j = nStartPos;
                    while ((j < sJavaDocs.length())
                            && ((sJavaDocs.charAt(j) == ' ') || (sJavaDocs
                                    .charAt(j) == '\t'))) {
                        j++;
                    }
                    if (j < sJavaDocs.length()) {
                        if (sJavaDocs.charAt(j) == '@') {
                            // if the last javadoc is on the last line
                            // no new line will be found, causing an
                            // indexoutofboundexception.
                            int lineEndPos = 0;
                            if (sJavaDocs.indexOf('\n', j) < 0) {
                                lineEndPos = sJavaDocs.length() - 2;
                            } else {
                                lineEndPos = sJavaDocs.indexOf('\n', j) + 1;
                            }
                            sbPureDocs.append(sJavaDocs
                                    .substring(j, lineEndPos));
                            // start standard tag potentially add
                            // current tag to set of tagged values...
                            if (sCurrentTagName != null) {
                                addJavadocTagContents(modelElement,
                                        sCurrentTagName, sCurrentTagData);
                            }
                            // open new tag
                            int nTemp = sJavaDocs.indexOf(' ', j + 1);
                            if (nTemp == -1) {
                                nTemp = sJavaDocs.length() - 1;
                            }
                            sCurrentTagName = sJavaDocs.substring(j + 1, nTemp);
                            int nTemp1 = sJavaDocs.indexOf('\n', ++nTemp);
                            if (nTemp1 == -1) {
                                nTemp1 = sJavaDocs.length();
                            } else {
                                nTemp1++;
                            }
                            sCurrentTagData[0] = sJavaDocs.substring(nTemp,
                                    nTemp1);
                            nStartPos = nTemp1;
                        } else {
                            // continue standard tag or comment text
                            int nTemp = sJavaDocs.indexOf('\n', nStartPos);
                            if (nTemp == -1) {
                                nTemp = sJavaDocs.length();
                            } else {
                                nTemp++;
                            }
                            if (sCurrentTagName != null) {
                                sbPureDocs.append(sJavaDocs.substring(
                                        nStartPos, nTemp));
                                sCurrentTagData[0] += " "
                                        + sJavaDocs.substring(nStartPos, nTemp);
                            } else {
                                sbPureDocs.append(sJavaDocs.substring(
                                        nStartPos, nTemp));
                            }
                            nStartPos = nTemp;
                        }
                    }
                    fHadAsterisk = false;
                }
            }
            sJavaDocs = sbPureDocs.toString();

            /*
             * After this, we have the documentation text, but there's still a
             * trailing '/' left, either at the end of the actual comment text
             * or at the end of the last tag.
             */
            sJavaDocs = removeTrailingSlash(sJavaDocs);

            // handle last tag, if any (strip trailing slash there too)
            if (sCurrentTagName != null) {
                sCurrentTagData[0] = removeTrailingSlash(sCurrentTagData[0]);
                addJavadocTagContents(modelElement, sCurrentTagName,
                        sCurrentTagData);
            }

            // Now store documentation text in a tagged value
            String[] javadocs = {
                sJavaDocs
            };
            buildTaggedValue(modelElement, Argo.DOCUMENTATION_TAG, javadocs);
            addStereotypes(modelElement);
        }
    }

    /*
     * Remove a trailing slash, including the entire line if it's the only thing
     * on the line.
     */
    private String removeTrailingSlash(String s) {
        if (s.endsWith("\n/")) {
            return s.substring(0, s.length() - 2);
        } else if (s.endsWith("*/")) {
            // need if end comment in the same line than comment
            return s.substring(0, s.length() - 2);
        } else if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        } else {
            return s;
        }
    }

    /*
     * Remove information that currently is not handled.
     * TODO: Handle them instead.
     */
    private String stripVarargAndGenerics(String name) {
        if (name != null) {
            if (name.endsWith("...")) {
                // handle vararg
                name = name.substring(0, name.length() - 3);
            }
            if (name.endsWith(">")) {
                // handle generics
                int i = name.length() - 2;
                int cnt = 1;
                while (i >= 0 && cnt > 0) {
                    if (name.charAt(i) == '<') {
                        cnt--;
                    } else if (name.charAt(i) == '>') {
                        cnt++;
                    }
                    i--;
                }
                name = name.substring(0, i + 1);
            }
        }
        return name;
    }

    /*
     * If there is a tagged value named 'stereotype', make it a real stereotype
     * and remove the tagged value. We allow multiple instances of this tagged
     * value AND parse a single instance for multiple stereotypes
     */
    private void addStereotypes(Object modelElement) {
        // TODO: What we do here is allowed for UML 1.x only!
        if (Model.getFacade().getUmlVersion().charAt(0) == '1') {
            Object tv = Model.getFacade()
                    .getTaggedValue(modelElement, "stereotype");
            if (tv != null) {
                String stereo = Model.getFacade().getValueOfTag(tv);
                if (stereo != null && stereo.length() > 0) {
                    StringTokenizer st = new StringTokenizer(stereo, ", ");
                    while (st.hasMoreTokens()) {
                        Model.getCoreHelper().addStereotype(modelElement,
                                getUML1Stereotype(st.nextToken().trim()));
                    }
                    ProjectManager.getManager().updateRoots();
                }
                Model.getUmlFactory().delete(tv);
            }
        }
    }

    /**
     * Manage collection of parsed method calls. Used for reverse engineering of
     * interactions.
     */
    /**
     * Add a parsed method call to the collection of method calls.
     * 
     * @param methodName The method name called.
     */
    public void addCall(String methodName) {
        methodCalls.add(methodName);
    }

    /**
     * Get collection of method calls.
     * 
     * @return list containing collected method calls
     */
    public synchronized List<String> getMethodCalls() {
        return methodCalls;
    }

    /**
     * Clear collected method calls.
     */
    public void clearMethodCalls() {
        methodCalls.clear();
    }

    /**
     * Add a local variable declaration to the list of variables.
     * 
     * @param type type of declared variable
     * @param name name of declared variable
     */
    public void addLocalVariableDeclaration(String type, String name) {
        localVariables.put(name, type);
    }

    /**
     * Return the collected set of local variable declarations.
     *
     * This is read from
     * {@link org.argouml.language.java.ui.RESequenceDiagramDialog#parseBody()}
     *
     * 
     * @return hash table containing all local variable declarations.
     */
    public Map<String, String> getLocalVariableDeclarations() {
        return Collections.unmodifiableMap(localVariables);
    }

    /**
     * Clear the set of local variable declarations.
     */
    public void clearLocalVariableDeclarations() {
        localVariables.clear();
    }

    /**
     * Get the elements which were created while reverse engineering this file.
     * 
     * @return the collection of elements
     */
    public Collection<Object> getNewElements() {
        return newElements;
    }

    /**
     * Set flag that controls name generation. Artificial names are generated by
     * default for historical reasons, but in most cases they are just clutter.
     * 
     * @param generateNamesFlag true to generate artificial names of the form
     *            "From->To" for Associations, Dependencies, etc.
     */
    public void setGenerateNames(boolean generateNamesFlag) {
        generateNames = generateNamesFlag;
    }
}
