// $Id$
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

package org.argouml.language.java.profile;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.argouml.i18n.Translator;
import org.argouml.moduleloader.ModuleInterface;
import org.argouml.profile.Profile;
import org.argouml.profile.ProfileFacade;

/**
 * This is the UML profile for the Java module. This being a module, provides
 * the ArgoUML Java module to offer the Java profile to the application by
 * registering it on enable and unregistering on disable.
 *
 * @author Luis Sergio Oliveira (euluis@tigris.org)
 * @author Thomas Neustupny (thn@tigris.org)
 */
public class ProfileModule implements ModuleInterface {

    private static final Logger LOG = Logger.getLogger(ProfileModule.class);

    private Profile profileJava;

    private Map<Integer, String> moduleInfo;

    private String moduleName;

    /**
     * @return <code>false</code> either if constructing the
     *         {@link ProfileJava} or registering it fails.
     * @see org.argouml.moduleloader.ModuleInterface#enable()
     */
    public boolean enable() {
        try {
            profileJava = new ProfileJava();
            register(profileJava);
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
        boolean removed = profileJava == null;
        if (!removed) {
            try {
                remove(profileJava);
                removed = true;
            } catch (Exception e) {
                LOG.error("Failed to remove the Java profile.", e);
            }
            profileJava = null;
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
            return "0.28.beta";
        case ModuleInterface.DOWNLOADSITE:
            return "http://argouml-java.tigris.org/";
        default:
            return null;
        }
    }

    void register(Profile profile) {
        ProfileFacade.register(profile);
    }

    void remove(Profile profile) {
        ProfileFacade.remove(profile);
    }

}
