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

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains methods used in Classfile.g to parse descriptors.
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
    public static String convertFieldTypeSignature(String s) {
        return convertFieldTypeSignature(new FieldTypeSignatureLexer(s).parse());
    }

    public static String convertFieldTypeSignature(List<Token> tokens) {
        StringBuilder buf = new StringBuilder();
        for (Token t : tokens) {
            buf.append(t.getValue());
        }
        System.out.println(buf.toString());
        return buf.toString();
    }

    /**
     * Convert a method type signature.
     * 
     * @param desc The signature as a string.
     * @return The signature as it would appear in a Java sourcefile.
     */
    public static String convertMethodTypeSignature(String desc) {
        throw new IllegalStateException("Not implemented yet");
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

    protected static class Token {

        public static final int BASE_TYPE = 0; // B|C|D...

        public static final int VOID_TYPE = 1; // V

        public static final int CLASS_NAME = 2; // with slashes

        public static final int ARRAY_BRACKET = 3; // [

        public static final int FIELD_DESCRIPTOR = 4; // method parameter

        public static final int PACKAGE = 5; // as a.b.c

        public static final int IDENTIFIER = 6; // 

        public static final int LABRACKET = 7; // <

        public static final int RABRACKET = 8; // >

        public static final int WILDCARD = 9; // +-*

        public static final int COMMA = 10; // ,

        public static final int POINT = 11; // .

        private String value;

        private int type;

        public Token(int type, String value) {
            this.type = type;
            this.value = value;
        }

        public int getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

    }

    protected static class FieldDescriptorLexer extends AbstractLexer {

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
         */
        public List<Token> parse() {
            // Object type?
            Matcher m = Pattern.compile("^(L)(.+)(;)").matcher(desc);
            if (m.matches()) {
                result.add(new Token(Token.CLASS_NAME, m.group(2))); // Classname
                desc = desc.substring(m.group(0).length()); // the rest after ;
                return result;
            }
            // Array type?
            m = Pattern.compile("^(\\[)((.+))").matcher(desc);
            if (m.matches()) {
                result.add(new Token(Token.ARRAY_BRACKET, "[]"));
                desc = desc.substring(1);
                parse();
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

    protected static class MethodDescriptorLexer extends AbstractLexer {

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
         * <pre>
         * 
         */
        public List<Token> parse() {
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

    protected static class FieldTypeSignatureLexer extends AbstractLexer {

        public FieldTypeSignatureLexer(String desc) {
            super(desc);
        }

        /**
         * Parse FieldTypeSignature according the grammar:
         * 
         * <pre>
         * FieldTypeSignature: ClassTypeSignature|ArrayTypeSignature|TypeVariableSignature
         * </pre>
         */
        @Override
        public List<Token> parse() {
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
         */
        @Override
        public List<Token> parse() {
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
         */
        @Override
        public List<Token> parse() {
            if (desc.charAt(0) == '[') {
                desc = desc.substring(1);
                Matcher m = Pattern.compile("^(B|C|D|F|I|J|S|Z)((.*))")
                        .matcher(desc);
                if (m.matches()) {
                    result.add(new Token(Token.BASE_TYPE, convertBaseType(m
                            .group(1))));
                    desc = desc.substring(1);
                } else {
                    AbstractLexer l = new FieldTypeSignatureLexer(desc);
                    result.addAll(l.parse());
                    desc = l.getRest();
                }
                result.add(new Token(Token.ARRAY_BRACKET, "[]"));
                return result;
            } else {
                throw new IllegalArgumentException(desc
                        + " is not an array type signature");
            }
        }
    }

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
         */
        @Override
        public List<Token> parse() {
            System.out.println("simple:" + desc + ":");
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
                    System.out.println("typeargs:" + typeArguments + ":");
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
                        System.out.println("arguments:" + arguments);
                        while (arguments.length() > 0) {
                            FieldTypeSignatureLexer l = new FieldTypeSignatureLexer(
                                    arguments);
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
         */
        @Override
        public List<Token> parse() {
            System.out.println("class:" + desc);
            // find signature in desc
            int count = 0, index = 0;
            for (char c : desc.toCharArray()) {
                if (c == ';' && count == 0) {
                    break;
                }
                if (c == '<') {
                    count++;
                }
                if (c == '>') {
                    count--;
                }
                index++;
            }
            String classTypeSignature = desc.substring(0, index + 1);
            System.out.println("classTypeSignature:" + classTypeSignature);
            desc = desc.substring(index + 1);
            Matcher m = Pattern.compile("L([^<\\.;]*/)*([^<\\.;]*)((.*));$")
                    .matcher(classTypeSignature);
            if (m.matches()) {
                for (int i = 0; i < m.groupCount(); i++) {
                    System.out.println(i + ": " + m.group(i));
                }
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
                SimpleClassTypeSignatureLexer sl = new SimpleClassTypeSignatureLexer(
                        identifier + arguments);
                result.addAll(sl.parse());
                // parse suffixes
                while (suffixes.length() > 0) {
                    // find suffix in suffixes
                    suffixes = suffixes.substring(1);
                    count = 0;
                    index = 0;
                    for (char c : suffixes.toCharArray()) {
                        if (c == '.' && count == 0) {
                            break;
                        }
                        if (c == '<') {
                            count++;
                        }
                        if (c == '>') {
                            count--;
                        }
                        index++;
                    }
                    String suffix = suffixes.substring(0, index);
                    result.add(new Token(Token.POINT, "."));
                    result.addAll(new SimpleClassTypeSignatureLexer(suffix)
                            .parse());
                    System.out.println("suffix=" + suffix);
                    suffixes = suffixes.substring(index);
                    System.out.println("suffixes=" + suffixes);
                }
            }
            return result;
        }
    }

    protected static abstract class AbstractLexer {
        protected String desc;

        protected List<Token> result;

        public AbstractLexer(String desc) {
            this.desc = desc;
            result = new LinkedList<Token>();
        }

        public abstract List<Token> parse();

        public String getRest() {
            return desc;
        }
    }

    protected static int balancedBracketPosition(String other, char open,
            char close) {
        int count = -1;
        char[] chars = other.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == open) {
                count++;
            } else if (c == close) {
                if (count == 0) {
                    return i;
                } else {
                    count--;
                }
            }
        }
        throw new IllegalArgumentException(other + " has no balanced bracket");
    }

}
