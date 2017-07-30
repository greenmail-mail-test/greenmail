package com.icegreen.greenmail.jboss;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.imap.ImapServer;
import com.icegreen.greenmail.pop3.Pop3Server;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.user.UserException;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.Service;
import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * Implements the GreenMailServiceMBean.
 *
 * @author Marcel May
 */
public class GreenMailService extends ServiceMBeanSupport implements GreenMailServiceMBean {
    /** New logger. */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private Managers managers;
    private Map<ServiceProtocol, Service> services =
            new EnumMap<>(ServiceProtocol.class);

    /** Default port offset is {@value}. */
    public static final int DEFAULT_PORT_OFFSET = 3000;

    /** SMTP server */
    private boolean mSmtpProtocol = true;  // activated by default
    /** SMTPS server */
    private boolean mSmtpsProtocol;
    /** POP3 server */
    private boolean mPop3Protocol = true;  // activated by default
    /** POP3S server */
    private boolean mPop3sProtocol;
    /** IMAP server. */
    private boolean mImapProtocol = true; // activated by default
    /** IMAPS server. */
    private boolean mImapsProtocol;
    /** Users. */
    private String[] mUsers;
    /** Port offset (default is {@value #DEFAULT_PORT_OFFSET}) */
    private int mPortOffset = DEFAULT_PORT_OFFSET;
    /** Hostname (defaults to {@value}). */
    private String mHostname = "127.0.0.1";

    // ****** GreenMail service methods


    /** {@inheritDoc} */
    @Override
    public void setHostname(final String pHostname) {
        mHostname = pHostname;
    }

    /** {@inheritDoc} */
    @Override
    public String getHostname() {
        return mHostname;
    }

