package com.icegreen.greenmail.filestore;

/**
 * Created by saladin on 11/2/16.
 */
public class UncheckedFileStoreException extends RuntimeException {

	public UncheckedFileStoreException(String message) {
		super(message);
	}

	public UncheckedFileStoreException(String message, Throwable t) {
		super(message, t);
	}

}
