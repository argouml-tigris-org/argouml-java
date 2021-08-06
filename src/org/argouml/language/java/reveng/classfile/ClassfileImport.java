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

package org.argouml.language.java.reveng.classfile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.argouml.kernel.Project;
import org.argouml.language.java.JavaModuleGlobals;
import org.argouml.language.java.reveng.JavaImportSettings;
import org.argouml.moduleloader.ModuleInterface;
import org.argouml.profile.Profile;
import org.argouml.taskmgmt.ProgressMonitor;
import org.argouml.uml.reveng.FileImportUtils;
import org.argouml.uml.reveng.ImportInterface;
import org.argouml.uml.reveng.ImportSettings;
import org.argouml.uml.reveng.ImporterManager;
import org.argouml.uml.reveng.SettingsTypes;
import org.argouml.util.SuffixFilter;

import antlr.ANTLRException;
import antlr.RecognitionException;
import antlr.TokenStreamException;


/**
 * This is the main class for the classfile import.  It shares some logic with
 * the Java source importer including the Modeler and the settings logic.
 *
 * @author Andreas Rueckert <a_rueckert@gmx.net>
 */
public class ClassfileImport implements ModuleInterface, ImportInterface {

    /** The files that needs a second RE pass. */
    private Collection secondPassFiles;

    /**
     * Java profile model.
     */
    private Profile javaProfile = null;
    
    /**
     * New model elements that were added
     */
    private Collection newElements;

    private int fileCount;

    /*
     * @see org.argouml.uml.reveng.ImportInterface#parseFiles(org.argouml.kernel.Project, java.util.Collection, org.argouml.uml.reveng.ImportSettings, org.argouml.application.api.ProgressMonitor)
     */
    public Collection parseFiles(Project p, Collection<File> files,
            ImportSettings settings, ProgressMonitor monitor)
        throws ImportException {

        secondPassFiles = new ArrayList();
        newElements = new HashSet();

        // get the Java profile from project, if available
        javaProfile = getJavaProfile(p);

        monitor.setMaximumProgress(countFiles(files));
        for (File file : files) {
            monitor.updateMainTask("Parsing file: " + file);
            if (monitor.isCanceled()) {
                break;
            }
            processFile(p, (File) file, monitor);
            monitor.updateProgress(fileCount++);
        }

        if (count2ndPassFiles(secondPassFiles) > 0 && !monitor.isCanceled()) {

            // Process all the files, that need a second pass.
            for (Object next : secondPassFiles) {
                try {
                    if (next instanceof Collection) {
                        do2ndJarPass(p, (Collection) next, monitor);
                    } else {
                        File nextFile = (File) next;
                        String fileName = nextFile.getName();
                        FileInputStream fis;
                        try {
                            fis = new FileInputStream(nextFile);
                        } catch (FileNotFoundException e) {
                            throw new ImportException(e);
                        }
                        // TODO: I18N
                        monitor.updateSubTask("Parsing class 2nd pass - "
                                + fileName);
                        if (monitor.isCanceled()) {
                            break;
                        }
                        parseFile(p, fis, fileName);
                        monitor.updateProgress(fileCount++);
                    }
                } catch (ANTLRException e) {
                    throw new ImportException(e);
                } catch (IOException e) {
                    throw new ImportException(e);
                }
            }
        }
        return newElements;
    }


    /**
     * Count all class files, including ones inside JAR files.
     *
     * @return The number of files to process
     */
    private int countFiles(Collection<File> files) {

        int total = 0;
        for (File f : files) {
            if (f.getName().endsWith(".jar")) {
                try {
                    for (Enumeration<JarEntry> e = (new JarFile(f)).entries();
                            e.hasMoreElements();) {
                        ZipEntry entry = e.nextElement();
                        if (!entry.isDirectory()
                                && entry.getName().endsWith(".class")) {
                            total++;
                        }
                    }
                } catch (IOException e) {
                    // Just count it as a normal file
                    total++;
                }
            } else {
                total++;
            }
        }
        return total;
    }