    /** {@inheritDoc} */
    @Override
    public void setUsers(final String[] theUsers) {
        mUsers = theUsers;
        // Cleanup new line and ws
        for (int i = 0; i < theUsers.length; i++) {
            mUsers[i] = mUsers[i].trim();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String[] getUsers() {
        return mUsers;
    }

    /** {@inheritDoc} */
    @Override
    public void sendMail(final String theTo,
                         final String theFrom,
                         final String theSubject,
                         final String theBody) {
        if (log.isDebugEnabled()) {
            log.debug("Sending mail, TO: <" + theTo + ">, FROM: <" + theFrom +
                    ">, SUBJECT: <" + theSubject + ">, CONTENT: <" + theBody + '>');
        }

        try {
            SmtpServer smtpOrSmtpsService = (SmtpServer) (services.containsKey(ServiceProtocol.SMTP) ?
                    services.get(ServiceProtocol.SMTP) : services.get(ServiceProtocol.SMTPS));
            if (null == smtpOrSmtpsService) {
                throw new IllegalStateException("No required smtp or smtps service configured!");
            }

            Session session = smtpOrSmtpsService.createSession();

            Address[] tos = new InternetAddress[]{new InternetAddress(theTo)};
            Address from = new InternetAddress(theFrom);
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setSubject(theSubject);
            mimeMessage.setFrom(from);

            mimeMessage.setText(theBody);
            Transport.send(mimeMessage, tos);
        } catch (Exception e) {
            throw new RuntimeException("Can not send mail", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String listUsersHTML() {
        StringBuilder buf = new StringBuilder();
        buf.append("Format: username:pwd@dns-resolvable-domain<br/><ul>");
        for (String mUser : mUsers) {
            buf.append("<li>").append(mUser).append("</li>");
        }
        buf.append("</ul>");
        return buf.toString();
    }

    /** {@inheritDoc} */
    @Override
    public String listMailsForUserHTML(String pEmail) {
        GreenMailUser user = managers.getUserManager().getUserByEmail(pEmail);
        if (null == user) {
            return " No such user for email " + pEmail;
        }
        StringBuilder builder = new StringBuilder("<table>");
        try {
            MailFolder mailFolder = managers.getUserManager().getImapHostManager().getInbox(user);
            builder.append("<caption>").append(mailFolder.getMessageCount())
                    .append(" Mails for ").append(pEmail).append("</caption>");
            builder.append(
                    "<tr><th>From</th><th>Subject</th><th>Received date</th><th>Content</th></tr>");
            for (StoredMessage msg : mailFolder.getMessages()) {
                MimeMessage mimeMessage = msg.getMimeMessage();
                builder.append("<tr>");
                builder.append("<td>").append(
                        Arrays.toString(mimeMessage.getFrom())).append("</td>");
                builder.append("<td>").append(mimeMessage.getSubject()).append("</td>");
                builder.append("<td>")
                        .append(null == mimeMessage.getReceivedDate()
                                ? msg.getReceivedDate()
                                : mimeMessage.getReceivedDate())
                        .append("</td>");

                Object content = mimeMessage.getContent();
                if (content instanceof MimeMultipart) {
                    MimeMultipart multipart = (MimeMultipart) content;
                    builder.append("<td>").append(multipart.getBodyPart(0).getContent()).append("</td>");
                } else {
                    builder.append("<td>").append(mimeMessage.getContent()).append("</td>");
                }
                builder.append("</tr>");
            }
        } catch (IOException | MessagingException | FolderException e) {
            throw new IllegalStateException(e);
        }
        builder.append("</table>");
        return builder.toString();
    }
    // ****** JBoss Service methods

    @Override
    public void startService() throws Exception {
        super.start();
        startGreenMailService();
    }

    public void startGreenMailService() {
        if (null == managers) {
            managers = new Managers();
        }

        // Add users
        for (String user : mUsers) {
            addMailUser(user);
        }

        // For start - start cycle
        stopGreenMailServices();

        log.info("Starting {}", getServiceName());
        StringBuilder buf = new StringBuilder("GreenMail configuration: ")
                .append(" hostname=")
                .append(getHostname()).append(", ");
        // Configure services
        if (mSmtpProtocol) {
            ServerSetup serverSetup = createTestServerSetup(ServerSetup.SMTP);
            startGreenMailService(ServiceProtocol.SMTP, new SmtpServer(serverSetup, managers));
            buf.append(", ").append(serverSetup.getProtocol()).append('=').append(serverSetup.getPort()).append(' ');
        }
        if (mSmtpsProtocol) {
            ServerSetup serverSetup = createTestServerSetup(ServerSetup.SMTPS);
            startGreenMailService(ServiceProtocol.SMTPS, new SmtpServer(serverSetup, managers));
            buf.append(serverSetup.getProtocol()).append('=').append(serverSetup.getPort());
        }
        if (mPop3Protocol) {
            ServerSetup serverSetup = createTestServerSetup(ServerSetup.POP3);
            startGreenMailService(ServiceProtocol.POP3, new Pop3Server(serverSetup, managers));
            buf.append(serverSetup.getProtocol()).append('=').append(serverSetup.getPort()).append(' ');
        }
        if (mPop3sProtocol) {
            ServerSetup serverSetup = createTestServerSetup(ServerSetup.POP3S);
            startGreenMailService(ServiceProtocol.POP3S,
                                  new Pop3Server(serverSetup, managers));
            buf.append(serverSetup.getProtocol()).append('=').append(serverSetup.getPort()).append(' ');
        }
        if (mImapProtocol) {
            ServerSetup serverSetup = createTestServerSetup(ServerSetup.IMAP);
            startGreenMailService(ServiceProtocol.IMAP,
                                  new ImapServer(serverSetup, managers));
            buf.append(serverSetup.getProtocol()).append('=').append(serverSetup.getPort()).append(' ');
        }
        if (mImapsProtocol) {
            ServerSetup serverSetup = createTestServerSetup(ServerSetup.IMAPS);
            startGreenMailService(ServiceProtocol.IMAPS,
                                  new ImapServer(serverSetup, managers));
            buf.append(serverSetup.getProtocol()).append('=').append(serverSetup.getPort()).append(' ');
        }

        log.info(buf.toString());
    }

    private void startGreenMailService(ServiceProtocol pProtocol, Service pNewService) {
        services.put(pProtocol, pNewService);
        log.debug("Starting {}", pNewService);
        pNewService.startService();
    }

    void stopGreenMailServices() {
        if (!services.isEmpty()) {
            for (Service service : services.values()) {
                if (service.isRunning()) {
                    service.stopService();
                    log.debug("Stopped {}", service);
                }
            }
        }
    }

    @Override
    public void stopService() {
        stopGreenMailServices();
        super.stop();
    }

    private void addMailUser(final String user) {
        // Parse ...
        int posColon = user.indexOf(':');
        int posAt = user.indexOf('@');
        String login = user.substring(0, posColon);
        String pwd = user.substring(posColon + 1, posAt);
        String domain = user.substring(posAt + 1);
        String email = login + '@' + domain;
        if (log.isDebugEnabled()) {
            // This is a test system, so we do not care about pwd in the log file.
            log.debug("Adding user " + login + ':' + pwd + '@' + domain);
        }


        GreenMailUser greenMailUser = managers.getUserManager().getUser(email);
        if (null == greenMailUser) {
            try {
                greenMailUser = managers.getUserManager().createUser(email, login, pwd);
                greenMailUser.setPassword(pwd);
            } catch (UserException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Creates a test server setup with configured offset.
     *
     * @param pServerSetup the server setup.
     * @return the test server setup.
     */
    private ServerSetup createTestServerSetup(final ServerSetup pServerSetup) {
        return new ServerSetup(mPortOffset + pServerSetup.getPort(),
                               mHostname,
                               pServerSetup.getProtocol());
    }

    /** {@inheritDoc} */
    @Override
    public void setSmtpsProtocol(final boolean theSmtpsProtocol) {
        mSmtpsProtocol = theSmtpsProtocol;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSmtpsProtocol() {
        return mSmtpsProtocol;
    }

    /** {@inheritDoc} */
    @Override
    public void setSmtpProtocol(final boolean theSmtpProtocol) {
        mSmtpProtocol = theSmtpProtocol;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSmtpProtocol() {
        return mSmtpProtocol;
    }

    /** {@inheritDoc} */
    @Override
    public void setPop3sProtocol(final boolean thePop3sProtocol) {
        mPop3sProtocol = thePop3sProtocol;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPop3sProtocol() {
        return mPop3sProtocol;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isImapsProtocol() {
        return mImapsProtocol;
    }

    /** {@inheritDoc} */
    @Override
    public void setImapsProtocol(final boolean theImapsProtocol) {
        mImapsProtocol = theImapsProtocol;
    }

    /** {@inheritDoc} */
    @Override
    public void setPop3Protocol(final boolean thePop3Protocol) {
        mPop3Protocol = thePop3Protocol;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPop3Protocol() {
        return mPop3Protocol;
    }

    /** {@inheritDoc} */
    @Override
    public void setImapProtocol(final boolean theImapFlag) {
        mImapProtocol = theImapFlag;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isImapProtocol() {
        return mImapProtocol;
    }

    /** {@inheritDoc} */
    @Override
    public void setPortOffset(final int thePortOffset) {
        mPortOffset = thePortOffset;
    }

    /** {@inheritDoc} */
    @Override
    public int getPortOffset() {
        return mPortOffset;
    }
}

