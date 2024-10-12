package com.icegreen.greenmail.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.FileSystems;

import org.eclipse.angus.mail.imap.IMAPStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.icegreen.greenmail.imap.ImapConstants;
import com.icegreen.greenmail.imap.ImapServer;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.user.GreenMailUser;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

class PreLoadEmailsTest {
    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_IMAP);

    @Test
    void testPreloadFromDirectory() throws IOException, MessagingException, FolderException {
        /* Expected structure:
            preload
            ├── bar@localhost
            │   └── INBOX
            │       └── test-5.eml
            ├── foo-bar@localhost
            │   └── INBOX # Auto-created
            ├── drafts@localhost
            |   ├── Drafts
            │   └── INBOX # Auto-created
            └── foo@localhost
                ├── Drafts
                │   └── draft.eml
                └── INBOX
                    ├── f1
                    │   ├── f2
                    │   │   ├── test-3.eml
                    │   │   └── test-4.eml
                    │   └── test-2.eml
                    └── test-1.eml
         */
        final GreenMailUser existingUser = greenMail.setUser("bar@localhost", "bar@localhost", "bar");
        greenMail.loadEmails(FileSystems.getDefault().getPath("test-classes/preload"));
        final ImapServer imap = greenMail.getImap();
        try (IMAPStore store = imap.createStore()) {
            store.connect("foo@localhost", "foo@localhost");

            try (Folder inbox = store.getFolder(ImapConstants.INBOX_NAME)) {
                inbox.open(Folder.READ_ONLY);
                final Message[] messages = inbox.getMessages();
                assertThat(messages).hasSize(1);
                assertThat(messages[0].getSubject()).isEqualTo("test-1");

                try (Folder f1 = inbox.getFolder("f1")) {
                    f1.open(Folder.READ_ONLY);
                    final Message[] f1Messages = f1.getMessages();
                    assertThat(f1Messages).hasSize(1);
                    assertThat(f1Messages[0].getSubject()).isEqualTo("test-2");

                    try (Folder f2 = f1.getFolder("f2")) {
                        f2.open(Folder.READ_ONLY);
                        final Message[] f2Messages = f2.getMessages();
                        assertThat(f2Messages).hasSize(2);
                        assertThat(f2Messages[0].getSubject()).isIn("test-3", "test-4");
                    }
                }
            }
            try (Folder inbox = store.getFolder("Drafts")) {
                inbox.open(Folder.READ_ONLY);
                final Message[] messages = inbox.getMessages();
                assertThat(messages).hasSize(1);
                assertThat(messages[0].getSubject()).isEqualTo("Draft-1");
            }
        }

        // Empty folder for user 'foo-bar'
        try (IMAPStore store = imap.createStore()) {
            store.connect("foo-bar@localhost", "foo-bar@localhost");
            try (Folder inbox = store.getFolder(ImapConstants.INBOX_NAME)) {
                inbox.open(Folder.READ_ONLY);
                assertThat(inbox.getMessages()).isEmpty();
            }
        }

        // Only Drafts folder
        try (IMAPStore store = imap.createStore()) {
            store.connect("drafts@localhost", "drafts@localhost");
            try (Folder inbox = store.getFolder(ImapConstants.INBOX_NAME)) { // Auto-created
                inbox.open(Folder.READ_ONLY);
                assertThat(inbox.getMessages()).isEmpty();
            }
            try (Folder drafts = store.getFolder("Drafts")) { // From filesystem structure
                drafts.open(Folder.READ_ONLY);
                assertThat(drafts.getMessages()).isEmpty();
            }
        }

        // Pre-created user 'bar'
        try (IMAPStore store = imap.createStore()) {
            store.connect(existingUser.getLogin(), existingUser.getPassword());
            try (Folder inbox = store.getFolder(ImapConstants.INBOX_NAME)) {
                inbox.open(Folder.READ_ONLY);
                final Message[] messages = inbox.getMessages();
                assertThat(messages).hasSize(1);
                assertThat(messages[0].getSubject()).isEqualTo("test-5");
            }
        }
    }
}
