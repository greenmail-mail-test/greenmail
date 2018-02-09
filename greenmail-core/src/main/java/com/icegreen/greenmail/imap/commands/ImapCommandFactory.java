/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import java.util.HashMap;
import java.util.Map;

/**
 * A factory for ImapCommand instances, provided based on the command name.
 * Command instances are created on demand, when first accessed.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public class ImapCommandFactory {
    private final Map<String, Class<? extends ImapCommand>> imapCommands = new HashMap<>();

    public ImapCommandFactory() {
        // Commands valid in any state
        // CAPABILITY, NOOP, and LOGOUT
        imapCommands.put(CapabilityCommand.NAME, CapabilityCommand.class);
        imapCommands.put(NoopCommand.NAME, NoopCommand.class);
        imapCommands.put(LogoutCommand.NAME, LogoutCommand.class);

        // Commands valid in NON_AUTHENTICATED state.
        // AUTHENTICATE and LOGIN
        imapCommands.put(AuthenticateCommand.NAME, AuthenticateCommand.class);
        imapCommands.put(LoginCommand.NAME, LoginCommand.class);

        // Commands valid in AUTHENTICATED or SELECTED state.
        // RFC2060: SELECT, EXAMINE, CREATE, DELETE, RENAME, SUBSCRIBE, UNSUBSCRIBE, LIST, LSUB, STATUS, and APPEND
        imapCommands.put(SelectCommand.NAME, SelectCommand.class);
        imapCommands.put(ExamineCommand.NAME, ExamineCommand.class);
        imapCommands.put(CreateCommand.NAME, CreateCommand.class);
        imapCommands.put(DeleteCommand.NAME, DeleteCommand.class);
        imapCommands.put(RenameCommand.NAME, RenameCommand.class);
        imapCommands.put(SubscribeCommand.NAME, SubscribeCommand.class);
        imapCommands.put(UnsubscribeCommand.NAME, UnsubscribeCommand.class);
        imapCommands.put(ListCommand.NAME, ListCommand.class);
        imapCommands.put(LsubCommand.NAME, LsubCommand.class);
        imapCommands.put(StatusCommand.NAME, StatusCommand.class);
        imapCommands.put(AppendCommand.NAME, AppendCommand.class);

//        // RFC2342 NAMESPACE
//        imapCommands.put( "NAMESPACE", NamespaceCommand.class );

        // RFC2086 GETACL, SETACL, DELETEACL, LISTRIGHTS, MYRIGHTS
//        imapCommands.put( "GETACL", GetAclCommand.class );
//        imapCommands.put( "SETACL", SetAclCommand.class );
//        imapCommands.put( "DELETEACL", DeleteAclCommand.class );
//        imapCommands.put( "LISTRIGHTS", ListRightsCommand.class );
//        imapCommands.put( "MYRIGHTS", MyRightsCommand.class );


        // Commands only valid in SELECTED state.
        // CHECK, CLOSE, EXPUNGE, SEARCH, FETCH, STORE, COPY, and UID
        imapCommands.put(CheckCommand.NAME, CheckCommand.class);
        imapCommands.put(CloseCommand.NAME, CloseCommand.class);
        imapCommands.put(ExpungeCommand.NAME, ExpungeCommand.class);
        imapCommands.put(CopyCommand.NAME, CopyCommand.class);
        imapCommands.put(SearchCommand.NAME, SearchCommand.class);
        imapCommands.put(FetchCommand.NAME, FetchCommand.class);
        imapCommands.put(StoreCommand.NAME, StoreCommand.class);
        imapCommands.put(UidCommand.NAME, UidCommand.class);
        imapCommands.put(SortCommand.NAME, SortCommand.class);

        // Quota support
        imapCommands.put(SetQuotaCommand.NAME, SetQuotaCommand.class);
        imapCommands.put(QuotaCommand.NAME, QuotaCommand.class);
        imapCommands.put(QuotaRootCommand.NAME, QuotaRootCommand.class);
    }

    public ImapCommand getCommand(String commandName) {
        Class<? extends ImapCommand> cmdClass = imapCommands.get(commandName.toUpperCase());

        if (cmdClass == null) {
            return null;
        } else {
            return createCommand(cmdClass);
        }
    }

    private ImapCommand createCommand(Class<? extends ImapCommand> commandClass) {
        try {
            ImapCommand cmd = commandClass.newInstance();

            if (cmd instanceof UidCommand) {
                ((UidCommand) cmd).setCommandFactory(this);
            }
            return cmd;
        } catch (Exception e) {
            throw new IllegalArgumentException("Can not create command instance " + commandClass.getName(), e);
        }
    }

}
