/* $Id$
 *******************************************************************************
 * Copyright (c) 2010 Contributors - see below
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Luis Sergio Oliveira (euluis)
 *******************************************************************************
 */

package org.argouml.language.java.reveng;

import junit.framework.Assert;

import org.argouml.Helper;
import org.argouml.kernel.Project;
import org.argouml.kernel.ProjectManager;
import org.argouml.language.java.profile.ProfileJava;
import org.argouml.profile.ProfileFacade;
import org.argouml.profile.init.InitProfileSubsystem;

/**
 * An helper class for implementing aspects of import fixtures to avoid code
 * duplication. 
 * @author Luis Sergio Oliveira (euluis)
 */
class ImportFixture {
    private String parserInput;
    private String fileName;
    private ProfileJava profileJava;
    private Object parsedModel;
    private Modeller modeller;

    ProfileJava getProfileJava() {
        return profileJava;
    }

    Object getParsedModel() {
        return parsedModel;
    }
    
    String getFileName() {
        return fileName;
    }
    
    String getParserInput() {
        return parserInput;
    }
    
    Modeller getModeller() {
        return modeller;
    }
    
    private static void checkNullArgument(String argName, Object arg) {
        if (arg == null) {
            throw new IllegalArgumentException(argName + " argument is null.");
        }
    }

    ImportFixture(String theParserInput, String theFileName) {
        checkNullArgument("theParserInput", theParserInput);
        checkNullArgument("theFileName", theFileName);
        parserInput = theParserInput;
        fileName = theFileName;
    }

    /**
     * @see junit.framework.TestCase#setUp()
     * @throws Exception if a setup step goes wrong
     */
    void setUp() throws Exception {
        Helper.initializeMDR();
        new InitProfileSubsystem().init();
        // TODO: When running offline, the indirect lazy loading of UML profile
        // model fails when the Java module is loading. This is only fixable
        // by forcing the direct loading of the UML model, which does work.
        // Eventually this should be fixed in XmiReferenceResolverImpl (see 
        // issue 6101 (http://argouml.tigris.org/issues/show_bug.cgi?id=6101)).
        ProfileFacade.getManager().getUMLProfile().getProfilePackages();
        profileJava = new ProfileJava();
        profileJava.enable();

        // This shouldn't be necessary, but the Modeller is going to look in
        // the project to find the default type for attributes
        Project project = ProjectManager.getManager().makeEmptyProject();
        parsedModel = project.getUserDefinedModelList().get(0);
        Assert.assertNotNull("Creation of model failed.", parsedModel);

        modeller = new Modeller(parsedModel, profileJava,
                false, false, fileName);
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     * @throws Exception if a tear down step goes wrong
     */
    void tearDown() throws Exception {
        profileJava.disable();
        ProfileFacade.reset();
    }
}
