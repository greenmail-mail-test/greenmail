package com.icegreen.greenmail.store;

import com.icegreen.greenmail.imap.ImapConstants;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryStoreConcurrentCreateTest {
    private static final int THREADS = 8;

    @Test
    public void concurrentCreateOfSameMailboxCreatesSingleFolder() throws Exception {
        for (int round = 0; round < 20; round++) {
            InMemoryStore store = new InMemoryStore();
            MailFolder parent = store.getMailbox(ImapConstants.USER_NAMESPACE);

            List<Exception> failures = Collections.synchronizedList(new ArrayList<>());
            List<MailFolder> created = Collections.synchronizedList(new ArrayList<>());
            CyclicBarrier barrier = new CyclicBarrier(THREADS);

            Thread[] threads = new Thread[THREADS];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    try {
                        barrier.await();
                        created.add(store.createMailbox(parent, "foo", true));
                    } catch (Exception e) {
                        failures.add(e);
                    }
                });
                threads[i].start();
            }
            for (Thread thread : threads) {
                thread.join();
            }

            // Exactly one thread creates the mailbox, the others are told it already exists
            assertThat(created).hasSize(1);
            assertThat(failures).hasSize(THREADS - 1)
                .allSatisfy(e -> assertThat(e).isInstanceOf(FolderException.class)
                    .hasMessageContaining("already exists"));
            assertThat(store.getChildren(parent)).hasSize(1);
            assertThat(store.getMailbox(parent, "foo")).isEqualTo(created.get(0));
        }
    }
}
