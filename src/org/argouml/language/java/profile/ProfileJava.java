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

package org.argouml.language.java.profile;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.log4j.Logger;
import org.argouml.language.java.JavaModuleGlobals;
import org.argouml.model.Model;
import org.argouml.moduleloader.ModuleInterface;
import org.argouml.profile.DefaultTypeStrategy;
import org.argouml.profile.Profile;
import org.argouml.profile.ProfileException;
import org.argouml.profile.ProfileFacade;
import org.argouml.profile.ProfileModelLoader;
import org.argouml.profile.ProfileReference;
import org.argouml.profile.ResourceModelLoader;

/**
 * This class represents the Java default Profile. This being a module, provides
 * the ArgoUML Java module to offer the Java profile to the application by
 * registering it on enable and unregistering on disable.
 * 
 * @author Marcos Aurelio
 * @author Thomas Neustupny (thn@tigris.org)
 */
public class ProfileJava extends Profile implements ModuleInterface {

    private static final String PROFILE_FILE =
        "/org/argouml/language/java/profile/default-java.xmi";

    private static final String PROFILE_URL =
        "http://argouml.org/profiles/uml14/default-java.xmi";

    private static final Logger LOG = Logger.getLogger(ProfileJava.class);

    static final String NAME = "Java";

    private Collection<Object> model = null;
    
    ProfileReference profileReference;

    /**
     * The default constructor for this class
     * 
     * @throws ProfileException
     */
    @SuppressWarnings("unchecked")
    public ProfileJava() throws ProfileException {
        try {
            URL profileURL = new URL(PROFILE_URL);
            profileReference = new ProfileReference(PROFILE_FILE, profileURL);
        } catch (MalformedURLException e) {
            throw new ProfileException(
                    "Exception while creating profile reference.", e);
        }
        addProfileDependency(ProfileFacade.getManager().getUMLProfile());
        addProfileDependency("CodeGeneration");
    }

    private Collection<Object> getModel() {
        if (model == null) {
            ProfileModelLoader profileModelLoader = 
                new ResourceModelLoader(ProfileJava.class);
            try {
                model = profileModelLoader.loadModel(profileReference);
            } catch (ProfileException e) {
                LOG.error("Exception loading profile file " + PROFILE_FILE, e);
            }

            if (model == null) {
                model = getFallbackModel();
                LOG.error("Using fallback profile");
            }
        }
        return model;
    }

    private Collection getFallbackModel() {
        Collection result = new ArrayList();
        Object profile = Model.getModelManagementFactory().createModel();
        Model.getCoreHelper().setName(profile,"Fallback Java profile");
        result.add(profile);
        return result;
    }

    public String getDisplayName() {
        return NAME;
    }

    @Override
    public Collection<Object> getProfilePackages() {
        return getModel();
    }

    @Override
    public Collection<Object> getLoadedPackages() {
        if (model == null) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableCollection(model);
        }
    }
    
    @Override
    public DefaultTypeStrategy getDefaultTypeStrategy() {
        return new DefaultTypeStrategy() {
            public Object getDefaultAttributeType() {
                return ModelUtils.findTypeInModel("int", getModel().iterator()
                        .next());
            }

            public Object getDefaultParameterType() {
                return ModelUtils.findTypeInModel("int", getModel().iterator()
                        .next());
            }

            public Object getDefaultReturnType() {
                return ModelUtils.findTypeInModel("void", getModel().iterator()
                        .next());
            }

        };
    }

    /**
     * @return <code>false</code> either if constructing the {@link ProfileJava}
     *         or registering it fails.
     * @see org.argouml.moduleloader.ModuleInterface#enable()
     */
    public boolean enable() {
        try {
            ProfileFacade.register(this);
        } catch (Exception e) {
            LOG.error("Failed to enable the Java profile.", e);
            return false;
        }
        return true;
    }

    /**
     * @return <code>false</code> if unregistering the ProfileJava fails.
     * @see org.argouml.moduleloader.ModuleInterface#disable()
     */
    public boolean disable() {
        boolean removed = false;
        try {
            ProfileFacade.remove(this);
            removed = true;
        } catch (Exception e) {
            LOG.error("Failed to remove the Java profile.", e);
        }
        return removed;
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#getName()
     */
    public String getName() {
        return "Java profile";
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#getInfo(int)
     */
    public String getInfo(int type) {
        switch (type) {
        case DESCRIPTION:
            return "Java profile.";
        case AUTHOR:
            return JavaModuleGlobals.MODULE_AUTHOR;
        case VERSION:
            return JavaModuleGlobals.MODULE_VERSION;
        case ModuleInterface.DOWNLOADSITE:
            return JavaModuleGlobals.MODULE_DOWNLOADSITE;
        default:
            return null;
        }
    }
}
