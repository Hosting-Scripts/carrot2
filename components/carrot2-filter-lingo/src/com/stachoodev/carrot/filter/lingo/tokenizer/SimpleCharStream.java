
/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2006, Dawid Weiss, Stanisław Osiński.
 * Portions (C) Contributors listed in "carrot2.CONTRIBUTORS" file.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * http://www.carrot2.org/carrot2.LICENSE
 */

package com.stachoodev.carrot.filter.lingo.tokenizer;

/**
 * An implementation of interface CharStream, where the stream is assumed to
 * contain only ASCII characters (without unicode processing).
 */
public final class SimpleCharStream {
    /** DOCUMENT ME! */
    public static final boolean staticFlag = false;

    /** DOCUMENT ME! */
    int bufsize;

    /** DOCUMENT ME! */
    int available;

    /** DOCUMENT ME! */
    int tokenBegin;

    /** DOCUMENT ME! */
    public int bufpos = -1;

    /** DOCUMENT ME! */
    private int[] bufline;

    /** DOCUMENT ME! */
    private int[] bufcolumn;

    /** DOCUMENT ME! */
    private int column = 0;

    /** DOCUMENT ME! */
    private int line = 1;

    /** DOCUMENT ME! */
    private boolean prevCharIsCR = false;

    /** DOCUMENT ME! */
    private boolean prevCharIsLF = false;

    /** DOCUMENT ME! */
    private java.io.Reader inputStream;

    /** DOCUMENT ME! */
    private char[] buffer;

    /** DOCUMENT ME! */
    private int maxNextCharInd = 0;

    /** DOCUMENT ME! */
    private int inBuf = 0;

    /**
     * DOCUMENT ME!
     *
     * @param wrapAround DOCUMENT ME!
     */
    private final void ExpandBuff(boolean wrapAround) {
        char[] newbuffer = new char[bufsize + 2048];
        int[] newbufline = new int[bufsize + 2048];
        int[] newbufcolumn = new int[bufsize + 2048];

        try {
            if (wrapAround) {
                System.arraycopy(buffer, tokenBegin, newbuffer, 0,
                    bufsize - tokenBegin);
                System.arraycopy(buffer, 0, newbuffer, bufsize - tokenBegin,
                    bufpos);
                buffer = newbuffer;

                System.arraycopy(bufline, tokenBegin, newbufline, 0,
                    bufsize - tokenBegin);
                System.arraycopy(bufline, 0, newbufline, bufsize - tokenBegin,
                    bufpos);
                bufline = newbufline;

                System.arraycopy(bufcolumn, tokenBegin, newbufcolumn, 0,
                    bufsize - tokenBegin);
                System.arraycopy(bufcolumn, 0, newbufcolumn,
                    bufsize - tokenBegin, bufpos);
                bufcolumn = newbufcolumn;

                maxNextCharInd = (bufpos += (bufsize - tokenBegin));
            } else {
                System.arraycopy(buffer, tokenBegin, newbuffer, 0,
                    bufsize - tokenBegin);
                buffer = newbuffer;

                System.arraycopy(bufline, tokenBegin, newbufline, 0,
                    bufsize - tokenBegin);
                bufline = newbufline;

                System.arraycopy(bufcolumn, tokenBegin, newbufcolumn, 0,
                    bufsize - tokenBegin);
                bufcolumn = newbufcolumn;

                maxNextCharInd = (bufpos -= tokenBegin);
            }
        } catch (Throwable t) {
            throw new Error(t.getMessage());
        }

        bufsize += 2048;
        available = bufsize;
        tokenBegin = 0;
    }

