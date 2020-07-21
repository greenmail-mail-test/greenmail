/* -------------------------------------------------------------------
* Copyright (c) 2007 Wael Chatila / Icegreen Technologies. All Rights Reserved.
* This software is released under the Apache license 2.0
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.test;

import java.util.ArrayList;
import java.util.List;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.server.AbstractServer;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.*;

/**
 * Test multiple senders and receivers using all available protocols
 */
public class MultiRequestTest {
    protected final static Logger log = LoggerFactory.getLogger(MultiRequestTest.class);

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.ALL);

    //~ INNER CLASSES -----------------------------------------------
    private static class SenderThread extends Thread {
        String to;
        int count;
        boolean finished = false;

        SenderThread(String to, int count, ThreadGroup threadGroup) {
            super(threadGroup, SenderThread.class.getName() + ':' + to);
            this.to = to;
            this.count = count;
        }

        public void run() {
            for (int i = 0; i < count; i++) {
                GreenMailUtil.sendTextEmailTest(to, "from@localhost.com", "subject", "body");
            }
            finished = true;
        }
    }

    private static class RetrieverThread extends Thread {
        String to;
        volatile int count;
        private int expectedCount;
        private AbstractServer server;

        RetrieverThread(String to, AbstractServer server, ThreadGroup group, int expectedCount) {
            super(group, RetrieverThread.class.getName());
            this.to = to;
            this.server = server;
            this.expectedCount = expectedCount;
        }

        public void run() {
            // Try several times, as message might not have been sent yet
            // If message is not sent after timeout period we abort
            int timeout = 10000;//ms
            int increment = timeout / 50;
            try (Retriever r = new Retriever(server)) {
                for (int time = 0; time < timeout; time += increment) {
                    try {
                        count = r.getMessages(to, to).length;
                        if (count == expectedCount) {
                            return;
                        }
                        sleep(increment);
                    } catch (Exception e) {
                        log.error("Error retrieving messages", e);
                    }
                }
            }
        }

        public int getCount() {
            return count;
        }
    }
    //~ END INNER CLASSES -----------------------------------------------

    @Test
    public void test20Senders() {
        final int num = 20;
        addUsers(num);
        startSenderThreads(num);
        final int tot = (num * (num + 1) / 2);
        assertThat(greenMail.waitForIncomingEmail(15000, tot)).isTrue();
        // No more mails can arrive now
        assertThat(greenMail.waitForIncomingEmail(1000, tot + 1)).isFalse();
    }

    @Test
    public void test20Senders20x4Retrievers() throws InterruptedException {
        final int num = 20;
        addUsers(num);
        startSenderThreads(num);

        // Now wait for senders to finish and mails to arrive
        final int sentMessages = (num * (num + 1) / 2);
        assertThat(greenMail.waitForIncomingEmail(15000, sentMessages)).isTrue();

        // Then start receivers
        ThreadGroup group = new ThreadGroup(RetrieverThread.class.getName());
        List<RetrieverThread> retrieverThreads = new ArrayList<RetrieverThread>();
        startRetrieverThreads(num, group, retrieverThreads);
        waitForThreadGroup(group, 20000);

        // Every message is received four times since there are four receivers for every mail account
        checkRetrieverThreadsMessagesArrived(sentMessages * 4, retrieverThreads);

        // But the total number of messages sent is still the same as above
        assertThat(greenMail.waitForIncomingEmail(5000, sentMessages)).isTrue();
    }

    @Test
    public void test20Senders20x4RetrieversAtTheSameTime() throws InterruptedException {
        final int num = 20;
        addUsers(num);
        startSenderThreads(num);

        // Start receivers at the same time senders are also started
        List<RetrieverThread> retrieverThreads = new ArrayList<RetrieverThread>();
        ThreadGroup group = new ThreadGroup(RetrieverThread.class.getName());
        startRetrieverThreads(num, group, retrieverThreads);
        waitForThreadGroup(group, 30000);

        final int sentMessages = (num * (num + 1) / 2);
        // Every message is received four times since there are four receivers for every mail account
        checkRetrieverThreadsMessagesArrived(sentMessages * 4, retrieverThreads);

        // Correct number of received messages
        assertThat(greenMail.waitForIncomingEmail(5000, sentMessages)).isTrue();
    }

    /**
     * Add [num] users, naming them from to1, to2, ..., to[num]
     *
     * @param num Number of users to create
     */
    private void addUsers(int num) {
        for (int i = 1; i <= num; i++) {
            final String newUserName = "to" + i;
            greenMail.setUser(newUserName, newUserName);
        }
    }

    /**
     * Start n threads that send messages to the addresses to1, to2, to3, ... Every account
     * receives the same number of messages as the suffix number suggests (to3 gets 3 messages)
     *
     * @param n Number of accounts to fill
     */
    private void startSenderThreads(int n) {
        ThreadGroup senders = new ThreadGroup("SenderThreads") {
            @Override
            public void uncaughtException(final Thread t, final Throwable e) {
                log.error("Exception in thread \"{}\"", t.getName(), e);
            }
        };
        for (int i = 1; i <= n; i++) {
            SenderThread s = new SenderThread("to" + i, i, senders);
            s.start();
        }
    }

    /**
     * Start the receiver threads under the given thread group. the threads retrieve messages via IMAP, IMAPS,
     * POP, POPS
     *
     * @param n                Number of threads to start
     * @param group            Thread group to add to
     * @param retrieverThreads List of threads that the threads are added to
     */
    private void startRetrieverThreads(int n, ThreadGroup group, List<RetrieverThread> retrieverThreads) {
        for (int i = 1; i <= n; i++) {
            RetrieverThread r = new RetrieverThread("to" + i, greenMail.getPop3(), group, i);
            retrieverThreads.add(r);
            r.start();
        }
        for (int i = 1; i <= n; i++) {
            RetrieverThread r = new RetrieverThread("to" + i, greenMail.getImap(), group, i);
            retrieverThreads.add(r);
            r.start();
        }
        for (int i = 1; i <= n; i++) {
            RetrieverThread r = new RetrieverThread("to" + i, greenMail.getPop3s(), group, i);
            retrieverThreads.add(r);
            r.start();
        }
        for (int i = 1; i <= n; i++) {
            RetrieverThread r = new RetrieverThread("to" + i, greenMail.getImaps(), group, i);
            retrieverThreads.add(r);
            r.start();
        }
    }

    /**
     * Wait for the thread group to finish for timeout milliseconds
     *
     * @param group   Thread group
     * @param timeout Timeout in milliseconds
     * @throws InterruptedException Thrown if interrupted
     */
    private void waitForThreadGroup(ThreadGroup group, int timeout) throws InterruptedException {
        long t = System.currentTimeMillis();
        while (group.activeCount() != 0 && (System.currentTimeMillis() - t) < timeout) {
            Thread.sleep(200);
        }
    }

    /**
     * Checks that the retriever threads received the correct number of messages
     *
     * @param receivedMessages Number of messages that should have been received
     * @param retrieverThreads List of threads
     */
    private void checkRetrieverThreadsMessagesArrived(int receivedMessages, List<RetrieverThread> retrieverThreads) {
        // Check that correct number of messages has arrived
        int sum = 0;
        for (RetrieverThread retrieverThread : retrieverThreads) {
            sum += retrieverThread.getCount();
        }
        assertThat(sum).isEqualTo(receivedMessages);
    }
}
