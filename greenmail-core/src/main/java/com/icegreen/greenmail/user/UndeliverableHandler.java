package com.icegreen.greenmail.user;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icegreen.greenmail.mail.MailAddress;
import com.icegreen.greenmail.mail.MovingMessage;

public class UndeliverableHandler {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected UserManager userManager;

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    public GreenMailUser handle(MovingMessage msg, MailAddress mailAddress) throws MessagingException, UserException {
        String login = mailAddress.getEmail();
        String email = mailAddress.getEmail();
        String password = mailAddress.getEmail();
        GreenMailUser user = userManager.createUser(email, login, password);
        log.info("Created user login {} for address {} with password {} because it didn't exist before.", login, email,
                password);
        return user;
    }
}
