/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.icegreen.greenmail.store;

import java.util.Date;

/**
 * Interface for objects holding IMAP4rev1 Message Attributes. Message
 * Attributes should be set when a message enters a mailbox. Implementations
 * are encouraged to implement and store MessageAttributes apart from the
 * underlying message. This allows the Mailbox to respond to questions about
 * very large message without needing to access them directly.
 * <p> Note that the message in a mailbox have the same order using either
 * Message Sequence Numbers or UIDs.
 * <p/>
 * Reference: RFC 2060 - para 2.3
 *
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */
public interface MailMessageAttributes {

    /**
     * Provides the date and time at which the message was received. In the
     * case of delivery by SMTP, this SHOULD be the date and time of final
     * delivery as defined for SMTP. In the case of messages copied from
     * another mailbox, it shuld be the internalDate of the source message. In
     * the case of messages Appended to the mailbox, example drafts,  the
     * internalDate is either specified in the Append command or is the
     * current dat and time at the time of the Append.
     *
     * @return Date imap internal date
     */
    Date getInternalDate();

    /**
     * Returns IMAP formatted String representation of Date
     */
    String getInternalDateAsString();

    /**
     * Provides the sizeof the message in octets.
     *
     * @return int number of octets in message.
     */
    int getSize();

    /**
     * Provides the Envelope structure information for this message.
     * This is a parsed representation of the rfc-822 envelope information.
     * This is not to be confused with the SMTP envelope!
     *
     * @return String satisfying envelope syntax in rfc 2060.
     */
    String getEnvelope();

    /**
     * Provides the Body Structure information for this message.
     * This is a parsed representtion of the MIME structure of the message.
     *
     * @return String satisfying body syntax in rfc 2060.
     */
    String getBodyStructure(boolean includeExtensions);
}


