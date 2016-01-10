package com.icegreen.greenmail.examples;

import com.icegreen.greenmail.imap.AuthorizationException;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.sun.mail.util.MailSSLSocketFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;

public class ExampleSendReceiveMessageWithInlineAttachmentTest {

  private GreenMail emailServer;
  private final String emailAddress = "test@email.com";
  private final String emailServerHost = "127.0.0.1";
  private final int smtpPort = 2525;
  private final String imapUserName = "emailUser";
  private final String imapPassword = "emailPassword";
  private final int imapsPort = 9933;

  @Before
  public void startEmailServer() throws FolderException, AuthorizationException {
    ServerSetup smtp = new ServerSetup(smtpPort, emailServerHost, ServerSetup.PROTOCOL_SMTP);
    ServerSetup imaps = new ServerSetup(imapsPort, emailServerHost, ServerSetup.PROTOCOL_IMAPS);
    emailServer = new GreenMail(new ServerSetup[]{ smtp, imaps });

    emailServer.setUser(emailAddress, imapUserName, imapPassword);

    emailServer.start();
  }

  @Before
  public void enableDebugLogging() {
    System.setProperty("mail.imap.parse.debug", Boolean.TRUE.toString());
  }

  @After
  public void stopEmailServer() {
    emailServer.stop();
  }

  @Test
  public void sendAndFetchMailMessageWithInlineAttachment() throws IOException, MessagingException, GeneralSecurityException {
    sendMailMessageWithInlineAttachment();
    fetchEmailWithInlineAttachment();
  }

  private void fetchEmailWithInlineAttachment() throws MessagingException, GeneralSecurityException {
    Store store = connectToIMAPServer();
    Folder folder = openFolder(store, "INBOX");

    Message[] messages = folder.getMessages();

    for (Message message : messages) {
      // Calling getContextType() throws "javax.mail.MessagingException: Unable to load BODYSTRUCTURE"
      System.out.println("  Content Type: " + message.getContentType());
    }
  }

  private Folder openFolder(Store store, String folderName) throws MessagingException {
    Folder folder = store.getDefaultFolder();
    folder = folder.getFolder(folderName);
    folder.open(Folder.READ_WRITE);
    return folder;
  }

  private Store connectToIMAPServer() throws GeneralSecurityException, MessagingException {
    Properties props = new Properties();

    MailSSLSocketFactory socketFactory = new MailSSLSocketFactory();
    socketFactory.setTrustedHosts(new String[]{"127.0.0.1", "localhost"});
    props.put("mail.imaps.ssl.socketFactory", socketFactory);

    Session session = Session.getInstance(props, null);
    Store store = session.getStore("imaps");
    store.connect(emailServerHost, imapsPort, imapUserName, imapPassword);

    return store;
  }

  protected MimeMessage newEmailTo(Session session, String recipient, String subject) throws MessagingException {
    MimeMessage message = new MimeMessage(session);

    InternetAddress[] to = {new InternetAddress(recipient)};
    message.setRecipients(Message.RecipientType.TO, to);
    message.setSubject(subject);
    return message;
  }

  protected Session newEmailSession(boolean debug) {
    Properties props = new Properties();
    props.put("mail.smtp.host", emailServerHost);
    props.put("mail.smtp.port", smtpPort);
    Session session = Session.getDefaultInstance(props, null);
    session.setDebug(debug);
    return session;
  }

  private void sendMailMessageWithInlineAttachment() throws MessagingException {
    MimeMessage message = newEmailTo(newEmailSession(true), emailAddress, "Message with inline attachment");

    MimeBodyPart textPart = new MimeBodyPart();
    textPart.setContent("This is some text to be displayed inline", "text/plain");

    // Try not to display text as separate attachment
    textPart.setDisposition("inline");

    Multipart mp = new MimeMultipart();
    mp.addBodyPart(textPart);

    message.setContent(mp);

    Transport.send(message);
  }

}
