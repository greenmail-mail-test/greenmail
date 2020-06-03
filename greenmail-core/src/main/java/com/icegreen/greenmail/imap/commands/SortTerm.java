package com.icegreen.greenmail.imap.commands;

import jakarta.mail.search.SearchTerm;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 10/03/2016.
 *
 * @author Reda.Housni-Alaoui
 */
class SortTerm {
    private final List<SortKey> sortCriteria = new ArrayList<>();
    private String charset;
    private SearchTerm searchTerm;


    public List<SortKey> getSortCriteria() {
        return sortCriteria;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public SearchTerm getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(SearchTerm searchTerm) {
        this.searchTerm = searchTerm;
    }
}
