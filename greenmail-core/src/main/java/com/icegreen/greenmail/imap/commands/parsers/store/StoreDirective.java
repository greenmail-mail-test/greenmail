package com.icegreen.greenmail.imap.commands.parsers.store;

public class StoreDirective {
    private int sign;
    private boolean silent;

    public StoreDirective(int sign, boolean silent) {
        this.sign = sign;
        this.silent = silent;
    }

    public int getSign() {
        return sign;
    }

    public boolean isSilent() {
        return silent;
    }
}
