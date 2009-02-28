// $Id: ParseState.java 116 2009-01-06 11:23:31Z thn $
// Copyright (c) 2003-2006 The Regents of the University of California. All
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

import junit.framework.TestCase;

/**
 *
 * @author Alexander Lepekhin
 */
public class TestParserUtils extends TestCase {

    public TestParserUtils(String str) {
        super(str);
    }

    public void testParseFieldDescriptor() {
        assertEquals("byte", ParserUtils.convertFieldDescriptor("B"));
        assertEquals("char", ParserUtils.convertFieldDescriptor("C"));
        assertEquals("double", ParserUtils.convertFieldDescriptor("D"));
        assertEquals("float", ParserUtils.convertFieldDescriptor("F"));
        assertEquals("int", ParserUtils.convertFieldDescriptor("I"));
        assertEquals("long", ParserUtils.convertFieldDescriptor("J"));
        assertEquals("short", ParserUtils.convertFieldDescriptor("S"));
        assertEquals("boolean", ParserUtils.convertFieldDescriptor("Z"));
        assertEquals("a.b.c", ParserUtils.convertFieldDescriptor("La.b.c;"));
        assertEquals("byte[][][]", ParserUtils.convertFieldDescriptor("[[[B"));
    }

    public void testParseMethodDescriptor() {
        String[] result = ParserUtils.convertMethodDescriptor("(ID[[[Ljava/lang/Thread;)Ljava/lang/Object;");
        assertEquals("int", result[0]);
        assertEquals("double", result[1]);
        assertEquals("java.lang.Thread[][][]", result[2]);
        assertEquals("java.lang.Object", result[3]);
    }

    public void testConvertClassTypeSignature() {
        assertEquals("java.lang.Comparable<?>",
                ParserUtils.convertClassTypeSignature("Ljava/lang/Comparable<*>"));
        assertEquals("java.lang.Comparable<java.lang.String>",
                ParserUtils.convertClassTypeSignature("Ljava/lang/Comparable<Ljava/lang/String;>;"));
    }

}
