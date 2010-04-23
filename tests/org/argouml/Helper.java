/* $Id$
 *****************************************************************************
 * Copyright (c) 2009 Contributors - see below
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tom Morris
 *    Thomas Neustupny
 *****************************************************************************
 *
 * Some portions of this file was previously release using the BSD License:
 */

// Copyright (c) 2007 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies. This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason. IN NO EVENT SHALL THE
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

package org.argouml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import junit.framework.TestCase;

import org.argouml.kernel.Project;
import org.argouml.kernel.ProjectManager;
import org.argouml.model.Model;
import org.argouml.model.ModelImplementation;
import org.argouml.model.XmiWriter;
import org.argouml.persistence.UmlFilePersister;
import org.argouml.profile.init.InitProfileSubsystem;

/**
 * A Helper for test classes.
 *
 * @author Luis Sergio Oliveira (euluis)
 */
public class Helper {

    /**
     * "st" the example stereotype name.
     */
    public static final String STEREOTYPE_NAME_ST = "st";

    /**
     * "SimpleProfile" is the name of the SimpleProfile which ProfileMother
     * uses for the most basic profile being used.
     */
    public static final String DEFAULT_SIMPLE_PROFILE_NAME = "SimpleProfile";

    /**
     * "TagDef" is the name of the Tag Definition applicable to model elements
     * to which the stereotype named {@link ProfileMother#STEREOTYPE_NAME_ST}
     * of SimpleProfile was applied.
     */
    public static final String TAG_DEFINITION_NAME_TD = "TagDef";

    public static Object getModel() {
        // TODO: This is making assumptions about the ordering of models which
        // may not be true! - tfm
        return ProjectManager.getManager().getCurrentProject().
            getUserDefinedModelList().iterator().next();
    }

    public static Collection<Object> getModels() {
        return ProjectManager.getManager().getCurrentProject().getUserDefinedModelList();
    }

    public static void newModel() {
        createProject();
    }

    public static Project createProject() {
        ensureModelSubsystemInitialized();
        new InitProfileSubsystem().init();
        return ProjectManager.getManager().makeEmptyProject();
    }

    static void ensureModelSubsystemInitialized() {
        if (!Model.isInitiated())
            initializeMDR();
    }

    /**
     * Initialize the Model subsystem with the MDR ModelImplementation.
     */
    public static void initializeMDR() {
        // TODO: Modules shouldn't have a dependency on internal implementation
        // artifacts of ArgoUML (and shouldn't use reflection to hide the fact
        // that they have that dependency).
        initializeModelImplementation(
                "org.argouml.model.mdr.MDRModelImplementation");
    }

    private static ModelImplementation initializeModelImplementation(
            String name) {
        ModelImplementation impl = null;

        Class implType;
        try {
            implType =
                Class.forName(name);
        } catch (ClassNotFoundException e) {
            TestCase.fail(e.toString());
            return null;
        }

        try {
            impl = (ModelImplementation) implType.newInstance();
        } catch (InstantiationException e) {
            TestCase.fail(e.toString());
        } catch (IllegalAccessException e) {
            TestCase.fail(e.toString());
        }
        Model.setImplementation(impl);
        return impl;
    }

    public static void assertNotEmpty(String string) {
        TestCase.assertNotNull(
            "Ha! The freaking string is null and you're asking about its "
            + "emptyness!", string);
        TestCase.assertTrue("The string size must be bigger than 0.",
                string.length() > 0);
    }

    /**
     * System temporary directory property name.
     */
    public static final String SYSPROPNAME_TMPDIR = "java.io.tmpdir";


    public static File getTmpDir() {
        return new File(System.getProperty(Helper.SYSPROPNAME_TMPDIR));
    }

    /**
     * Setup a directory with the given name for the caller test.
     *
     * @param dirName
     *            the directory to be created in the system temporary dir
     * @return the created directory
     */
    public static File setUpDir4Test(String dirName) {
        File generationDir = new File(getTmpDir(), dirName);
        generationDir.mkdirs();
        return generationDir;
    }

    public static void deleteDir(File dir) throws IOException {
        if (dir != null && dir.exists()) {
            deleteDirectory(dir);
        }
    }

    /**
     * Delete a directory recursively.
     *
     * @param dir  directory to be deleted
     * @throws IOException if deletion failed
     */
    public static void deleteDirectory(File dir) throws IOException {
        if (!dir.exists()) {
            return;
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(dir + " is not a directory");
        }
        File[] files = dir.listFiles();
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + dir);
        }
        IOException e = null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else if (!file.delete()) {
                    throw new IOException("Unable to delete file: " + file);
                }
            } catch (IOException ioe) {
                e = ioe;
            }
        }
        if (e != null) {
            throw e;
        }
        if (!dir.delete()) {
            throw new IOException("Unable to delete directory " + dir + ".");
        }
    }

    /**
     * Create a simple profile model with name {@link ProfileMother#DEFAULT_SIMPLE_PROFILE_NAME}
     * with a class named "foo" and with a stereotype named
     * {@link ProfileMother#STEREOTYPE_NAME_ST}.
     *
     * @return the profile model.
     */
    public static Object createSimpleProfileModel() {
        return createSimpleProfileModel(DEFAULT_SIMPLE_PROFILE_NAME);
    }

    /**
     * Create a simple profile model with name profileName,
     * with a class named "foo" and with a stereotype named
     * {@link ProfileMother#STEREOTYPE_NAME_ST}.
     *
     * @param profileName the name that the created profile shall have.
     * @return the profile model.
     */
    public static Object createSimpleProfileModel(String profileName) {
        // TODO: should it remove the leftovers from other tests?
//        cleanAllExtents();
//        assert getFacade().getRootElements().size() == 0;
        Object model = Model.getModelManagementFactory().createProfile();
        Object fooClass = Model.getCoreFactory().buildClass("foo", model);
        Object stereotype = Model.getExtensionMechanismsFactory().buildStereotype(fooClass,
            STEREOTYPE_NAME_ST, model);
        Model.getCoreHelper().setName(model, profileName);
        Model.getExtensionMechanismsFactory().buildTagDefinition(
            TAG_DEFINITION_NAME_TD, stereotype, null);
        return model;
    }

    /**
     * Save the profile model into the given file.
     *
     * @param model the profile model.
     * @param file the file into which to save the profile model.
     * @throws IOException if IO goes wrong.
     */
    public static void saveProfileModel(Object model, File file) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(file);
//        cleanAllExtentsBut(model); // TODO: why is this causing a crash?!?
        try {
            XmiWriter xmiWriter = Model.getXmiWriter(model, fileOut, "x("
                + UmlFilePersister.PERSISTENCE_VERSION + ")");
            xmiWriter.write();
            fileOut.flush();
        } catch (Exception e) {
            String msg = "Exception while saving profile model.";
            //LOG.error(msg, e);
            throw new IOException(msg);
        } finally {
            fileOut.close();
        }
    }
}
