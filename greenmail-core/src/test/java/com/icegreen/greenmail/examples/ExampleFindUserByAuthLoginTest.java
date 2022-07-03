package com.icegreen.greenmail.examples;

import com.icegreen.greenmail.mail.MailAddress;
import com.icegreen.greenmail.mail.MovingMessage;
import com.icegreen.greenmail.smtp.auth.AuthenticationState;
import com.icegreen.greenmail.smtp.auth.UsernameAuthentication;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.MessageDeliveryHandler;
import com.icegreen.greenmail.user.NoSuchUserException;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.user.UserManager;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * By default, GreenMail delivers messages to the user based on the email address
 * to which the mail was sent.
 * <p>
 * This example illustrates how you can use a custom message delivery handler to deliver
 * messages based on the login that was used to authenticate against the mail server.
 */
public class ExampleFindUserByAuthLoginTest {
    public GreenMail greenMail;

    @Test
    public void testSend() throws MessagingException, UserException, FolderException {
        final UserManager userManager = greenMail.getUserManager();
        // Create a new user
        userManager.createUser("from@localhost", "login", "pass");

        // Set a message delivery handler that find the user and inbox by
        // the login that was used.
        userManager.setMessageDeliveryHandler(new MessageDeliveryHandler() {
            @Override
            public GreenMailUser handle(MovingMessage msg, MailAddress mailAddress)
                    throws MessagingException, UserException {
                AuthenticationState authState = msg.getAuthenticationState();
                if (!(authState instanceof UsernameAuthentication)) {
                    throw new MessagingException("Authentication is required");
                }
                String login = ((UsernameAuthentication)authState).getUsername();
                GreenMailUser user = userManager.getUser(login);
                if (user == null) {
                    throw new NoSuchUserException("No user found for login " + login + ", make sure to create the user first");
                }
                return user;
            }
        });

        // Send a mail with an arbitrary FROM / TO address
        MimeMessage message = GreenMailUtil.createTextEmail("john@example.com", "mary@example.com",
                "some subject", "some body", ServerSetupTest.SMTP); // --- Place your sending code here instead
        GreenMailUtil.sendMimeMessage(message, "login", "pass");

        // Check that the mail was still sent to the user we created.
        GreenMailUser user = greenMail.getUserManager().getUser("login");
        MailFolder inbox = greenMail.getManagers().getImapHostManager().getInbox(user);
        assertThat(inbox.getMessages().get(0).getMimeMessage().getSubject()).isEqualTo("some subject");
        assertThat(inbox.getMessages().get(0).getMimeMessage().getRecipients(RecipientType.TO)[0].toString()).isEqualTo("john@example.com");
        assertThat(inbox.getMessages().get(0).getMimeMessage().getFrom()[0].toString()).isEqualTo("mary@example.com");
    }

    @Before
    public void setupMail() {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
    }

    @After
    public void tearDownMail() {
        greenMail.stop();
    }
}
