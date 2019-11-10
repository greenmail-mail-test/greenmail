/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.store;

/**
 * Thrown on an inappropriate attempt to reference a mailbox.
 * Includes attempting to create a mailbox that already exists and attempting
 * to open a mailbox that does not exist.
 * If status is ALREADY_EXISTS_REMOTELY or IF_CREATED_REMOTE then field
 * remoteServer should be set to the url of the remote server, formatted for
 * Mailbox Referral.
 *
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */
public class FolderException extends Exception {
    static final long serialVersionUID = 8759050490529078967L;

    public static final String ALREADY_EXISTS_LOCALLY
            = "Already exists locally";
    public static final String ALREADY_EXISTS_REMOTELY
            = "Already exists remotely";
    public static final String IF_CREATED_LOCAL
            = "If created, mailbox would be local";
    public static final String IF_CREATED_REMOTE
            = "If created, mailbox would be remote";
    public static final String NOT_LOCAL
            = "Does not exist locally, no further information available";
    public static final String LOCAL_BUT_DELETED
            = "Was local but has been deleted.";

    private String status = null;
    private String remoteServer = null;
    private String responseCode = null;

    /**
     * Construct a new <code>MailboxException</code> instance.
     *
     * @param message The detail message for this exception (mandatory).
     */
    public FolderException(String message) {
        super(message);
    }

    /**
     * Constructor wrapping original exception.
     *
     * @param ex The cause.
     */
    public FolderException(Exception ex) {
        super(ex);
    }

    /**
     * Constructor wrapping original exception.
     *
     * @param message The detail message for this exception (mandatory).
     * @param ex The cause.
     */
    public FolderException(String message, Exception ex) {
        super(message, ex);
    }

    /**
     * Construct a new <code>MailBoxException</code> instance.
     *
     * @param message The detail message for this exception (mandatory).
     * @param aStatus String constant indicating condition
     */
    public FolderException(String message, String aStatus) {
        super(message);
        this.status = aStatus;
    }

    /**
     * Construct a new <code>MailBoxException</code> instance.
     *
     * @param message The detail message for this exception (mandatory).
     * @param aStatus String constant indicating condition
     * @param aServer String indicating another server where Mailbox should be.
     */
    public FolderException(String message, String aStatus, String aServer) {
        super(message);
        this.status = aStatus;
        this.remoteServer = aServer;
    }

    public String getStatus() {
        return status;
    }

    public String getRemoteServer() {
        return remoteServer;
    }

    public boolean isRemote() {
        return status.equals(ALREADY_EXISTS_REMOTELY)
                || status.equals(IF_CREATED_REMOTE);
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }
}
