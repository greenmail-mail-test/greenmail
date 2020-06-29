package com.icegreen.greenmail.imap.commands;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.search.AndTerm;
import javax.mail.search.BodyTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.FromStringTerm;
import javax.mail.search.FromTerm;
import javax.mail.search.HeaderTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.RecipientStringTerm;
import javax.mail.search.RecipientTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SentDateTerm;
import javax.mail.search.SizeTerm;
import javax.mail.search.SubjectTerm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icegreen.greenmail.store.StoredMessage;

/**
 * Builder for search terms.
 *
 * @author mm
 */
public abstract class SearchTermBuilder {
    private SearchKey key;
    private List<String> parameters = Collections.<String>emptyList();
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchTermBuilder.class);
    public static final AllSearchTerm ALL_SEARCH_TERM = new AllSearchTerm();
    
    public static SearchTermBuilder create(final String pTerm) {
        return create(SearchKey.valueOf(pTerm));
    }

    public static SearchTermBuilder create(final SearchKey key) {
        SearchTermBuilder builder;
        LOGGER.debug("Creating search term for '{}'", key);
        switch (key) {
            // Non flags first
            case HEADER:
                builder = createHeaderTermBuilder();
                break;
            // Flags
            case ALL:
                builder = createSearchTermBuilder(ALL_SEARCH_TERM);
                break;
            case ANSWERED:
                builder = createFlagSearchTermBuilder("ANSWERED", true);
                break;
            case BCC:
                builder = createRecipientSearchTermBuilder(Message.RecipientType.BCC);
                break;
            case CC:
                builder = createRecipientSearchTermBuilder(Message.RecipientType.CC);
                break;
            case DELETED:
                builder = createFlagSearchTermBuilder("DELETED", true);
                break;
            case DRAFT:
                builder = createFlagSearchTermBuilder("DRAFT", true);
                break;
            case FLAGGED:
                builder = createFlagSearchTermBuilder("FLAGGED", true);
                break;
            case FROM:
                builder = createFromSearchTermBuilder();
                break;
            case NEW:
                builder = createSearchTermBuilder(
                        new AndTerm(createFlagSearchTerm("RECENT", true), createFlagSearchTerm("SEEN", false))
                );
                break;
            case OLD:
                builder = createSearchTermBuilder(createFlagSearchTerm("RECENT", false));
                break;
            case RECENT:
                builder = createSearchTermBuilder(createFlagSearchTerm("RECENT", true));
                break;
            case SEEN:
                builder = createSearchTermBuilder(createFlagSearchTerm("SEEN", true));
                break;
            case SUBJECT:
                builder = createSubjectTermBuilder();
                break;
            case BODY:
                builder = createBodySearchTermBuilder();
                break;
            case TEXT:
                builder = createTextSearchTermBuilder();
                break;
            case TO:
                builder = createRecipientSearchTermBuilder(Message.RecipientType.TO);
                break;
            case UID:
                builder = createUidTermBuilder();
                break;
            case UNANSWERED:
                builder = createSearchTermBuilder(createFlagSearchTerm("ANSWERED", false));
                break;
            case UNDELETED:
                builder = createSearchTermBuilder(createFlagSearchTerm("DELETED", false));
                break;
            case UNDRAFT:
                builder = createSearchTermBuilder(createFlagSearchTerm("DRAFT", false));
                break;
            case UNFLAGGED:
                builder = createSearchTermBuilder(createFlagSearchTerm("FLAGGED", false));
                break;
            case UNSEEN:
                builder = createSearchTermBuilder(createFlagSearchTerm("SEEN", false));
                break;
            case KEYWORD:
            case UNKEYWORD:
                builder = createKeywordSearchTermBuilder(key);
                break;
            case SEQUENCE_SET:
                builder = createSequenceSetTermBuilder();
                break;
            case OR:
                builder = createORTermBuilder();
                break;
            case SINCE:
                builder = createReceivedDateTermBuilder(ComparisonTerm.GE);
                break;
            case ON:
                builder = createReceivedDateTermBuilder(ComparisonTerm.EQ);
                break;
            case BEFORE:
                builder = createReceivedDateTermBuilder(ComparisonTerm.LT);
                break;
            case SENTSINCE:
                builder = createSentDateTermBuilder(ComparisonTerm.GE);
                break;
            case SENTON:
                builder = createSentDateTermBuilder(ComparisonTerm.EQ);
                break;
            case SENTBEFORE:
                builder = createSentDateTermBuilder(ComparisonTerm.LT);
                break;
            case LARGER:
                builder = createMessageSizeTermBuilder(ComparisonTerm.GT);
                break;
            case SMALLER:
                builder = createMessageSizeTermBuilder(ComparisonTerm.LT);
                break;
            default:
                throw new IllegalStateException("Unsupported search term '" + key + '\'');
        }
        builder.setSearchKey(key);
        return builder;
    }

    private static SearchTermBuilder createSentDateTermBuilder(final int searchTerm) {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                return new SentDateTerm(searchTerm, parseDate(getParameters()));
            }
        };
    }

    private static SearchTermBuilder createReceivedDateTermBuilder(final int searchTerm) {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                return new ReceivedDateTerm(searchTerm, parseDate(getParameters()));
            }
        };
    }

    private static SearchTermBuilder createMessageSizeTermBuilder(final int searchTerm) {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                return new SizeTerm(searchTerm, SearchTermBuilder.parseInteger(getParameters()));
            }
        };
    }

    private static Date parseDate(List<String> parameters) {
        DateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
        String date = parameters.get(0);
        try {
            Date d = df.parse(date);
            LOGGER.debug("Using date '{}'.", d);
            return d;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unable to parse date '" + date+"'",e);
        }
    }

    private static int parseInteger(List<String> parameters) {
        String integer = parameters.get(0);
        try {
            int i = Integer.parseInt(integer);
            LOGGER.debug("Using date '{}'.", i);
            return i;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unable to parse integer '" + integer + "'",e);
        }
    }

	private static SearchTermBuilder createORTermBuilder() {
			 return new SearchTermBuilder() {
	            @Override
	            public SearchTerm build() {
                return new OrTerm(new SubjectTerm(getParameters().get(0)),new SubjectTerm(getParameters().get(1)));
	            }
	        };	}
    
    private void setSearchKey(final SearchKey pKey) {
        key = pKey;
    }

    private static SearchTermBuilder createHeaderTermBuilder() {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                return new HeaderTerm(getParameters().get(0), getParameters().get(1));
            }
        };
    }

    SearchTermBuilder addParameter(final String pParameter) {
        if (Collections.<String>emptyList() == parameters) {
            parameters = new ArrayList<>();
        }
        parameters.add(pParameter);
        return this;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public String getParameter(final int pIdx) {
        return getParameters().get(pIdx);
    }

    public boolean expectsParameter() {
        return parameters.size() < key.getNumberOfParameters();
    }

    boolean isCharsetAware() {
        return key.isCharsetAware();
    }

    public abstract SearchTerm build();

    private static SearchTermBuilder createSearchTermBuilder(final SearchTerm pSearchTerm) {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                return pSearchTerm;
            }
        };
    }

    private static SearchTermBuilder createRecipientSearchTermBuilder(final Message.RecipientType type) {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                try {
                    return new RecipientTerm(type, new InternetAddress(getParameters().get(0)));
                } catch (AddressException e) {
                    throw new IllegalArgumentException("Address is not correct", e);
                }
            }
        };
    }

    private static SearchTermBuilder createBodySearchTermBuilder() {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                String query = getParameters().get(0);
                return new BodyTerm(query);
            }
        };
    }
    private static SearchTermBuilder createTextSearchTermBuilder() {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                String query = getParameters().get(0);
                SearchTerm[] terms = {
                  new RecipientStringTerm(Message.RecipientType.TO, query),
                  new RecipientStringTerm(Message.RecipientType.CC, query),
                  new RecipientStringTerm(Message.RecipientType.BCC, query),
                  new FromStringTerm(query),
                  new SubjectTerm(query),
                  new BodyTerm(query)
                };
                return new OrTerm(terms);
            }
        };
    }

    private static SearchTermBuilder createFromSearchTermBuilder() {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                try {
                    return new FromTerm(new InternetAddress(getParameters().get(0)));
                } catch (AddressException e) {
                    throw new IllegalArgumentException("Address is not correct", e);
                }
            }
        };
    }

    private static SearchTermBuilder createSubjectTermBuilder() {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                return new SubjectTerm(getParameters().get(0));
            }
        };
    }

    private static SearchTermBuilder createFlagSearchTermBuilder(final String pFlagName, final boolean pValue) {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                return createFlagSearchTerm(pFlagName, pValue);
            }
        };
    }

    private static SearchTermBuilder createKeywordSearchTermBuilder(final SearchKey pKey) {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                return createFlagSearchTerm(getParameter(0), pKey == SearchKey.KEYWORD);
            }
        };
    }

    private static SearchTerm createFlagSearchTerm(String pFlagName, boolean pValue) {
        Flags.Flag flag = toFlag(pFlagName);
        Flags flags = new javax.mail.Flags();
        if (null == flag) { // user flags
            flags.add(pFlagName);
        } else {
            flags.add(flag);
        }
        return new FlagTerm(flags, pValue);
    }

    private static SearchTermBuilder createUidTermBuilder() {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                final List<IdRange> uidSetList = IdRange.parseRangeSequence(getParameter(0));
                return new UidSearchTerm(uidSetList);
            }
        };
    }

    private static SearchTermBuilder createSequenceSetTermBuilder() {
        return new SearchTermBuilder() {
            @Override
            public SearchTerm build() {
                final List<IdRange> idRanges = IdRange.parseRangeSequence(getParameter(0));
                return new MessageNumberSearchTerm(idRanges);
            }
        };
    }

    private static javax.mail.Flags.Flag toFlag(String pFlag) {
        if (pFlag == null || pFlag.trim().length() < 1) {
            throw new IllegalArgumentException("Can not convert empty string to mail flag");
        }
        String flag = pFlag.trim().toUpperCase();
        if ("ANSWERED".equals(flag)) {
            return javax.mail.Flags.Flag.ANSWERED;
        }
        if ("DELETED".equals(flag)) {
            return javax.mail.Flags.Flag.DELETED;
        }
        if ("DRAFT".equals(flag)) {
            return javax.mail.Flags.Flag.DRAFT;
        }
        if ("FLAGGED".equals(flag)) {
            return javax.mail.Flags.Flag.FLAGGED;
        }
        if ("RECENT".equals(flag)) {
            return javax.mail.Flags.Flag.RECENT;
        }
        if ("SEEN".equals(flag)) {
            return javax.mail.Flags.Flag.SEEN;
        }
        return null;
    }

    @Override
    public String toString() {
        return "SearchTermBuilder{" +
                "key=" + key +
                ", parameters=" + parameters +
                '}';
    }

    /**
     * Search term that matches all messages
     */
    private static class AllSearchTerm extends SearchTerm {
        private static final long serialVersionUID = 135627179677024837L;

        @Override
        public boolean match(Message msg) {
            return true;
        }
    }

    /**
     * Supports general searching by id sequences such as MSN or UID.
     *
     * Note:
     * Not very efficient due to underlying JavaMail based impl.
     * The term compares each mail if matching.
     *
     * @see MessageNumberSearchTerm
     * @see UidSearchTerm
     */
    public abstract static class AbstractIdSearchTerm extends SearchTerm {
        private static final long serialVersionUID = -5935470270189992292L;
        private final List<IdRange> idRanges;

        public AbstractIdSearchTerm(final List<IdRange> idRanges) {
            this.idRanges = idRanges;
        }

        @Override
        public abstract boolean match(Message msg);

        /**
         * Matches id against sequence numbers.
         *
         * @param id the identifier
         * @return true, if matching
         */
        public boolean match(final long id) {
            for (IdRange idRange : idRanges) {
                if (idRange.includes(id)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Term for searching by message number ids.
     */
    public static class MessageNumberSearchTerm extends AbstractIdSearchTerm {
        private static final long serialVersionUID = -2792493451441320161L;

        /**
         * @param idRanges the MSNs to search for.
         */
        public MessageNumberSearchTerm(List<IdRange> idRanges) {
            super(idRanges);
        }

        @Override
        public boolean match(Message msg) {
            return match(msg.getMessageNumber());
        }
    }

    /**
     * Term for searching uids.
     */
    public static class UidSearchTerm extends AbstractIdSearchTerm {
        private static final long serialVersionUID = 1135219503729412087L;

        /**
         * @param idRanges the UIDs to search for.
         */
        public UidSearchTerm(List<IdRange> idRanges) {
            super(idRanges);
        }

        @Override
        public boolean match(Message msg) {
            if (msg instanceof StoredMessage.UidAwareMimeMessage) {
                long uid = ((StoredMessage.UidAwareMimeMessage) msg).getUid();
                return match(uid);
            } else {
                final Logger log = LoggerFactory.getLogger(UidSearchTerm.class);
                log.warn("No uid support for message {}, failing to match.", msg);
                return false;
            }
        }
    }

}
