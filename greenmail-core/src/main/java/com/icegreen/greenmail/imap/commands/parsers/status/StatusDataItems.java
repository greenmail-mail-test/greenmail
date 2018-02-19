package com.icegreen.greenmail.imap.commands.parsers.status;

public class StatusDataItems {
    private boolean messages;
    private boolean recent;
    private boolean uidNext;
    private boolean uidValidity;
    private boolean unseen;

    public boolean isMessages() {
        return messages;
    }

    public void setMessages(boolean messages) {
        this.messages = messages;
    }

    public boolean isRecent() {
        return recent;
    }

    public void setRecent(boolean recent) {
        this.recent = recent;
    }

    public boolean isUidNext() {
        return uidNext;
    }

    public void setUidNext(boolean uidNext) {
        this.uidNext = uidNext;
    }

    public boolean isUidValidity() {
        return uidValidity;
    }

    public void setUidValidity(boolean uidValidity) {
        this.uidValidity = uidValidity;
    }

    public boolean isUnseen() {
        return unseen;
    }

    public void setUnseen(boolean unseen) {
        this.unseen = unseen;
    }
}