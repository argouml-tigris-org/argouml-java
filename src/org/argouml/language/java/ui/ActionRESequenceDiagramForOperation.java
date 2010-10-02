/* $Id$
 *****************************************************************************
 * Copyright (c) 2009 Contributors - see below
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    bobtarling
 *****************************************************************************
 *
 * Some portions of this file was previously release using the BSD License:
 */

// Copyright (c) 1996-2008 The Regents of the University of California. All
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

package org.argouml.language.java.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.argouml.i18n.Translator;
import org.argouml.kernel.Project;
import org.argouml.kernel.ProjectManager;
import org.argouml.model.Model;
import org.argouml.ui.targetmanager.TargetManager;
import org.argouml.uml.diagram.ArgoDiagram;
import org.argouml.uml.diagram.DiagramFactory;
import org.argouml.uml.diagram.DiagramSettings;
import org.argouml.uml.diagram.DiagramFactory.DiagramType;

/**
 * Action to reverse engineer a sequence diagram from the operation bodies.
 * 
 * @author Thomas Neustupny (thn@tigris.org)
 */
public class ActionRESequenceDiagramForOperation extends AbstractAction {

    /**
     * The UID.
     */
    private static final long serialVersionUID = -7836156263016892787L;

    /**
     * The constructor. If a figure is given, then it is invoked inside of a
     * sequence diagram, so it will work with this diagram. If figure is null,
     * then this causes the creation of a new sequence diagram.
     */
    public ActionRESequenceDiagramForOperation() {
        super(Translator.localize("java.action.create-seq-operation"));
    }

    /*
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        final Object target = TargetManager.getInstance().getTarget();
        if (!Model.getFacade().isAOperation(target)) {
            return;
        }
        buildSequenceDiagram(target);
    }

    /*
     * @see javax.swing.Action#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        // TODO: the following is not invoked when showing the menu item, so
        // it need to be made work:
        // Object target = TargetManager.getInstance().getModelTarget();
        // if (Model.getFacade().isAOperation(target)) {
        // //Model.getFacade().getBody(target);
        // return true;
        // }
        return true;
    }

    /**
     * Builds the sequence diagram for an operation.
     * <p>
     * TODO: find a better place for a similar method.
     */
    private void buildSequenceDiagram(Object operation) {
        // create an empty diagram for the operations owner
        Object classifier = Model.getFacade().getOwner(operation);
        Object collaboration = Model.getCollaborationsFactory()
                .buildCollaboration(classifier);
        Project project = ProjectManager.getManager().getOpenProjects().get(0);
        DiagramSettings settings =
            project.getProjectSettings().getDefaultDiagramSettings();
        final ArgoDiagram diagram = DiagramFactory.getInstance().create(
                DiagramType.Sequence, collaboration,
                settings);
        // create an anonymous classifier role in the diagram
        // Object node = 
        Model.getCollaborationsFactory().buildClassifierRole(collaboration);
        //Fig crFig =
        //    new FigClassifierRole(node,
        //        new Rectangle(10, 10, 50, 200), settings);
        //diagram.add(crFig);
        //((MutableGraphModel)
        //        (diagram.getGraphModel())).addNode(node);
        // create a classifier role for the operations owner
        // create a message between the two CRs for the operation
        // add diagram to project and show it
        project.addMember(diagram);
        TargetManager.getInstance().setTarget(diagram);
        // Object model = project.getUserDefinedModelList().get(0);
    }
}
