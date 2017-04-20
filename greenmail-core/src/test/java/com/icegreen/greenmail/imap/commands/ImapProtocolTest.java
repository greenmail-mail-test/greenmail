package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.iap.Argument;
import com.sun.mail.iap.ByteArray;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.protocol.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Low level IMAP protocol test cases.
 */
public class ImapProtocolTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);
    private GreenMailUser user;
    private IMAPStore store;

    @Before
    public void beforeEachTest() throws NoSuchProviderException {
        user = greenMail.setUser("foo@localhost", "pwd");

        int numberOfMails = 10;
        for (int i = 0; i < numberOfMails; i++) {
            GreenMailUtil.sendTextEmail("foo@localhost", "bar@localhost", "Test search " + i,
                    "Test message content" + i,
                    greenMail.getSmtp().getServerSetup());
        }
        greenMail.waitForIncomingEmail(numberOfMails);

        store = greenMail.getImap().createStore();
    }

    @Test
    public void testFetchUidsAndSize() throws MessagingException {
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            Response[] ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("UID FETCH 1:* RFC822.SIZE", null);
                }
            });

            FetchResponse fetchResponse = (FetchResponse) ret[0];
            assertFalse(fetchResponse.isBAD());
            assertEquals(2, fetchResponse.getItemCount()); // UID and SIZE

            RFC822SIZE size = fetchResponse.getItem(RFC822SIZE.class);
            assertNotNull(size);
            assertTrue(size.size > 0);

            UID uid = fetchResponse.getItem(UID.class);
            assertEquals(folder.getUID(folder.getMessage(1)), uid.uid);
        } finally {
            store.close();
        }
    }

    @Test
    public void testFetchSpaceBeforeSize() throws MessagingException {
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);

            // Fetch without partial as reference
            Response[] ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("UID FETCH 1 (BODY[HEADER])", null);
                }
            });
            FetchResponse fetchResponse = (FetchResponse) ret[0];
            assertFalse(fetchResponse.isBAD());
            assertEquals(3, fetchResponse.getItemCount()); // UID, BODY, FLAGS

            BODY body = fetchResponse.getItem(BODY.class);
            assertTrue(body.isHeader());
            final String content = new String(body.getByteArray().getNewBytes());

            UID uid = fetchResponse.getItem(UID.class);
            assertEquals(folder.getUID(folder.getMessage(1)), uid.uid);

            // partial size only
            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("UID FETCH 1 (BODY[HEADER]<50>)", null);
                }
            });
            fetchResponse = (FetchResponse) ret[0];
            assertFalse(fetchResponse.isBAD());
            assertEquals(2, fetchResponse.getItemCount()); // UID, BODY

            body = fetchResponse.getItem(BODY.class);
            assertTrue(body.isHeader());
            assertEquals(50, body.getByteArray().getCount());

            uid = fetchResponse.getItem(UID.class);
            assertEquals(folder.getUID(folder.getMessage(1)), uid.uid);

            // partial size and zero offset
            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("UID FETCH 1 (BODY[HEADER]<0.30>)", null);
                }
            });
            fetchResponse = (FetchResponse) ret[0];
            assertFalse(fetchResponse.isBAD());
            assertEquals(2, fetchResponse.getItemCount()); // UID , BODY

            body = fetchResponse.getItem(BODY.class);
            assertTrue(body.isHeader());
            assertEquals(30, body.getByteArray().getCount());

            uid = fetchResponse.getItem(UID.class);
            assertEquals(folder.getUID(folder.getMessage(1)), uid.uid);

            // partial size and non zero offset
            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("UID FETCH 1 (BODY[HEADER]<10.30>)", null);
                }
            });
            fetchResponse = (FetchResponse) ret[0];
            assertFalse(fetchResponse.isBAD());
            assertEquals(2, fetchResponse.getItemCount()); // UID and SIZE

            body = fetchResponse.getItem(BODY.class);
            assertTrue(body.isHeader());
            final ByteArray byteArray = body.getByteArray();
            assertEquals(30, byteArray.getCount());
            assertEquals(content.substring(10, 10 + 30), new String(byteArray.getNewBytes()));

            uid = fetchResponse.getItem(UID.class);
            assertEquals(folder.getUID(folder.getMessage(1)), uid.uid);
        } finally {
            store.close();
        }
    }

    @Test
    public void testSearchSequenceSet() throws MessagingException {
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            Response[] ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("SEARCH 1", null);
                }
            });
            IMAPResponse response = (IMAPResponse) ret[0];
            assertFalse(response.isBAD());
            assertEquals("1", response.getRest());

            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("SEARCH 2:2", null);
                }
            });
            response = (IMAPResponse) ret[0];
            assertFalse(response.isBAD());
            assertTrue(ret[1].isOK());
            assertEquals("2", response.getRest());

            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("SEARCH 2:4", null);
                }
            });
            response = (IMAPResponse) ret[0];
            assertFalse(response.isBAD());
            assertEquals("2 3 4", response.getRest());
            assertTrue(ret[1].isOK());

            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("SEARCH 1 2:4 8", null);
                }
            });
            response = (IMAPResponse) ret[0];
            assertFalse(response.isBAD());
            assertEquals("1 2 3 4 8", response.getRest());
            assertTrue(ret[1].isOK());
        } finally {
            store.close();
        }
    }

    @Test
    public void testUidSearchSequenceSet() throws MessagingException {
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);

            final Message[] messages = folder.getMessages();
            Map<Integer, Long> uids = new HashMap<>();
            for (Message msg : messages) {
                uids.put(msg.getMessageNumber(), folder.getUID(msg));
            }

            Response[] ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("UID SEARCH 1", null);
                }
            });
            IMAPResponse response = (IMAPResponse) ret[0];
            assertFalse(response.isBAD());
            assertEquals(uids.get(1).toString(), response.getRest());

            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("UID SEARCH 2:2", null);
                }
            });
            response = (IMAPResponse) ret[0];
            assertFalse(response.isBAD());
            assertTrue(ret[1].isOK());
            assertEquals(uids.get(2).toString(), response.getRest());

            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("UID SEARCH 2:4", null);
                }
            });
            response = (IMAPResponse) ret[0];
            assertFalse(response.isBAD());
            assertEquals(msnListToUidString(uids, 2, 3, 4), response.getRest());
            assertTrue(ret[1].isOK());

            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("UID SEARCH 1 2:4 8", null);
                }
            });
            response = (IMAPResponse) ret[0];
            assertFalse(response.isBAD());
            assertEquals(msnListToUidString(uids, 1, 2, 3, 4, 8), response.getRest());
            assertTrue(ret[1].isOK());
        } finally {
            store.close();
        }
    }

    @Test
    public void testGetMessageByUnknownUidInEmptyINBOX() throws MessagingException, FolderException {
        greenMail.getManagers()
            .getImapHostManager()
            .getInbox(user)
            .deleteAllMessages();
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            Message message = folder.getMessageByUID(666);
            assertEquals(null, message);
        } finally {
            store.close();
        }
    }

    @Test
    public void testUidSearchText() throws MessagingException, IOException {
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);

            final Message[] messages = folder.getMessages();
            Map<Integer, String> uids = new HashMap<>();
            for (Message msg : messages) {
                uids.put(msg.getMessageNumber(), Long.toString(folder.getUID(msg)));
            }

            // messages[2] contains content with search text, match must be case insensitive
            final String searchText1 = "conTEnt2";
            Response[] ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("UID SEARCH TEXT " + searchText1, null);
                }
            });
            IMAPResponse response = (IMAPResponse) ret[0];
            assertFalse(response.isBAD());
            assertEquals(uids.get(messages[2].getMessageNumber()), response.getRest());

            // messages[2] contains search text in CC, with different upper case
            final String searchText2 = "foo@localHOST";
            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("UID SEARCH TEXT " + searchText2, null);
                }
            });
            response = (IMAPResponse) ret[0];
            assertFalse(response.isBAD());
            // Match all
            assertArrayEquals(uids.values().toArray(), response.getRest().split(" "));
        } finally {
            store.close();
        }
    }

    @Test
    public void testRenameFolder() throws MessagingException {
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);
            Response[] ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("CREATE foo", null);
                }
            });

            IMAPResponse response = (IMAPResponse) ret[0];
            assertFalse(response.isBAD());

            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("RENAME foo bar", null);
                }
            });

            Response response2 = ret[0];
            assertTrue(response2.isOK());

            final Folder bar = store.getFolder("bar");
            bar.open(Folder.READ_ONLY);
            assertTrue(bar.exists());
        } finally {
            store.close();
        }
    }

    @Test
    public void testUidSearchTextWithCharset() throws MessagingException, IOException {
        greenMail.setUser("foo2@localhost", "pwd");
        store.connect("foo2@localhost", "pwd");
        try {
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);

            final MimeMessage email = GreenMailUtil.createTextEmail("foo2@localhost", "foo@localhost",
                    "some subject", "some content",
                    greenMail.getSmtp().getServerSetup());

            String[][] s = {
                    {"US-ASCII", "ABC", "1"},
                    {"ISO-8859-15", "\u00c4\u00e4\u20AC", "2"},
                    {"UTF-8", "\u00c4\u00e4\u03A0", "3"}
            };

            for (String[] charsetAndQuery : s) {
                final String charset = charsetAndQuery[0];
                final String search = charsetAndQuery[1];

                email.setSubject("subject " + search, charset);
                GreenMailUtil.sendMimeMessage(email);

                // messages[2] contains content with search text, match must be case insensitive
                final byte[] searchBytes = search.getBytes(charset);
                final Argument arg = new Argument();
                arg.writeBytes(searchBytes);
                Response[] ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                    @Override
                    public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                        return protocol.command("UID SEARCH CHARSET " + charset + " TEXT", arg);
                    }
                });
                IMAPResponse response = (IMAPResponse) ret[0];
                assertFalse(response.isBAD());
                String number = response.getRest();
                assertEquals(charsetAndQuery[2], number);
            }
        } finally {
            store.close();
        }
    }

    private String msnListToUidString(Map<Integer, Long> uids, int... msnList) {
        StringBuilder buf = new StringBuilder();
        for (int msn : msnList) {
            if (buf.length() > 0) {
                buf.append(' ');
            }
            buf.append(uids.get(msn));
        }
        return buf.toString();
    }
}
