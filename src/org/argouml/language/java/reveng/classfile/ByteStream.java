package org.argouml.language.java.reveng.classfile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.antlr.runtime.ANTLRStringStream;

/**
 * A simple byte stream that completely loads a specified amount of data from
 * an InputStream instance into memory (byte array) at once.
 * 
 * For simplicity, ANTLRStringStream was reused, which uses a char array. For
 * using a byte array, a lot of stuff need to be coded.
 * 
 * @author Thomas Neustupny <thn@tigris.org>
 */
public class ByteStream extends ANTLRStringStream {

    /**
     * Constructor. Loads all data from the input stream to an array.
     * 
     * @param is the input stream
     * @param size the amount of bytes to read
     */
    public ByteStream(InputStream is, int size) {
        if (size > 0) {
            data = new char[size];
            byte[] bytes = new byte[size];
            BufferedInputStream bis = new BufferedInputStream(is);
            try {
                bis.read(bytes);
                System.arraycopy(bytes, 0, data, 0, size);
            } catch (IOException e) {
                // TODO: add something here
            }
        }
    }
}
