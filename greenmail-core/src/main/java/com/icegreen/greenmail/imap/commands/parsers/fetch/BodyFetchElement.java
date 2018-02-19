package com.icegreen.greenmail.imap.commands.parsers.fetch;

public class BodyFetchElement {
    private String name;
    private String sectionIdentifier;
    private Partial partial;

    public BodyFetchElement(String name, String sectionIdentifier) {
        this(name, sectionIdentifier, null);
    }

    public BodyFetchElement(String name, String sectionIdentifier, Partial partial) {
        this.name = name;
        this.sectionIdentifier = sectionIdentifier;
        this.partial = partial;
    }

    public String getParameters() {
        return this.sectionIdentifier;
    }

    public String getResponseName() {
        return this.name;
    }

    public Partial getPartial() {
        return partial;
    }
}