    /**
     * DOCUMENT ME!
     *
     * @throws java.io.IOException DOCUMENT ME!
     */
    private final void FillBuff() throws java.io.IOException {
        if (maxNextCharInd == available) {
            if (available == bufsize) {
                if (tokenBegin > 2048) {
                    bufpos = maxNextCharInd = 0;
                    available = tokenBegin;
                } else if (tokenBegin < 0) {
                    bufpos = maxNextCharInd = 0;
                } else {
                    ExpandBuff(false);
                }
            } else if (available > tokenBegin) {
                available = bufsize;
            } else if ((tokenBegin - available) < 2048) {
                ExpandBuff(true);
            } else {
                available = tokenBegin;
            }
        }

        int i;

        try {
            if ((i = inputStream.read(buffer, maxNextCharInd,
                            available - maxNextCharInd)) == -1) {
                inputStream.close();
                throw new java.io.IOException();
            } else {
                maxNextCharInd += i;
            }

            return;
        } catch (java.io.IOException e) {
            --bufpos;
            backup(0);

            if (tokenBegin == -1) {
                tokenBegin = bufpos;
            }

            throw e;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws java.io.IOException DOCUMENT ME!
     */
    public final char BeginToken() throws java.io.IOException {
        tokenBegin = -1;

        char c = readChar();
        tokenBegin = bufpos;

        return c;
    }

    /**
     * DOCUMENT ME!
     *
     * @param c DOCUMENT ME!
     */
    private final void UpdateLineColumn(char c) {
        column++;

        if (prevCharIsLF) {
            prevCharIsLF = false;
            line += (column = 1);
        } else if (prevCharIsCR) {
            prevCharIsCR = false;

            if (c == '\n') {
                prevCharIsLF = true;
            } else {
                line += (column = 1);
            }
        }

        switch (c) {
        case '\r':
            prevCharIsCR = true;

            break;

        case '\n':
            prevCharIsLF = true;

            break;

        case '\t':
            column--;
            column += (8 - (column & 07));

            break;

        default:
            break;
        }

        bufline[bufpos] = line;
        bufcolumn[bufpos] = column;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws java.io.IOException DOCUMENT ME!
     */
    public final char readChar() throws java.io.IOException {
        if (inBuf > 0) {
            --inBuf;

            if (++bufpos == bufsize) {
                bufpos = 0;
            }

            return buffer[bufpos];
        }

        if (++bufpos >= maxNextCharInd) {
            FillBuff();
        }

        char c = buffer[bufpos];

        UpdateLineColumn(c);

        return (c);
    }

    /**
     * @see #getEndColumn
     * @deprecated
     */
    public final int getColumn() {
        return bufcolumn[bufpos];
    }

    /**
     * @see #getEndLine
     * @deprecated
     */
    public final int getLine() {
        return bufline[bufpos];
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public final int getEndColumn() {
        return bufcolumn[bufpos];
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public final int getEndLine() {
        return bufline[bufpos];
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public final int getBeginColumn() {
        return bufcolumn[tokenBegin];
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public final int getBeginLine() {
        return bufline[tokenBegin];
    }

    /**
     * DOCUMENT ME!
     *
     * @param amount DOCUMENT ME!
     */
    public final void backup(int amount) {
        inBuf += amount;

        if ((bufpos -= amount) < 0) {
            bufpos += bufsize;
        }
    }

    /**
     * Creates a new SimpleCharStream object.
     *
     * @param dstream DOCUMENT ME!
     * @param startline DOCUMENT ME!
     * @param startcolumn DOCUMENT ME!
     * @param buffersize DOCUMENT ME!
     */
    public SimpleCharStream(java.io.Reader dstream, int startline,
        int startcolumn, int buffersize) {
        inputStream = dstream;
        line = startline;
        column = startcolumn - 1;

        available = bufsize = buffersize;
        buffer = new char[buffersize];
        bufline = new int[buffersize];
        bufcolumn = new int[buffersize];
    }

    /**
     * Creates a new SimpleCharStream object.
     *
     * @param dstream DOCUMENT ME!
     * @param startline DOCUMENT ME!
     * @param startcolumn DOCUMENT ME!
     */
    public SimpleCharStream(java.io.Reader dstream, int startline,
        int startcolumn) {
        this(dstream, startline, startcolumn, 4096);
    }

    /**
     * Creates a new SimpleCharStream object.
     *
     * @param dstream DOCUMENT ME!
     */
    public SimpleCharStream(java.io.Reader dstream) {
        this(dstream, 1, 1, 4096);
    }

    /**
     * DOCUMENT ME!
     *
     * @param dstream DOCUMENT ME!
     * @param startline DOCUMENT ME!
     * @param startcolumn DOCUMENT ME!
     * @param buffersize DOCUMENT ME!
     */
    public void ReInit(java.io.Reader dstream, int startline, int startcolumn,
        int buffersize) {
        inputStream = dstream;
        line = startline;
        column = startcolumn - 1;

        if ((buffer == null) || (buffersize != buffer.length)) {
            available = bufsize = buffersize;
            buffer = new char[buffersize];
            bufline = new int[buffersize];
            bufcolumn = new int[buffersize];
        }

        prevCharIsLF = prevCharIsCR = false;
        tokenBegin = inBuf = maxNextCharInd = 0;
        bufpos = -1;
    }

    /**
     * DOCUMENT ME!
     *
     * @param dstream DOCUMENT ME!
     * @param startline DOCUMENT ME!
     * @param startcolumn DOCUMENT ME!
     */
    public void ReInit(java.io.Reader dstream, int startline, int startcolumn) {
        ReInit(dstream, startline, startcolumn, 4096);
    }

    /**
     * DOCUMENT ME!
     *
     * @param dstream DOCUMENT ME!
     */
    public void ReInit(java.io.Reader dstream) {
        ReInit(dstream, 1, 1, 4096);
    }

    /**
     * Creates a new SimpleCharStream object.
     *
     * @param dstream DOCUMENT ME!
     * @param startline DOCUMENT ME!
     * @param startcolumn DOCUMENT ME!
     * @param buffersize DOCUMENT ME!
     */
    public SimpleCharStream(java.io.InputStream dstream, int startline,
        int startcolumn, int buffersize) {
        this(new java.io.InputStreamReader(dstream), startline, startcolumn,
            4096);
    }

    /**
     * Creates a new SimpleCharStream object.
     *
     * @param dstream DOCUMENT ME!
     * @param startline DOCUMENT ME!
     * @param startcolumn DOCUMENT ME!
     */
    public SimpleCharStream(java.io.InputStream dstream, int startline,
        int startcolumn) {
        this(dstream, startline, startcolumn, 4096);
    }

    /**
     * Creates a new SimpleCharStream object.
     *
     * @param dstream DOCUMENT ME!
     */
    public SimpleCharStream(java.io.InputStream dstream) {
        this(dstream, 1, 1, 4096);
    }

    /**
     * DOCUMENT ME!
     *
     * @param dstream DOCUMENT ME!
     * @param startline DOCUMENT ME!
     * @param startcolumn DOCUMENT ME!
     * @param buffersize DOCUMENT ME!
     */
    public void ReInit(java.io.InputStream dstream, int startline,
        int startcolumn, int buffersize) {
        ReInit(new java.io.InputStreamReader(dstream), startline, startcolumn,
            4096);
    }

    /**
     * DOCUMENT ME!
     *
     * @param dstream DOCUMENT ME!
     */
    public void ReInit(java.io.InputStream dstream) {
        ReInit(dstream, 1, 1, 4096);
    }

    /**
     * DOCUMENT ME!
     *
     * @param dstream DOCUMENT ME!
     * @param startline DOCUMENT ME!
     * @param startcolumn DOCUMENT ME!
     */
    public void ReInit(java.io.InputStream dstream, int startline,
        int startcolumn) {
        ReInit(dstream, startline, startcolumn, 4096);
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public final String GetImage() {
        if (bufpos >= tokenBegin) {
            return new String(buffer, tokenBegin, bufpos - tokenBegin + 1);
        } else {
            return new String(buffer, tokenBegin, bufsize - tokenBegin) +
            new String(buffer, 0, bufpos + 1);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param len DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public final char[] GetSuffix(int len) {
        char[] ret = new char[len];

        if ((bufpos + 1) >= len) {
            System.arraycopy(buffer, bufpos - len + 1, ret, 0, len);
        } else {
            System.arraycopy(buffer, bufsize - (len - bufpos - 1), ret, 0,
                len - bufpos - 1);
            System.arraycopy(buffer, 0, ret, len - bufpos - 1, bufpos + 1);
        }

        return ret;
    }

    /**
     * DOCUMENT ME!
     */
    public void Done() {
        buffer = null;
        bufline = null;
        bufcolumn = null;
    }

    /**
     * Method to adjust line and column numbers for the start of a token.<BR>
     */
    public void adjustBeginLineColumn(int newLine, int newCol) {
        int start = tokenBegin;
        int len;

        if (bufpos >= tokenBegin) {
            len = bufpos - tokenBegin + inBuf + 1;
        } else {
            len = bufsize - tokenBegin + bufpos + 1 + inBuf;
        }

        int i = 0;
        int j = 0;
        int k = 0;
        int nextColDiff = 0;
        int columnDiff = 0;

        while ((i < len) &&
                (bufline[j = start % bufsize] == bufline[k = ++start % bufsize])) {
            bufline[j] = newLine;
            nextColDiff = (columnDiff + bufcolumn[k]) - bufcolumn[j];
            bufcolumn[j] = newCol + columnDiff;
            columnDiff = nextColDiff;
            i++;
        }

        if (i < len) {
            bufline[j] = newLine++;
            bufcolumn[j] = newCol + columnDiff;

            while (i++ < len) {
                if (bufline[j = start % bufsize] != bufline[++start % bufsize]) {
                    bufline[j] = newLine++;
                } else {
                    bufline[j] = newLine;
                }
            }
        }

        line = bufline[j];
        column = bufcolumn[j];
    }
}
