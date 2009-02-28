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

import java.util.ArrayList;
import java.util.List;

/**
 * Contains methods used in Classfile.g to parse descriptors.
 * @author Alexander Lepekhin
 */
public class ParserUtils {

	/**
	 * Convert a classfile field descriptor.
	 * @param desc The descriptor as a string.
	 * @return The descriptor as it would appear in a Java sourcefile.
	 */
    public static String convertFieldDescriptor(String desc) {
        return convertComponentType(desc);
    }

	/**
	 * Convert the descriptor of a method.
	 * @param desc The method descriptor as a String.
	 * @return The method descriptor as a array of Strings, that holds Java types.
	 */
    public static String[] convertMethodDescriptor(String desc) {
        List<String> buf = new ArrayList<String>();
        String parameters = desc.substring(desc.indexOf("(")+1, desc.indexOf(")"));
        if (parameters.length() > 0) {
            for (String parameter : parseParameters(parameters)) {
                buf.add(convertFieldDescriptor(parameter));
            }
        }
        buf.add(convertReturnDescriptor(desc.substring(desc.indexOf(")")+1)));
        return buf.toArray(new String[]{});
    }

	/**
	 * Convert a class signature.
	 * @param desc The signature as a string.
	 * @return The signature as it would appear in a Java sourcefile.
	 */
    public static String convertClassSignature(String desc) {
        throw new IllegalStateException("Not implemented yet");
    }

	/**
	 * Convert a class type signature.
	 * @param desc The signature as a string.
	 * @return The signature as it would appear in a Java sourcefile.
	 */
    public static String convertClassTypeSignature(String s) {

        StringBuilder result = new StringBuilder();
        String packageString = getPackageSpecifier(s.substring(1)); // remove leading "L"
        if (packageString.length() != 0) {
            result.append(packageString.replaceAll("/", "."));
        }
        String rest = s.substring(packageString.length() + 1);
        String simpleClassTypeSignature = getSimpleClassTypeSignature(rest);
        result.append(convertSimpleClassTypeSignature(simpleClassTypeSignature));
        // TODO: Inner classes
//        for (int i = 1; i < c.length; i++) {
//            result.append(".");
//            result.append(convertSimpleClassTypeSignature(c[i]));
//        }
        return result.toString();
    }

	/**
	 * Convert a method type signature.
	 * @param desc The signature as a string.
	 * @return The signature as it would appear in a Java sourcefile.
	 */
    public static String convertMethodTypeSignature(String desc) {
        throw new IllegalStateException("Not implemented yet");
    }

    //
    // protected methods for parsing descriptors according to class file specification
    //

    protected static boolean isBaseType(String s) {
        char[] baseTypes = {'B','C','D','F','I','J','S','Z'};
        for (char c : baseTypes) {
            if (s.charAt(0) == c) {
                return true;
            }
        }
        return false;
    }

    protected static boolean isObjectType(String s) {
        return s.charAt(0) == 'L';
    }

    protected static boolean isArrayType(String s) {
        return s.charAt(0) == '[';
    }

