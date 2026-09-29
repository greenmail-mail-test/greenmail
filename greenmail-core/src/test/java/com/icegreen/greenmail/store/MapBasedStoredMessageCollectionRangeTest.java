package com.icegreen.greenmail.store;

import com.icegreen.greenmail.foedus.util.MsgRangeFilter;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class MapBasedStoredMessageCollectionRangeTest {

    private StoredMessage message(long uid) throws Exception {
        Session session = Session.getInstance(new Properties());
        String raw = "From: a@b.com\r\nSubject: m" + uid + "\r\n\r\nbody\r\n";
        MimeMessage msg = new MimeMessage(session,
                new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII)));
        return new StoredMessage(msg, new Date(), uid);
    }

    @Test
    public void getMessagesResolvesMessageNumbersOneBased() throws Exception {
        // Message numbers are 1-based. MapBasedStoredMessageCollection.getMessages filtered on
        // the 0-based index, so a POP3 "RETR 1" resolved to the second message and the last
        // message was unreachable, unlike the list-based collection which filters on i + 1.
        MapBasedStoredMessageCollection collection = new MapBasedStoredMessageCollection(100);
        collection.add(message(10)); // position 1
        collection.add(message(11)); // position 2
        collection.add(message(12)); // position 3

        List<StoredMessage> first = collection.getMessages(new MsgRangeFilter("1", false));
        assertThat(first).hasSize(1);
        assertThat(first.get(0).getUid()).isEqualTo(10L);

        List<StoredMessage> last = collection.getMessages(new MsgRangeFilter("3", false));
        assertThat(last).hasSize(1);
        assertThat(last.get(0).getUid()).isEqualTo(12L);

        List<StoredMessage> outOfRange = collection.getMessages(new MsgRangeFilter("4", false));
        assertThat(outOfRange).isEmpty();
    }
}
