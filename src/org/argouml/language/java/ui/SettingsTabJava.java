/* $Id$
 *****************************************************************************
 * Copyright (c) 2009-2013 Contributors - see below
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    tfmorris
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.argouml.application.api.GUISettingsTabInterface;
import org.argouml.i18n.Translator;
import org.argouml.moduleloader.ModuleInterface;
import org.argouml.ui.GUI;


/**
 * Settings tab for the Java generator module.
 */
public class SettingsTabJava implements ModuleInterface, GUISettingsTabInterface
{
    private static final String REVISION_DATE =
        "$Date$"; //$NON-NLS-1$

    private static final Logger LOG =
        Logger.getLogger(SettingsTabJava.class.getName());

    private JPanel topPanel;
    private JSpinner indent;
    private JCheckBox verboseDocs;
    private JCheckBox lfBeforeCurly;
    private JCheckBox headerGuardUpperCase;
    private JCheckBox headerGuardGUID;

    /*
     * Build the panel to be used for our settings tab.
     */
    private JPanel buildPanel() {
        LOG.fine("SettingsTabJava being created...");
        JPanel top = new JPanel();
        top.setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridy = GridBagConstraints.RELATIVE;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(0, 30, 0, 5);

        // adds indent width spinner
        JLabel label = new JLabel(Translator.localize("java.indent"));
        // The actual value is loaded in handleSettingsTabRefresh()
        Integer spinVal = Integer.valueOf(4);
        Integer spinMin = Integer.valueOf(0);
        Integer spinStep = Integer.valueOf(1);
        indent = new JSpinner(
                new SpinnerNumberModel(spinVal, spinMin, null, spinStep));
        label.setLabelFor(indent);

        JPanel indentPanel = new JPanel();
        indentPanel.setLayout(new BoxLayout(indentPanel, BoxLayout.LINE_AXIS));
        indentPanel.add(label);
        indentPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        indentPanel.add(indent);
        indentPanel.add(Box.createHorizontalGlue());
        panel.add(indentPanel, constraints);

        verboseDocs = new JCheckBox(Translator.localize("java.verbose-docs"));
        panel.add(verboseDocs, constraints);

        lfBeforeCurly = new JCheckBox(Translator
                .localize("java.lf-before-curly"));
        panel.add(lfBeforeCurly, constraints);

        headerGuardUpperCase = new JCheckBox(Translator
                .localize("java.header-guard-case"));
        panel.add(headerGuardUpperCase, constraints);

        headerGuardGUID = new JCheckBox(Translator
                .localize("java.header-guard-guid"));
        panel.add(headerGuardGUID, constraints);

        // TODO: add more options

        top.add(panel, BorderLayout.NORTH);

        LOG.fine("SettingsTabJava created!");
        return top;
    }

    /*
     * @see org.argouml.ui.GUISettingsTabInterface#handleSettingsTabSave()
     */
    public void handleSettingsTabSave() {
		/*
        GeneratorJava gen = GeneratorJava.getInstance();
        int indWidth = ((Integer) indent.getValue()).intValue();
        gen.setIndent(indWidth);
        gen.setLfBeforeCurly(lfBeforeCurly.isSelected());
        gen.setVerboseDocs(verboseDocs.isSelected());
        gen.setHeaderGuardUpperCase(headerGuardUpperCase.isSelected());
        gen.setHeaderGuardGUID(headerGuardGUID.isSelected());
        */
    }

    /*
     * @see org.argouml.ui.GUISettingsTabInterface#handleSettingsTabCancel()
     */
    public void handleSettingsTabCancel() {
    }

    /*
     * @see org.argouml.ui.GUISettingsTabInterface#handleSettingsTabRefresh()
     */
    public void handleSettingsTabRefresh() {
		/*
        GeneratorJava gen = GeneratorJava.getInstance();
        lfBeforeCurly.setSelected(gen.isLfBeforeCurly());
        verboseDocs.setSelected(gen.isVerboseDocs());
        indent.setValue(Integer.valueOf(gen.getIndent()));
        headerGuardUpperCase.setSelected(gen.isHeaderGuardUpperCase());
        headerGuardGUID.setSelected(gen.isHeaderGuardGUID());
        */
    }

    /*
     * @see org.argouml.ui.GUISettingsTabInterface#handleResetToDefault()
     */
    public void handleResetToDefault() {
        // Do nothing - these buttons are not shown.
    }

    /*
     * @see org.argouml.ui.GUISettingsTabInterface#getTabKey()
     */
    public String getTabKey() { return "java.tabname"; }

    /*
     * @see org.argouml.ui.GUISettingsTabInterface#getTabPanel()
     */
    public JPanel getTabPanel() {
        // defer building this until needed
        if (topPanel == null) {
            topPanel = buildPanel();
        }
        return topPanel;
    }


    /*
     * @see org.argouml.moduleloader.ModuleInterface#getName()
     */
    public String getName() {
        return "SettingsTabJava";
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#getInfo(int)
     */
    public String getInfo(int type) {
        switch (type) {
        case ModuleInterface.DESCRIPTION:
            return "Java Generator Settings";
        case ModuleInterface.AUTHOR:
            return "The ArgoUML project team";
        case ModuleInterface.VERSION:
            return "0.28";
        case ModuleInterface.DOWNLOADSITE:
            return "http://argouml-java.tigris.org/";
        default:
            return null;
        }
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#enable()
     */
    public boolean enable() {
        GUI.getInstance().addSettingsTab(this);
        return true;
    }

    /*
     * Does nothing.  Settings tabs can't be removed after they've been added.
     *
     * @see org.argouml.moduleloader.ModuleInterface#disable()
     */
    public boolean disable() {
        return false;
    }

}
