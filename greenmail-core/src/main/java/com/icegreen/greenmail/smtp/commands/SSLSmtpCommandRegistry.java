package com.icegreen.greenmail.smtp.commands;

public class SSLSmtpCommandRegistry extends SmtpCommandRegistry {

    static {
        commands.put("HELO", new HeloCommand());
        commands.put("EHLO", new SSLEhloCommand());
        commands.put("NOOP", new NoopCommand());
        commands.put("RSET", new RsetCommand());
        commands.put("QUIT", new QuitCommand());
        commands.put("MAIL", new MailCommand());
        commands.put("RCPT", new RcptCommand());
        commands.put("DATA", new DataCommand());
        commands.put("VRFY", new VrfyCommand());
    }
    
}
