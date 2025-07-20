package com.icegreen.greenmail.smtp.auth;

import com.icegreen.greenmail.smtp.commands.AuthCommand;
import com.icegreen.greenmail.util.SaslXoauth2Message;

public class XOAuth2AuthenticationState  implements AuthenticationState, UsernameAuthentication {
    private final SaslXoauth2Message xoauth2Message;

    /**
     * @param xoauth2Message parsed XOAuth2 message
     */
    public XOAuth2AuthenticationState(SaslXoauth2Message xoauth2Message) {
        this.xoauth2Message = xoauth2Message;
    }

    @Override
    public String getType() {
        return AuthCommand.AuthMechanism.XOAUTH2.name();
    }

    @Override
    public String getUsername() {
        return xoauth2Message.getUsername();
    }

    public String getAccessToken() {
        return xoauth2Message.getAccessToken();
    }
}
