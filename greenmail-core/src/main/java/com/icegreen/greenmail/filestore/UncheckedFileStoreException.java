package com.icegreen.greenmail.filestore;

public class UncheckedFileStoreException extends RuntimeException {

    public UncheckedFileStoreException(String message) {
        super(message);
    }

    public UncheckedFileStoreException(String message, Throwable t) {
        super(message, t);
    }

}
