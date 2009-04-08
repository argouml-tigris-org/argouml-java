// $Id$
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

import org.apache.log4j.Logger;
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
        "/org/argouml/language/java/profile/JavaUmlProfile.xmi";

    private static final String PROFILE_URL =
        "http://argouml-java.tigris.org/profile/JavaUmlProfile.xmi";

    private static final Logger LOG = Logger.getLogger(ProfileJava.class);

    static final String NAME = "Java";

    private ProfileModelLoader profileModelLoader;

    private Collection<Object> model;

    /**
     * The default constructor for this class
     * 
     * @throws ProfileException
     */
    @SuppressWarnings("unchecked")
    public ProfileJava() throws ProfileException {
        profileModelLoader = new ResourceModelLoader(ProfileJava.class);
        ProfileReference profileReference = null;
        try {
            URL profileURL = new URL(PROFILE_URL);
            profileReference = new ProfileReference(PROFILE_FILE, profileURL);
        } catch (MalformedURLException e) {
            throw new ProfileException(
                    "Exception while creating profile reference.", e);
        }
        model = profileModelLoader.loadModel(profileReference);

        if (model == null) {
            model = new ArrayList();
            model.add(Model.getModelManagementFactory().createModel());
        }

        addProfileDependency(ProfileFacade.getManager().getUMLProfile());
        addProfileDependency("CodeGeneration");
    }

    public String getDisplayName() {
        return NAME;
    }

    @Override
    public Collection<Object> getProfilePackages() {
        return model;
    }

    @Override
    public DefaultTypeStrategy getDefaultTypeStrategy() {
        return new DefaultTypeStrategy() {
            public Object getDefaultAttributeType() {
                return ModelUtils.findTypeInModel("int", model.iterator()
                        .next());
            }

            public Object getDefaultParameterType() {
                return ModelUtils.findTypeInModel("int", model.iterator()
                        .next());
            }

            public Object getDefaultReturnType() {
                return ModelUtils.findTypeInModel("void", model.iterator()
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
            return "The ArgoUML project team";
        case VERSION:
            return "0.29.1";
        case ModuleInterface.DOWNLOADSITE:
            return "http://argouml-java.tigris.org/";
        default:
            return null;
        }
    }
}
