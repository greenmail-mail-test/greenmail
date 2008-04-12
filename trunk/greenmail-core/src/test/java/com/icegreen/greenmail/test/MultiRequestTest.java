/* -------------------------------------------------------------------
* Copyright (c) 2007 Wael Chatila / Icegreen Technologies. All Rights Reserved.
* This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.test;

import junit.framework.TestCase;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.icegreen.greenmail.util.Retriever;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Wael Chatila
 * @version $Id: $
 * @since Jan 11, 2007
 */
public class MultiRequestTest extends TestCase {
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

        SenderThread(String to,int count) {
            this.to = to;
            this.count = count;
        }

        public void run() {
            for (int i=0;i<count;i++) {
                GreenMailUtil.sendTextEmailTest(to, "from@localhost.com", "subject", "body");
            }
        }
    }
    private static class RetrieverThread extends Thread {
        String to;
        int count;
        Retriever r;

        RetrieverThread(String to, Retriever r, ThreadGroup group) {
            super(group,RetrieverThread.class.getName());
            this.to = to;
            this.r = r;
        }

        public void run() {
            try {
                count = r.getMessages(to).length;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public int getCount() {
            return count;
        }
    }
    //~ END INNER CLASSES -----------------------------------------------

    public void test40Senders() throws InterruptedException {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();

        final int num = 40;
        for (int i=1;i<=num;i++) {
            SenderThread s = new SenderThread("to"+i,i);
            s.start();
        }
        final int tot = (num*(num+1)/2);
        for (int i=1;i<=tot;i+=2) {
            assertTrue(greenMail.waitForIncomingEmail(15000,i));
        }
        assertTrue(greenMail.waitForIncomingEmail(15000,tot));
        assertFalse(greenMail.waitForIncomingEmail(15000,tot+1));
    }

    public void test40Senders20x4Retrievers() throws InterruptedException {
        greenMail = new GreenMail();
        greenMail.start();

        final int num = 40;
        for (int i=1;i<=num;i++) {
            SenderThread s = new SenderThread("to"+i,i);
            s.start();
        }

        final int tot = (num*(num+1)/2);
        assertTrue(greenMail.waitForIncomingEmail(15000,tot));

        final int num2 = 20;
        assertTrue(num>num2);
        ThreadGroup group = new ThreadGroup(RetrieverThread.class.getName());
        List retriverThreads = new ArrayList();
        for (int i=(num-num2+1);i<=num;i++) {
            RetrieverThread r = new RetrieverThread("to"+i,new Retriever(greenMail.getPop3()),group);
            retriverThreads.add(r);
            r.start();
        }
        for (int i=(num-num2+1);i<=num;i++) {
            RetrieverThread r = new RetrieverThread("to"+i,new Retriever(greenMail.getImap()),group);
            retriverThreads.add(r);
            r.start();
        }
        for (int i=(num-num2+1);i<=num;i++) {
            RetrieverThread r = new RetrieverThread("to"+i,new Retriever(greenMail.getPop3s()),group);
            retriverThreads.add(r);
            r.start();
        }
        for (int i=(num-num2+1);i<=num;i++) {
            RetrieverThread r = new RetrieverThread("to"+i,new Retriever(greenMail.getImaps()),group);
            retriverThreads.add(r);
            r.start();
        }
        long t = System.currentTimeMillis();
        while(group.activeCount() != 0 && (System.currentTimeMillis() - t)<30000) {
            Thread.sleep(1000);
        }
        int sum = 0;
        for (Iterator iterator = retriverThreads.iterator(); iterator.hasNext();) {
            RetrieverThread retrieverThread = (RetrieverThread) iterator.next();
            sum += retrieverThread.getCount();
        }
        assertEquals((num*(num+1)/2-num2*(num2+1)/2)*4, sum);

    }

    public void test40Senders20x4RetrieversAtTheSameTime() throws InterruptedException {
        greenMail = new GreenMail();
        greenMail.start();

        final int num = 40;
        for (int i=1;i<=num;i++) {
            SenderThread s = new SenderThread("to"+i,i);
            s.start();
        }

        final int num2 = 20;
        assertTrue(num>num2);
        ThreadGroup group = new ThreadGroup(RetrieverThread.class.getName());
        for (int i=(num-num2+1);i<=num;i++) {
            RetrieverThread r = new RetrieverThread("to"+i,new Retriever(greenMail.getPop3()),group);
            r.start();
        }
        for (int i=(num-num2+1);i<=num;i++) {
            RetrieverThread r = new RetrieverThread("to"+i,new Retriever(greenMail.getImap()),group);
            r.start();
        }
        for (int i=(num-num2+1);i<=num;i++) {
            RetrieverThread r = new RetrieverThread("to"+i,new Retriever(greenMail.getPop3s()),group);
            r.start();
        }
        for (int i=(num-num2+1);i<=num;i++) {
            RetrieverThread r = new RetrieverThread("to"+i,new Retriever(greenMail.getImaps()),group);
            r.start();
        }

        final int tot = (num*(num+1)/2);
        assertTrue(greenMail.waitForIncomingEmail(15000,tot));
    }
}
