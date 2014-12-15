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

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.argouml.application.api.AbstractArgoJPanel;
import org.argouml.ui.LookAndFeelMgr;

public class CodeSyncJPanel extends AbstractArgoJPanel {

    private static final Logger LOG =
        Logger.getLogger(CodeSyncJPanel.class.getName());

    private JTable table = new JTable(10, 1);

    /**
     * Generated serial version UID.
     */
    private static final long serialVersionUID = -3670136920429109101L;

    public CodeSyncJPanel() {
        super("java.codesync.tabname");
        table.setModel(new CodeSyncTableModel());
        table.setRowSelectionAllowed(false);

        JScrollPane sp = new JScrollPane(table);
        Font labelFont = LookAndFeelMgr.getInstance().getStandardFont();
        table.setFont(labelFont);

        JLabel titleLabel = new JLabel("Bla");
        setLayout(new BorderLayout());
    }
}
