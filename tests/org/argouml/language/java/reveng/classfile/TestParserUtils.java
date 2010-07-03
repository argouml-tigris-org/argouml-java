/* $Id$
 *****************************************************************************
 * Copyright (c) 2009-2010 Contributors - see below
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    lepekhine
 *    Luis Sergio Oliveira (euluis)
 *****************************************************************************
 *
 * Some portions of this file was previously release using the BSD License:
 */

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

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
import antlr.RecognitionException;
import antlr.TokenStreamException;

/**
 * Tests conversion of classfile signature to java lang signature.
 * 
 * @author alepekhin
 */
public class TestParserUtils extends TestCase {

    public TestParserUtils(String str) {
        super(str);
    }

    public void testFirstIndexNotInside() throws Exception {
        assertEquals(17, ParserUtils.AbstractLexer.firstIndexNotInside(';',
                '<', '>', "Ljava/lang/Object;"));
    }

    public void testFieldDescriptorLexer() throws Exception {
        ParserUtils.FieldDescriptorLexer lexer =
            new ParserUtils.FieldDescriptorLexer("Bwhatelse");
        List<ParserUtils.Token> tokens = lexer.parse();
        assertEquals("B", tokens.get(0).getValue());
        assertEquals("whatelse", lexer.getRest());
    }

    public void testParseFieldDescriptor() throws Exception {
        assertEquals("byte", ParserUtils.convertFieldDescriptor("B"));
        assertEquals("char", ParserUtils.convertFieldDescriptor("C"));
        assertEquals("double", ParserUtils.convertFieldDescriptor("D"));
        assertEquals("float", ParserUtils.convertFieldDescriptor("F"));
        assertEquals("int", ParserUtils.convertFieldDescriptor("I"));
        assertEquals("long", ParserUtils.convertFieldDescriptor("J"));
        assertEquals("short", ParserUtils.convertFieldDescriptor("S"));
        assertEquals("boolean", ParserUtils.convertFieldDescriptor("Z"));
        assertEquals("byte[][][]", ParserUtils.convertFieldDescriptor("[[[B"));
        assertEquals("a", ParserUtils.convertFieldDescriptor("La;"));
        assertEquals("a.b", ParserUtils.convertFieldDescriptor("La/b;"));
        assertEquals("java.lang.String[][]", ParserUtils
                .convertFieldDescriptor("[[Ljava/lang/String;"));
    }

    public void testParseMethodDescriptor() throws RecognitionException,
        TokenStreamException {
        String[] result = ParserUtils.convertMethodDescriptor(
                "(ID[[[Ljava/lang/Thread;)Ljava/lang/Object;");
        assertEquals("int", result[0]);
        assertEquals("double", result[1]);
        assertEquals("java.lang.Thread[][][]", result[2]);
        assertEquals("java.lang.Object", result[3]);
        result = ParserUtils.convertMethodDescriptor("()V");
        assertEquals("void", result[0]);
    }

    public void testConvertFieldTypeSignature() {
        assertEquals("java.lang.Comparable<?>",
            ParserUtils.convertFieldTypeSignature(
                "Ljava/lang/Comparable<*>;"));
        assertEquals("java.lang.Comparable<? extends a.b.C>", ParserUtils
            .convertFieldTypeSignature("Ljava/lang/Comparable<+La/b/C;>;"));
        assertEquals("java.lang.Comparable<? super a.b.C>", ParserUtils
            .convertFieldTypeSignature("Ljava/lang/Comparable<-La/b/C;>;"));
        assertEquals("java.lang.Comparable<java.lang.String>",
            ParserUtils.convertFieldTypeSignature(
                "Ljava/lang/Comparable<Ljava/lang/String;>;"));
        assertEquals("java.lang.Map<java.lang.String,java.lang.Integer>",
            ParserUtils.convertFieldTypeSignature(
                "Ljava/lang/Map<Ljava/lang/String;Ljava/lang/Integer;>;"));
        assertEquals("java.lang.Map<byte[],int[][]>", ParserUtils
            .convertFieldTypeSignature("Ljava/lang/Map<[B[[I>;"));
        assertEquals("java.lang.Map<byte[],java.lang.Map<E,E>[][]>",
            ParserUtils.convertFieldTypeSignature(
                "Ljava/lang/Map<[B[[Ljava/lang/Map<TE;TE;>;>;"));
        assertEquals(
            "java.lang.Map<byte[],java.lang.Map<E,E>[][]>.Inner<d.inner>.Inner2",
            ParserUtils.convertFieldTypeSignature(
                "Ljava/lang/Map<[B[[Ljava/lang/Map<TE;TE;>;>.Inner<Ld.inner;>.Inner2;"));
    }

    public void testConvertClassTypeSignature() {
        assertEquals("java.lang.Object",
            ParserUtils.convertClassTypeSignature("Ljava/lang/Object;"));
    }

    public void testConvertClassSignature() {
        assertEquals(
            "<T extends java.lang.Number,E extends java.lang.Object> extends "
                + "java.lang.Object implements java.lang.Comparable,java.lang.Serializable",
            ParserUtils.convertClassSignature(
                "<T:Ljava/lang/Number;E:Ljava/lang/Object;>Ljava/lang/Object;Ljava/lang/Comparable;Ljava/lang/Serializable;"));
        assertEquals(
            "<T extends java.lang.Number & java.lang.Comparable> extends java.lang.Object implements java.lang.Comparable,java.lang.Serializable",
            ParserUtils.convertClassSignature(
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable;>Ljava/lang/Object;Ljava/lang/Comparable;Ljava/lang/Serializable;"));
        assertEquals(
            "<T extends java.lang.Number & java.lang.Comparable & java.lang.Serializable> extends java.lang.Object implements java.lang.Comparable,java.lang.Serializable",
            ParserUtils.convertClassSignature(
                "<T:Ljava/lang/Number;:Ljava/lang/Comparable;:Ljava/lang/Serializable;>Ljava/lang/Object;Ljava/lang/Comparable;Ljava/lang/Serializable;"));
        assertEquals(
            "<T extends java.lang.Comparable & java.lang.Serializable> extends java.lang.Object implements java.lang.Comparable,java.lang.Serializable",
            ParserUtils.convertClassSignature(
                "<T::Ljava/lang/Comparable;:Ljava/lang/Serializable;>Ljava/lang/Object;Ljava/lang/Comparable;Ljava/lang/Serializable;"));
    }

    public void testConvertMethodTypeSignature() {
        assertEquals("() return void", ParserUtils
            .convertMethodTypeSignature("()V"));
        assertEquals(
            "(E,java.lang.Integer) return java.lang.String throws java.lang.Exception,T",
            ParserUtils.convertMethodTypeSignature(
                "(TE;Ljava/lang/Integer;)Ljava/lang/String;^Ljava/lang/Exception;^TT;"));
    }

    public void testExtractTypeParameters() {
        List<String> expected = new LinkedList<String>();
        expected.add("T");
        assertEquals(expected, ParserUtils.extractTypeParameters("<T>"));
        expected = new LinkedList<String>();
        expected.add("T");
        expected.add("V");
        assertEquals(expected, ParserUtils.extractTypeParameters("<T, V>"));
        expected = new LinkedList<String>();
        expected.add("T");
        expected.add("V extends C<Integer, Double>");
        assertEquals(expected, ParserUtils.extractTypeParameters(
            "<T, V extends C<Integer, Double>>"));
    }
}
