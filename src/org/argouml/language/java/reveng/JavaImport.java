/* $Id$
 *****************************************************************************
 * Copyright (c) 2009-2013 Contributors - see below
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.CommonTokenStream;
import org.argouml.i18n.Translator;
import org.argouml.kernel.Project;
import org.argouml.language.java.JavaModuleGlobals;
import org.argouml.model.IllegalModelElementConnectionException;
import org.argouml.moduleloader.ModuleInterface;
import org.argouml.profile.Profile;
import org.argouml.taskmgmt.ProgressMonitor;
import org.argouml.uml.reveng.FileImportUtils;
import org.argouml.uml.reveng.ImportClassLoader;
import org.argouml.uml.reveng.ImportInterface;
import org.argouml.uml.reveng.ImportSettings;
import org.argouml.uml.reveng.ImporterManager;
import org.argouml.uml.reveng.SettingsTypes;
import org.argouml.uml.util.ModelUtil;
import org.argouml.util.SuffixFilter;

/**
 * This is the main class for Java reverse engineering. It's based on the Antlr
 * Java example.
 * 
 * @author Andreas Rueckert <a_rueckert@gmx.net>
 */
public class JavaImport implements ModuleInterface, ImportInterface {

    /** Logger. */
    private static final Logger LOG =
        Logger.getLogger(JavaImport.class.getName());

    /**
     * Java profile model.
     */
    private Profile javaProfile = null;

    /**
     * New model elements that were added
     */
    private Collection<Object> newElements;

    /*
     * @see org.argouml.uml.reveng.ImportInterface#parseFiles(org.argouml.kernel.Project,
     *      java.util.Collection, org.argouml.uml.reveng.ImportSettings,
     *      org.argouml.application.api.ProgressMonitor)
     */
    public Collection parseFiles(Project p, Collection<File> files,
            ImportSettings settings, ProgressMonitor monitor)
        throws ImportException {

        JavaImportSettings.getInstance().saveSettings();
        updateImportClassloader();
        newElements = new HashSet<Object>();
        monitor.updateMainTask(Translator.localize("dialog.import.pass1"));

        // get the Java profile from project, if available
        javaProfile = getJavaProfile(p);

        try {
            if ((settings.getImportLevel() 
                    == ImportSettings.DETAIL_CLASSIFIER_FEATURE)
                || settings.getImportLevel() == ImportSettings.DETAIL_FULL) {
                monitor.setMaximumProgress(files.size() * 2);
                doImportPass(p, files, settings, monitor, 0, 0);
                if (!monitor.isCanceled()) {
                    monitor.updateMainTask(Translator
                            .localize("dialog.import.pass2"));
                    doImportPass(p, files, settings, monitor, files.size(), 1);
                }
            } else {
                monitor.setMaximumProgress(files.size() * 2);
                doImportPass(p, files, settings, monitor, 0, 0);
            }
            
            ModelUtil.generatePackageDependencies(p);
        } catch (IllegalModelElementConnectionException e) {
        } finally {
            //this prevents parse problems to be displayed, so I disabled it:
            // --thn
            //monitor.close();
        }
        return newElements;
    }

    private void doImportPass(Project p, Collection<File> files,
            ImportSettings settings, ProgressMonitor monitor, int startCount,
            int pass) {

        int count = startCount;
        for (File file : files) {
            if (monitor.isCanceled()) {
                monitor.updateSubTask(Translator
                        .localize("dialog.import.cancelled"));
                return;
            }
            try {
                parseFile(p, file, settings, pass);
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new java.io.PrintWriter(sw);
                e.printStackTrace(pw);
                monitor.notifyMessage(
                    Translator.localize(
                            "dialog.title.import-problems"), //$NON-NLS-1$
                    Translator.localize("label.import-problems"), //$NON-NLS-1$
                    sw.toString());
                if (monitor.isCanceled()) {
                    break;
                }
            }
            monitor.updateProgress(count++);
            monitor.updateSubTask(Translator.localize(
                    "dialog.import.parsingAction", 
                    new Object[] { 
                        file.getAbsolutePath() 
                    }));
        }
    }

