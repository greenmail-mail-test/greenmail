package com.icegreen.greenmail.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Helps to extract info from mime messages.
 */
public class MimeMessageHelper {
    /** Prevent instantiation */
    private MimeMessageHelper() {
    }

    /**
     * Gets subject or default if error.
     * Wraps exception-loaded method for simplified usage in e.g. predicates.
     *
     * @param msg            the mime message
     * @param defaultIfError default to return should an exception occur.
     * @return
     */
    public static String getSubject(MimeMessage msg, String defaultIfError) {
        try {
            return msg.getSubject();
        } catch (MessagingException e) {
            return defaultIfError;
        }
    }
}
