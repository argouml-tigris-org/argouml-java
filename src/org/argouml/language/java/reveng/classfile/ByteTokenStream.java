package org.argouml.language.java.reveng.classfile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.Token;
import org.antlr.runtime.TokenSource;
import org.antlr.runtime.TokenStream;

/**
 * A byte token stream that completely loads a specified amount of data from an
 * InputStream instance into memory (byte array) at once.
 *
 * For simplicity, ANTLRStringStream was reused, which uses a char array. For
 * using a byte array, a lot of stuff need to be coded.
 *
 * @author Thomas Neustupny <thn@tigris.org>
 */
public class ByteTokenStream implements TokenStream {

    /** The data being scanned */
    private byte[] data;

    /** 0..n-1 index into data of next byte */
    private int p = 0;

    /** tracks how deep mark() calls are nested */
    private int markDepth = 0;

    /**
     * A list of Integer objects that tracks the stream state value p that can
     * change as you move through the byte stream. Indexed from 1..markDepth. A
     * null is kept at index 0. Create upon first call to mark().
     */
    private List<Integer> markers;

    /** Track the last mark() call result value for use in rewind(). */
    private int lastMarker;

    /** What is name or source of this char stream? */
    public String name;

    /**
     * Constructor. Loads all data from the input stream to an array.
     *
     * @param is the input stream
     * @param size the amount of bytes to read
     */
    public ByteTokenStream(InputStream is, int size) {
        if (is != null && size > 0) {
            data = new byte[size];
            BufferedInputStream bis = new BufferedInputStream(is);
            try {
                bis.read(data);
            } catch (IOException e) {
                // TODO: add something here
            }
            name = is.toString();
        }
    }

    public int LA(int i) {
        if (i == 0) {
            return 0; // undefined
        }
        if (i < 0) {
            i++; // e.g., translate LA(-1) to use offset i=0; then
            // data[p+0-1]
            if ((p + i - 1) < 0) {
                //return ByteToken.EOF; // invalid; no byte before first byte
            }
        }
        if ((p + i - 1) >= data.length) {
            //return ByteToken.EOF;
        }
        // this is tricky: bytes match with the token type that has a value of
        // 4 higher than the byte value, so we add 4:
        return (data[p + i - 1] & 0xff) + 4;
    }

    public void consume() {
        // trivial because each byte is a token
        if (p < data.length) {
            p++;
        }
    }

    public String getSourceName() {
        return name;
    }

    public int index() {
        return p;
    }

    public int mark() {
        if (markers == null) {
            markers = new ArrayList<Integer>();
            markers.add(null); // depth 0 means no backtracking, leave blank
        }
        markDepth++;
        Integer state = null;
        if (markDepth >= markers.size()) {
            state = new Integer(p);
            markers.add(state);
        } else {
            state = markers.get(markDepth);
        }
        lastMarker = markDepth;
        return markDepth;
    }

    public void release(int marker) {
        // unwind any other markers made after m and release m
        markDepth = marker;
        // release this marker
        markDepth--;
    }

    public void rewind() {
        rewind(lastMarker);
    }

    public void rewind(int m) {
        Integer state = markers.get(m);
        // restore stream state
        seek(state.intValue());
        release(m);
    }

    public void seek(int index) {
        // just jump, trivial because each byte is a token
        // otherwise, a sequence of consume() call might be needed
        p = index;
    }

    public int size() {
        return data.length;
    }

    public Token LT(int i) {
        return null; //new ByteToken((byte) LA(i));
    }

    public Token get(int i) {
        return null; //new ByteToken(data[i - 1]);
    }

    public TokenSource getTokenSource() {
        // not needed in the parser
        return null;
    }

    public String toString(int arg0, int arg1) {
        // not needed in the parser
        return null;
    }

    public String toString(Token arg0, Token arg1) {
        // not needed in the parser
        return null;
    }
}
