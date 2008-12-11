// $Id$
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

import org.antlr.runtime.CharStream;
import org.antlr.runtime.Token;

/**
 * A class representing a token that is a byte. The token and the token type is
 * identified simply by its byte value.
 */
public class ByteToken implements Token {

    private byte val = 0;

    private int channel = Token.DEFAULT_CHANNEL;

    private int charPositionInLine = -1; // set to invalid position

    /**
     * Constructor. Create a new ByteToken instance with a given byte value.
     * 
     * @param value The byte value of the token.
     */
    public ByteToken(byte value) {
        setValue(value);
    }

    /**
     * Set the byte value of this token.
     * 
     * @param value The new byte value.
     */
    final void setValue(byte value) {
        val = value;
    }

    /**
     * Get the byte value of this token.
     * 
     * @return the byte value of this token.
     */
    final byte getValue() {
        return val;
    }

    /**
     * Get the value of the byte as a masked short (no sign extension if < 0).
     * 
     * @return The byte value of this token as a masked short.
     */
    final short getShortValue() {
        return (short) (val & (short) 0xff);
    }

    /**
     * Get the value of the byte as a masked int (no sign extension if < 0).
     * 
     * @return The byte value of this token as a masked int.
     */
    final int getIntValue() {
        return val & 0xff;
    }

    public int getChannel() {
        return channel;
    }

    public int getCharPositionInLine() {
        return charPositionInLine;
    }

    public CharStream getInputStream() {
        // returns null, we don't deal with CharStream, the Token interface
        // shouldn't depend on CharStream
        return null;
    }

    public int getLine() {
        // returns 0, because a binary file has no lines
        return 0;
    }

    public String getText() {
        return Byte.toString(val);
    }

    public int getTokenIndex() {
        return getIntValue();
    }

    public int getType() {
        return getIntValue();
    }

    public void setChannel(int arg0) {
        channel = arg0;
    }

    public void setCharPositionInLine(int arg0) {
        charPositionInLine = arg0;
    }

    public void setInputStream(CharStream arg0) {
        // do nothing, we don't deal with CharStream, the Token interface
        // shouldn't depend on CharStream
    }

    public void setLine(int arg0) {
        // do nothing, because a binary file has no lines
    }

    public void setText(String arg0) {
        try {
            val = Byte.parseByte(arg0);
        } catch (NumberFormatException e) {
            // silently keep the old value, what else can we do?
        }
    }

    public void setTokenIndex(int arg0) {
        val = (byte) arg0;
    }

    public void setType(int arg0) {
        val = (byte) arg0;
    }
}
