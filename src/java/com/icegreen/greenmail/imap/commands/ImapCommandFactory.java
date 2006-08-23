/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
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
    private Map _imapCommands;

    public ImapCommandFactory() {
        _imapCommands = new HashMap();

        // Commands valid in any state
        // CAPABILITY, NOOP, and LOGOUT
        _imapCommands.put(CapabilityCommand.NAME, CapabilityCommand.class);
        _imapCommands.put(NoopCommand.NAME, NoopCommand.class);
        _imapCommands.put(LogoutCommand.NAME, LogoutCommand.class);

        // Commands valid in NON_AUTHENTICATED state.
        // AUTHENTICATE and LOGIN
        _imapCommands.put(AuthenticateCommand.NAME, AuthenticateCommand.class);
        _imapCommands.put(LoginCommand.NAME, LoginCommand.class);

        // Commands valid in AUTHENTICATED or SELECTED state.
        // RFC2060: SELECT, EXAMINE, CREATE, DELETE, RENAME, SUBSCRIBE, UNSUBSCRIBE, LIST, LSUB, STATUS, and APPEND
        _imapCommands.put(SelectCommand.NAME, SelectCommand.class);
        _imapCommands.put(ExamineCommand.NAME, ExamineCommand.class);
        _imapCommands.put(CreateCommand.NAME, CreateCommand.class);
        _imapCommands.put(DeleteCommand.NAME, DeleteCommand.class);
        _imapCommands.put(RenameCommand.NAME, RenameCommand.class);
        _imapCommands.put(SubscribeCommand.NAME, SubscribeCommand.class);
        _imapCommands.put(UnsubscribeCommand.NAME, UnsubscribeCommand.class);
        _imapCommands.put(ListCommand.NAME, ListCommand.class);
        _imapCommands.put(LsubCommand.NAME, LsubCommand.class);
        _imapCommands.put(StatusCommand.NAME, StatusCommand.class);
        _imapCommands.put(AppendCommand.NAME, AppendCommand.class);

//        // RFC2342 NAMESPACE
//        _imapCommands.put( "NAMESPACE", NamespaceCommand.class );

        // RFC2086 GETACL, SETACL, DELETEACL, LISTRIGHTS, MYRIGHTS
//        _imapCommands.put( "GETACL", GetAclCommand.class );
//        _imapCommands.put( "SETACL", SetAclCommand.class );
//        _imapCommands.put( "DELETEACL", DeleteAclCommand.class );
//        _imapCommands.put( "LISTRIGHTS", ListRightsCommand.class );
//        _imapCommands.put( "MYRIGHTS", MyRightsCommand.class );


        // Commands only valid in SELECTED state.
        // CHECK, CLOSE, EXPUNGE, SEARCH, FETCH, STORE, COPY, and UID
        _imapCommands.put(CheckCommand.NAME, CheckCommand.class);
        _imapCommands.put(CloseCommand.NAME, CloseCommand.class);
        _imapCommands.put(ExpungeCommand.NAME, ExpungeCommand.class);
        _imapCommands.put(CopyCommand.NAME, CopyCommand.class);
        _imapCommands.put(SearchCommand.NAME, SearchCommand.class);
        _imapCommands.put(FetchCommand.NAME, FetchCommand.class);
        _imapCommands.put(StoreCommand.NAME, StoreCommand.class);
        _imapCommands.put(UidCommand.NAME, UidCommand.class);
    }

    public ImapCommand getCommand(String commandName) {
        Class cmdClass = (Class) _imapCommands.get(commandName.toUpperCase());

        if (cmdClass == null) {
            return null;
        } else {
            return createCommand(cmdClass);
        }
    }

    private ImapCommand createCommand(Class commandClass) {
        try {
            ImapCommand cmd = (ImapCommand) commandClass.newInstance();

            if (cmd instanceof UidCommand) {
                ((UidCommand) cmd).setCommandFactory(this);
            }
            return cmd;
        } catch (Exception e) {
            throw new RuntimeException("Could not create command instance: " + commandClass.getName(), e);
        }
    }

}
