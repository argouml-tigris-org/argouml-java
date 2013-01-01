/* $Id$
 *****************************************************************************
 * Copyright (c) 2009-2013 Contributors - see below
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thomas Neustupny
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

/*
 JavaRE - Code generation and reverse engineering for UML and Java
 Copyright (C) 2000 Marcus Andersson andersson@users.sourceforge.net
 */

package org.argouml.language.java.reveng;

import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.argouml.model.Model;
import org.argouml.profile.Profile;
import org.argouml.uml.reveng.ImportClassLoader;

/**
 * The context is the current available namespaces via import in the class that
 * is currently parsed. It is non mutable and a new context can be based on the
 * current context with an additional namespace.
 */
abstract class Context {
    private static final Logger LOG =
        Logger.getLogger(Context.class.getName());

    /** The parent context. May be null. */
    private Context context;

    /**
     * Create a new context.
     * 
     * @param base Based on this context, may be null.
     */
    public Context(Context base) {
        context = base;
    }

    /**
     * Get a classifier from the model. If it is not in the model, try to find
     * it with the CLASSPATH. If found, in the classpath, the classifier is
     * created and added to the model. If not found at all, a datatype is
     * created and added to the model.
     * 
     * @param name The name of the classifier to find.
     * @param interfacesOnly Filter for interfaces only.
     * @param profile The Java profile or null.
     * @return Found classifier.
     */
    public abstract Object get(String name, boolean interfacesOnly,
            Profile profile) throws ClassifierNotFoundException;

    /**
     * Get the complete java name for a package.
     * 
     * @param mPackage The package.
     * @return Package name in java format
     */
    protected String getJavaName(Object mPackage) {
        if (mPackage == null) {
            return "";
        }
        Object parent = Model.getFacade().getNamespace(mPackage);
        if (Model.getFacade().isAModel(parent)) {
            return Model.getFacade().getName(mPackage);
        } else if (parent != null) {
            return getJavaName(parent) + "."
                    + Model.getFacade().getName(mPackage);
        } else {
            return "";
        }
    }

    /**
     * @param c The context to set.
     */
    protected void setContext(Context c) {
        this.context = c;
    }

    /**
     * @return Returns the context.
     */
    protected Context getContext() {
        return context;
    }

    protected Class<?> findClass(String name, boolean interfacesOnly) {
        // Classpath lookup was inherited from the program JavaRE from Marcus
        // Andersson, which was the basis for the Java RE in ArgoUML. JavaRE
        // had no configurable user classpath and thus had to find the class
        // or interface via Class.forName(name) (catching
        // ClassNotFoundException and LinkageError, and the case that a class
        // is returned but interfacesOnly==true, in all these cases the
        // following method call is made anyway). For a very long time, the
        // lookup in the ArgoUML classpath via Class.forName(name) was left
        // in the code, but it was not needed because a user classpath can be
        // configured, so only this is looked up, and all that remains is:
        return findClassOnUserClasspath(name, interfacesOnly);
    }

    private Class<?> findClassOnUserClasspath(String name,
            boolean interfacesOnly) {
        Class<?> clazz = null;
        try {
            clazz = ImportClassLoader.getInstance().loadClass(name);
        } catch (MalformedURLException e) {
            // TODO: Need to make this visible to the user
            LOG.log(Level.WARNING, "Classpath configuration error", e);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (LinkageError e) {
            // We found the class, but we couldn't load it for some reason
            // most likely a missing dependency on the class path
            // TODO: Need to make this visible to the user
            LOG.log(Level.WARNING,
                    "Linkage error loading found class " + name, e);
            return null;
        }
        if (clazz != null && interfacesOnly && !clazz.isInterface()) {
            return null;
        }
        return clazz;
    }
}
