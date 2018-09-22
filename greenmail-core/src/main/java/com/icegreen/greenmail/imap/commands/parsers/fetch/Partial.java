package com.icegreen.greenmail.imap.commands.parsers.fetch;

/** See https://tools.ietf.org/html/rfc3501#page-55 : partial */
public class Partial {
    private int start;
    private int size;

    public int computeLength(final int contentSize) {
        if ( size > 0) {
            return Math.min(size, contentSize - start); // Only up to max available bytes
        } else {
            // First len bytes
            return contentSize;
        }
    }

    public int computeStart(final int contentSize) {
        return Math.min(start, contentSize);
    }

    public static Partial as(int start, int size) {
        Partial p = new Partial();
        p.start = start;
        p.size = size;
        return p;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
