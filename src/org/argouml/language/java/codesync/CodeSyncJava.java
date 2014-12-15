/* $Id$
 *******************************************************************************
 * Copyright (c) 2014 Contributors - see below
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thomas Neustupny
 *******************************************************************************
 */

package org.argouml.language.java.codesync;

import javax.swing.JPanel;

import org.argouml.language.java.JavaModuleGlobals;
import org.argouml.language.java.codesync.ui.CodeSyncJPanel;
import org.argouml.moduleloader.ModuleInterface;
import org.argouml.ui.DetailsPane;
import org.argouml.ui.ProjectBrowser;

public class CodeSyncJava implements ModuleInterface {
    
    private final static JPanel codesyncPane = new CodeSyncJPanel();

    @Override
    public boolean enable() {
        DetailsPane dp = (DetailsPane) ProjectBrowser.getInstance().getDetailsPane();
        dp.addTab(codesyncPane, true);
        return true;
    }

    @Override
    public boolean disable() {
        DetailsPane dp = (DetailsPane) ProjectBrowser.getInstance().getDetailsPane();
        dp.removeTab(codesyncPane);
        return true;
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#getName()
     */
    public String getName() {
        return "Java code sync";
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#getInfo(int)
     */
    public String getInfo(int type) {
        switch (type) {
        case DESCRIPTION:
            return "Java code synchronization tab.";
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
