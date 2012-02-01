/* $Id$
 *****************************************************************************
 * Copyright (c) 2009-2012 Contributors - see below
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    lepekhine
 *****************************************************************************
 *
 * Some portions of this file was previously release using the BSD License:
 */

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains methods used in Classfile.g to parse descriptors and signatures.
 * 
 * @author Alexander Lepekhin
 */
public class ParserUtils {

    /**
     * Convert a classfile field descriptor.
     * 
     * @param desc The descriptor as a string.
     * @return The descriptor as it would appear in a Java sourcefile.
     */
    public static String convertFieldDescriptor(String desc) {
        return convertFieldDescriptor(new FieldDescriptorLexer(desc).parse());
    }

    /**
     * Convert a method descriptor.
     * 
     * @param desc The method descriptor as a String.
     * @return The method descriptor as a array of Strings, that holds Java
     *         types.
     */
    public static String[] convertMethodDescriptor(String desc) {
        List<String> buf = new LinkedList<String>();
        MethodDescriptorLexer lexer = new MethodDescriptorLexer(desc);
        for (Token t : lexer.parse()) {
            buf.add(t.getValue());
        }
        return buf.toArray(new String[] {});
    }

    /**
     * 
     * Convert a field type signature.
     * 
     * @param desc The signature as a string.
     * @return The signature as it would appear in a Java sourcefile.
     */
    public static String convertFieldTypeSignature(String desc) {
        List<Token> lexer = new FieldTypeSignatureLexer(desc).parse();
        return convertFieldTypeSignature(lexer);
    }

    /**
     * 
     * Convert class type signature.
     * 
     * @param desc The signature as a string.
     * @return The signature as it would appear in a Java sourcefile.
     */
    public static String convertClassTypeSignature(String desc) {
        List<Token> lexer = new ClassTypeSignatureLexer(desc).parse();
        return convertClassTypeSignature(lexer);
    }

    /**
     * 
     * Convert class signature.
     * 
     * @param desc The signature as a string.
     * @return The signature as it would appear in a Java sourcefile.
     */
    public static String convertClassSignature(String desc) {
        return convertClassSignature(new ClassSignatureLexer(desc).parse());
    }

    /**
     * 
     * Convert class signature.
     * 
     * @param desc The signature as a string in a java source .
     * @return A list of type parameters.
     */
    public static List<String> extractTypeParameters(String desc) {
        List<String> result = new LinkedList<String>();
        if (desc.startsWith("<")) {
            int endIndex =
                AbstractLexer.balancedBracketPosition(desc, '<', '>');
            desc = desc.substring(1, endIndex);
            while (desc.length() > 0) {
                int index =
                    AbstractLexer.firstIndexNotInside(',', '<', '>', desc);
                if (index == 0 || index == desc.length()) {
                    result.add(desc.trim());
                    desc = "";
                } else {
                    result.add(desc.substring(0, index).trim());
                    desc = desc.substring(index + 1);
                }
            }
        }
        return result;
    }

    public static List<Token> parseClassSignature(String s) {
        return new ClassSignatureLexer(s).parse();
    }

    /**
     * Convert a method type signature.
     * 
     * @param desc The signature as a string.
     * @return The signature as it would appear in a Java sourcefile.
     */
    public static String convertMethodTypeSignature(String desc) {
        return convertMethodTypeSignature(new MethodTypeSignatureLexer(desc)
                .parse());
    }

    protected static String convertFieldDescriptor(List<Token> tokens) {
        String brackets = "";
        for (Token t : tokens) {
            if (t.getType() == Token.BASE_TYPE) {
                return convertBaseType(t.getValue()) + brackets;
            } else if (t.getType() == Token.CLASS_NAME) {
                return t.getValue().replaceAll("/", ".") + brackets;
            } else if (t.getType() == Token.ARRAY_BRACKET) {
                brackets += t.getValue();
            }
        }
        throw new IllegalArgumentException("Can not parse field descriptor");
    }

    protected static String convertClassTypeSignature(List<Token> tokens) {
        StringBuilder buf = new StringBuilder();
        for (Token t : tokens) {
            buf.append(t.getValue());
        }
        return buf.toString();
    }

