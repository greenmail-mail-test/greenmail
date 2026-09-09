package com.icegreen.greenmail.store;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class ListBasedStoredMessageCollectionTest {

    private StoredMessage message(long uid) throws Exception {
        Session session = Session.getInstance(new Properties());
        String raw = "From: a@b.com\r\nSubject: m" + uid + "\r\n\r\nbody\r\n";
        MimeMessage msg = new MimeMessage(session,
                new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII)));
        return new StoredMessage(msg, new Date(), uid);
    }

    /**
     * {@link ListBasedStoredMessageCollection#getMessages()} must return a snapshot, matching
     * {@link MapBasedStoredMessageCollection#getMessages()}. A live view over the shared backing
     * list lets a message added by another session (a folder is shared across sessions in the
     * #user namespace) surface mid-iteration and raise a ConcurrentModificationException in the
     * unsynchronized callers that iterate the result (for example InMemoryStore.updateQuota).
     */
    @Test
    public void getMessagesReturnsSnapshotSafeForConcurrentAdd() throws Exception {
        ListBasedStoredMessageCollection collection = new ListBasedStoredMessageCollection();
        collection.add(message(1));
        collection.add(message(2));

        List<StoredMessage> messages = collection.getMessages();

        // Structurally modify the backing collection while iterating the returned list, the way a
        // concurrent delivery/APPEND would. A live view raises ConcurrentModificationException here.
        int seen = 0;
        for (StoredMessage ignored : messages) {
            if (seen == 0) {
                collection.add(message(3));
            }
            seen++;
        }

        assertThat(seen).isEqualTo(2);
        assertThat(messages).hasSize(2);
        assertThat(collection.size()).isEqualTo(3);
    }
}