    /**
     * Do a single import pass of a single file.
     * 
     * @param p the project
     * @param f the source file
     * @param settings the user provided import settings
     * @param pass current import pass - 0 = single pass, 1 = pass 1 of 2, 2 =
     *                pass 2 of 2
     */
    private void parseFile(Project p, File f, ImportSettings settings, int pass)
        throws ImportException {

        try {
            // Create a scanner that reads from the input stream
            String encoding = settings.getInputSourceEncoding();
            FileInputStream in = new FileInputStream(f);
            InputStreamReader isr;
            try {
                isr = new InputStreamReader(in, encoding);
            } catch (UnsupportedEncodingException e) {
                // fall back to default encoding
                isr = new InputStreamReader(in);
            }
            JavaLexer lexer = new JavaLexer(new ANTLRReaderStream(isr));

            // Create a parser that reads from the scanner
            JavaParser parser = new JavaParser(new CommonTokenStream(lexer));

            // Pass == 0 means single pass recognition
            int parserMode = JavaParser.MODE_IMPORT_PASS1
                    | JavaParser.MODE_IMPORT_PASS2;
            if (pass == 0) {
                parserMode = JavaParser.MODE_IMPORT_PASS1;
            } else if (pass == 1) {
                parserMode = JavaParser.MODE_IMPORT_PASS2;
            }
            parser.setParserMode(parserMode);

            // Create a modeller for the parser
            Modeller modeller = new Modeller(
                    p.getUserDefinedModelList().get(0), javaProfile,
                    JavaImportSettings.getInstance().isAttributeSelected(),
                    JavaImportSettings.getInstance().isDatatypeSelected(), f
                            .getName());

            // Print the name of the current file, so we can associate
            // exceptions to the file.
            LOG.info("Parsing " + f.getAbsolutePath());

            // Calculate the import level
            int level = 0;
            int importlevel = settings.getImportLevel();
            if (importlevel == ImportSettings.DETAIL_CLASSIFIER_FEATURE) {
                level = 1;
            } else if (importlevel == ImportSettings.DETAIL_FULL) {
                // full level only needed for the second pass
                level = (pass == 0) ? 0 : 2;
            }
            modeller.setAttribute("level", Integer.valueOf(level));

            try {
                // start parsing at the compilationUnit rule
                parser.compilationUnit(modeller, lexer);
            } catch (Exception e) {
                String errorString = buildErrorString(f);
                LOG.log(Level.SEVERE, 
                        e.getClass().getName() + errorString,
                        e);
                throw new ImportException(errorString, e);
            } finally {
                newElements.addAll(modeller.getNewElements());
                in.close();
            }
        } catch (IOException e) {
            throw new ImportException(buildErrorString(f), e);
        }
    }

    private String buildErrorString(File f) {
        String path = "";
        try {
            path = f.getCanonicalPath();
        } catch (IOException e) {
            // Just ignore - we'll use the simple file name
            LOG.log(Level.FINEST,
                    "Cannot get the Canonical Path, using name without path",
                    e);
        }
        return "Exception in file: " + path + " " + f.getName();
    }

    /*
     * @see org.argouml.uml.reveng.ImportInterface#getSuffixFilters()
     */
    public SuffixFilter[] getSuffixFilters() {
        SuffixFilter[] result = {
            new SuffixFilter("java", 
                    Translator.localize("java.filefilter.java")),
        };
        return result;
    }

    /*
     * @see org.argouml.uml.reveng.ImportInterface#isParseable(java.io.File)
     */
    public boolean isParseable(File file) {
        return FileImportUtils.matchesSuffix(file, getSuffixFilters());
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#getName()
     */
    public String getName() {
        return "Java";
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#getInfo(int)
     */
    public String getInfo(int type) {
        switch (type) {
        case DESCRIPTION:
            return "Java import from Java files.";
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

    /*
     * @see org.argouml.moduleloader.ModuleInterface#disable()
     */
    public boolean disable() {
        // We are permanently enabled
        return false;
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#enable()
     */
    public boolean enable() {
        ImporterManager.getInstance().addImporter(this);
        return true;
    }

    /*
     * @see org.argouml.uml.reveng.ImportInterface#getImportSettings()
     */
    public List<SettingsTypes.Setting> getImportSettings() {
        return JavaImportSettings.getInstance().getImportSettings();
    }

    private void updateImportClassloader() {
        List<String> pathList = JavaImportSettings.getInstance().getPathList();
        URL[] urls = new URL[pathList.size()];

        int i = 0;
        for (String path : pathList) {
            try {
                urls[i++] = new File(path).toURI().toURL();
            } catch (MalformedURLException e) {
                LOG.severe("Bad path in classpath " + path);
            }
        }

        try {
            ImportClassLoader.getInstance(urls);
            ImportClassLoader.getInstance().saveUserPath();
        } catch (MalformedURLException e) {
            LOG.log(Level.FINEST, "Bad path in classpaths", e);
        }
    }

    /**
     * Get the Java profile from project, if available.
     * 
     * @param p the project
     * @return the Java profile
     */
    private Profile getJavaProfile(Project p) {
        for (Profile profile : p.getProfileConfiguration().getProfiles()) {
            if ("Java".equals(profile.getDisplayName())) {
                return profile;
            }
        }
        return null;
    }
}