    /**
     * Count the files in the 2nd pass buffer. We can't just use size() because
     * the collection can contain nested collections which need to be counted
     * independently.  In a nested collection, the first entry is the name of
     * the JAR file, so we use size()-1.
     *
     * @param buffer
     *            The buffer with the files for the 2nd pass.
     */
    private int count2ndPassFiles(Collection buffer) {
	int nfiles = 0;

	for (Iterator i = secondPassFiles.iterator(); i.hasNext();) {
	    Object next = i.next();
	    nfiles += ((next instanceof Collection)
                ? ((Collection) next).size() - 1 : 1);
	}
	return nfiles;
    }

    /**
     * The main method for all parsing actions. It calls the
     * actual parser methods depending on the type of the
     * file.
     *
     * @param f The file or directory, we want to parse.
     * @throws ImportException containing nested exception with original error
     */
    private void processFile(Project p, File f, ProgressMonitor monitor)
        throws ImportException {

        monitor.updateMainTask("Importing " + f.getName());
        // Is this file a Jarfile?
        if ( f.getName().endsWith(".jar")) { //$NON-NLS-1$
            processJarFile(p, f, monitor);
        } else {
            String fileName = f.getName();
            try {    // Try to parse this file.
                InputStream is;
                try {
                    is = new FileInputStream(f);
                } catch (FileNotFoundException e) {
                    throw new ImportException(e);
                }
                parseFile(p, is, fileName);
            } catch (ANTLRException e) {
                // TODO: Is this still needed/appropriate? It looks like
                // Modeller has been changed so that it no longer throws
                // exceptions... - tfm
                secondPassFiles.add(f);
            }
        }

    }


    /**
     * Process a Jar file, that contains classfiles.
     *
     * @param f The Jar file.
     */
    private void processJarFile(Project p, File f, ProgressMonitor monitor)
        throws ImportException {

        JarFile jarfile;
        try {
            jarfile = new JarFile(f);
        } catch (IOException e) {
            throw new ImportException("IO exception opening Jar file: " + f, e);
        }

	// A second pass buffer just for this jar.
	Collection jarSecondPassFiles = new ArrayList();

	for ( Enumeration<JarEntry> e = jarfile.entries();
	        e.hasMoreElements(); ) {
	    JarEntry entry = e.nextElement();
	    String entryName = entry.getName();
	    if ( !entry.isDirectory()
                    && entryName.endsWith(".class")) { //$NON-NLS-1$
		try {
                    InputStream is;
                    try {
                        is = jarfile.getInputStream(entry);
                    } catch (IOException e1) {
                        // If this happens, something bad is going on ...
                        throw new ImportException(e1);
                    }
                    // TODO: I18N
                    monitor.updateSubTask("Parsing class - " + entryName);
                    if (monitor.isCanceled()) {
                        break;
                    }
		    parseFile(p, is, entryName);
		    monitor.updateProgress(fileCount++);
		} catch (ANTLRException e1) {
		    if (jarSecondPassFiles.isEmpty()) {
		        // If there are no files tagged for a second pass,
		        // add the jar file as the 1st element.
			jarSecondPassFiles.add(f);
		    }
		    // Store the entry to be parsed a 2nd time.
		    jarSecondPassFiles.add(entryName);
		}
            }
	}

	// If there are files to parse again, add the jar to the 2nd pass.
	if ( !jarSecondPassFiles.isEmpty()) {
	    secondPassFiles.add(jarSecondPassFiles);
        }

        try {
            jarfile.close();
        } catch (IOException e) {
            throw new ImportException("IO exception closing Jar file: " + f, e);
        }
    }

