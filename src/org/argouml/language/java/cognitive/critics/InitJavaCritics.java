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

// Copyright (c) 2007-2008 The Regents of the University of California. All
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

package org.argouml.language.java.cognitive.critics;

import java.util.Collections;
import java.util.List;

import org.argouml.application.api.AbstractArgoJPanel;
import org.argouml.application.api.GUISettingsTabInterface;
import org.argouml.application.api.InitSubsystem;
import org.argouml.cognitive.Agency;
import org.argouml.cognitive.Critic;
import org.argouml.language.java.JavaModuleGlobals;
import org.argouml.model.Model;
import org.argouml.moduleloader.ModuleInterface;

/**
 * Initialise the Java related critics. <p>
 * 
 * TODO: Test and then enable the CrReservedNameJava.
 *
 * @author Michiel, Thomas Neustupny
 */
public class InitJavaCritics implements InitSubsystem, ModuleInterface {

    private static Critic crMultiInherit = new CrMultipleInheritance();
    private static Critic crMultiRealization = new CrMultipleRealization();
    //private static Critic crReservedNameJava = new CrReservedNameJava();

    public List<GUISettingsTabInterface> getProjectSettingsTabs() {
        return Collections.emptyList();
    }

    public List<GUISettingsTabInterface> getSettingsTabs() {
        return Collections.emptyList();
    }

    public void init() {
        Object classCls = Model.getMetaTypes().getUMLClass();
        Object interfaceCls = Model.getMetaTypes().getInterface();
        Agency.register(crMultiInherit, classCls);
        Agency.register(crMultiRealization, interfaceCls);
        //Agency.register(crReservedNameJava, interfaceCls);
    }

    public List<AbstractArgoJPanel> getDetailsTabs() {
        return Collections.emptyList();
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#getName()
     */
    public String getName() {
        return "JavaCritics";
    }
    
    /*
     * @see org.argouml.moduleloader.ModuleInterface#getInfo(int)
     */
    public String getInfo(int type) {
        switch (type) {
        case DESCRIPTION:
            return "Java related critics";
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
     * Initialize the Java critics.
     * 
     * @see org.argouml.moduleloader.ModuleInterface#enable()
     */
    public boolean enable() {
        (new InitJavaCritics()).init();
        return true;
    }

    /*
     * Not supported.  Always returns false.
     * 
     * @see org.argouml.moduleloader.ModuleInterface#disable()
     */
    public boolean disable() {
        return false;
    }
}
