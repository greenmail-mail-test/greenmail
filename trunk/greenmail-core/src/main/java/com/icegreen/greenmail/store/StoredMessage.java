/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.icegreen.greenmail.store;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;
import java.util.Date;

/**
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public interface StoredMessage {
    MimeMessage getMimeMessage();

    Flags getFlags();

    Date getInternalDate();

    long getUid();

    MailMessageAttributes getAttributes() throws FolderException;
}