    /**
     * Do a 2nd pass on a Jar file.
     *
     * @param secondPassBuffer A buffer, that holds the jarfile and the names of
     *                the entries to parse again.
     * @throws TokenStreamException
     * @throws RecognitionException
     */
    private void do2ndJarPass(Project p, Collection secondPassBuffer,
            ProgressMonitor monitor) throws IOException, RecognitionException,
        TokenStreamException {
        if (!secondPassBuffer.isEmpty()) {
	    Iterator iterator = secondPassBuffer.iterator();
	    JarFile jarfile = new JarFile( (File) iterator.next());

	    while (iterator.hasNext()) {
		String filename = (String) iterator.next();
		// TODO: I18N
                monitor.updateSubTask("Parsing class 2nd pass - " + filename);
                if (monitor.isCanceled()) {
                    break;
                }
		parseFile(
		        p,
		        jarfile.getInputStream(jarfile.getEntry(filename)),
		        filename);
		monitor.updateProgress(fileCount++);
	    }
	    jarfile.close();
	}
    }

    /**
     * This method parses 1 Java classfile.
     *
     * @param p The current project.
     * @param is The inputStream for the file to parse.
     * @param fileName the name of the file to parse
     * @throws RecognitionException ANTLR parser error
     * @throws TokenStreamException ANTLR parser error
     */

    public void parseFile(Project p, InputStream is, String fileName)
        throws RecognitionException, TokenStreamException {

        int lastSlash = fileName.lastIndexOf('/');
	if (lastSlash != -1) {
	    fileName = fileName.substring(lastSlash + 1);
	}

        ClassfileParser parser =
                new ClassfileParser(new SimpleByteLexer(
                        new BufferedInputStream(is)));

        // start parsing at the classfile rule
        parser.classfile();

        // Create a modeller for the parser
        org.argouml.language.java.reveng.Modeller modeller =
            new org.argouml.language.java.reveng.Modeller(
                p.getUserDefinedModelList().get(0),
                javaProfile,
                JavaImportSettings.getInstance().isAttributeSelected(),
                JavaImportSettings.getInstance().isDatatypeSelected(),
                fileName);


	// do something with the tree
	ClassfileTreeParser tparser = new ClassfileTreeParser();
	tparser.classfile(parser.getAST(), modeller);
        newElements.addAll(modeller.getNewElements());

        // Was there an exception thrown during modelling?
        //Exception e = modeller.getException();
        //if(e != null) {
        //    throw e;
        //}
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#enable()
     */
    public boolean enable() {
        ImporterManager.getInstance().addImporter(this);
        return true;
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#disable()
     */
    public boolean disable() {
        return true;
    }

    /*
     * @see org.argouml.uml.reveng.FileImportSupport#getName()
     */
    public String getName() {
        // TODO: I18N
        return "Java from classes";
    }

    /*
     * @see org.argouml.moduleloader.ModuleInterface#getInfo(int)
     */
    public String getInfo(int type) {
        switch (type) {
        case AUTHOR:
            return JavaModuleGlobals.MODULE_AUTHOR;
        case DESCRIPTION:
            return "Java import from class or jar files";
        case VERSION:
            return JavaModuleGlobals.MODULE_VERSION;
        case ModuleInterface.DOWNLOADSITE:
            return JavaModuleGlobals.MODULE_DOWNLOADSITE;
        default:
            return null;
        }
    }

    /*
     * @see org.argouml.uml.reveng.FileImportSupport#getSuffixFilters()
     */
    public SuffixFilter[] getSuffixFilters() {
        SuffixFilter[] result = {
            // TODO: I18N
            new SuffixFilter(new String[] {"class", "jar"} , "Java files"),
            new SuffixFilter("class", "Java class files"),
            new SuffixFilter("jar", "Java JAR files"), };
	return result;
    }

    /*
     * @see org.argouml.uml.reveng.ImportInterface#isParseable(java.io.File)
     */
    public boolean isParseable(File file) {
        return FileImportUtils.matchesSuffix(file, getSuffixFilters());
    }

    /*
     * @see org.argouml.uml.reveng.ImportInterface#getImportSettings()
     */
    public List<SettingsTypes.Setting> getImportSettings() {
        return JavaImportSettings.getInstance().getImportSettings();
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