    protected static String convertFieldTypeSignature(List<Token> tokens) {
        StringBuilder buf = new StringBuilder();
        for (Token t : tokens) {
            buf.append(t.getValue());
        }
        return buf.toString();
    }

    protected static String convertClassSignature(List<Token> tokens) {
        StringBuilder buf = new StringBuilder();
        for (Token t : tokens) {
            buf.append(t.getValue());
        }
        return buf.toString();
    }

    protected static String convertMethodTypeSignature(List<Token> tokens) {
        StringBuilder buf = new StringBuilder();
        for (Token t : tokens) {
            buf.append(t.getValue());
        }
        return buf.toString();
    }

    protected static String convertBaseType(String s) {
        switch (s.charAt(0)) {
        case 'B':
            return "byte";
        case 'C':
            return "char";
        case 'D':
            return "double";
        case 'F':
            return "float";
        case 'I':
            return "int";
        case 'J':
            return "long";
        case 'S':
            return "short";
        case 'Z':
            return "boolean";
        }
        throw new IllegalArgumentException(s + " is not a base type");
    }

    /**
     * Tokens.
     */
    /**
     *
     * @author Linus
     */
    public static class Token {

        /**
         * B|C|D
         */
        public static final int BASE_TYPE = 0; // B|C|D...

        /**
         * V
         */
        public static final int VOID_TYPE = 1; // V

        /**
         * with slashes
         */
        public static final int CLASS_NAME = 2; // with slashes

        /**
         * [
         */
        public static final int ARRAY_BRACKET = 3; // [

        /**
         * method parameter
         */
        public static final int FIELD_DESCRIPTOR = 4; // method parameter

        /**
         * as a.b.c
         */
        public static final int PACKAGE = 5; // as a.b.c

        /**
         * identifier
         */
        public static final int IDENTIFIER = 6; // 

        /**
         * &lt;
         */
        public static final int LABRACKET = 7; // <

        /**
         * &gt;
         */
        public static final int RABRACKET = 8; // >

        /**
         * +-*
         */
        public static final int WILDCARD = 9; // +-*

        /**
         * ,
         */
        public static final int COMMA = 10; // ,

        /**
         * .
         */
        public static final int POINT = 11; // .

        /**
         * :
         */
        public static final int COLON = 12; // .

        /**
         * extends
         */
        public static final int SUPERCLASS = 13; // extends

        /**
         * implements
         */
        public static final int SUPERINTERFACE = 15; // implements

        /**
         * (
         */
        public static final int LBRACKET = 16; // (

        /**
         * )
         */
        public static final int RBRACKET = 17; // )

        /**
         * throws
         */
        public static final int THROWS = 18; // throws

        /**
         * return
         */
        public static final int RETURN = 19; // return

        /**
         * &amp;
         */
        public static final int AMPERSAND = 20; // &

        private String value;

        private int type;

        public Token(int t, String v) {
            type = t;
            value = v;
        }

