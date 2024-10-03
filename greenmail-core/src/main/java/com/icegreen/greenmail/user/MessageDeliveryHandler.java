package com.icegreen.greenmail.user;

import com.icegreen.greenmail.mail.MailAddress;
import com.icegreen.greenmail.mail.MovingMessage;

import jakarta.mail.MessagingException;

/**
 * Handles delivery when receiving messages.
 * <p>
 * Can be used for alternative implementation (e.g. Delivery Status Notification DSN).
 */
public interface MessageDeliveryHandler {
    GreenMailUser handle(MovingMessage msg, MailAddress mailAddress) throws MessagingException, UserException;
}
