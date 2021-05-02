package com.icegreen.greenmail.user;

import com.icegreen.greenmail.mail.MailAddress;
import com.icegreen.greenmail.mail.MovingMessage;

import javax.mail.MessagingException;

public interface MessageDeliveryHandler {
    GreenMailUser handle(MovingMessage msg, MailAddress mailAddress) throws MessagingException, UserException;
}
