package com.icegreen.greenmail.imap.commands.parsers.fetch;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class FetchRequest {
    private boolean flags;
    private boolean uid;
    private boolean internalDate;
    private boolean size;
    private boolean envelope;
    private boolean body;
    private boolean bodyStructure;

    private boolean setSeen = false;

    private Set<BodyFetchElement> bodyElements = new HashSet<>();

    public Collection<BodyFetchElement> getBodyElements() {
        return bodyElements;
    }

    public boolean isSetSeen() {
        return setSeen;
    }

    public void add(BodyFetchElement element, boolean peek) {
        if (!peek) {
            setSeen = true;
        }
        bodyElements.add(element);
    }

    public boolean isFlags() {
        return flags;
    }

    public void setFlags(boolean flags) {
        this.flags = flags;
    }

    public boolean isUid() {
        return uid;
    }

    public void setUid(boolean uid) {
        this.uid = uid;
    }

    public boolean isInternalDate() {
        return internalDate;
    }

    public void setInternalDate(boolean internalDate) {
        this.internalDate = internalDate;
    }

    public boolean isSize() {
        return size;
    }

    public void setSize(boolean size) {
        this.size = size;
    }

    public boolean isEnvelope() {
        return envelope;
    }

    public void setEnvelope(boolean envelope) {
        this.envelope = envelope;
    }

    public boolean isBody() {
        return body;
    }

    public void setBody(boolean body) {
        this.body = body;
    }

    public boolean isBodyStructure() {
        return bodyStructure;
    }

    public void setBodyStructure(boolean bodyStructure) {
        this.bodyStructure = bodyStructure;
    }

    public void setSetSeen(boolean setSeen) {
        this.setSeen = setSeen;
    }

    public void setBodyElements(Set<BodyFetchElement> bodyElements) {
        this.bodyElements = bodyElements;
    }
}