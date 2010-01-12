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

package org.argouml.language.java.reveng;

/**
 * Class to hold components of a parameter declaration including type, name, and
 * any modifiers (e.g. final).
 *
 * @author Tom Morris <tfmorris@gmail.com>
 */
public class ParameterDeclaration {

    private final short theModifiers;
    private final String theType;
    private final String theName;

    /**
     * Construct a new parameter declaration object.
     *
     * @param modifiers a short bitfield containing a bit set for each modifier.
     *                See the ACC_ definitions in the java.g grammar file or the
     *                Java spec to find the meaning of each individual bit.
     * @param type the type of the parameter, possibly followed by array
     *                notation brackets
     * @param name the name of the parameter
     */
    public ParameterDeclaration(final short modifiers, final String type,
            final String name) {
        theModifiers = modifiers;
        theType = type;
        theName = name;
    }

    /**
     * @return a bitfield containing the modifiers for the parameter.
     */
    public short getModifiers() {
        return theModifiers;
    }

    /**
     * @return the type of the parameter in string form.
     */
    public String getType() {
        return theType;
    }

    /**
     * @return the name of the parameter.
     */
    public String getName() {
        return theName;
    }
}
