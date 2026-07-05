package com.icegreen.greenmail.store;

import jakarta.mail.Flags;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class MapBasedStoredMessageCollectionExpungeTest {

    private StoredMessage message(long uid) throws Exception {
        Session session = Session.getInstance(new Properties());
        String raw = "From: a@b.com\r\nSubject: m" + uid + "\r\n\r\nbody\r\n";
        MimeMessage msg = new MimeMessage(session,
                new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII)));
        return new StoredMessage(msg, new Date(), uid);
    }

    private static class RecordingListener implements FolderListener {
        final List<Integer> expunged = new ArrayList<>();

        @Override
        public void expunged(int msn) {
            expunged.add(msn);
        }

        @Override
        public void added(int msn) {
        }

        @Override
        public void flagsUpdated(int msn, Flags flags, Long uid) {
        }

        @Override
        public void mailboxDeleted() {
        }
    }

    @Test
    public void expungeReportsSequenceNumbersRelativeToPriorExpunges() throws Exception {
        MapBasedStoredMessageCollection collection = new MapBasedStoredMessageCollection(100);
        for (long uid = 1; uid <= 5; uid++) {
            collection.add(message(uid));
        }
        // Mark the 2nd and 4th message (MSN 2 and 4) as deleted.
        collection.get(1).setFlag(Flags.Flag.DELETED, true);
        collection.get(3).setFlag(Flags.Flag.DELETED, true);

        RecordingListener listener = new RecordingListener();
        collection.expunge(Collections.singletonList(listener));

        // After MSN 2 is expunged the remaining messages shift down, so the message
        // originally at MSN 4 is now at MSN 3 and must be reported as such.
        assertThat(listener.expunged).containsExactly(2, 3);
        assertThat(collection.size()).isEqualTo(3);
    }

    @Test
    public void expungeReportsSingleDeletionUnchanged() throws Exception {
        MapBasedStoredMessageCollection collection = new MapBasedStoredMessageCollection(100);
        for (long uid = 1; uid <= 3; uid++) {
            collection.add(message(uid));
        }
        collection.get(1).setFlag(Flags.Flag.DELETED, true);

        RecordingListener listener = new RecordingListener();
        collection.expunge(Collections.singletonList(listener));

        assertThat(listener.expunged).containsExactly(2);
        assertThat(collection.size()).isEqualTo(2);
    }
}
