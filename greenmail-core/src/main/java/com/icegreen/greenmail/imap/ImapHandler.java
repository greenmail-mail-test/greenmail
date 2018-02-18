package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.server.ProtocolHandler;
import com.icegreen.greenmail.user.UserManager;

import java.net.Socket;

public interface ImapHandler extends ProtocolHandler {
    ImapHandler init(UserManager userManager, ImapHostManager imapHost, Socket socket);

    void forceConnectionClose(String message);
}
