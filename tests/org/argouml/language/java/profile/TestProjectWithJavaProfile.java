/* $Id$
 *****************************************************************************
 * Copyright (c) 2009-2013 Contributors - see below
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

// Copyright (c) 2009 The Regents of the University of California. All
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

package org.argouml.language.java.profile;

import static org.argouml.model.Model.getFacade;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import junit.framework.TestCase;

import org.argouml.Helper;
import org.argouml.application.helpers.ApplicationVersion;
import org.argouml.kernel.Project;
import org.argouml.kernel.ProjectManager;
import org.argouml.model.Model;
import org.argouml.persistence.AbstractFilePersister;
import org.argouml.persistence.OpenException;
import org.argouml.persistence.PersistenceManager;
import org.argouml.persistence.SaveException;
import org.argouml.profile.Profile;
import org.argouml.profile.ProfileFacade;
import org.argouml.profile.ProfileManager;
import org.argouml.profile.init.InitProfileSubsystem;

/**
 * Tests the {@link ProjectImpl} with profiles, specifically this enables the
 * testing of the org.argouml.profile subsystem API for the project and the
 * interactions between {@link ProfileConfiguration} and the {@link Project}.
 * 
 * @author Luis Sergio Oliveira (euluis)
 */
public class TestProjectWithJavaProfile extends TestCase {

    private File testCaseDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Helper.initializeMDR();
        new InitProfileSubsystem().init();
        new ProfileJava().enable();

        if (ApplicationVersion.getVersion() == null) {
            Class<?> argoVersionClass = Class
                    .forName("org.argouml.application.ArgoVersion");
            Method initMethod = argoVersionClass.getDeclaredMethod("init");
            initMethod.setAccessible(true);
            initMethod.invoke(null);
            assertNotNull(ApplicationVersion.getVersion());
        }
        String testCaseDirName = getClass().getPackage().getName();

	// Clean the directory before starting the test.
	// TODO: This is more of a symptom that something is wrong
	// in the test that makes it impossible to delete the file.
	// When this problem is fixed this workaround can be removed.
        testCaseDir = Helper.setUpDir4Test(testCaseDirName);
	Helper.deleteDirectory(testCaseDir);

        testCaseDir = Helper.setUpDir4Test(testCaseDirName);
    }

    @Override
    protected void tearDown() throws Exception {
	try {
	    Helper.deleteDirectory(testCaseDir);
	} catch (IOException e) {
	    // This is a workaround to get the tests not to fail.
	    // The directory is cleaned also on setUp.
	    // TODO: This is more of a symptom that something is wrong
	    // in the test that makes it impossible to delete the file.
	    // When this problem is fixed this workaround can be removed.
	}
        super.tearDown();
    }

    /**
     * WARNING: not a unit test, this is more like a functional test, where
     * several subsystems are tested.
     * 
     * This test does:
     * <ol>
     * <li>set UML Profile for Java as a default profile</li>
     * <li>create a new project and assert that it has the UML profile for Java
     * as part of the project's profile configuration</li>
     * <li>create a dependency from the project's model to the UML profile for
     * Java</li>
     * <li>remove the Java profile from the project's profile configuration</li>
     * <li>assert that the project's model elements that had a dependency to the
     * UML profile for Java don't get inconsistent</li>
     * <li>save the project into a new file</li>
     * <li>reopen the project and assert that the Java profile isn't part of the
     * profile configuration</li>
     * <li>assert that the project's model elements that had a dependency to the
     * UML profile for Java are consistent</li>
     * </ol>
     * 
     * @throws OpenException if there was an error during a project load
     * @throws SaveException if there was an error during a project save
     * @throws InterruptedException if save or load was interrupted
     */
    public void testRemoveProfileWithModelThatRefersToProfile()
        throws OpenException, SaveException, InterruptedException {
        // set UML Profile for Java as a default profile
        ProfileManager profileManager = ProfileFacade.getManager();
        Profile javaProfile = profileManager.getProfileForClass(
                "org.argouml.language.java.profile.ProfileJava");
        assertNotNull("The UML profile for Java shouldn't be null.",
                javaProfile);
        if (!profileManager.getDefaultProfiles().contains(javaProfile)) {
            profileManager.addToDefaultProfiles(javaProfile);
        }
        // create a new project and assert that it has the UML profile for
        // Java as part of the project's profile configuration
        Project project = ProjectManager.getManager().makeEmptyProject();
        assertTrue(project.getProfileConfiguration().getProfiles().contains(
                javaProfile));
        // create a dependency from the project's model to the UML profile for
        // Java
        Object model = project.getUserDefinedModelList().get(0);
        assertNotNull(model);
        Object fooClass = Model.getCoreFactory().buildClass("Foo", model);
        Object javaListType = project.findType("List", false);
        assertNotNull(javaListType);
        Object barOperation = Model.getCoreFactory().buildOperation2(fooClass,
                javaListType, "bar");
        assertEquals(barOperation, getFacade().getOperations(fooClass)
                .iterator().next());
        Object returnParam = getFacade().getParameter(barOperation, 0);
        assertNotNull(returnParam);
        Object returnParamType = getFacade().getType(returnParam);
        checkJavaListTypeExistsAndMatchesReturnParamType(project,
                returnParamType);
        // remove the Java profile from the project's profile configuration
        project.getProfileConfiguration().removeProfile(javaProfile, model);
        // assert that the project's model elements that had a dependency to
        // the UML profile for Java don't get inconsistent
        returnParamType = getFacade().getType(returnParam);
        checkJavaListTypeExistsAndMatchesReturnParamType(project,
                returnParamType);
        assertNotNull(project.findType("Foo", false));
        // save the project into a new file
        File file = getFileInTestDir(
                "testRemoveProfileWithModelThatRefersToProfile.zargo");
        AbstractFilePersister persister = getProjectPersister(file);
        project.setVersion(ApplicationVersion.getVersion());
        persister.save(project, file);
        project.remove();

        // reopen the project and assert that the Java profile isn't part of
        // the profile configuration, including the fact that the type
        // java.util.List isn't found
        project = persister.doLoad(file);
        project.postLoad();
        assertFalse(project.getProfileConfiguration().getProfiles().contains(
                javaProfile));
        assertNull(project.findType("List", false));
        // assert that the project's model elements that had a dependency to
        // the UML profile for Java are consistent
        fooClass = project.findType("Foo", false);
        assertNotNull(fooClass);
        barOperation = getFacade().getOperations(fooClass).iterator().next();
        returnParam = getFacade().getParameter(barOperation, 0);
        assertNotNull(returnParam);
        // Return type was java.util.List from Java profile - should be gone
        returnParamType = getFacade().getType(returnParam);
        // TODO: with new reference resolving scheme, the model sub-system will
        // cache the systemId of the profile, open it and resolve the profile
        // on its own. Thus, the java.util.List will be found and the return
        // value will be present again...
        assertNotNull(returnParamType);
        project.remove();
    }

    private void checkJavaListTypeExistsAndMatchesReturnParamType(
            Project project, Object returnParamType) {
        Object javaListType = project.findType("List", false);
        assertNotNull(javaListType);
        assertEquals(getFacade().getName(javaListType), getFacade().getName(
                returnParamType));
        assertEquals(getFacade().getNamespace(javaListType), getFacade()
                .getNamespace(returnParamType));
    }

    private File getFileInTestDir(String fileName) {
        return new File(testCaseDir, fileName);
    }

    private AbstractFilePersister getProjectPersister(File file) {
        AbstractFilePersister persister = PersistenceManager.getInstance()
                .getPersisterFromFileName(file.getAbsolutePath());
        return persister;
    }
}
