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

package org.argouml.language.java.reveng;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.argouml.application.api.Argo;
import org.argouml.configuration.Configuration;
import org.argouml.configuration.ConfigurationKey;
import org.argouml.i18n.Translator;
import org.argouml.uml.reveng.ImportClassLoader;
import org.argouml.uml.reveng.Setting;
import org.argouml.uml.reveng.SettingsTypes;

/**
 * Singleton class for managing the settings for the Java sources and classfile
 * import. These settings objects implement the SettingsTypes.* interfaces.
 * <p>
 * The caller must determine what interface an object is implementing iterating
 * the interfaces SettingsTypes.*
 * <p>
 * This is done this way to eliminate the need to use GUI elements. The settings
 * can easily be mapped into any GUI elements, this way we are independent from
 * the type of GUI.
 * 
 * @author Thomas Neustupny <thn@tigris.org>
 */
public class JavaImportSettings {

    private static JavaImportSettings theInstance;

    private List<SettingsTypes.Setting> settingsList;

    private SettingsTypes.UniqueSelection2 attributeSetting;

    private SettingsTypes.UniqueSelection2 datatypeSetting;

    private SettingsTypes.PathListSelection pathlistSetting;

    /**
     * Key for RE extended settings: model attributes as: 0: attributes 1:
     * associations
     */
    private static final ConfigurationKey
    KEY_IMPORT_EXTENDED_MODEL_ATTR =
        Configuration
            .makeKey("import", "extended", "java", "model", "attributes");

    /**
     * Key for RE extended settings: model arrays as: 0: datatype 1:
     * associations
     */
    private static final ConfigurationKey
    KEY_IMPORT_EXTENDED_MODEL_ARRAYS =
        Configuration
            .makeKey("import", "extended", "java", "model", "arrays");

    /**
     * Key for RE extended settings: flag for modeling of listed collections, if
     * to model them as associations with multiplicity *.
     */
    private static final ConfigurationKey
    KEY_IMPORT_EXTENDED_COLLECTIONS_FLAG =
        Configuration
            .makeKey("import", "extended", "java", "collections", "flag");

    /**
     * Key for RE extended settings: list of collections, that will be modelled
     * as associations with multiplicity *.
     */
    private static final ConfigurationKey
    KEY_IMPORT_EXTENDED_COLLECTIONS_LIST =
        Configuration
            .makeKey("import", "extended", "java", "collections", "list");

    /**
     * Key for RE extended settings: flag for modeling of listed collections,
     * if to model them as ordered associations with multiplicity *.
     */
    private static final ConfigurationKey
    KEY_IMPORT_EXTENDED_ORDEREDCOLLS_FLAG =
        Configuration
            .makeKey("import", "extended", "java", "orderedcolls", "flag");

    /**
     * Key for RE extended settings: list of collections, that will be modelled
     * as ordered associations with multiplicity *.
     */
    private static final ConfigurationKey
    KEY_IMPORT_EXTENDED_ORDEREDCOLLS_LIST =
        Configuration
            .makeKey("import", "extended", "java", "orderedcolls", "list");

    /**
     * Gets the singleton instance.
     * 
     * @return the instance
     */
    public static synchronized JavaImportSettings getInstance() {
        if (theInstance == null) {
            theInstance = new JavaImportSettings();
        }
        return theInstance;
    }

    /**
     * Returns if references should be modeled as UML Attributes instead of UML
     * Associations.
     * 
     * @return true if references should be modeled as UML Attributes instead of
     *         UML Associations.
     */
    public boolean isAttributeSelected() {
        return attributeSetting.getSelection() == 0;
    }

    /**
     * Returns if arrays should be modeled as datatypes instead of using UML's
     * multiplicities.
     * 
     * @return true if arrays should be modeled as datatypes instead of using
     *         UML's multiplicities.
     */
    public boolean isDatatypeSelected() {
        return datatypeSetting.getSelection() == 0;
    }

    /**
     * Returns the path list as a list of String objects.
     * 
     * @return the path list
     */
    public List<String> getPathList() {
        if (pathlistSetting == null) {
            return new ArrayList<String>();
        }
        return pathlistSetting.getPathList();
    }

    /*
     * Provides the implementation of
     * org.argouml.uml.reveng.ImportInterface#getImportSettings() for
     * implementors of ImportInterface.
     * 
     * @return the list of import settings
     */
    public List<SettingsTypes.Setting> getImportSettings() {

        settingsList = new ArrayList<SettingsTypes.Setting>();

        // Settings from ConfigPanelExtension
        List<String> options = new ArrayList<String>();
        options.add(Translator.localize("action.import-java-UML-attr"));
        options.add(Translator.localize("action.import-java-UML-assoc"));

        int selected;
        String modelattr = Configuration.getString(
                KEY_IMPORT_EXTENDED_MODEL_ATTR, "0");
        selected = Integer.parseInt(modelattr);

        attributeSetting = new Setting.UniqueSelection(Translator
                .localize("action.import-java-attr-model"), options, selected);
        settingsList.add(attributeSetting);

        options.clear();
        options.add(Translator
                .localize("action.import-java-array-model-datatype"));
        options
                .add(Translator
                        .localize("action.import-java-array-model-multi"));

        String modelarrays = Configuration.getString(
                KEY_IMPORT_EXTENDED_MODEL_ARRAYS, "0");
        selected = Integer.parseInt(modelarrays);

        datatypeSetting = new Setting.UniqueSelection(Translator
                .localize("action.import-java-array-model"), options, selected);
        settingsList.add(datatypeSetting);

        List<String> paths = new ArrayList<String>();
        URL[] urls = ImportClassLoader.getURLs(Configuration.getString(
                Argo.KEY_USER_IMPORT_CLASSPATH, ""));

        for (URL url : urls) {
            paths.add(url.getFile());
        }
        pathlistSetting = new Setting.PathListSelection(Translator
                .localize("dialog.import.classpath.title"), Translator
                .localize("dialog.import.classpath.text"), paths);
        settingsList.add(pathlistSetting);

        return settingsList;
    }

    /**
     * Saves the settings in the configuration.
     */
    public void saveSettings() {
        if (attributeSetting != null) {
            Configuration.setString(KEY_IMPORT_EXTENDED_MODEL_ATTR, String
                .valueOf(attributeSetting.getSelection()));
        }
        if (datatypeSetting != null) {
            Configuration.setString(KEY_IMPORT_EXTENDED_MODEL_ARRAYS, String
                .valueOf(datatypeSetting.getSelection()));
        }
    }

}
