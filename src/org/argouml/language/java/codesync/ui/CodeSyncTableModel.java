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

package org.argouml.language.java.codesync.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.argouml.kernel.Project;
import org.argouml.kernel.ProjectManager;
import org.argouml.model.Model;
import org.argouml.uml.generator.GeneratorManager;

/**
 * This class implements the table model for the code sync tab. The table
 * mainly consists of two lists. The left list contains all model elements
 * marked for code generation, whereas the right list contains the
 * corresponding files. Other data in this model is for the maintenance of the
 * synchronization state.
 *
 * @author Thomas Neustupny
 */
public class CodeSyncTableModel extends AbstractTableModel {
    
    List<Object> classes = new ArrayList<Object>();

    /**
     * Generated serial version UID.
     */
    private static final long serialVersionUID = -3746764405327100427L;

    public CodeSyncTableModel() {
        classes.clear();
        Project p = ProjectManager.getManager().getCurrentProject();
        if (p != null) {
            for (Object ns : p.getUserDefinedModelList()) {
                //Model.getPump().addModelEventListener(this, ns);
                while (Model.getFacade().getNamespace(ns) != null) {
                    ns = Model.getFacade().getNamespace(ns);
                }
                Collection elems =
                    Model.getModelManagementHelper()
                        .getAllModelElementsOfKind(
                                ns,
                                Model.getMetaTypes().getClassifier());
                for (Object cls : elems) {
                    if (isCodeRelevantClassifier(cls)) {
                        classes.add(cls);
                    }
                }
            }
        }
    }

    @Override
    public int getRowCount() {
        return classes.size();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return classes.get(rowIndex);
    }

    /**
     * Checks if a given classifier is relevant for code generation.
     * TODO: move this as a static class to GeneratorManager.
     * 
     * @param cls the classifier that is candidate for generation
     * @return true if the candidate is sound
     */
    private boolean isCodeRelevantClassifier(Object cls) {
        if (cls == null) {
            return false;
        }
        if (!Model.getFacade().isAClass(cls)
                && !Model.getFacade().isAInterface(cls)) {
            return false;
        }
        String path = GeneratorManager.getCodePath(cls);
        String name = Model.getFacade().getName(cls);
        if (name == null
            || name.length() == 0
            || Character.isDigit(name.charAt(0))) {
            return false;
        }
        if (path != null) {
            return (path.length() > 0);
        }
        Object parent = Model.getFacade().getNamespace(cls);
        while (parent != null) {
            path = GeneratorManager.getCodePath(parent);
            if (path != null) {
                return (path.length() > 0);
            }
            parent = Model.getFacade().getNamespace(parent);
        }
        return false;
    }
}