    protected static boolean isVoidType(String s) {
        return s.charAt(0) == 'V';
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

    protected static String convertObjectType(String s) {
        return s.substring(1, s.indexOf(";")).replaceAll("/", ".");
    }

    protected static String convertVoidType(String s) {
        return "void";
    }

    protected static String convertArrayType(String s) {
        return convertComponentType(s.substring(1))+"[]";
    }

    protected static String convertComponentType(String s) {
        if (isArrayType(s)) {
            return convertArrayType(s);
        } else if (isBaseType(s)) {
            return convertBaseType(s);
        } else if (isObjectType(s)) {
            return convertObjectType(s);
        }
        throw new IllegalArgumentException(s + " is not a component type");
    }

    protected static List<String> parseParameters(String parameters) {
        List<String> result = new ArrayList<String>();
        while (parameters.length() > 0) {
            String firstParameter = getFirstParameter(parameters);
            result.add(firstParameter);
            parameters = parameters.substring(firstParameter.length());
        }
        return result;
    }

    protected static String getFirstParameter(String parameters) {
        if (isBaseType(parameters)) {
            return parameters.substring(0,1);
        } else if (isObjectType(parameters)) {
            return parameters.substring(0, parameters.indexOf(";")+1);
        } else if (isArrayType(parameters)) {
            return "[" +  getFirstParameter(parameters.substring(1));
        }
        return null;
    }

    public static String convertReturnDescriptor(String s) {
        if (isVoidType(s)) {
            return convertVoidType(s);
        } else {
            return convertFieldDescriptor(s);
        }
    }

    //
    // protected methods for parsing signatures according to class file specification
    //

    public static boolean isClassTypeSignature(String s) {
        return s.charAt(0) == 'L';
    }

    public static boolean isArrayTypeSignature(String s) {
        return s.charAt(0) == '[';
    }

    public static boolean isTypeVariableSignature(String s) {
        return s.charAt(0) == 'T';
    }

    protected static String convertFieldTypeSignature(String s) {
        if (isClassTypeSignature(s)) {
            return convertClassTypeSignature(s);
        } else if (isArrayTypeSignature(s)) {
            return convertArrayTypeSignature(s);
        } else if (isTypeVariableSignature(s)) {
            return convertTypeVariableSignature(s);
        }
        throw new IllegalArgumentException(s + " is not a FieldTypeSignature");
    }

    protected static String convertArrayTypeSignature(String s) {
        if (isBaseType(s.substring(1))) {
            return convertBaseType(s.substring(1)) + "[]";
        } else {
            return convertFieldTypeSignature(s.substring(1)) + "[]";
        }
    }

    protected static String convertTypeVariableSignature(String s) {
        return s.substring(1, s.indexOf(";"));
    }

    protected static String convertSimpleClassTypeSignature(String s) {
        if (s.indexOf("<") == -1) {
            return s;
        } else {
            String identifier = s.substring(0, s.indexOf("<"));
            String typeArguments = s.substring(s.indexOf("<") + 1, s.lastIndexOf(">"));
            String argList = "";
            for (String argument : parseArguments(typeArguments)) {
                argList += "," + convertArgument(argument);
            }
            return identifier + "<" + argList.substring(1) + ">";
        }
    }

    protected static List<String> parseArguments(String arguments) {
        List<String> result = new ArrayList<String>();
        while (arguments.length() > 0) {
            String firstArgument = getFirstArgument(arguments);
            result.add(firstArgument);
            arguments = arguments.substring(firstArgument.length());
        }
        return result;
    }

    protected static String getFirstArgument(String arguments) {
        if (arguments.charAt(0) == '*') {
            return "*";
        } else if (arguments.charAt(0) == '+') {
            return "+" + getFirstArgument(arguments.substring(1));
        } else if (arguments.charAt(0) == '-') {
            return "-" + getFirstArgument(arguments.substring(1));
        } else {
            // this is FieldTypeSignature
            if (arguments.charAt(0) == 'L') {
                return arguments.substring(0, arguments.indexOf(";") + 1);
            } else if (arguments.charAt(0) == 'T') {
                return arguments.substring(0, arguments.indexOf(";") + 1);
            } else if (arguments.charAt(0) == '[') {
                if (isBaseType(arguments.substring(1))) {
                    return arguments.substring(0,1);
                } else {
                    return "[" + getFirstArgument(arguments.substring(1));
                }
            }
            throw new IllegalArgumentException(arguments + " is not a valid argument");
        }
    }

    protected static String convertArgument(String argument) {
        if (argument.charAt(0) == '*') {
            return "?";
        } else if (argument.charAt(0) == '+') {
            return "? extends " + convertArgument(argument.substring(1));
        } else if (argument.charAt(0) == '-') {
            return "? super " + convertArgument(argument.substring(1));
        } else {
            // this is FieldTypeSignature
            return convertFieldTypeSignature(argument);
        }
    }

    protected static String getPackageSpecifier(String s) {
        String packageString = s;
        if (packageString.indexOf("<") != -1) {
            packageString = packageString.substring(0, packageString.indexOf("<"));
        }
        if (packageString.indexOf(".") != -1) {
            packageString = packageString.substring(0, packageString.indexOf("."));
        }
        if (packageString.indexOf("/") != -1) {
            packageString = packageString.substring(0, packageString.lastIndexOf("/") + 1);
        } else {
            packageString = "";
        }
        return packageString;
    }

    protected static String getSimpleClassTypeSignature(String s) {
        int p1 = s.indexOf("<");
        int p2 = s.indexOf(".");
        if ( p1 == -1) {
            if (p2 == -1) {
                if (s.endsWith(";")) {
                    return s.substring(0, s.length() - 1);
                } else {
                    return s;
                }
            } else {
                return s.substring(0, p2);
            }
        } else {
            int m = 0, p = 1;
            for (char c : s.substring(p1 + 1).toCharArray()) {
                if (c == '>') {
                    if (m == 0) {
                        String t = s.substring(0, p1 + p + 1);
                        return s.substring(0, p1 + p + 1);
                    } else {
                        m--;
                    }
                }
                if (c == '<') {
                    m++;
                }
                p++;
            }
        }
        throw new IllegalStateException(s + " no matched <> found");
    }
}
