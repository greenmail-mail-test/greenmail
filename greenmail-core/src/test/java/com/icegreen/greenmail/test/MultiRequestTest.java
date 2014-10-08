/* -------------------------------------------------------------------
* Copyright (c) 2007 Wael Chatila / Icegreen Technologies. All Rights Reserved.
* This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.test;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetupTest;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.AuthenticationFailedException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 11, 2007
 */
public class MultiRequestTest extends TestCase {
    protected final static Logger log = LoggerFactory.getLogger(MultiRequestTest.class);
    GreenMail greenMail;

    protected void tearDown() throws Exception {
        try {
            greenMail.stop();
        } catch (NullPointerException ignored) {
            //empty
        }
        super.tearDown();
    }

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
        int count;
        Retriever r;
        private int expectedCount;

        RetrieverThread(String to, Retriever r, ThreadGroup group, int expectedCount) {
            super(group, RetrieverThread.class.getName());
            this.to = to;
            this.r = r;
            this.expectedCount = expectedCount;
        }

        public void run() {
            // Try several times, as message might not have been sent yet
            // If message is not sent after timeout period we abort
            int timeout = 10000;//ms
            int increment = timeout / 10;
            for (int time = 0; time < timeout; time += increment) {
                try {
                    count = r.getMessages(to, to).length;
                    if (count == expectedCount) {
                        return;
                    }
                    sleep(increment);
                } catch (AuthenticationFailedException e) {
                    // Ignore, user has not been created yet. It will be created
                } catch (Exception e) {
                    log.error("Error retrieving messages", e);
                }
            }
        }

        public int getCount() {
            return count;
        }
    }
    //~ END INNER CLASSES -----------------------------------------------

    public void test20Senders() throws InterruptedException {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();

        final int num = 20;
        startSenderThreads(num);
        final int tot = (num * (num + 1) / 2);
        assertTrue(greenMail.waitForIncomingEmail(15000, tot));
        // No more mails can arrive now
        assertFalse(greenMail.waitForIncomingEmail(1000, tot + 1));
    }

    public void test20Senders20x4Retrievers() throws InterruptedException {
        greenMail = new GreenMail();
        greenMail.start();

        final int num = 20;
        startSenderThreads(num);

        // Now wait for senders to finish and mails to arrive
        final int sentMessages = (num * (num + 1) / 2);
        assertTrue(greenMail.waitForIncomingEmail(15000, sentMessages));

        // Then start receivers
        ThreadGroup group = new ThreadGroup(RetrieverThread.class.getName());
        List<RetrieverThread> retrieverThreads = new ArrayList<RetrieverThread>();
        startRetrieverThreads(num, group, retrieverThreads);
        waitForThreadGroup(group, 20000);

        // Every message is received four times since there are four receivers for every mail account
        checkRetrieverThreadsMessagesArrived(sentMessages * 4, retrieverThreads);

        // But the total number of messages sent is still the same as above
        assertTrue(greenMail.waitForIncomingEmail(5000, sentMessages));
    }

    public void test20Senders20x4RetrieversAtTheSameTime() throws InterruptedException {
        greenMail = new GreenMail();
        greenMail.start();

        final int num = 20;
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
        assertTrue(greenMail.waitForIncomingEmail(5000, sentMessages));
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
                log.error("Exception in thread \"" + t.getName(), e);
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
        for (int i = 0; i <= n; i++) {
            RetrieverThread r = new RetrieverThread("to" + i, new Retriever(greenMail.getPop3()), group, i);
            retrieverThreads.add(r);
            r.start();
        }
        for (int i = 0; i <= n; i++) {
            RetrieverThread r = new RetrieverThread("to" + i, new Retriever(greenMail.getImap()), group, i);
            retrieverThreads.add(r);
            r.start();
        }
        for (int i = 0; i <= n; i++) {
            RetrieverThread r = new RetrieverThread("to" + i, new Retriever(greenMail.getPop3s()), group, i);
            retrieverThreads.add(r);
            r.start();
        }
        for (int i = 0; i <= n; i++) {
            RetrieverThread r = new RetrieverThread("to" + i, new Retriever(greenMail.getImaps()), group, i);
            retrieverThreads.add(r);
            r.start();
        }
    }

    /**
     * Wait for the thread group to finish for timeout milliseconds
     *
     * @param group Thread group
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
     * @param receivedMessages Number of messages that should have been received
     * @param retrieverThreads List of threads
     */
    private void checkRetrieverThreadsMessagesArrived(int receivedMessages, List<RetrieverThread> retrieverThreads) {
        // Check that correct number of messages has arrived
        int sum = 0;
        for (RetrieverThread retrieverThread : retrieverThreads) {
            sum += retrieverThread.getCount();
        }
        assertEquals(receivedMessages, sum);
    }
}