        public int getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return type + ":" + value;
        }

    }

    /**
     * Lexer for the a field descriptor.
     */
    protected static class FieldDescriptorLexer 
        extends AbstractLexer {

        public FieldDescriptorLexer(String desc) {
            super(desc);
        }

        /**
         * Parse field descriptor according the grammar:
         * 
         * <pre>
         * FieldDescriptor:=BaseType|ObjectType|ArrayType
         *         BaseType:=B|C|D|F|J|S|Z
         *         ObjectType:=L Classname;
         *         ArrayType:=[FieldDescriptor
         * </pre>
         * 
         * @return a List of matched Token.
         */
        public List<Token> parse() {
            List<Token> result = new LinkedList<Token>();

            // Object type?
            Matcher m = Pattern.compile("^(L)(.+)(;)").matcher(desc);
            if (m.matches()) {
                // Classname
                result.add(new Token(Token.CLASS_NAME, m.group(2)));
                desc = desc.substring(m.group(0).length()); // the rest after ;
                return result;
            }
            // Array type?
            m = Pattern.compile("^(\\[)((.+))").matcher(desc);
            if (m.matches()) {
                result.add(new Token(Token.ARRAY_BRACKET, "[]"));
                desc = desc.substring(1);
                result.addAll(parse());
                return result;
            }
            // Base type?
            m = Pattern.compile("^(B|C|D|F|I|J|S|Z)((.*))").matcher(desc);
            if (m.matches()) {
                result.add(new Token(Token.BASE_TYPE, m.group(1)));
                desc = desc.substring(1);
                return result;
            }
            if (desc.length() == 0) {
                return result;
            }
            throw new IllegalArgumentException(desc
                    + " is not a FieldDescriptor");
        }

    }

    /**
     * Lexer for the method descriptor.
     */
    protected static class MethodDescriptorLexer 
        extends AbstractLexer {

        public MethodDescriptorLexer(String desc) {
            super(desc);
        }

        /**
         * Parse method descriptor according the grammar:
         * 
         * <pre>
         * MethodDescriptor:= ( FieldDescriptor* ) ReturnDescriptor
         * ReturnDescriptor:=FieldDescriptor|V
         * 
         * </pre>
         * 
         * @return a List of matched Token.
         */
        public List<Token> parse() {
            List<Token> result = new LinkedList<Token>();

            // Object type?
            Matcher m = Pattern.compile("^(\\()(.*)(\\))((.+))").matcher(desc);
            if (m.matches()) {
                String returnDescriptor = m.group(4);
                String parameters = m.group(2);
                while (parameters.length() > 0) {
                    FieldDescriptorLexer lexer = new FieldDescriptorLexer(
                            parameters);
                    result.add(new Token(Token.FIELD_DESCRIPTOR,
                            convertFieldDescriptor(lexer.parse())));
                    parameters = lexer.getRest();
                }
                if (returnDescriptor.equals("V")) {
                    result.add(new Token(Token.VOID_TYPE, "void"));
                } else {
                    FieldDescriptorLexer lexer = new FieldDescriptorLexer(
                            returnDescriptor);
                    result.add(new Token(Token.FIELD_DESCRIPTOR,
                            convertFieldDescriptor(lexer.parse())));
                }
                return result;
            }
            throw new IllegalArgumentException(desc
                    + " is not a MethodDescriptor");

        }

    }

    /**
     * Lexer for the signature of a field type.
     */
    protected static class FieldTypeSignatureLexer extends AbstractLexer {

        public FieldTypeSignatureLexer(String desc) {
            super(desc);
        }

        /**
         * Parse FieldTypeSignature according the grammar:
         * 
         * <pre>
         * FieldTypeSignature: ClassTypeSignature
         *                     |ArrayTypeSignature
         *                     |TypeVariableSignature
         * </pre>
         * 
         * @return a List of matched Token.
         */
        @Override
        public List<Token> parse() {
            List<Token> result = new LinkedList<Token>();

            // while (desc.length() > 0) {
            AbstractLexer lexer = null;
            switch (desc.charAt(0)) {
            case 'L':
                lexer = new ClassTypeSignatureLexer(desc);
                break;
            case '[':
                lexer = new ArrayTypeSignatureLexer(desc);
                break;
            case 'T':
                lexer = new TypeVariableSignatureLexer(desc);
                break;
            default:
                throw new IllegalArgumentException(desc
                        + " is not a field type signature");
            }
            result.addAll(lexer.parse());
            desc = lexer.getRest();
            // }
            return result;
        }
    }

    /**
     * Lexer for the signature of a variable declaration.
     */
    protected static class TypeVariableSignatureLexer extends AbstractLexer {

        public TypeVariableSignatureLexer(String desc) {
            super(desc);
        }

        /**
         * Parse type variable signature according the grammar:
         * 
         * <pre>
         * TypeVariableSignature: T Identifer ;
         * </pre>
         * 
         * @return a List of matched Token.
         */
        @Override
        public List<Token> parse() {
            List<Token> result = new LinkedList<Token>();

            Matcher m = Pattern.compile("T([^;]*);((.*))").matcher(desc);
            if (m.matches()) {
                result.add(new Token(Token.IDENTIFIER, m.group(1)));
                desc = m.group(2);
            } else {
                new IllegalArgumentException(desc
                        + " is not a type variable signature");
            }
            return result;
        }
    }

    /**
     * Lexer for the signature of an array type.
     */
    protected static class ArrayTypeSignatureLexer extends AbstractLexer {

        public ArrayTypeSignatureLexer(String desc) {
            super(desc);
        }

        /**
         * Parse array type signature according the grammar:
         * 
         * <pre>
         * ArrayTypeSignature:  [TypeSignature
         * 	       TypeSignature: FieldTypeSignature|BaseType
         * </pre>
         * 
         * @return a List of matched Token.
         */
        @Override
        public List<Token> parse() {
            List<Token> result = new LinkedList<Token>();

            if (desc.charAt(0) == '[') {
                desc = desc.substring(1);
                TypeSignatureLexer l = new TypeSignatureLexer(desc);
                result.addAll(l.parse());
                desc = l.getRest();
                result.add(new Token(Token.ARRAY_BRACKET, "[]"));
                return result;
            } else {
                throw new IllegalArgumentException(desc
                        + " is not an array type signature");
            }
        }
    }

    /**
     * Lexer for the signature of a type.
     */
    protected static class TypeSignatureLexer extends AbstractLexer {

        public TypeSignatureLexer(String desc) {
            super(desc);
        }

        /**
         * Parse type signature according the grammar:
         * 
         * <pre>
         * TypeSignature: FieldTypeSignature|BaseType
         * </pre>
         * 
         * @return a List of matched Token.
         */
        @Override
        public List<Token> parse() {
            List<Token> result = new LinkedList<Token>();

            Matcher m = Pattern.compile("^(B|C|D|F|I|J|S|Z)((.*))").matcher(
                    desc);
            if (m.matches()) {
                result.add(new Token(Token.BASE_TYPE, convertBaseType(m
                        .group(1))));
                desc = desc.substring(1);
            } else {
                AbstractLexer l = new FieldTypeSignatureLexer(desc);
                result.addAll(l.parse());
                desc = l.getRest();
            }
            return result;
        }
    }

    /**
     * Lexer for the signature of a simple class type.
     */
    protected static class SimpleClassTypeSignatureLexer extends AbstractLexer {

        public SimpleClassTypeSignatureLexer(String desc) {
            super(desc);
        }

        /**
         * Parse simple class type signature according the grammar:
         * 
         * <pre>
         * SimpleClassTypeSignature:=Identifier TypeArgumentsopt
         * TypeArguments:&lt;TypeArgument+&gt;
         * TypeArgument:WildcardIndicatoropt FieldTypeSignature|*
         * WildcardIndicator:+|-
         * </pre>
         * 
         * @return a List of matched Token.
         */
        @Override
        public List<Token> parse() {
            List<Token> result = new LinkedList<Token>();

            Matcher m = Pattern.compile("([^<.]*)(<.*>)?((.*))").matcher(desc);
            if (m.matches()) {
                String identifier = m.group(1);
                String typeArguments = m.group(2);
                if (identifier != null && identifier.length() > 0) {
                    result.add(new Token(Token.IDENTIFIER, identifier));
                } else {
                    throw new IllegalArgumentException(desc
                            + " is not a SimpleClassTypeSignature");
                }
                if (typeArguments != null && typeArguments.length() > 0) {
                    result.add(new Token(Token.LABRACKET, "<"));
                    String arguments = typeArguments.substring(1, typeArguments
                            .length() - 1);
                    if (arguments.charAt(0) == '*') {
                        result.add(new Token(Token.WILDCARD, "?"));
                    } else {
                        if (arguments.charAt(0) == '+') {
                            result.add(new Token(Token.WILDCARD, "? extends "));
                            arguments = arguments.substring(1);
                        } else if (arguments.charAt(0) == '-') {
                            result.add(new Token(Token.WILDCARD, "? super "));
                            arguments = arguments.substring(1);
                        }
                        while (arguments.length() > 0) {
                            FieldTypeSignatureLexer l = 
                                new FieldTypeSignatureLexer(arguments);
                            result.addAll(l.parse());
                            arguments = l.getRest();
                            if (arguments.length() > 0) {
                                result.add(new Token(Token.COMMA, ","));
                            }
                        }
                    }
                    result.add(new Token(Token.RABRACKET, ">"));
                }
            }
            desc = m.group(3);
            return result;
        }
    }

    /**
     * Lexer for the signature of a class type.
     */
    protected static class ClassTypeSignatureLexer extends AbstractLexer {

        public ClassTypeSignatureLexer(String desc) {
            super(desc);
        }

        /**
         * Parse class type signature according the grammar:
         * 
         * <pre>
         * ClassTypeSignature:=L PackageSpecifier* SimpleClassTypeSignature ClassTypeSignatureSuffix* ; 
         * PackageSpecifier:=Identifier / PackageSpecifier* 
         * ClassTypeSignatureSuffix:=. SimpleClassTypeSignature
         * </pre>
         * 
         * @return a List of matched Token.
         */
        @Override
        public List<Token> parse() {
            List<Token> result = new LinkedList<Token>();

            // find signature in desc
            int index = firstIndexNotInside(';', '<', '>', desc);
            String classTypeSignature = desc.substring(0, index + 1);
            desc = desc.substring(index + 1);
            Matcher m = Pattern.compile("L([^<\\.;]*/)*([^<\\.;]*)((.*));$")
                    .matcher(classTypeSignature);
            if (m.matches()) {
                String packageSpecifier = m.group(1);
                String identifier = m.group(2);
                String other = m.group(3);
                String arguments = "";
                String suffixes = "";
                if (other != null && other.length() > 0) {
                    if (other.charAt(0) == '<') {
                        // this is type arguments
                        arguments = other.substring(0, balancedBracketPosition(
                                other, '<', '>') + 1);
                    }
                    suffixes = other.substring(arguments.length());
                }
                if (packageSpecifier != null && packageSpecifier.length() > 0) {
                    result.add(new Token(Token.PACKAGE, packageSpecifier
                            .replaceAll("/", ".")));
                }
                SimpleClassTypeSignatureLexer sl =
                    new SimpleClassTypeSignatureLexer(
                        identifier + arguments);
                result.addAll(sl.parse());
                // parse suffixes
                while (suffixes.length() > 0) {
                    // find suffix in suffixes
                    suffixes = suffixes.substring(1);
                    index = firstIndexNotInside('.', '<', '>', suffixes);
                    String suffix = suffixes.substring(0, index);
                    result.add(new Token(Token.POINT, "."));
                    result.addAll(new SimpleClassTypeSignatureLexer(suffix)
                            .parse());
                    suffixes = suffixes.substring(index);
                }
            }
            return result;
        }
    }

    /**
     * Lexer for a signature of a class.
     */
    protected static class ClassSignatureLexer extends AbstractLexer {

        public ClassSignatureLexer(String desc) {
            super(desc);
        }

        /**
         * Parse class signature according the grammar:
         * 
         * <pre>
         * ClassSignature: FormalTypeParametersopt SuperclassSignature SuperinterfaceSignature*
         * FormalTypeParameters: &lt;FormalTypeParameter+&gt;
         * FormalTypeParameter: Identifier ClassBound InterfaceBound*
         * ClassBound:  : FieldTypeSignatureopt
         * InterfaceBound:  : FieldTypeSignature
         * SuperclassSignature:   ClassTypeSignature
         * SuperinterfaceSignature:  ClassTypeSignature
         * </pre>
         * 
         * @return a List of matched Token.
         */
        @Override
        public List<Token> parse() {
            List<Token> result = new LinkedList<Token>();

            if (desc.charAt(0) == '<') {
                result.add(new Token(Token.LABRACKET, "<"));
                int formalTypeParametersEndIndex = balancedBracketPosition(
                        desc, '<', '>');
                String formalTypeParameters = desc.substring(1,
                        formalTypeParametersEndIndex);
                desc = desc.substring(formalTypeParametersEndIndex + 1);
                FormalTypeParameterLexer l = new FormalTypeParameterLexer(
                        formalTypeParameters);
                result.addAll(l.parse());
                Pattern.compile("([^:]*):((.*))").matcher(formalTypeParameters);
                result.add(new Token(Token.RABRACKET, ">"));
            }
            // parse super type
            ClassTypeSignatureLexer l = new ClassTypeSignatureLexer(desc);
            result.add(new Token(Token.SUPERCLASS, " extends "));
            result.addAll(l.parse());
            String interfaces = l.getRest();
            if (interfaces.length() > 0) {
                // parse super interfaces
                result.add(new Token(Token.SUPERCLASS, " implements "));
                ClassTypeSignatureLexer l2 = new ClassTypeSignatureLexer(
                        interfaces);
                result.addAll(l2.parse());
                String others = l2.getRest();
                while (others.length() > 0) {
                    l2 = new ClassTypeSignatureLexer(others);
                    result.add(new Token(Token.COMMA, ","));
                    result.addAll(l2.parse());
                    others = l2.getRest();
                }
            }

            return result;
        }
    }

    /**
     * Lexer for the formal type parameter.
     */
    protected static class FormalTypeParameterLexer extends AbstractLexer {

        public FormalTypeParameterLexer(String desc) {
            super(desc);
        }

        /**
         * Parse formal type parameters according the grammar:
         * 
         * <pre>
         * FormalTypeParameters: &lt;FormalTypeParameter+&gt;
         * FormalTypeParameter: Identifier ClassBound InterfaceBound*
         * ClassBound:  : FieldTypeSignatureopt
         * InterfaceBound:  : FieldTypeSignature
         * </pre>
         * 
         * @return a List of matched Token.
         */
        @Override
        public List<Token> parse() {
            List<Token> result = new LinkedList<Token>();

            Matcher m = Pattern.compile("([^:]*):((.*))").matcher(desc);
            if (m.matches()) {
                String identifier = m.group(1);
                String other = m.group(2);
                result.add(new Token(Token.IDENTIFIER, identifier));
                result.add(new Token(Token.SUPERCLASS, " extends "));
                // other begins with ":" if ClassBound is absent
                // that is parameter extends interface, not a class
                if (other.charAt(0) == ':') {
                    other = other.substring(1);
                }
                FieldTypeSignatureLexer f = new FieldTypeSignatureLexer(other);
                result.addAll(f.parse());
                other = f.getRest();
                if (other.length() > 0) {
                    if (other.startsWith(":")) {
                        // interface bounds begins here
                        while (other.startsWith(":")) {
                            result.add(new Token(Token.AMPERSAND, " & "));
                            FieldTypeSignatureLexer l =
                                new FieldTypeSignatureLexer(
                                    other.substring(1));
                            result.addAll(l.parse());
                            other = l.getRest();
                        }
                    }
                }
                if (other.length() > 0) {
                    // new FormalTypeParameter begins here
                    result.add(new Token(Token.COMMA, ","));
                    FormalTypeParameterLexer l = new FormalTypeParameterLexer(
                            other);
                    result.addAll(l.parse());
                }
                desc = other;
            }
            return result;
        }
    }

    /**
     * Lexer for the signature of the method type.
     */
    protected static class MethodTypeSignatureLexer extends AbstractLexer {

        public MethodTypeSignatureLexer(String desc) {
            super(desc);
        }

        /**
         * Parse method type signature according the grammar:
         * 
         * <pre>
         * MethodTypeSignature:  FormalTypeParametersopt (TypeSignature*) ReturnType ThrowsSignature*
         *          ReturnType:  TypeSignature VoidDescriptor
         *     ThrowsSignature:   &circ;ClassTypeSignature &circ;TypeVariableSignature
         * </pre>
         * 
         * @return a List of matched Token.
         */
        @Override
        public List<Token> parse() {
            List<Token> result = new LinkedList<Token>();

            if (desc.charAt(0) == '<') {
                result.add(new Token(Token.LABRACKET, "<"));
                int formalTypeParametersEndIndex = balancedBracketPosition(
                        desc, '<', '>');
                String formalTypeParameters = desc.substring(1,
                        formalTypeParametersEndIndex);
                desc = desc.substring(formalTypeParametersEndIndex + 1);
                FormalTypeParameterLexer l = new FormalTypeParameterLexer(
                        formalTypeParameters);
                result.addAll(l.parse());
                Pattern.compile("([^:]*):((.*))")
                    .matcher(formalTypeParameters);
                result.add(new Token(Token.RABRACKET, ">"));
            }
            // parse type signatures
            result.add(new Token(Token.LBRACKET, "("));
            int typeSignaturesEndIndex =
                balancedBracketPosition(desc, '(', ')');
            String typeSignatures = desc.substring(1, typeSignaturesEndIndex);
            desc = desc.substring(typeSignaturesEndIndex + 1);
            if (typeSignatures.length() > 0) {
                TypeSignatureLexer t = new TypeSignatureLexer(typeSignatures);
                result.addAll(t.parse());
                typeSignatures = t.getRest();
                while (typeSignatures.length() > 0) {
                    result.add(new Token(Token.COMMA, ","));
                    TypeSignatureLexer t2 = new TypeSignatureLexer(
                            typeSignatures);
                    result.addAll(t2.parse());
                    typeSignatures = t2.getRest();
                }
            }
            result.add(new Token(Token.RBRACKET, ")"));
            // parse return type
            result.add(new Token(Token.RETURN, " return "));
            if (desc.charAt(0) == 'V') {
                result.add(new Token(Token.VOID_TYPE, "void"));
                desc = desc.substring(1);
            } else {
                TypeSignatureLexer t3 = new TypeSignatureLexer(desc);
                result.addAll(t3.parse());
                desc = t3.getRest();
            }
            // parse throws signatures
            if (desc.length() > 0) {
                result.add(new Token(Token.THROWS, " throws "));
                ThrowsSignatureLexer t4 = new ThrowsSignatureLexer(desc);
                result.addAll(t4.parse());
                desc = t4.getRest();
                while (desc.length() > 0) {
                    result.add(new Token(Token.COMMA, ","));
                    ThrowsSignatureLexer t2 = new ThrowsSignatureLexer(desc);
                    result.addAll(t2.parse());
                    desc = t2.getRest();
                }
            }
            return result;
        }
    }

    /**
     * Lexer for the throws signature.
     */
    protected static class ThrowsSignatureLexer extends AbstractLexer {

        public ThrowsSignatureLexer(String desc) {
            super(desc);
        }

        /**
         * Parse throws signature according the grammar:
         * 
         * <pre>
         * ThrowsSignature: &circ;ClassTypeSignature &circ;TypeVariableSignature
         * </pre>
         * 
         * @return a List of matched Token.
         */
        @Override
        public List<Token> parse() {
            List<Token> result = new LinkedList<Token>();

            if (desc.charAt(0) == '^') {
                desc = desc.substring(1);
                if (desc.charAt(0) == 'T') {
                    TypeVariableSignatureLexer l =
                        new TypeVariableSignatureLexer(desc);
                    result.addAll(l.parse());
                    desc = l.getRest();
                } else {
                    ClassTypeSignatureLexer l = new ClassTypeSignatureLexer(
                            desc);
                    result.addAll(l.parse());
                    desc = l.getRest();
                }
            } else {
                throw new IllegalArgumentException(desc
                        + " is not a throws signature");
            }
            return result;
        }
    }

    /**
     * The abstract lexer.
     */
    protected abstract static class AbstractLexer {
        /**
         * Description of the lexer.
         */
        protected String desc;

        public AbstractLexer(String d) {
            desc = d;
        }

        public abstract List<Token> parse();

        public String getRest() {
            return desc;
        }

        /**
         * Find the index of the first occurrence of symbol in text outside of
         * brackets.
         * 
         * @param symbol
         * @param openBracket
         * @param closeBracket
         * @param text
         * @return index
         */
        protected static int firstIndexNotInside(char symbol, char openBracket,
                char closeBracket, String text) {
            int count = 0;
            int index = 0;
            for (char c : text.toCharArray()) {
                if (c == symbol && count == 0) {
                    break;
                }
                if (c == openBracket) {
                    count++;
                }
                if (c == closeBracket) {
                    count--;
                }
                index++;
            }
            return index;
        }

        protected static int balancedBracketPosition(String text,
                char openBracket, char closeBracket) {
            if (text.charAt(0) != openBracket) {
                throw new IllegalArgumentException(text
                        + " does not start with open bracket ");
            }
            if (text.indexOf(closeBracket) == -1) {
                throw new IllegalArgumentException(text
                        + " does not contain close bracket ");
            }
            int count = -1;
            char[] chars = text.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (c == openBracket) {
                    count++;
                } else if (c == closeBracket) {
                    if (count == 0) {
                        return i;
                    } else {
                        count--;
                    }
                }
            }
            throw new IllegalArgumentException(text
                    + " has no balanced bracket for " + openBracket);
        }

    }

}
