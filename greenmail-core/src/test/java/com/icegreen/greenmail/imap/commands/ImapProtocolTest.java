package com.icegreen.greenmail.imap.commands;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.mail.*;
import javax.mail.internet.MimeMessage;

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

import static org.assertj.core.api.Assertions.*;

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
    public void testListAndStatusWithNonExistingFolder() throws MessagingException {
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            assertThat(folder.getFolder("non existent folder").exists()).isFalse();
            for (final String cmd : new String[]{
                    "STATUS \"non existent folder\" (MESSAGES UIDNEXT UIDVALIDITY UNSEEN)",
                    "SELECT \"non existent folder\""
            }) {
                Response[] ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                    @Override
                    public Object doCommand(IMAPProtocol protocol) {
                        return protocol.command(cmd, null);
                    }
                });

                IMAPResponse response = (IMAPResponse) ret[0];
                assertThat(response.isNO()).isTrue();
            }
        } finally {
            store.close();
        }
    }

    @Test
    public void testFetchUidsAndSize() throws MessagingException {
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            Response[] ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) {
                    return protocol.command("UID FETCH 1:* RFC822.SIZE", null);
                }
            });

            FetchResponse fetchResponse = (FetchResponse) ret[0];
            assertThat(fetchResponse.isBAD()).isFalse();
            assertThat(fetchResponse.getItemCount()).isEqualTo(2); // UID and SIZE

            RFC822SIZE size = fetchResponse.getItem(RFC822SIZE.class);
            assertThat(size).isNotNull();
            assertThat(size.size > 0).isTrue();

            UID uid = fetchResponse.getItem(UID.class);
            assertThat(uid.uid).isEqualTo(folder.getUID(folder.getMessage(1)));
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
                public Object doCommand(IMAPProtocol protocol) {
                    return protocol.command("UID FETCH 1 (BODY[HEADER])", null);
                }
            });
            FetchResponse fetchResponse = (FetchResponse) ret[0];
            assertThat(fetchResponse.isBAD()).isFalse();
            assertThat(fetchResponse.getItemCount()).isEqualTo(3); // UID, BODY, FLAGS

            BODY body = fetchResponse.getItem(BODY.class);
            assertThat(body.isHeader()).isTrue();
            final String content = new String(body.getByteArray().getNewBytes());

            UID uid = fetchResponse.getItem(UID.class);
            assertThat(uid.uid).isEqualTo(folder.getUID(folder.getMessage(1)));

            // partial size only
            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("UID FETCH 1 (BODY[HEADER]<50>)", null);
                }
            });
            fetchResponse = (FetchResponse) ret[0];
            assertThat(fetchResponse.isBAD()).isFalse();
            assertThat(fetchResponse.getItemCount()).isEqualTo(2); // UID, BODY

            body = fetchResponse.getItem(BODY.class);
            assertThat(body.isHeader()).isTrue();
            assertThat(body.getByteArray().getCount()).isEqualTo(50);

            uid = fetchResponse.getItem(UID.class);
            assertThat(uid.uid).isEqualTo(folder.getUID(folder.getMessage(1)));

            // partial size and zero offset
            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("UID FETCH 1 (BODY[HEADER]<0.30>)", null);
                }
            });
            fetchResponse = (FetchResponse) ret[0];
            assertThat(fetchResponse.isBAD()).isFalse();
            assertThat(fetchResponse.getItemCount()).isEqualTo(2); // UID , BODY

            body = fetchResponse.getItem(BODY.class);
            assertThat(body.isHeader()).isTrue();
            assertThat(body.getByteArray().getCount()).isEqualTo(30);

            uid = fetchResponse.getItem(UID.class);
            assertThat(uid.uid).isEqualTo(folder.getUID(folder.getMessage(1)));

            // partial size and non zero offset
            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
                    return protocol.command("UID FETCH 1 (BODY[HEADER]<10.30>)", null);
                }
            });
            fetchResponse = (FetchResponse) ret[0];
            assertThat(fetchResponse.isBAD()).isFalse();
            assertThat(fetchResponse.getItemCount()).isEqualTo(2); // UID and SIZE

            body = fetchResponse.getItem(BODY.class);
            assertThat(body.isHeader()).isTrue();
            final ByteArray byteArray = body.getByteArray();
            assertThat(byteArray.getCount()).isEqualTo(30);
            assertThat(content.substring(10, 10 + 30)).isEqualTo(new String(byteArray.getNewBytes()));

            uid = fetchResponse.getItem(UID.class);
            assertThat(uid.uid).isEqualTo(folder.getUID(folder.getMessage(1)));
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
                public Object doCommand(IMAPProtocol protocol) {
                    return protocol.command("SEARCH 1", null);
                }
            });
            IMAPResponse response = (IMAPResponse) ret[0];
            assertThat(response.isBAD()).isFalse();
            assertThat(response.getRest()).isEqualTo("1");

            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) {
                    return protocol.command("SEARCH 2:2", null);
                }
            });
            response = (IMAPResponse) ret[0];
            assertThat(response.isBAD()).isFalse();
            assertThat(ret[1].isOK()).isTrue();
            assertThat(response.getRest()).isEqualTo("2");

            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) {
                    return protocol.command("SEARCH 2:4", null);
                }
            });
            response = (IMAPResponse) ret[0];
            assertThat(response.isBAD()).isFalse();
            assertThat(response.getRest()).isEqualTo("2 3 4");
            assertThat(ret[1].isOK()).isTrue();

            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) {
                    return protocol.command("SEARCH 1,2:4,8", null);
                }
            });
            response = (IMAPResponse) ret[0];
            assertThat(response.isBAD()).isFalse();
            assertThat(response.getRest()).isEqualTo("1 2 3 4 8");
            assertThat(ret[1].isOK()).isTrue();

            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) {
                    return protocol.command("SEARCH 1,2:4 3,8", null);
                }
            });
            response = (IMAPResponse) ret[0];
            assertThat(response.isBAD()).isFalse();
            assertThat(response.getRest()).isEqualTo("3");
            assertThat(ret[1].isOK()).isTrue();
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
                public Object doCommand(IMAPProtocol protocol) {
                    return protocol.command("UID SEARCH 1", null);
                }
            });
            IMAPResponse response = (IMAPResponse) ret[0];
            assertThat(response.isBAD()).isFalse();
            assertThat(response.getRest()).isEqualTo(uids.get(1).toString());

            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) {
                    return protocol.command("UID SEARCH 2:2", null);
                }
            });
            response = (IMAPResponse) ret[0];
            assertThat(response.isBAD()).isFalse();
            assertThat(ret[1].isOK()).isTrue();
            assertThat(response.getRest()).isEqualTo(uids.get(2).toString());

            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) {
                    return protocol.command("UID SEARCH 2:4", null);
                }
            });
            response = (IMAPResponse) ret[0];
            assertThat(response.isBAD()).isFalse();
            assertThat(msnListToUidString(uids, 2, 3, 4)).isEqualTo(response.getRest());
            assertThat(ret[1].isOK()).isTrue();

            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) {
                    return protocol.command("UID SEARCH 1,2:4,8", null);
                }
            });
            response = (IMAPResponse) ret[0];
            assertThat(response.isBAD()).isFalse();
            assertThat(msnListToUidString(uids, 1, 2, 3, 4, 8)).isEqualTo(response.getRest());
            assertThat(ret[1].isOK()).isTrue();
        } finally {
            store.close();
        }
    }

    @Test
    public void testSearchNotFlags() throws MessagingException {
        store.connect("foo@localhost", "pwd");
        try {
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_WRITE);
            folder.setFlags(new int[]{2, 3}, new Flags(Flags.Flag.ANSWERED), true);
            Response[] ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) {
                    return protocol.command("SEARCH NOT (ANSWERED) NOT (DELETED) NOT (SEEN) NOT (FLAGGED) ALL", null);
                }
            });
            IMAPResponse response = (IMAPResponse) ret[0];
            assertThat(response.isBAD()).isFalse();
            assertThat("1 4 5 6 7 8 9 10" /* 2 and 3 set to answered */).isEqualTo(response.getRest());
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
            assertThat(message).isNull();
        } finally {
            store.close();
        }
    }

    @Test
    public void testUidSearchText() throws MessagingException {
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
                public Object doCommand(IMAPProtocol protocol) {
                    return protocol.command("UID SEARCH TEXT " + searchText1, null);
                }
            });
            IMAPResponse response = (IMAPResponse) ret[0];
            assertThat(response.isBAD()).isFalse();
            assertThat(response.getRest()).isEqualTo(uids.get(messages[2].getMessageNumber()));

            // messages[2] contains search text in CC, with different upper case
            final String searchText2 = "foo@localHOST";
            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) {
                    return protocol.command("UID SEARCH TEXT " + searchText2, null);
                }
            });
            response = (IMAPResponse) ret[0];
            assertThat(response.isBAD()).isFalse();
            // Match all
            assertThat(response.getRest().split(" ")).isEqualTo(uids.values().toArray());
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
                public Object doCommand(IMAPProtocol protocol) {
                    return protocol.command("CREATE foo", null);
                }
            });

            IMAPResponse response = (IMAPResponse) ret[0];
            assertThat(response.isBAD()).isFalse();

            ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) {
                    return protocol.command("RENAME foo bar", null);
                }
            });

            Response response2 = ret[0];
            assertThat(response2.isOK()).isTrue();

            final Folder bar = store.getFolder("bar");
            bar.open(Folder.READ_ONLY);
            assertThat(bar.exists()).isTrue();
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
                // Try with and without quotes
                searchAndValidateWithCharset(folder, charsetAndQuery[2], charset, arg);
                searchAndValidateWithCharset(folder, charsetAndQuery[2], '"' + charset + '"', arg);
            }
        } finally {
            store.close();
        }
    }

    private void searchAndValidateWithCharset(IMAPFolder folder, String expected, String charset, Argument arg) throws MessagingException {
        Response[] ret = (Response[]) folder.doCommand(new IMAPFolder.ProtocolCommand() {
            @Override
            public Object doCommand(IMAPProtocol protocol) {
                return protocol.command("UID SEARCH CHARSET " + charset + " TEXT", arg);
            }
        });
        IMAPResponse response = (IMAPResponse) ret[0];
        assertThat(response.isBAD()).isFalse();
        String number = response.getRest();
        assertThat(expected).as("Failed for charset " + charset).isEqualTo(number);
    }

    @Test
    public void testUidSearchAll() throws MessagingException, IOException {
        greenMail.setUser("foo2@localhost", "pwd");
        store.connect("foo2@localhost", "pwd");
        try {
            IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);

            final MimeMessage email = GreenMailUtil.createTextEmail("foo2@localhost", "foo@localhost",
                    "some subject", "some content",
                    greenMail.getSmtp().getServerSetup());


            final IMAPFolder.ProtocolCommand uid_search_all = new IMAPFolder.ProtocolCommand() {
                @Override
                public Object doCommand(IMAPProtocol protocol) {
                    return protocol.command("UID SEARCH ALL", null);
                }
            };

            // Search empty
            Response[] ret = (Response[]) folder.doCommand(uid_search_all);
            IMAPResponse response = (IMAPResponse) ret[0];
            assertThat(response.isBAD()).isFalse();
            assertThat(response.toString()).isEqualTo("* SEARCH");
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
